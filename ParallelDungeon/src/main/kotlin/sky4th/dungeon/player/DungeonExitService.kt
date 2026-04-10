
package sky4th.dungeon.player

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.persistence.PersistentDataType
import sky4th.core.api.StorageAPI
import sky4th.core.api.EconomyAPI
import sky4th.core.model.StorageEntry
import sky4th.core.model.StorageEntryType
import sky4th.dungeon.config.ConfigManager
import sky4th.dungeon.loadout.storage.StorageUI
import sky4th.dungeon.Dungeon
import sky4th.dungeon.command.DungeonContext
import sky4th.dungeon.player.PlayerManager
import net.kyori.adventure.text.Component
import java.util.*

/**
 * 玩家撤离地牢时的物品转移服务
 * 负责将玩家背包中的物品转移到仓库，并处理堆叠、耐久度等信息
 */
class DungeonExitService(
    private val plugin: org.bukkit.plugin.java.JavaPlugin,
    private val configManager: ConfigManager,
    private val backpackManager: BackpackManager
) {
    private val lootIdKey: NamespacedKey by lazy { NamespacedKey(plugin, "dungeon_loot_id") }
    private val dungeonIdKey: NamespacedKey by lazy { NamespacedKey(plugin, "dungeon_id") } 
    private val loadoutPriceKey: NamespacedKey by lazy { NamespacedKey(plugin, "loadout_price") }
    private val loadoutSetKey: NamespacedKey by lazy { NamespacedKey(plugin, "loadout_set") }
    private val loadoutShopIdKey: NamespacedKey by lazy { NamespacedKey(plugin, "loadout_shop_id") }

    /**
     * 处理玩家撤离时的物品转移
     * @param player 玩家
     */
    fun handlePlayerExit(player: Player) {
        try {
            if (!StorageAPI.isAvailable()) {
                plugin.logger.warning("仓库服务不可用，无法转移玩家${player.name}的物品")
            } else {
                plugin.logger.info("[DungeonExit] 开始处理玩家 ${player.name} 的物品转移")

                // 将背包中的现金转换为信用点
                val cashAmount = backpackManager.getPlayerCash(player)
                if (cashAmount > 0) {
                    if (EconomyAPI.isAvailable()) {
                        EconomyAPI.deposit(player, cashAmount.toDouble())
                        plugin.logger.info("[DungeonExit] 玩家 ${player.name} 的背包现金 $cashAmount 已转换为信用点")
                    } else {
                        plugin.logger.warning("[DungeonExit] 经济服务不可用，无法转换玩家${player.name}的背包现金")
                    }
                }

                // 收集所有需要转移的物品
                val itemsToTransfer = mutableListOf<ItemStack>()

                // 收集背包中的物品
                for (item in player.inventory.storageContents) {
                    if (item != null && shouldTransferItem(item)) {
                        itemsToTransfer.add(item.clone())
                        plugin.logger.info("[DungeonExit] 收集到背包物品: ${item.type.name}, 数量: ${item.amount}")
                    }
                }

                // 收集护甲槽的物品
                for (item in player.inventory.armorContents) {
                    if (item != null && shouldTransferItem(item)) {
                        itemsToTransfer.add(item.clone())
                        plugin.logger.info("[DungeonExit] 收集到护甲物品: ${item.type.name}, 数量: ${item.amount}")
                    }
                }

                // 收集副手物品
                val offhandItem = player.inventory.itemInOffHand
                if (shouldTransferItem(offhandItem)) {
                    itemsToTransfer.add(offhandItem.clone())
                    plugin.logger.info("[DungeonExit] 收集到副手物品: ${offhandItem.type.name}, 数量: ${offhandItem.amount}")
                }

                if (itemsToTransfer.isEmpty()) {
                    plugin.logger.info("[DungeonExit] 玩家 ${player.name} 没有需要转移的物品")
                } else {
                    plugin.logger.info("[DungeonExit] 共收集到 ${itemsToTransfer.size} 个物品需要转移")

                    // 处理物品堆叠和转换
                    val processedEntries = processItemsForStorage(itemsToTransfer)
                    plugin.logger.info("[DungeonExit] 处理后得到 ${processedEntries.size} 个StorageEntry")

                    // 打印每个StorageEntry的详细信息
                    processedEntries.forEachIndexed { index, entry ->
                        plugin.logger.info("[DungeonExit] Entry[$index]: type=${entry.type}, count=${entry.count}, loadoutId=${entry.loadoutId}, durability=${entry.durability}, itemData=${entry.itemData}")
                    }

                    // 将物品添加到仓库
                    addToStorage(player.uniqueId, processedEntries)
                }
            }
        } catch (e: Exception) {
            plugin.logger.severe("处理玩家${player.name}的物品转移时出错: ${e.message}")
            e.printStackTrace()
        } finally {
            // 无论是否成功，都清空背包
            clearPlayerInventory(player)
            plugin.logger.info("[DungeonExit] 玩家 ${player.name} 的背包已清空")
        }
    }

    /**
     * 处理玩家撤离失败时的物品转移（死亡撤离）
     * 只保留背包中安全箱的东西和装备到仓库中，装备每部分扣除25%的耐久度
     * @param player 玩家
     */
    fun handlePlayerExitWithPenalty(player: Player) {
        try {
            if (!StorageAPI.isAvailable()) {
                plugin.logger.warning("仓库服务不可用，无法转移玩家${player.name}的物品")
            } else {
                plugin.logger.info("[DungeonExit] 开始处理玩家 ${player.name} 的失败撤离物品转移")

                // 收集所有需要转移的物品
                val itemsToTransfer = mutableListOf<ItemStack>()

                // 安全箱槽位（背包第一行中间5个）
                val safeSlots = listOf(11, 12, 13, 14, 15)

                // 收集安全箱中的物品
                for (slot in safeSlots) {
                    val item = player.inventory.getItem(slot)
                    if (item != null && !item.type.isAir && shouldTransferItem(item)) {
                        itemsToTransfer.add(item.clone())
                        plugin.logger.info("[DungeonExit] 收集到安全箱物品: ${item.type.name}, 数量: ${item.amount}")
                    }
                }

                // 收集护甲槽的物品（装备），并扣除25%的耐久度
                for (item in player.inventory.armorContents) {
                    if (item != null && !item.type.isAir && shouldTransferItem(item)) {
                        // 克隆物品以避免修改原物品
                        val itemWithPenalty = item.clone()

                        // 如果物品有耐久度，扣除25%
                        if (itemWithPenalty.type.maxDurability > 0) {
                            val meta = itemWithPenalty.itemMeta as? Damageable
                            if (meta != null) {
                                val maxD = itemWithPenalty.type.maxDurability.toInt()
                                val currentDurability = maxD - meta.damage
                                // 扣除25%的耐久度，但确保不低于0
                                val newDurability = (currentDurability * 0.75).toInt().coerceAtLeast(0)
                                val newDamage = maxD - newDurability
                                meta.damage = newDamage
                                itemWithPenalty.itemMeta = meta
                                plugin.logger.info("[DungeonExit] 装备耐久度扣除: ${item.type.name}, 原耐久度: $currentDurability, 新耐久度: $newDurability")
                            }
                        }

                        itemsToTransfer.add(itemWithPenalty)
                        plugin.logger.info("[DungeonExit] 收集到护甲物品: ${item.type.name}, 数量: ${item.amount}")
                    }
                }

                if (itemsToTransfer.isEmpty()) {
                    plugin.logger.info("[DungeonExit] 玩家 ${player.name} 没有需要转移的物品")
                } else {
                    plugin.logger.info("[DungeonExit] 共收集到 ${itemsToTransfer.size} 个物品需要转移")

                    // 处理物品堆叠和转换
                    val processedEntries = processItemsForStorage(itemsToTransfer)
                    plugin.logger.info("[DungeonExit] 处理后得到 ${processedEntries.size} 个StorageEntry")

                    // 打印每个StorageEntry的详细信息
                    processedEntries.forEachIndexed { index, entry ->
                        plugin.logger.info("[DungeonExit] Entry[$index]: type=${entry.type}, count=${entry.count}, loadoutId=${entry.loadoutId}, durability=${entry.durability}, itemData=${entry.itemData}")
                    }

                    // 将物品添加到仓库
                    addToStorage(player.uniqueId, processedEntries)
                }
            }
        } catch (e: Exception) {
            plugin.logger.severe("处理玩家${player.name}的物品转移时出错: ${e.message}")
            e.printStackTrace()
        } finally {
            // 无论是否成功，都清空背包
            clearPlayerInventory(player)
            plugin.logger.info("[DungeonExit] 玩家 ${player.name} 的背包已清空")
        }
    }

    /**
     * 判断物品是否需要转移到仓库
     */
    private fun shouldTransferItem(item: ItemStack?): Boolean {
        if (item == null || item.type.isAir) return false
        val meta = item.itemMeta ?: return false
        val pdc = meta.persistentDataContainer

        // 检查是否是配装物品（使用loadout_shop_id）
        val hasLoadoutShopId = pdc.has(loadoutShopIdKey, PersistentDataType.STRING)
        if (hasLoadoutShopId) {
            plugin.logger.info("[DungeonExit] 物品是配装物品: ${item.type.name}")
            return true
        }

        // 检查是否是地牢战利品（使用dungeon_loot_id）
        val hasLootId = pdc.has(lootIdKey, PersistentDataType.STRING)
        if (hasLootId) {
            plugin.logger.info("[DungeonExit] 物品是地牢战利品: ${item.type.name}")
            return true
        }

        plugin.logger.info("[DungeonExit] 物品不需要转移: ${item.type.name}")
        return false
    }

    /**
     * 处理物品，转换为StorageEntry列表
     * 处理堆叠、耐久度等信息
     */
    private fun processItemsForStorage(items: List<ItemStack>): List<StorageEntry> {
        plugin.logger.info("[DungeonExit] 开始处理 ${items.size} 个物品")
        val entries = mutableListOf<StorageEntry>()
        val groupedItems = mutableMapOf<String, MutableList<ItemStack>>()

        // 按物品类型分组（用于堆叠）
        for (item in items) {
            val key = getItemKey(item)
            if (!groupedItems.containsKey(key)) {
                groupedItems[key] = mutableListOf()
            }
            groupedItems[key]!!.add(item)
        }

        plugin.logger.info("[DungeonExit] 物品分组结果: ${groupedItems.size} 组")

        // 处理每组物品
        for ((key, group) in groupedItems) {
            plugin.logger.info("[DungeonExit] 处理组: $key, 物品数量: ${group.size}")
            val firstItem = group.first()

            // 检查是否是配装物品
            val loadoutId = firstItem.itemMeta?.persistentDataContainer?.get(
                loadoutShopIdKey, PersistentDataType.STRING
            )

            if (loadoutId != null) {
                plugin.logger.info("[DungeonExit] 配装物品: loadoutId=$loadoutId")
                // 配装物品：检查是否是套装
                val actualConfigList = configManager.getActualEquipmentConfigList(loadoutId)
                val isSet = actualConfigList != null && actualConfigList.size > 1
                plugin.logger.info("[DungeonExit] 是否为套装: $isSet")

                if (isSet) {
                    // 套装：合并耐久度，在描述中添加各部位耐久度
                    val setEntry = processSetItem(group, loadoutId)
                    if (setEntry != null) {
                        entries.add(setEntry)
                        plugin.logger.info("[DungeonExit] 添加套装Entry: durability=${setEntry.durability}, itemData=${setEntry.itemData}")
                    }
                } else {
                    // 单件装备：正常处理
                    for (item in group) {
                        val entry = convertToStorageEntry(item)
                        if (entry != null) {
                            entries.add(entry)
                            plugin.logger.info("[DungeonExit] 添加单件Entry: loadoutId=${entry.loadoutId}, durability=${entry.durability}")
                        }
                    }
                }
            } else {
                plugin.logger.info("[DungeonExit] 普通物品（非配装）")
                // 普通物品：尝试堆叠
                val stackedEntry = stackItems(group)
                if (stackedEntry != null) {
                    entries.add(stackedEntry)
                    plugin.logger.info("[DungeonExit] 添加堆叠Entry: type=${stackedEntry.type}, itemData=${stackedEntry.itemData}, count=${stackedEntry.count}")
                }
            }
        }

        plugin.logger.info("[DungeonExit] 处理完成，共生成 ${entries.size} 个StorageEntry")
        return entries
    }

    /**
     * 处理套装物品，合并耐久度信息
     */
    private fun processSetItem(items: List<ItemStack>, loadoutId: String): StorageEntry? {
        if (items.isEmpty()) return null

        plugin.logger.info("[DungeonExit] 处理套装物品: loadoutId=$loadoutId, 物品数量=${items.size}")

        val slotDurability = mutableMapOf<String, Pair<Int, Int>>() // 存储每个部位的(当前耐久度, 最大耐久度)

        // 收集各部位的耐久度信息
        for (item in items) {
            val slotType = getArmorSlotType(item.type)
            if (slotType != null) {
                val meta = item.itemMeta as? Damageable ?: continue
                val maxD = item.type.maxDurability.toInt()
                val currentDurability = (maxD - meta.damage).coerceIn(0, maxD)

                plugin.logger.info("[DungeonExit] 物品: ${item.type.name}, 部位: $slotType, 当前耐久度: $currentDurability, 最大耐久度: $maxD")

                // 每个部位只保留一个耐久度值，不累加
                if (!slotDurability.containsKey(slotType)) {
                    slotDurability[slotType] = Pair(currentDurability, maxD)
                    plugin.logger.info("[DungeonExit] 记录部位 $slotType 的耐久度: $currentDurability/$maxD")
                }
            }
        }

        // 获取套装配置，确定应该有多少个部件
        val actualConfigList = configManager.getActualEquipmentConfigList(loadoutId)
        val expectedPartCount = actualConfigList?.size ?: 1

        plugin.logger.info("[DungeonExit] 套装应有部件数: $expectedPartCount, 实际部件数: ${slotDurability.size}")

        // 如果装备缺失（数量少于套装部件数），将缺失部件的耐久度设为0
        if (slotDurability.size < expectedPartCount) {
            plugin.logger.info("[DungeonExit] 套装部件缺失，将缺失部件耐久度设为0")
            // 遍历所有配置的部件，找出缺失的
            actualConfigList?.forEach { config ->
                val material = Material.matchMaterial(config.material.uppercase())
                if (material == null) return@forEach
                val slotType = getArmorSlotType(material)
                if (slotType != null && !slotDurability.containsKey(slotType)) {
                    val maxD = material.maxDurability.toInt()
                    // 缺失部件的耐久度设为0
                    slotDurability[slotType] = Pair(0, maxD)
                    plugin.logger.info("[DungeonExit] 缺失部件 $slotType 的耐久度设为: 0/$maxD")
                }
            }
        }

        // 计算总耐久度比例
        val totalCurrent = slotDurability.values.sumOf { it.first }
        val totalMax = slotDurability.values.sumOf { it.second }
        val durabilityRatio = if (totalMax > 0) totalCurrent.toDouble() / totalMax else 1.0

        plugin.logger.info("[DungeonExit] 总耐久度: $totalCurrent/$totalMax, 比例: $durabilityRatio")

        // 构建各部位耐久度信息字符串，格式：部位:当前耐久度/最大耐久度
        val durabilityInfo = slotDurability.keys.sorted().joinToString(",") { slotType ->
            val (current, max) = slotDurability[slotType] ?: Pair(0, 1)
            "$slotType:$current/$max"
        }

        plugin.logger.info("[DungeonExit] 耐久度信息字符串: $durabilityInfo")

        // 创建StorageEntry，保存各部位耐久度信息
        // 套装的durability字段设为null，因为耐久度完全由itemData决定
        val entry = StorageEntry(
            type = StorageEntryType.LOADOUT,
            count = 1,
            loadoutId = loadoutId,
            durability = null,
            itemData = if (durabilityInfo.isNotEmpty()) durabilityInfo else null
        )

        plugin.logger.info("[DungeonExit] 创建StorageEntry: durability=${entry.durability}, itemData=${entry.itemData}")

        return entry
    }

    /**
     * 获取护甲部位类型
     */
    private fun getArmorSlotType(material: Material): String? {
        return when (material) {
            Material.LEATHER_HELMET, Material.CHAINMAIL_HELMET, Material.IRON_HELMET,
            Material.GOLDEN_HELMET, Material.DIAMOND_HELMET, Material.NETHERITE_HELMET -> "头盔"
            Material.LEATHER_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE, Material.IRON_CHESTPLATE,
            Material.GOLDEN_CHESTPLATE, Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE -> "胸甲"
            Material.LEATHER_LEGGINGS, Material.CHAINMAIL_LEGGINGS, Material.IRON_LEGGINGS,
            Material.GOLDEN_LEGGINGS, Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS -> "腿甲"
            Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS,
            Material.GOLDEN_BOOTS, Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS -> "靴子"
            else -> null
        }
    }

    /**
     * 堆叠相同类型的物品
     */
    private fun stackItems(items: List<ItemStack>): StorageEntry? {
        if (items.isEmpty()) return null

        val firstItem = items.first()
        val loadoutShopId = firstItem.itemMeta?.persistentDataContainer?.get(
            loadoutShopIdKey, PersistentDataType.STRING
        )

        if (loadoutShopId != null) {
            // 配装物品不能堆叠
            return convertToStorageEntry(firstItem)
        }

        // 检查是否是搜刮物品
        val lootId = firstItem.itemMeta?.persistentDataContainer?.get(
            lootIdKey, PersistentDataType.STRING
        )

        if (lootId != null) {
            // 从物品上读取dungeonId
            val dungeonId = firstItem.itemMeta?.persistentDataContainer?.get(
                dungeonIdKey, PersistentDataType.STRING
            )
            
            // 搜刮物品可以堆叠，只保存物品ID和地牢ID
            val totalAmount = items.sumOf { it.amount }
            plugin.logger.info("[DungeonExit] 堆叠战利品: dungeonId=$dungeonId, lootId=$lootId, 数量=$totalAmount")
            
            // 保存物品ID和地牢ID，格式：lootId:<物品ID>;dungeonId:<地牢ID>
            val itemData = if (dungeonId != null) {
                "lootId:$lootId;dungeonId:$dungeonId"
            } else {
                "lootId:$lootId"
            }
            
            return StorageEntry(
                type = StorageEntryType.LOOT,
                count = totalAmount.coerceIn(1, firstItem.maxStackSize),
                itemData = itemData
            )
        }

        return null
    }

    /**
     * 将ItemStack转换为StorageEntry
     */
    private fun convertToStorageEntry(item: ItemStack): StorageEntry? {
        if (item.type.isAir) return null

        plugin.logger.info("[DungeonExit] 转换物品到StorageEntry: ${item.type.name}, 数量: ${item.amount}")

        val loadoutId = item.itemMeta?.persistentDataContainer?.get(
            loadoutShopIdKey, PersistentDataType.STRING
        )

        if (loadoutId != null) {
            plugin.logger.info("[DungeonExit] 物品是配装物品: loadoutId=$loadoutId")
            var durability: Int? = null
            if (item.type.maxDurability > 0) {
                val meta = item.itemMeta as? Damageable
                if (meta != null) {
                    val maxD = item.type.maxDurability.toInt()
                    val damage = meta.damage
                    durability = (maxD - damage).coerceIn(0, maxD)
                    if (durability >= maxD) durability = null
                    plugin.logger.info("[DungeonExit] 物品耐久度: $durability (最大: $maxD, 损伤: $damage)")
                }
            }

            val entry = StorageEntry(
                type = StorageEntryType.LOADOUT,
                count = item.amount.coerceIn(1, item.maxStackSize),
                loadoutId = loadoutId,
                durability = durability?.takeIf { it > 0 }
            )
            plugin.logger.info("[DungeonExit] 创建LOADOUT Entry: loadoutId=${entry.loadoutId}, durability=${entry.durability}")
            return entry
        }

        val lootId = item.itemMeta?.persistentDataContainer?.get(
            lootIdKey, PersistentDataType.STRING
        )

        if (lootId != null) {
            plugin.logger.info("[DungeonExit] 物品是战利品: lootId=$lootId")
            // 保存完整的物品信息，包括lootId、材质、名字、描述、价值和耐久度
            val itemData = "lootId:$lootId"
            val entry = StorageEntry(
                type = StorageEntryType.LOOT,
                count = item.amount.coerceIn(1, item.maxStackSize),
                itemData = itemData
            )
            plugin.logger.info("[DungeonExit] 创建LOOT Entry: itemData=${entry.itemData}, count=${entry.count}")
            return entry
        }

        plugin.logger.warning("[DungeonExit] 物品无法转换为StorageEntry: ${item.type.name}")
        return null
    }

    /**
     * 获取物品的唯一标识键（用于分组）
     */
    private fun getItemKey(item: ItemStack): String {
        // 优先检查loadout_shop_id（配装物品）
        val loadoutShopId = item.itemMeta?.persistentDataContainer?.get(
            loadoutShopIdKey, PersistentDataType.STRING
        )
        if (loadoutShopId != null) {
            return "loadout:$loadoutShopId"
        }

        // 检查dungeon_loot_id（搜刮物品）
        val lootId = item.itemMeta?.persistentDataContainer?.get(
            lootIdKey, PersistentDataType.STRING
        )
        if (lootId != null) {
            return "loot:$lootId"
        }

        return "material:${item.type.name}"
    }

    /**
     * 将物品添加到仓库
     */
    private fun addToStorage(playerUuid: UUID, entries: List<StorageEntry>) {
        // 获取当前仓库内容
        val currentStorage = StorageAPI.getStorage(playerUuid, StorageAPI.STORAGE_SLOT_COUNT).toMutableList()

        // 尝试堆叠现有物品
        for (entry in entries) {
            var added = false
            for (i in currentStorage.indices) {
                val existing = currentStorage[i]
                if (existing != null && canStack(existing, entry)) {
                    val newCount = existing.count + entry.count
                    currentStorage[i] = existing.copy(count = newCount.coerceAtMost(64))
                    added = true
                    break
                }
            }

            // 如果没有堆叠成功，找到空槽位添加
            if (!added) {
                val emptySlot = currentStorage.indexOfFirst { it == null }
                if (emptySlot >= 0) {
                    currentStorage[emptySlot] = entry
                }
            }
        }

        // 保存回仓库
        StorageAPI.setStorage(playerUuid, currentStorage)
    }

    /**
     * 判断两个StorageEntry是否可以堆叠
     */
    private fun canStack(entry1: StorageEntry, entry2: StorageEntry): Boolean {
        if (entry1.type != entry2.type) return false

        return when (entry1.type) {
            StorageEntryType.LOOT -> entry1.itemData == entry2.itemData
            StorageEntryType.LOADOUT -> {
                // 套装：比较loadoutId和itemData（各部位耐久度信息）
                // 单件：比较loadoutId和durability
                val isSet = configManager.getActualEquipmentConfigList(entry1.loadoutId ?: "")?.size?.let { it > 1 } ?: false
                if (isSet) {
                    entry1.loadoutId == entry2.loadoutId && entry1.itemData == entry2.itemData
                } else {
                    entry1.loadoutId == entry2.loadoutId && entry1.durability == entry2.durability
                }
            }
        }
    }

    /**
     * 清空玩家背包
     */
    private fun clearPlayerInventory(player: Player) {
        player.inventory.clear()
    }
}



