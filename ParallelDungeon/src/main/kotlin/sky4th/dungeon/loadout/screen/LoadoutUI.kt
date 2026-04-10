package sky4th.dungeon.loadout.screen

import sky4th.dungeon.loadout.LoadoutCategory
import sky4th.dungeon.loadout.storage.LootItemHelper
import sky4th.dungeon.loadout.shop.LoadoutShopAPI
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.plugin.java.JavaPlugin
import sky4th.core.api.EconomyAPI
import sky4th.core.api.StorageAPI
import sky4th.core.api.LanguageAPI
import sky4th.core.model.StorageEntry
import sky4th.core.model.StorageEntryType
import sky4th.dungeon.command.DungeonContext
import sky4th.dungeon.config.ConfigManager
import java.util.*

/**
 * 配装界面：进入地牢前选择最多 1 装备、1 近战、1 远程，以及若干补给（有上限）。
 * 点击某类时：若仓库有该类物品则打开“从仓库选择”界面；否则提示需从商店购买。
 */
object LoadoutUI {

    private const val SIZE = 54
    /** 配装界面：左边第一列中间 4 格为四个分类（装备/近战/远程/补给） */
    private val SLOT_CATEGORY_EQUIPMENT = 9
    private val SLOT_CATEGORY_MELEE = 18
    private val SLOT_CATEGORY_RANGED = 27
    private val SLOT_CATEGORY_SUPPLIES = 36
    /** 选择箭矢槽位（仅当已选远程武器时可用） */
    const val SLOT_ARROWS = 28
    /** 补给分类图标（瓶子）右侧：已装备补给逐一展示，堆叠展示数量（槽位 37-42） */
    private val SLOT_SUPPLIES_DISPLAY = intArrayOf(37, 38, 39, 40, 41, 42)
    /** 进入地牢按钮 */
    const val SLOT_ENTER_DUNGEON = 49
    /** 商店按钮槽位：最后一列的倒数第二个（第44槽位） */
    const val SLOT_SHOP = 44
    /** 仓库按钮槽位：右下角（第53槽位） */
    const val SLOT_STORAGE = 53
    /** 右上角信用点展示*/
    private const val SLOT_CREDITS = 8

    /** 四个分类槽位（左列中间） */
    private val LOADOUT_CATEGORY_SLOTS = listOf(
        LoadoutCategory.EQUIPMENT to SLOT_CATEGORY_EQUIPMENT,
        LoadoutCategory.MELEE to SLOT_CATEGORY_MELEE,
        LoadoutCategory.RANGED to SLOT_CATEGORY_RANGED,
        LoadoutCategory.SUPPLIES to SLOT_CATEGORY_SUPPLIES
    )

    /** 获取仓库中属于某分类的 LOADOUT 条目（下标 + 条目） */
    fun getStorageEntriesByCategory(
        uuid: UUID,
        category: LoadoutCategory,
        configManager: ConfigManager
    ): List<Pair<Int, StorageEntry>> {
        val entries = StorageAPI.getStorage(uuid, StorageAPI.STORAGE_SLOT_COUNT)
        return entries.mapIndexedNotNull { index, entry ->
            if (entry == null || entry.type != StorageEntryType.LOADOUT) return@mapIndexedNotNull null
            val config = configManager.getLoadoutShopItemById(entry.loadoutId ?: return@mapIndexedNotNull null)
                ?: return@mapIndexedNotNull null
            if (config.category != category) return@mapIndexedNotNull null
            index to entry
        }
    }

    /** 某 loadoutId 在仓库中的槽位数 */
    @JvmStatic
    fun getStorageCountByLoadoutId(uuid: UUID, loadoutId: String): Int {
        val entries = StorageAPI.getStorage(uuid, StorageAPI.STORAGE_SLOT_COUNT)
        return entries.count { it?.type == StorageEntryType.LOADOUT && it.loadoutId == loadoutId }
    }

