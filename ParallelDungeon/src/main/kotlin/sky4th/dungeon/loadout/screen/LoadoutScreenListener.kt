package sky4th.dungeon.loadout.screen

import sky4th.dungeon.loadout.category.CategoryDetailUI
import sky4th.dungeon.loadout.equipment.ActualEquipmentResolver
import sky4th.dungeon.loadout.pick.PickArrowFromStorageUI
import sky4th.dungeon.loadout.ranged.RangedWeaponBehaviorRegistry
import sky4th.dungeon.loadout.storage.LootItemHelper
import sky4th.dungeon.loadout.storage.StorageUI
import sky4th.dungeon.loadout.shop.LoadoutShopAPI
import sky4th.core.api.EconomyAPI
import sky4th.dungeon.util.LanguageUtil.sendLang
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType
import sky4th.core.api.StorageAPI
import sky4th.core.model.StorageEntry
import sky4th.core.model.StorageEntryType
import sky4th.dungeon.command.CommandContext
import sky4th.dungeon.command.DungeonContext
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.plugin.java.JavaPlugin
import sky4th.dungeon.config.ConfigManager
import java.util.UUID

/**
 * 配装界面点击：选择装备/近战/远程/补给（从仓库选或提示去商店）；确认进入地牢时扣费并传送。
 */
