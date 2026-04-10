package sky4th.dungeon.loadout.category

import sky4th.dungeon.Dungeon
import sky4th.dungeon.loadout.screen.LoadoutScreenHolder
import sky4th.dungeon.loadout.screen.LoadoutUI
import sky4th.dungeon.loadout.shop.LoadoutShopAPI
import sky4th.dungeon.loadout.LoadoutCategory
import sky4th.dungeon.loadout.LoadoutShopItemConfig
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionType
import sky4th.core.api.EconomyAPI
import sky4th.core.api.LanguageAPI
import sky4th.dungeon.config.ConfigManager
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.Component
import java.util.*

/**
 * 分类详情界面：点开某分类后展示该分类下所有商品及仓库数量。
 * 首次点击选中某商品，再次点击：0 件则直接购买，1 件则直接选中，n 件则跳转仓库选择界面。
 */
object CategoryDetailUI {
    
    private const val SIZE = 54
    /** 左侧目录：装备/近战/远程/补给（与配装界面一致，当前分类附魔高亮） */
    private val SLOT_CATEGORY_EQUIPMENT = 9
    private val SLOT_CATEGORY_MELEE = 18
    private val SLOT_CATEGORY_RANGED = 27
    private val SLOT_CATEGORY_SUPPLIES = 36
    private val CATEGORY_SLOTS = listOf(
        LoadoutCategory.EQUIPMENT to SLOT_CATEGORY_EQUIPMENT,
        LoadoutCategory.MELEE to SLOT_CATEGORY_MELEE,
        LoadoutCategory.RANGED to SLOT_CATEGORY_RANGED,
        LoadoutCategory.SUPPLIES to SLOT_CATEGORY_SUPPLIES
    )
    /** 内容区：每行 5 格，3 行 (slot 20-24, 29-33, 38-42)，与商店一致 */
    private val CONTENT_SLOT_INDICES = intArrayOf(
        20, 21, 22, 23, 24,
        29, 30, 31, 32, 33,
        38, 39, 40, 41, 42
    )
    const val CONTENT_SLOTS = 15
    const val SLOT_BACK = 53
    /** 翻页按钮槽位（内容区右侧） */
    private const val SLOT_PREV_PAGE = 25
    private const val SLOT_NEXT_PAGE = 26
    /** 右上角信用点 */
    private const val SLOT_CREDITS = 8

    @JvmStatic
    fun open(
        player: Player,
        loadoutHolder: LoadoutScreenHolder,
        category: LoadoutCategory,
        plugin: JavaPlugin,
        configManager: ConfigManager
    ) {
        val items = when (category) {
            LoadoutCategory.SUPPLIES -> {
                val ids = configManager.getEquippableSupplyIds().toSet()
                val suppliesItems = configManager.getLoadoutShopItems(LoadoutCategory.SUPPLIES).filter { it.id in ids }
                val repairItems = configManager.getLoadoutShopItems(LoadoutCategory.REPAIR)
                suppliesItems + repairItems
            }
            else -> configManager.getLoadoutShopItems(category)
        }
        val holder = CategoryDetailHolder(player.uniqueId, loadoutHolder, category, -1)
        holder.categoryItems = items
        val categoryName = LanguageAPI.getText(plugin, category.displayKey)
        val title = LanguageAPI.getText(plugin, "loadout.screen.category-title", "category" to categoryName)
        val inv = Bukkit.createInventory(holder, SIZE, MiniMessage.miniMessage().deserialize(title))
        holder.backingInv = inv

        fillDecoration(inv, configManager)
        fillCategoryRow(inv, holder, configManager)
        fillContent(inv, player.uniqueId, holder, configManager, plugin)
        setCreditDisplay(inv, player, configManager)

        player.openInventory(inv)
    }