    /** 某 loadoutId 在仓库中的总件数（各槽位 count 之和） */
    @JvmStatic
    fun getStorageTotalCountByLoadoutId(uuid: UUID, loadoutId: String): Int {
        val entries = StorageAPI.getStorage(uuid, StorageAPI.STORAGE_SLOT_COUNT)
        return entries.filter { it?.type == StorageEntryType.LOADOUT && it.loadoutId == loadoutId }.sumOf { it!!.count }
    }

    /** 可装配补给品在仓库中的总件数（用于装配页补给占位显示） */
    @JvmStatic
    fun getStorageCountEquippableSupplies(uuid: UUID, configManager: ConfigManager): Int {
        val supplyIds = configManager.getEquippableSupplyIds().toSet()
        val repairIds = configManager.getLoadoutShopItems(LoadoutCategory.REPAIR).map { it.id }.toSet()
        val entries = StorageAPI.getStorage(uuid, StorageAPI.STORAGE_SLOT_COUNT)
        return entries.filter { 
            it?.type == StorageEntryType.LOADOUT && it.loadoutId != null && 
            (it.loadoutId in supplyIds || it.loadoutId in repairIds) 
        }.sumOf { it!!.count }
    }

    /** 仓库中某 loadoutId 的所有槽位（下标 + 条目） */
    @JvmStatic
    fun getStorageEntriesByLoadoutId(uuid: UUID, loadoutId: String): List<Pair<Int, StorageEntry>> {
        val entries = StorageAPI.getStorage(uuid, StorageAPI.STORAGE_SLOT_COUNT)
        return entries.mapIndexedNotNull { index, entry ->
            if (entry == null || entry.type != StorageEntryType.LOADOUT || entry.loadoutId != loadoutId) return@mapIndexedNotNull null
            index to entry
        }
    }

    /** 特种弓可装备的 5 种特殊箭矢 loadoutId */
    @JvmStatic
    val SPECIAL_BOW_ARROW_IDS: List<String> = listOf("guangling_jian", "huanman_jian", "zhongdu_jian", "zhiliao_jian", "xunjie_jian")

    /**
     * 仓库中可作箭矢的条目。
     * @param rangedLoadoutId explosive_crossbow 只返回爆炸箭；special_bow 返回普通箭+5种特殊箭；否则只返回普通箭
     */
    @JvmStatic
    fun getStorageEntriesArrows(uuid: UUID, rangedLoadoutId: String? = null): List<Pair<Int, StorageEntry>> {
        val entries = StorageAPI.getStorage(uuid, StorageAPI.STORAGE_SLOT_COUNT)
        return entries.mapIndexedNotNull { index, entry ->
            if (entry == null) return@mapIndexedNotNull null
            when {
                rangedLoadoutId == "explosive_crossbow" ->
                    if (entry.type == StorageEntryType.LOADOUT && entry.loadoutId == "baozha_jian") index to entry else null
                rangedLoadoutId == "special_bow" -> when {
                    entry.type == StorageEntryType.LOADOUT && entry.loadoutId == "normal_arrow" -> index to entry
                    entry.type == StorageEntryType.LOADOUT && entry.loadoutId != null && entry.loadoutId in SPECIAL_BOW_ARROW_IDS -> index to entry
                    else -> null
                }
                else -> when {
                    entry.type == StorageEntryType.LOADOUT && entry.loadoutId == "normal_arrow" -> index to entry
                    else -> null
                }
            }
        }
    }

    /** 当前远程武器是否使用爆炸箭（仅爆破弩） */
    @JvmStatic
    fun isExplosiveArrowRanged(rangedLoadoutId: String?): Boolean = rangedLoadoutId == "explosive_crossbow"

    @JvmStatic
    fun open(player: Player, dungeonName: String, instanceId: String? = null) {
        val ctx = DungeonContext.get() ?: run {
            player.sendMessage("[ParallelDungeon] 上下文未初始化。")
            return
        }
        open(player, dungeonName, instanceId, ctx.plugin, ctx.configManager)
    }

