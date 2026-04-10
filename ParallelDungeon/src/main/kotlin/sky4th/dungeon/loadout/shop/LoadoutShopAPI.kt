package sky4th.dungeon.loadout.shop

import sky4th.dungeon.loadout.LoadoutCategory
import sky4th.dungeon.loadout.LoadoutShopItemConfig
import sky4th.core.api.EconomyAPI
import sky4th.core.api.LanguageAPI
import sky4th.dungeon.command.DungeonContext
import sky4th.dungeon.config.ConfigManager
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionType
/**
 * 配装商店对外 API：统一入口打开商店 UI。
 * 当前由 /dungeon shop 调用，未来可被地图上的 NPC、区域触发等复用。
 */
object LoadoutShopAPI {

    private const val SHOP_SIZE = 54
    /** 内容区每页最多显示商品数（紧接着下一格为「即将上架」） */
    const val CONTENT_SLOTS = 14
    /** 内容区槽位：每行 5 格，按行顺序 [行1: 20-24, 行2: 29-33, 行3: 38-42]；商品按序填入后下一格为即将上架 */
    private val CONTENT_SLOT_INDICES = intArrayOf(
        20, 21, 22, 23, 24,
        29, 30, 31, 32, 33,
        38, 39, 40, 41, 42
    )
    /** 分类按钮在首行 slot 2,3,4,5,6（装备/近战/远程/补给/维修） */
    private val CATEGORY_SLOTS = intArrayOf(2, 3, 4, 5, 6)
    /** 商店右下角：箱子，左键打开仓库 */
    const val STORAGE_BUTTON_SLOT = 53
    /** 配装按钮槽位：最后一列的倒数第二个（第44槽位），点击打开配装界面 */
    //const val LOADOUT_BUTTON_SLOT = 44
    /** 右上角信用点 */
    private const val SLOT_CREDITS = 8

    /**
     * 为玩家打开配装商店界面。
     * @param player 玩家
     * @param category 默认选中的分类（默认装备）
     */
    @JvmStatic
    fun openShop(player: Player, category: LoadoutCategory = LoadoutCategory.EQUIPMENT) {
        val ctx = DungeonContext.get() ?: run {
            player.sendMessage("[ParallelDungeon] 商店上下文未初始化。")
            return
        }
        openShop(player, ctx.plugin, ctx.configManager,category)
    }

    /**
     * 内部/测试用：直接传入依赖打开商店。
     */
    @JvmStatic
    fun openShop(
        player: Player,
        plugin: JavaPlugin,
        configManager: ConfigManager,
        category: LoadoutCategory = LoadoutCategory.EQUIPMENT
    ) {
        val titleComponent = LanguageAPI.getText(plugin, "shop.title")
        val holder = LoadoutShopHolder(category, page = 0, selectedContentIndex = -1)
        val inv = Bukkit.getServer().createInventory(holder, SHOP_SIZE, MiniMessage.miniMessage().deserialize(titleComponent))
        holder.backingInv = inv

        fillDecoration(inv, plugin)
        setCreditDisplay(inv, player, configManager)
        fillCategoryRow(inv, plugin, category)
        fillContent(inv, configManager, category, holder.page, holder.selectedContentIndex, plugin)

        player.openInventory(inv)
    }

    /**
     * 刷新商店内容与底部按钮（选中/翻页后不关界面，只更新内容区与按钮）。
     */
    @JvmStatic
    fun refreshShopContent(inv: Inventory, configManager: ConfigManager, plugin: JavaPlugin) {
        val holder = inv.holder as? LoadoutShopHolder ?: return
        fillContent(inv, configManager, holder.category, holder.page, holder.selectedContentIndex, plugin)
        // 更新信用点头颅的显示
        val player = inv.viewers.firstOrNull() as? Player
        if (player != null) {
            updateCreditDisplay(inv, player, configManager)
        }
    }