class LoadoutScreenListener : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder ?: return
        if (!LoadoutUI.isLoadoutScreen(holder)) return

        event.isCancelled = true
        val who = event.whoClicked
        if (who !is Player) return

        val loadoutHolder = holder as LoadoutScreenHolder
        val ctx = DungeonContext.get() ?: return
        val plugin = ctx.plugin
        val configManager = ctx.configManager
        val slot = event.rawSlot

        // 进入地牢
        if (slot == LoadoutUI.SLOT_ENTER_DUNGEON) {
            tryEnterDungeon(who, loadoutHolder, ctx)
            return
        }

        // 打开商店
        if (slot == LoadoutUI.SLOT_SHOP) {
            who.closeInventory()
            LoadoutShopAPI.openShop(who, ctx.plugin, ctx.configManager)
            return
        }

        // 打开仓库
        if (slot == LoadoutUI.SLOT_STORAGE) {
            who.closeInventory()
            StorageUI.open(who)
            return
        }

        // 选择箭矢槽：仅当已选远程武器时，打开从仓库选择箭矢界面
        if (slot == LoadoutUI.SLOT_ARROWS && loadoutHolder.selectedRanged != null) {
            who.closeInventory()
            PickArrowFromStorageUI.open(who, loadoutHolder, plugin, configManager)
            return
        }

        // 四个分类槽（左列中间）：打开该分类详情（展示所有商品及仓库数量，首次点击选中、再次点击选择/购买）
        val category = LoadoutUI.getCategorySlot(slot) ?: return
        who.closeInventory()
        CategoryDetailUI.open(who, loadoutHolder, category, plugin, configManager)
    }

    private fun tryEnterDungeon(player: Player, loadoutHolder: LoadoutScreenHolder, ctx: CommandContext) {
        val plugin = ctx.plugin
        val configManager = ctx.configManager
        val playerManager = ctx.playerManager
        val dungeonInstanceManager = ctx.dungeonInstanceManager

        if (playerManager.isPlayerInDungeon(player)) {
            player.sendLang(plugin, "command.already-in-dungeon")
            return
        }

        // 验证地牢配置是否存在
        if (!dungeonInstanceManager.hasDungeonConfig(loadoutHolder.dungeonName)) {
            player.sendLang(plugin, "command.invalid-dungeon", "dungeon" to loadoutHolder.dungeonName)
            return
        }

        // 获取地牢配置
        val dungeonConfig = dungeonInstanceManager.getDungeonConfig(loadoutHolder.dungeonName)!!

        // 构建完整实例ID
        val fullInstanceId = if (loadoutHolder.instanceId != null) {
            "${loadoutHolder.dungeonName}_${loadoutHolder.instanceId}"
        } else {
            player.sendLang(plugin, "command.enter.instance-required")
            return
        }

        // 获取要进入的实例
        val targetInstance = dungeonInstanceManager.getInstance(fullInstanceId)
        if (targetInstance == null) {
            player.sendLang(plugin, "command.enter.instance-not-found", "instanceId" to fullInstanceId)
            return
        }

        // 检查实例是否已满
        if (targetInstance.getPlayerCount() >= dungeonConfig.maxPlayersPerInstance) {
            player.sendLang(plugin, "command.enter.instance-full", "instanceId" to fullInstanceId)
            return
        }

        val enterCost = dungeonConfig.cost
        if (enterCost > 0) {
            if (!EconomyAPI.isAvailable()) {
                player.sendLang(plugin, "command.enter-cost-failed")
                return
            }
            if (!EconomyAPI.hasEnough(player, enterCost)) {
                player.sendLang(plugin, "command.enter-cost-not-enough", "cost" to EconomyAPI.format(enterCost))
                return
            }
            if (!EconomyAPI.withdraw(player, enterCost)) {
                player.sendLang(plugin, "command.enter-cost-failed")
                return
            }
        }
        val success = playerManager.teleportToDungeon(player, loadoutHolder.dungeonName, loadoutHolder.instanceId)
        if (!success) {
            if (enterCost > 0 && EconomyAPI.isAvailable()) EconomyAPI.deposit(player, enterCost)
            player.sendLang(plugin, "command.enter-failed")
        } else {
            // 传送成功后发放装备
            giveLoadoutToPlayer(player, loadoutHolder, plugin, configManager)
            player.closeInventory()
        }
    }

    private fun giveLoadoutToPlayer(
        player: Player,
        loadoutHolder: LoadoutScreenHolder,
        plugin: JavaPlugin,
        configManager: ConfigManager
    ) {
        // 从仓库中获取物品并保留完整的itemData和durability信息
        val storageEntries = deductSelectedItemsFromStorage(player.uniqueId, loadoutHolder)

        val toGive = mutableListOf<org.bukkit.inventory.ItemStack>()
        listOfNotNull(
            loadoutHolder.selectedEquipment,
            loadoutHolder.selectedMelee
        ).forEach { entry ->
            // 使用从仓库中获取的完整entry（通过loadoutId查找）
            val fullEntry = storageEntries.values.firstOrNull { it.loadoutId == entry.loadoutId } ?: entry
            plugin.logger.info("[LoadoutScreenListener] giveLoadoutToPlayer: entry.itemData=${entry.itemData}, entry.durability=${entry.durability}")
            plugin.logger.info("[LoadoutScreenListener] giveLoadoutToPlayer: fullEntry.itemData=${fullEntry.itemData}, fullEntry.durability=${fullEntry.durability}")
            ActualEquipmentResolver.toActualItems(fullEntry, plugin, configManager).forEach { toGive.add(it) }
        }
        loadoutHolder.selectedRanged?.let { entry ->
            // 使用从仓库中获取的完整entry（通过loadoutId查找）
            val fullEntry = storageEntries.values.firstOrNull { it.loadoutId == entry.loadoutId } ?: entry
            val items = ActualEquipmentResolver.toActualItems(fullEntry, plugin, configManager).toMutableList()
            entry.loadoutId?.let { id -> RangedWeaponBehaviorRegistry.process(id, items, plugin) }
            items.forEach { toGive.add(it) }
        }
        loadoutHolder.selectedSupplies.forEach { entry ->
            // 使用从仓库中获取的完整entry（通过loadoutId查找）
            val fullEntry = storageEntries.values.firstOrNull { it.loadoutId == entry.loadoutId } ?: entry
            ActualEquipmentResolver.toActualItems(fullEntry, plugin, configManager).forEach { toGive.add(it) }
        }
        loadoutHolder.selectedArrows.forEach { (_, entry) ->
            when {
                // 普通箭矢
                entry.type == sky4th.core.model.StorageEntryType.LOADOUT && entry.loadoutId == "normal_arrow" ->
                    ActualEquipmentResolver.toActualItems(entry, plugin, configManager).forEach { toGive.add(it) }
                // 爆炸箭（TNT箭）
                entry.type == sky4th.core.model.StorageEntryType.LOADOUT && entry.loadoutId == "baozha_jian" ->
                    ActualEquipmentResolver.toActualItems(entry, plugin, configManager).forEach { toGive.add(it) }
                // 特种弓的特殊箭矢
                entry.type == sky4th.core.model.StorageEntryType.LOADOUT && entry.loadoutId != null && entry.loadoutId in LoadoutUI.SPECIAL_BOW_ARROW_IDS ->
                    ActualEquipmentResolver.toActualItems(entry, plugin, configManager).forEach { toGive.add(it) }
            }
        }
        toGive.forEach { stack ->
            val slot = equipmentSlotFor(stack.type)
            if (slot != null) {
                val inv = player.inventory
                val current = when (slot) {
                    EquipmentSlot.HEAD -> inv.helmet
                    EquipmentSlot.CHEST -> inv.chestplate
                    EquipmentSlot.LEGS -> inv.leggings
                    EquipmentSlot.FEET -> inv.boots
                    EquipmentSlot.OFF_HAND -> inv.itemInOffHand
                    else -> null
                }
                if (current != null && !current.type.isAir) {
                    val remaining = player.inventory.addItem(current)
                    remaining.values.forEach { player.world.dropItem(player.location, it) }
                }
                when (slot) {
                    EquipmentSlot.HEAD -> inv.helmet = stack
                    EquipmentSlot.CHEST -> inv.chestplate = stack
                    EquipmentSlot.LEGS -> inv.leggings = stack
                    EquipmentSlot.FEET -> inv.boots = stack
                    EquipmentSlot.OFF_HAND -> inv.setItemInOffHand(stack)
                    else -> { }
                }
            } else {
                val remaining = player.inventory.addItem(stack)
                remaining.values.forEach { player.world.dropItem(player.location, it) }
            }
        }
    }

    /** 护甲/盾牌对应装备槽，非装备返回 null（盾牌到副手） */
    private fun equipmentSlotFor(material: Material): EquipmentSlot? = when (material) {
        Material.LEATHER_HELMET, Material.CHAINMAIL_HELMET, Material.IRON_HELMET, Material.GOLDEN_HELMET,
        Material.DIAMOND_HELMET, Material.NETHERITE_HELMET, Material.TURTLE_HELMET,
        Material.CARVED_PUMPKIN -> EquipmentSlot.HEAD
        Material.LEATHER_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE, Material.IRON_CHESTPLATE, Material.GOLDEN_CHESTPLATE,
        Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE, Material.ELYTRA -> EquipmentSlot.CHEST
        Material.LEATHER_LEGGINGS, Material.CHAINMAIL_LEGGINGS, Material.IRON_LEGGINGS, Material.GOLDEN_LEGGINGS,
        Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS -> EquipmentSlot.LEGS
        Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS, Material.GOLDEN_BOOTS,
        Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS -> EquipmentSlot.FEET
        Material.SHIELD -> EquipmentSlot.OFF_HAND
        else -> null
    }

    /**
     * 从仓库中扣除所有选中的物品（装备、近战、远程、补给、箭矢）
     */
    private fun deductSelectedItemsFromStorage(uuid: UUID, loadoutHolder: LoadoutScreenHolder): Map<StorageEntry, StorageEntry> {
        val storage = StorageAPI.getStorage(uuid, StorageAPI.STORAGE_SLOT_COUNT).toMutableList()
        val result = mutableMapOf<StorageEntry, StorageEntry>()

        // 扣除选中的装备
        loadoutHolder.selectedEquipment?.let { entry ->
            val fullEntry = deductEntryFromStorage(uuid, storage, entry, loadoutHolder.selectedEquipmentStorageIndex)
            if (fullEntry != null) result[entry] = fullEntry
        }
        
        // 扣除选中的近战
        loadoutHolder.selectedMelee?.let { entry ->
            val fullEntry = deductEntryFromStorage(uuid, storage, entry, loadoutHolder.selectedMeleeStorageIndex)
            if (fullEntry != null) result[entry] = fullEntry
        }
        
        // 扣除选中的远程
        loadoutHolder.selectedRanged?.let { entry ->
            val fullEntry = deductEntryFromStorage(uuid, storage, entry, loadoutHolder.selectedRangedStorageIndex)
            if (fullEntry != null) result[entry] = fullEntry
        }
        
        // 扣除选中的补给
        loadoutHolder.selectedSupplies.forEach { entry ->
            val fullEntry = deductEntryFromStorage(uuid, storage, entry)
            if (fullEntry != null) result[entry] = fullEntry
        }
        
        // 扣除选中的箭矢（按"已选数量"扣除；若扣完为 0 才清空槽位）
        loadoutHolder.selectedArrows.forEach { (slotIdx, selectedEntry) ->
            val current = storage.getOrNull(slotIdx) ?: return@forEach
            val remaining = current.count - selectedEntry.count
            val newEntry = if (remaining <= 0) null else current.copy(count = remaining)
            StorageAPI.setSlot(uuid, slotIdx, newEntry)
            storage[slotIdx] = newEntry
        }

        return result
    }

    /**
     * 从仓库中扣除指定的StorageEntry
     * @param uuid 玩家UUID
     * @param storage 仓库数据
     * @param entry 要扣除的条目
     * @param storageIndex 仓库槽位索引（可选，如果提供则精确匹配该槽位的装备）
     * @return 扣除的完整StorageEntry（包含itemData和durability）
     */
    private fun deductEntryFromStorage(uuid: UUID, storage: MutableList<StorageEntry?>, entry: StorageEntry, storageIndex: Int? = null): StorageEntry? {
        val loadoutId = entry.loadoutId ?: return null
        var remainingToDeduct = entry.count
        var fullEntry: StorageEntry? = null
        val ctx = DungeonContext.get() ?: return null
        val plugin = ctx.plugin

        // 添加调试日志
        plugin.logger.info("[LoadoutScreenListener] deductEntryFromStorage: loadoutId=$loadoutId, storageIndex=$storageIndex, entry.itemData=${entry.itemData}, entry.durability=${entry.durability}")
        plugin.logger.info("[LoadoutScreenListener] deductEntryFromStorage: storage.size=${storage.size}")

        // 打印所有LOADOUT类型的entry
        storage.forEachIndexed { index, current ->
            if (current != null && current.type == StorageEntryType.LOADOUT) {
                plugin.logger.info("[LoadoutScreenListener] storage[$index]: loadoutId=${current.loadoutId}, itemData=${current.itemData}, durability=${current.durability}")
            }
        }

        // 直接从数据库获取完整的仓库数据，确保itemData字段正确
        val dbStorage = StorageAPI.getStorage(uuid, StorageAPI.STORAGE_SLOT_COUNT)

        // 创建loadoutId到StorageEntry的映射，用于追踪物品移动
        val loadoutIdToEntries = mutableMapOf<String, MutableList<Pair<Int, StorageEntry>>>()
        dbStorage.forEachIndexed { index, dbEntry ->
            if (dbEntry != null && dbEntry.loadoutId != null) {
                loadoutIdToEntries.getOrPut(dbEntry.loadoutId!!) { mutableListOf() }.add(index to dbEntry)
            }
        }

        // 如果提供了storageIndex，优先从指定槽位扣除
        if (storageIndex != null && storageIndex >= 0 && storageIndex < storage.size) {
            val current = storage[storageIndex]
            val dbEntry = dbStorage.getOrNull(storageIndex)

            if (current != null && current.type == StorageEntryType.LOADOUT && current.loadoutId == loadoutId) {
                // 使用数据库中的完整entry（包含itemData和durability）
                fullEntry = dbEntry ?: current
                plugin.logger.info("[LoadoutScreenListener] 从指定槽位扣除: storageIndex=$storageIndex, itemData=${fullEntry.itemData}, durability=${fullEntry.durability}")

                val deduct = minOf(current.count, remainingToDeduct)
                val newCount = current.count - deduct
                val newEntry = if (newCount <= 0) null else current.copy(count = newCount)

                StorageAPI.setSlot(uuid, storageIndex, newEntry)
                storage[storageIndex] = newEntry
                remainingToDeduct -= deduct

                plugin.logger.info("[LoadoutScreenListener] 返回fullEntry: ${fullEntry.itemData}, ${fullEntry.durability}")
                return fullEntry
            }
        }

        // 如果没有指定storageIndex或指定槽位不匹配，则按原有逻辑查找
        for (i in storage.indices) {
            if (remainingToDeduct <= 0) break

            val current = storage[i] ?: continue
            if (current.type != StorageEntryType.LOADOUT || current.loadoutId != loadoutId) continue

            // 从数据库获取完整的entry，确保itemData字段正确
            val dbEntry = dbStorage.getOrNull(i)

            // 保存第一个匹配的完整entry（包含itemData和durability）
            if (fullEntry == null) {
                // 优先使用数据库中的完整entry
                fullEntry = if (dbEntry?.itemData != null && dbEntry.itemData!!.isNotEmpty()) {
                    dbEntry
                } else if (current.itemData != null && current.itemData!!.isNotEmpty()) {
                    current
                } else {
                    // 如果都没有itemData，尝试从同loadoutId的其他槽位获取
                    val matchingEntries = loadoutIdToEntries[loadoutId]
                    if (matchingEntries != null && matchingEntries.isNotEmpty()) {
                        val (_, matchingEntry) = matchingEntries.first()
                        plugin.logger.info("[LoadoutScreenListener] 从同loadoutId的其他槽位获取itemData: loadoutId=$loadoutId, itemData=${matchingEntry.itemData}, durability=${matchingEntry.durability}")
                        matchingEntry
                    } else {
                        // 最后尝试使用原始entry的itemData
                        if (entry.itemData != null && entry.itemData!!.isNotEmpty()) {
                            current.copy(itemData = entry.itemData)
                        } else {
                            current
                        }
                    }
                }
                plugin.logger.info("[LoadoutScreenListener] 找到匹配的仓库物品: itemData=${fullEntry.itemData}, durability=${fullEntry.durability}")
            }

            val deduct = minOf(current.count, remainingToDeduct)
            val newCount = current.count - deduct
            val newEntry = if (newCount <= 0) null else current.copy(count = newCount)

            StorageAPI.setSlot(uuid, i, newEntry)
            storage[i] = newEntry
            remainingToDeduct -= deduct
        }

        plugin.logger.info("[LoadoutScreenListener] 返回fullEntry: ${fullEntry?.itemData}, ${fullEntry?.durability}")

        return fullEntry
    }



}