    @JvmStatic
    fun open(
        player: Player,
        dungeonName: String,
        instanceId: String?,
        plugin: JavaPlugin,
        configManager: ConfigManager
    ) {
        val holder = LoadoutScreenHolder(player.uniqueId, dungeonName, instanceId)
        openWithHolder(player, holder, plugin, configManager)
    }

    /** 使用已有 holder 打开（从仓库选择/分类详情返回时保留选中状态） */
    @JvmStatic
    fun openWithHolder(
        player: Player,
        loadoutHolder: LoadoutScreenHolder,
        plugin: JavaPlugin,
        configManager: ConfigManager
    ) {
        val title = LanguageAPI.getComponent(plugin, "loadout.screen.title")
        val inv = Bukkit.createInventory(loadoutHolder, SIZE, title)
        loadoutHolder.backingInv = inv

        fillDecoration(plugin, inv, configManager)
        fillLoadoutSlots(inv, loadoutHolder.playerId, loadoutHolder, configManager, plugin)
        setCreditDisplay(inv, player, configManager)

        player.openInventory(inv)
    }

    private fun fillDecoration(plugin: JavaPlugin,inv: Inventory, configManager: ConfigManager) {
        val gray = ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
            editMeta { it.displayName(MiniMessage.miniMessage().deserialize(" ")) }
        }
        for (i in 0 until SIZE) {
            if (i in LOADOUT_CATEGORY_SLOTS.map { it.second }) continue
            if (i == SLOT_ENTER_DUNGEON || i == SLOT_CREDITS || i == SLOT_ARROWS || i == SLOT_SHOP || i == SLOT_STORAGE) continue
            inv.setItem(i, gray.clone())
        }
        inv.setItem(SLOT_ENTER_DUNGEON, ItemStack(Material.END_PORTAL_FRAME).apply {
            editMeta { meta ->
                meta.displayName(LanguageAPI.getComponent(plugin, "loadout.screen.enter-dungeon"))
            }
        })
        inv.setItem(SLOT_SHOP, sky4th.dungeon.head.ShopHead.createOpenShopHead(configManager))
        inv.setItem(SLOT_STORAGE, ItemStack(Material.CHEST).apply {
            editMeta { meta ->
                meta.displayName(LanguageAPI.getComponent(plugin, "loadout.screen.open-storage"))
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

    private fun fillLoadoutSlots(
        inv: Inventory,
        uuid: UUID,
        holder: LoadoutScreenHolder,
        configManager: ConfigManager,
        plugin: JavaPlugin
    ) {
        // 从仓库获取完整数据，用于显示正确的耐久度信息
        val storageEntries = StorageAPI.getStorage(uuid, StorageAPI.STORAGE_SLOT_COUNT)

        for ((category, slot) in LOADOUT_CATEGORY_SLOTS) {
            val ownedCount = when (category) {
                LoadoutCategory.SUPPLIES -> getStorageCountEquippableSupplies(uuid, configManager)
                else -> getStorageEntriesByCategory(uuid, category, configManager).size
            }
            // 获取选中的entry，如果提供了storageIndex，则从仓库获取完整信息
            val selected = when (category) {
                LoadoutCategory.EQUIPMENT -> {
                    val index = holder.selectedEquipmentStorageIndex
                    if (index != null && index >= 0 && index < storageEntries.size) {
                        storageEntries[index]
                    } else {
                        holder.selectedEquipment
                    }
                }
                LoadoutCategory.MELEE -> {
                    val index = holder.selectedMeleeStorageIndex
                    if (index != null && index >= 0 && index < storageEntries.size) {
                        storageEntries[index]
                    } else {
                        holder.selectedMelee
                    }
                }
                LoadoutCategory.RANGED -> {
                    val index = holder.selectedRangedStorageIndex
                    if (index != null && index >= 0 && index < storageEntries.size) {
                        storageEntries[index]
                    } else {
                        holder.selectedRanged
                    }
                }
                LoadoutCategory.SUPPLIES -> null
                LoadoutCategory.REPAIR -> null
            }
            val display = when (category) {
                LoadoutCategory.EQUIPMENT -> {
                    if (selected != null) entryToDisplayItem(selected, plugin, configManager, "loadout.screen.selected", "name" to (configManager.getLoadoutShopItemById(selected.loadoutId ?: "")?.name?.replace("&", "§") ?: "?"))
                    else makeCategoryPlaceholder(plugin, category, ownedCount, "loadout.screen.not-selected", "loadout.screen.owned")
                }
                LoadoutCategory.MELEE, LoadoutCategory.RANGED -> {
                    if (selected != null) entryToDisplayItem(selected, plugin, configManager, "loadout.screen.selected", "name" to (configManager.getLoadoutShopItemById(selected.loadoutId ?: "")?.name?.replace("&", "§") ?: "?"))
                    else makeCategoryPlaceholder(plugin, category, ownedCount, "loadout.screen.not-selected", "loadout.screen.owned")
                }
                LoadoutCategory.SUPPLIES -> {
                    val totalEquipped = holder.selectedSupplies.sumOf { it.count }
                    ItemStack(Material.POTION).apply {
                        editMeta { meta ->
                            meta.displayName(LanguageAPI.getComponent(plugin, "§f${LanguageAPI.getText(plugin, category.displayKey)}"))
                            meta.lore(listOf(
                                "§7${LanguageAPI.getText(plugin, "loadout.screen.supplies-equipped", "count" to totalEquipped)}",
                                "§8${LanguageAPI.getText(plugin, "loadout.screen.owned", "count" to ownedCount)}",
                                "",
                                "§e${LanguageAPI.getText(plugin, "loadout.screen.click-to-select")}"
                            ).map { MiniMessage.miniMessage().deserialize(it) })
                        }
                    }
                }
                LoadoutCategory.REPAIR -> null
            }
            if (display != null) inv.setItem(slot, display)
        }
        // 瓶子右侧槽位 37-42：无补给时全用玻璃板填充；有补给时从左往右紧凑展示，剩余格用玻璃板填充
        val grayPane = ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
            editMeta { it.displayName(MiniMessage.miniMessage().deserialize(" ")) }
        }
        val groupedSupplies = holder.selectedSupplies.groupBy { it.loadoutId }.mapValues { (_, entries) -> entries.sumOf { it.count } }.entries.toList().filter { it.key != null }.take(SLOT_SUPPLIES_DISPLAY.size)
        for (i in SLOT_SUPPLIES_DISPLAY.indices) {
            val slot = SLOT_SUPPLIES_DISPLAY[i]
            if (i >= groupedSupplies.size) {
                inv.setItem(slot, grayPane.clone())
                continue
            }
            val (loadoutId, count) = groupedSupplies[i]
            val id = loadoutId ?: run {
                inv.setItem(slot, grayPane.clone())
                continue
            }
            val config = configManager.getLoadoutShopItemById(id) ?: run {
                inv.setItem(slot, grayPane.clone())
                continue
            }
            val stack = LoadoutShopAPI.createPurchasedItem(plugin, config)
            stack.amount = count.coerceIn(1, stack.maxStackSize)
            stack.editMeta { meta ->
                val lore = (meta.lore() ?: emptyList()).toMutableList()
                lore.add(LanguageAPI.getComponent(plugin, "loadout.screen.supplies-equipped", "count" to count))
                meta.lore(lore)
            }
            inv.setItem(slot, stack)
        }
        // 箭矢槽位：已选远程时显示“选择箭矢/爆炸箭”或“已选 n 堆”；未选远程时显示占位
        val rangedId = holder.selectedRanged?.loadoutId
        val isExplosive = LoadoutUI.isExplosiveArrowRanged(rangedId)
        val arrowDisplay = if (holder.selectedRanged != null) {
            if (holder.selectedArrows.isNotEmpty()) {
                // 统计三类弹药已选总数：普通 / TNT / 特种
                var normalTotal = 0
                var tntTotal = 0
                var specialTotal = 0
                holder.selectedArrows.forEach { (_, e) ->
                    when {
                        e.type == sky4th.core.model.StorageEntryType.LOADOUT && e.loadoutId == "normal_arrow" ->
                            normalTotal += e.count
                        e.type == sky4th.core.model.StorageEntryType.LOADOUT && e.loadoutId == "baozha_jian" ->
                            tntTotal += e.count
                        e.type == sky4th.core.model.StorageEntryType.LOADOUT && e.loadoutId != null && e.loadoutId in SPECIAL_BOW_ARROW_IDS ->
                            specialTotal += e.count
                    }
                }
                val total = holder.selectedArrows.sumOf { (_, e) -> e.count }
                val arrowKey = if (isExplosive) "loadout.screen.arrows-selected-explosive" else "loadout.screen.arrows-selected"
                ItemStack(if (isExplosive) Material.SPECTRAL_ARROW else Material.ARROW).apply {
                    amount = total.coerceIn(1, 64)
                    editMeta { meta ->
                        meta.displayName(MiniMessage.miniMessage().deserialize(LanguageAPI.getText(plugin, arrowKey, "count" to holder.selectedArrows.size, "total" to total)))
                        val loreLines = mutableListOf<String>()
                        loreLines.add("§e${LanguageAPI.getText(plugin, "loadout.screen.click-to-select")}")
                        when {
                            isExplosive ->
                                loreLines.add(LanguageAPI.getText(plugin, "loadout.screen.arrows-explosive", "count" to tntTotal))
                            rangedId == "special_bow" ->
                                loreLines.addAll(
                                    listOf(
                                        LanguageAPI.getText(plugin, "loadout.screen.arrows-normal", "count" to normalTotal),
                                        LanguageAPI.getText(plugin, "loadout.screen.arrows-special", "count" to specialTotal)
                                    )
                                )
                            else ->
                                loreLines.add(LanguageAPI.getText(plugin, "loadout.screen.arrows-normal", "count" to normalTotal))
                        }
                        meta.lore(loreLines.map { MiniMessage.miniMessage().deserialize(it) })
                        LoadoutShopAPI.addEnchantGlowPublic(meta)
                    }
                }
            } else {
                val selectKey = if (isExplosive) "loadout.screen.select-explosive-arrows" else "loadout.screen.select-arrows"
                val hintKey = when {
                    isExplosive -> "loadout.screen.arrows-hint-explosive"
                    rangedId == "special_bow" -> "loadout.screen.arrows-hint-special"
                    else -> "loadout.screen.arrows-hint-normal"
                }
                ItemStack(if (isExplosive) Material.SPECTRAL_ARROW else Material.ARROW).apply {
                    editMeta { meta ->
                        meta.displayName(LanguageAPI.getComponent(plugin, selectKey))
                        val loreLines = listOf(
                            "§e${LanguageAPI.getText(plugin, "loadout.screen.click-to-select")}",
                            LanguageAPI.getText(plugin, hintKey)
                        )
                        meta.lore(loreLines.map { MiniMessage.miniMessage().deserialize(it) })
                    }
                }
            }
        } else {
            ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
                editMeta { meta ->
                    meta.displayName(LanguageAPI.getComponent(plugin, "loadout.screen.select-arrows-first"))
                }
            }
        }
        inv.setItem(SLOT_ARROWS, arrowDisplay)
    }

    private fun makeCategoryPlaceholder(
        plugin: JavaPlugin, 
        category: LoadoutCategory,
        ownedCount: Int,
        line1Key: String,
        line2Key: String,
        vararg pairs: Pair<String, Any>
    ): ItemStack {
        val icon = when (category) {
            LoadoutCategory.EQUIPMENT -> Material.LEATHER_HELMET
            LoadoutCategory.MELEE -> Material.IRON_SWORD
            LoadoutCategory.RANGED -> Material.BOW
            LoadoutCategory.SUPPLIES -> Material.POTION
            LoadoutCategory.REPAIR -> Material.ANVIL
        }
        val name = LanguageAPI.getText(plugin, category.displayKey)
        val line1 = LanguageAPI.getText(plugin, line1Key, *pairs)
        val line2 = LanguageAPI.getText(plugin, line2Key, "count" to ownedCount)
        return ItemStack(icon).apply {
            editMeta { meta ->
                meta.displayName(MiniMessage.miniMessage().deserialize("<white>$name"))
                meta.lore(listOf("<gray>$line1", "<dark_gray>$line2", "", "<yellow>${LanguageAPI.getText(plugin, "loadout.screen.click-to-select")}").map { MiniMessage.miniMessage().deserialize(it) })
            }
        }
    }

    private fun entryToDisplayItem(
        entry: StorageEntry,
        plugin: JavaPlugin,
        configManager: ConfigManager,
        loreKey: String,
        vararg pairs: Pair<String, Any>
    ): ItemStack? {
        if (entry.type != StorageEntryType.LOADOUT) return null
        val config = configManager.getLoadoutShopItemById(entry.loadoutId ?: return null) ?: return null
        val item = LoadoutShopAPI.createPurchasedItem(plugin, config)
        item.amount = entry.count.coerceIn(1, item.maxStackSize)
        val dur = entry.durability

        // 检查是否是套装
        val actualConfigList = configManager.getActualEquipmentConfigList(entry.loadoutId ?: "")
        val isSet = actualConfigList != null && actualConfigList.size > 1

        // 使用DurabilityManager统一处理耐久度
        if (isSet) {
            // 套装：从itemData计算整体耐久度比例
            val durabilityInfo = sky4th.dungeon.loadout.storage.DurabilityManager.getSetDurabilityInfo(entry)
            val overallRatio = durabilityInfo?.calculateOverallRatio() ?: 100
            sky4th.dungeon.loadout.storage.DurabilityManager.applyDurability(item, overallRatio, isSet = true)
        } else if (dur != null) {
            // 单件：应用保存的耐久度
            sky4th.dungeon.loadout.storage.DurabilityManager.applyDurability(item, dur, isSet = false)
        }
        item.editMeta { meta ->
            val lore = (meta.lore() ?: emptyList()).toMutableList()
            // 如果是套装且有耐久度损失，添加详细耐久度信息
            if (isSet) {
                val durabilityInfo = sky4th.dungeon.loadout.storage.DurabilityManager.getSetDurabilityInfo(entry)
                if (durabilityInfo != null) {
                    // 检查是否有任何部位的耐久度损失
                    val hasDamage = durabilityInfo.slotDurabilities.any { it.value.first < it.value.second }
                    if (hasDamage) {
                        lore.add(LanguageAPI.getComponent(plugin, "storage.set-durability-title"))
                        durabilityInfo.slotDurabilities.forEach { entry ->
                            val slot = entry.key
                            val (current, max) = entry.value
                            lore.add(LanguageAPI.getComponent(plugin, "storage.set-durability-line", "slot" to slot.displayName, "current" to current, "max" to max))
                        }
                    }
                }
            }
            lore.add(MiniMessage.miniMessage().deserialize("<dark_gray>" + LanguageAPI.getText(plugin, loreKey, *pairs)))
            meta.lore(lore)
        }
        return item
    }

    @JvmStatic
    fun refreshLoadoutContent(inv: Inventory, configManager: ConfigManager, plugin: JavaPlugin) {
        val holder = inv.holder as? LoadoutScreenHolder ?: return
        val player = Bukkit.getPlayer(holder.playerId) ?: return
        fillLoadoutSlots(inv, holder.playerId, holder, configManager, plugin)
        // 更新信用点头颅的显示
        setCreditDisplay(inv, player, configManager)
    }

    @JvmStatic
    fun isLoadoutScreen(holder: Any?): Boolean = holder is LoadoutScreenHolder

    @JvmStatic
    fun getCategorySlot(slot: Int): LoadoutCategory? = when (slot) {
        SLOT_CATEGORY_EQUIPMENT -> LoadoutCategory.EQUIPMENT
        SLOT_CATEGORY_MELEE -> LoadoutCategory.MELEE
        SLOT_CATEGORY_RANGED -> LoadoutCategory.RANGED
        SLOT_CATEGORY_SUPPLIES -> LoadoutCategory.SUPPLIES
        else -> null
    }
}