    private fun fillDecoration(inv: Inventory, plugin: JavaPlugin) {
        val space = Component.empty()
        val black = ItemStack(Material.BLACK_STAINED_GLASS_PANE).apply {
            editMeta { it.displayName(space) }
        }
        val gray = ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
            editMeta { it.displayName(space) }
        }
        // 首行两侧 0,1 和 7（中间 2-6 为 5 个分类按钮，8 为信用点纸在 openShop 里设置）
        for (i in intArrayOf(0, 1, 7)) inv.setItem(i, black)
        // 第 2 行整行边框 (9-17)
        for (i in 9..17) inv.setItem(i, gray)
        // 第 3 行：玻璃 18,19 | 内容 20-24 | 玻璃 25,26
        for (i in intArrayOf(18, 19, 25, 26)) inv.setItem(i, gray)
        for (i in 20..24) inv.setItem(i, null)
        // 第 4 行：玻璃 27,28 | 内容 29-33 | 玻璃 34,35
        inv.setItem(27, gray)
        inv.setItem(28, gray)
        for (i in 29..33) inv.setItem(i, null)
        inv.setItem(34, gray)
        inv.setItem(35, gray)
        // 第 5 行：玻璃 36,37 | 内容 38-42 | 玻璃 43-44, (配装按钮 44)
        inv.setItem(36, gray)
        inv.setItem(37, gray)
        for (i in 38..42) inv.setItem(i, null)
        inv.setItem(43, gray)
        inv.setItem(44, gray)
        // 第 6 行：45-52 装饰，53 为仓库按钮（箱子，左键打开仓库）
        for (i in 45..52) inv.setItem(i, gray)
        inv.setItem(STORAGE_BUTTON_SLOT, ItemStack(Material.CHEST).apply {
            editMeta { meta ->
                meta.displayName(LanguageAPI.getComponent(plugin, "shop.button.storage"))
            }
        })
    }

    private fun setCreditDisplay(inv: Inventory, player: Player, configManager: ConfigManager) {
        val balance = if (EconomyAPI.isAvailable()) EconomyAPI.format(EconomyAPI.getBalance(player)) else "—"
        inv.setItem(SLOT_CREDITS, sky4th.dungeon.head.CreditsHead.createCreditsHead(
            balance,
            configManager
        ))
    }

    /**
     * 更新商店界面右上角的信用点显示
     * @param inv 商店界面
     * @param player 玩家
     * @param configManager 配置管理器
     */
    @JvmStatic
    fun updateCreditDisplay(inv: Inventory, player: Player, configManager: ConfigManager) {
        val balance = if (EconomyAPI.isAvailable()) EconomyAPI.format(EconomyAPI.getBalance(player)) else "—"
        inv.setItem(SLOT_CREDITS, sky4th.dungeon.head.CreditsHead.createCreditsHead(
            balance,
            configManager
        ))
    }

    private fun fillCategoryRow(inv: Inventory, plugin: JavaPlugin, currentCategory: LoadoutCategory) {
        LoadoutCategory.entries.forEachIndexed { index, cat ->
            if (index >= CATEGORY_SLOTS.size) return@forEachIndexed
            val display = LanguageAPI.getText(plugin, cat.displayKey)
            val icon = when (cat) {
                LoadoutCategory.EQUIPMENT -> Material.LEATHER_HELMET
                LoadoutCategory.MELEE -> Material.IRON_SWORD
                LoadoutCategory.RANGED -> Material.BOW
                LoadoutCategory.SUPPLIES -> Material.POTION
                LoadoutCategory.REPAIR -> Material.ANVIL
            }
            val isSelected = cat == currentCategory
            val item = ItemStack(icon)
            item.editMeta { meta ->
                meta.displayName(LanguageAPI.getComponent(plugin, if (isSelected) LanguageAPI.getText(plugin, "shop.category.selected", "name" to display) else "§7$display"))
                meta.lore(if (isSelected) {
                    listOf(
                        LanguageAPI.getText(plugin, "shop.category.current"),
                        LanguageAPI.getText(plugin, "shop.category.switch")
                    ).map { MiniMessage.miniMessage().deserialize(it) }
                } else {
                    listOf(LanguageAPI.getText(plugin, "shop.category.switch")).map { MiniMessage.miniMessage().deserialize(it) }
                })
                if (isSelected) {
                    addEnchantGlow(meta)
                }
            }
            inv.setItem(CATEGORY_SLOTS[index], item)
        }
    }

    /** 为物品添加附魔发光效果（隐藏附魔文字，仅保留光效） */
    private fun addEnchantGlow(meta: ItemMeta) {
        addEnchantGlowPublic(meta)
    }

    @JvmStatic
    fun addEnchantGlowPublic(meta: ItemMeta) {
        val enchant = Enchantment.UNBREAKING ?: return
        meta.addEnchant(enchant, 1, true)
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
    }

    private fun fillContent(
        inv: Inventory,
        configManager: ConfigManager,
        category: LoadoutCategory,
        page: Int,
        selectedContentIndex: Int,
        plugin: JavaPlugin
    ) {
        val allItems = configManager.getLoadoutShopItems(category)
        val items = allItems.drop(page * CONTENT_SLOTS).take(CONTENT_SLOTS)
        val shopIdKey = NamespacedKey(plugin, "loadout_shop_id")
        val comingSoonPaper = ItemStack(Material.PAPER).apply {
            editMeta { it.displayName(LanguageAPI.getComponent(plugin, "shop.coming-soon")) }
        }
        for (i in CONTENT_SLOT_INDICES.indices) {
            val slot = CONTENT_SLOT_INDICES[i]
            when {
                i < items.size -> {
                    val config = items[i]
                    val withGlow = (i == selectedContentIndex)
                    inv.setItem(slot, createShopDisplayItem(plugin, configManager, config, shopIdKey, withGlow = withGlow))
                }
                i == items.size -> inv.setItem(slot, comingSoonPaper)
                else -> inv.setItem(slot, null)
            }
        }
    }

    /** 该 slot 是否为内容区商品格（可点击购买） */
    @JvmStatic
    fun isContentSlot(slot: Int): Boolean = slot in CONTENT_SLOT_INDICES

    /** 内容区 slot 对应的商品下标（0..14），非内容区返回 null */
    @JvmStatic
    fun getContentIndex(slot: Int): Int? {
        val idx = CONTENT_SLOT_INDICES.indexOfFirst { it == slot }
        return if (idx >= 0) idx else null
    }

    /** 判断 slot 是否为分类按钮（用于 Listener） */
    @JvmStatic
    fun isCategorySlot(slot: Int): Boolean = slot in CATEGORY_SLOTS

    /** 判断 slot 是否为仓库按钮（右下角箱子，左键打开仓库） */
    @JvmStatic
    fun isStorageButtonSlot(slot: Int): Boolean = slot == STORAGE_BUTTON_SLOT

    /** 判断 slot 是否为配装按钮（右下角倒数第二格，点击打开配装界面） */
    @JvmStatic
    fun isLoadoutButtonSlot(slot: Int) = false //slot == LOADOUT_BUTTON_SLOT

    /**
     * 根据配置创建商店展示用物品（带价格 lore、PDC 标记 id）
     * @param withGlow 是否添加附魔发光效果（当前分类下的商品展示用）
     */
    fun createShopDisplayItem(
        plugin: JavaPlugin,
        configManager: ConfigManager,
        config: LoadoutShopItemConfig,
        idKey: NamespacedKey,
        withGlow: Boolean = false
    ): ItemStack {
        val material = Material.matchMaterial(config.material.uppercase()) ?: Material.PAPER
        val baseItem = ItemStack(material)
        baseItem.editMeta { meta ->
            val name = config.name
            meta.displayName(MiniMessage.miniMessage().deserialize(name))
            val loreLines = mutableListOf<net.kyori.adventure.text.Component>()
            if (config.shopLore.isNotEmpty()) {
                loreLines.addAll(config.shopLore.map { MiniMessage.miniMessage().deserialize(it) })
            }
            loreLines.add(MiniMessage.miniMessage().deserialize(""))
            loreLines.add(LanguageAPI.getComponent(plugin, "shop.item.price", "price" to config.buyPrice))
            loreLines.add(LanguageAPI.getComponent(plugin, "shop.item.click-hint"))
            meta.lore(loreLines)
            meta.persistentDataContainer.set(idKey, PersistentDataType.STRING, config.id)
            if (withGlow) addEnchantGlow(meta)
        }
        // 药水箭（TIPPED_ARROW）需设置 basePotionType，图标才显示对应效果（与仓库/配装一致）
        if (material == Material.TIPPED_ARROW) {
            val actual = configManager.getActualEquipmentConfigList(config.id)?.firstOrNull()
            actual?.basePotionType?.let { typeName ->
                try {
                    val potionType = PotionType.valueOf(typeName.uppercase())
                    baseItem.editMeta { meta ->
                        (meta as? PotionMeta)?.setBasePotionType(potionType)
                    }
                } catch (_: IllegalArgumentException) { }
            }
        }
        return baseItem
    }

    /**
     * 根据配置创建可给予玩家的物品（无"点击购买"等商店用 lore，保留 id 便于后续配装逻辑）
     */
    fun createPurchasedItem(plugin: JavaPlugin, config: LoadoutShopItemConfig): ItemStack {
        val material = Material.matchMaterial(config.material.uppercase()) ?: Material.PAPER
        val item = ItemStack(material)
        val setKey = NamespacedKey(plugin, "loadout_set")
        val idKey = NamespacedKey(plugin, "loadout_shop_id")
        val tierKey = NamespacedKey(plugin, "loadout_tier")  // 添加品级键
        item.editMeta { meta ->
            val name = config.name
            meta.displayName(MiniMessage.miniMessage().deserialize(name))
            // 购买后的物品使用 normalLore（用于配装、仓库等常规界面）
            val loreLines = mutableListOf<net.kyori.adventure.text.Component>()
            if (config.normalLore.isNotEmpty()) {
                loreLines.addAll(config.normalLore.map { MiniMessage.miniMessage().deserialize(it) })
            }
            // 添加价值显示
            if (config.sellPrice > 0) {
                loreLines.add(MiniMessage.miniMessage().deserialize(""))
                loreLines.add(MiniMessage.miniMessage().deserialize(LanguageAPI.getText(plugin, "price-value", "value" to config.sellPrice)))
                meta.persistentDataContainer.set(NamespacedKey(plugin, "loadout_price"), PersistentDataType.INTEGER, config.sellPrice)
            }
            // 添加品级标记
            if (config.tier.isNotBlank()) {
                meta.persistentDataContainer.set(tierKey, PersistentDataType.STRING, config.tier)
            }
            if (loreLines.isNotEmpty()) {
                meta.lore(loreLines)
            }
            meta.persistentDataContainer.set(setKey, PersistentDataType.STRING, config.id)
            meta.persistentDataContainer.set(idKey, PersistentDataType.STRING, config.id)
        }
        return item
    }

    /** 判断该 Inventory 是否为配装商店界面 */
    @JvmStatic
    fun isLoadoutShop(holder: Any?): Boolean = holder is LoadoutShopHolder
}