    private fun fillDecoration(inv: Inventory, configManager: ConfigManager) {
        val gray = ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
            editMeta { it.displayName(Component.empty()) }
        }
        val categorySlotSet = CATEGORY_SLOTS.map { it.second }.toSet()
        val contentSlotSet = CONTENT_SLOT_INDICES.toSet()
        for (i in 0 until SIZE) {
            if (i in contentSlotSet || i == SLOT_PREV_PAGE || i == SLOT_NEXT_PAGE) continue
            if (i in categorySlotSet) continue
            if (i == SLOT_BACK) {
                inv.setItem(i, sky4th.dungeon.head.EquipmentHead.createBackToEquipmentHead(configManager))
            } else if (i == SLOT_CREDITS) {
                // 在 open / setCreditDisplay 里设置
            } else {
                inv.setItem(i, gray.clone())
            }
        }
    }

    /** 左侧目录：四个分类，当前选中的附魔高亮 */
    private fun fillCategoryRow(inv: Inventory, holder: CategoryDetailHolder, configManager: ConfigManager) {
        val plugin = Dungeon.instance
        val current = holder.category
        for ((category, slot) in CATEGORY_SLOTS) {
            val icon = when (category) {
                LoadoutCategory.EQUIPMENT -> Material.LEATHER_HELMET
                LoadoutCategory.MELEE -> Material.IRON_SWORD
                LoadoutCategory.RANGED -> Material.BOW
                LoadoutCategory.SUPPLIES -> Material.POTION
                LoadoutCategory.REPAIR -> Material.ANVIL
            }
            val name = LanguageAPI.getText(plugin, category.displayKey)
            val isCurrent = category == current
            val item = ItemStack(icon)
            item.editMeta { meta ->
                meta.displayName(MiniMessage.miniMessage().deserialize(if (isCurrent) "<green><bold><underline>$name" else "<gray>$name"))
                meta.lore(listOf(
                    if (isCurrent) LanguageAPI.getText(plugin, "loadout.screen.category-current") else LanguageAPI.getText(plugin, "loadout.screen.category-switch")
                ).map { MiniMessage.miniMessage().deserialize("<dark_gray>$it") })
                if (isCurrent) LoadoutShopAPI.addEnchantGlowPublic(meta)
            }
            inv.setItem(slot, item)
        }
    }

    private fun setCreditDisplay(inv: Inventory, player: Player, configManager: ConfigManager) {
        val balance = if (EconomyAPI.isAvailable()) EconomyAPI.format(EconomyAPI.getBalance(player)) else "—"
        inv.setItem(SLOT_CREDITS, sky4th.dungeon.head.CreditsHead.createCreditsHead(
            balance,
            configManager
        ))
    }

    private fun fillContent(
        inv: Inventory,
        uuid: UUID,
        holder: CategoryDetailHolder,
        configManager: ConfigManager,
        plugin: JavaPlugin
    ) {
        val page = holder.page
        val pageItems = holder.categoryItems.drop(page * CONTENT_SLOTS).take(CONTENT_SLOTS)
        val selected = holder.selectedItemIndex
        val gray = ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
            editMeta { it.displayName(Component.empty()) }
        }
        for (i in CONTENT_SLOT_INDICES.indices) {
            val slot = CONTENT_SLOT_INDICES[i]
            if (i >= pageItems.size) {
                inv.setItem(slot, null)
                continue
            }
            val config = pageItems[i]
            val count = if (holder.category == LoadoutCategory.SUPPLIES) LoadoutUI.getStorageTotalCountByLoadoutId(uuid, config.id) else LoadoutUI.getStorageCountByLoadoutId(uuid, config.id)
            val stack = LoadoutShopAPI.createPurchasedItem(plugin, config)
            val isSupplies = holder.category == LoadoutCategory.SUPPLIES
            val equippedCount = if (isSupplies) holder.loadoutHolder.selectedSupplies.filter { it.loadoutId == config.id }.sumOf { it.count } else 0
            stack.editMeta { meta ->
                val lore = config.normalLore.toMutableList()
                lore.add("")
                lore.add(LanguageAPI.getText(plugin, "price-value", "value" to config.buyPrice))
                lore.add(LanguageAPI.getText(plugin, "loadout.screen.storage-count", "count" to count))
                if (isSupplies) {
                    lore.add(LanguageAPI.getText(plugin, "loadout.screen.supplies-equipped", "count" to equippedCount))
                    lore.add(LanguageAPI.getText(plugin, "loadout.screen.supplies-click-hint"))
                } else {
                    lore.add(LanguageAPI.getText(plugin, "loadout.screen.click-select-then"))
                }
                meta.lore(lore.map { MiniMessage.miniMessage().deserialize(it) })
                if (i == selected) {
                    LoadoutShopAPI.addEnchantGlowPublic(meta)
                }
            }
            // 药水箭需设置 basePotionType 才能显示对应效果图标（与商店一致）
            if (stack.type == Material.TIPPED_ARROW) {
                configManager.getActualEquipmentConfigList(config.id)?.firstOrNull()?.basePotionType?.let { typeName ->
                    try {
                        val potionType = PotionType.valueOf(typeName.uppercase())
                        stack.editMeta { (it as? PotionMeta)?.setBasePotionType(potionType) }
                    } catch (_: IllegalArgumentException) { }
                }
            }
            inv.setItem(slot, stack)
        }
        // 翻页按钮
        inv.setItem(SLOT_PREV_PAGE, if (page > 0) {
            ItemStack(Material.ARROW).apply {
                editMeta { meta ->
                    meta.displayName(MiniMessage.miniMessage().deserialize("<gray><bold>« 上一页"))
                }
            }
        } else gray.clone())
        inv.setItem(SLOT_NEXT_PAGE, if ((page + 1) * CONTENT_SLOTS < holder.categoryItems.size) {
            ItemStack(Material.ARROW).apply {
                editMeta { meta ->
                    meta.displayName(MiniMessage.miniMessage().deserialize("<gray><bold>下一页 »"))
                }
            }
        } else gray.clone())
    }

    @JvmStatic
    fun refreshContent(inv: Inventory, configManager: ConfigManager, plugin: JavaPlugin) {
        val holder = inv.holder as? CategoryDetailHolder ?: return
        fillContent(inv, holder.playerId, holder, configManager, plugin)
        // 更新信用点头颅的显示
        val player = Bukkit.getPlayer(holder.playerId)
        if (player != null) {
            setCreditDisplay(inv, player, configManager)
        }
    }

    /** 刷新左侧目录 + 内容区（切换分类时调用） */
    @JvmStatic
    fun refreshAll(inv: Inventory, configManager: ConfigManager, plugin: JavaPlugin) {
        val holder = inv.holder as? CategoryDetailHolder ?: return
        fillCategoryRow(inv, holder, configManager)
        fillContent(inv, holder.playerId, holder, configManager, plugin)
    }

    @JvmStatic
    fun isCategoryDetailGui(holder: Any?): Boolean = holder is CategoryDetailHolder

    /** 左侧目录槽位对应的分类，非目录槽返回 null */
    @JvmStatic
    fun getCategorySlot(slot: Int): LoadoutCategory? = when (slot) {
        SLOT_CATEGORY_EQUIPMENT -> LoadoutCategory.EQUIPMENT
        SLOT_CATEGORY_MELEE -> LoadoutCategory.MELEE
        SLOT_CATEGORY_RANGED -> LoadoutCategory.RANGED
        SLOT_CATEGORY_SUPPLIES -> LoadoutCategory.SUPPLIES
        else -> null
    }

    @JvmStatic
    fun getContentSlotIndex(slot: Int): Int? {
        val idx = CONTENT_SLOT_INDICES.indexOf(slot)
        return if (idx >= 0) idx else null
    }

    @JvmStatic
    fun isContentSlot(slot: Int): Boolean = slot in CONTENT_SLOT_INDICES

    @JvmStatic
    fun isPrevPageSlot(slot: Int): Boolean = slot == SLOT_PREV_PAGE

    @JvmStatic
    fun isNextPageSlot(slot: Int): Boolean = slot == SLOT_NEXT_PAGE
}
