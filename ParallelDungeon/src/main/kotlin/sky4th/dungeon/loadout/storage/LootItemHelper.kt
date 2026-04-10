package sky4th.dungeon.loadout.storage

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import sky4th.core.api.StorageAPI
import sky4th.core.model.StorageEntry
import sky4th.core.model.StorageEntryType
import sky4th.dungeon.config.ConfigManager
import sky4th.dungeon.Dungeon
import java.util.UUID

/**
 * LOOT 类型仓库条目的物品表示。
 * 使用物品ID直接从配置创建物品，确保与地牢中生成的物品完全一致。
 */
object LootItemHelper {

    /**
     * 向玩家仓库添加指定 loadoutId 的 LOADOUT 物品，优先堆叠到已有同 id 格（每格最多 64），不足再占空格。
     * 用于购买爆破弩时赠送爆炸箭等。
     * @param toGenerateDurability 是否生成耐久度信息（补给物品应为false）
     */
    @JvmStatic
    fun addLoadoutItemToStorage(uuid: UUID, loadoutId: String, amountToAdd: Int, toGenerateDurability: Boolean = true) {
        if (amountToAdd <= 0) return
        var remainder = amountToAdd
        val entries = StorageAPI.getStorage(uuid, StorageAPI.STORAGE_SLOT_COUNT)
        for (i in entries.indices) {
            if (remainder <= 0) break
            val entry = entries[i] ?: continue
            if (entry.type != StorageEntryType.LOADOUT || entry.loadoutId != loadoutId) continue
            val space = 64 - entry.count
            if (space <= 0) continue
            val add = minOf(remainder, space)
            // 保留原有的itemData和durability字段
            StorageAPI.setSlot(uuid, i, entry.copy(count = entry.count + add))
            remainder -= add
        }
        while (remainder > 0) {
            val entriesNow = StorageAPI.getStorage(uuid, StorageAPI.STORAGE_SLOT_COUNT)
            val emptySlot = entriesNow.indexOfFirst { it == null }
            if (emptySlot < 0) break
            val put = minOf(remainder, 64)
            // 新增的物品，检查是否是套装
            val ctx = sky4th.dungeon.command.DungeonContext.get()
            val configManager = ctx?.configManager
            val actualConfigList = configManager?.getActualEquipmentConfigList(loadoutId)
            val isSet = actualConfigList != null && actualConfigList.size > 1

            // 如果是套装且需要生成耐久度，生成各部位耐久度信息
            val itemData = if (isSet && toGenerateDurability) {
                // 套装：生成各部位耐久度信息字符串
                val materials = actualConfigList.mapNotNull { actualConfig ->
                    Material.matchMaterial(actualConfig.material.uppercase())
                }

                val durabilityInfo = DurabilityManager.SetDurabilityInfo.createFullDurability(materials)
                durabilityInfo.toStringFormat()
            } else {
                null
            }

            // 计算总耐久度比例（仅当需要生成耐久度时）
            val durability = if (isSet && toGenerateDurability) {
                // 套装：计算总耐久度比例（100%）
                100
            } else {
                null
            }

            StorageAPI.setSlot(uuid, emptySlot, StorageEntry(
                type = StorageEntryType.LOADOUT,
                count = put,
                loadoutId = loadoutId,
                durability = durability,
                itemData = itemData
            ))
            remainder -= put
        }
    }

    /**
     * 从玩家仓库中取出 1 个指定 loadoutId 的 LOADOUT 物品（扣减数量或清空槽位）。
     * @return 是否成功取出（仓库中有该物品）
     */
    @JvmStatic
    fun takeOneLoadoutFromStorage(uuid: UUID, loadoutId: String): Boolean {
        val entries = StorageAPI.getStorage(uuid, StorageAPI.STORAGE_SLOT_COUNT)
        for (i in entries.indices) {
            val entry = entries[i] ?: continue
            if (entry.type != StorageEntryType.LOADOUT || entry.loadoutId != loadoutId) continue
            if (entry.count <= 1) {
                StorageAPI.setSlot(uuid, i, null)
            } else {
                StorageAPI.setSlot(uuid, i, entry.copy(count = entry.count - 1))
            }
            return true
        }
        return false
    }

    /**
     * 向玩家仓库添加指定 lootId 的 LOOT 物品，优先堆叠到已有同 id 格（每格最多 64），不足再占空格。
     * @param uuid 玩家UUID
     * @param dungeonId 地牢ID
     * @param lootId 物品ID
     * @param amountToAdd 要添加的数量
     */
    @JvmStatic
    fun addLootItemToStorage(uuid: UUID, dungeonId: String, lootId: String, amountToAdd: Int) {
        if (amountToAdd <= 0) return
        var remainder = amountToAdd
        val entries = StorageAPI.getStorage(uuid, StorageAPI.STORAGE_SLOT_COUNT)
        
        // 首先尝试堆叠到已有的同lootId物品
        for (i in entries.indices) {
            if (remainder <= 0) break
            val entry = entries[i] ?: continue
            if (entry.type != StorageEntryType.LOOT) continue
            
            // 检查是否是相同的lootId
            val entryLootId = extractLootIdFromItemData(entry.itemData ?: "")
            if (entryLootId != lootId) continue
            
            val space = 64 - entry.count
            if (space <= 0) continue
            val add = minOf(remainder, space)
            StorageAPI.setSlot(uuid, i, entry.copy(count = entry.count + add))
            remainder -= add
        }
        
        // 如果还有剩余，创建新的槽位
        while (remainder > 0) {
            val entriesNow = StorageAPI.getStorage(uuid, StorageAPI.STORAGE_SLOT_COUNT)
            val emptySlot = entriesNow.indexOfFirst { it == null }
            if (emptySlot < 0) break
            val put = minOf(remainder, 64)
            
            // 构建itemData，包含lootId和dungeonId
            val itemData = "lootId:$lootId;dungeonId:$dungeonId"
            
            StorageAPI.setSlot(uuid, emptySlot, StorageEntry(
                type = StorageEntryType.LOOT,
                count = put,
                itemData = itemData
            ))
            remainder -= put
        }
    }

    /**
     * 从itemData中提取lootId
     * @param itemData 物品数据字符串
     * @return lootId，如果找不到则返回null
     */
    private fun extractLootIdFromItemData(itemData: String): String? {
        val pattern = Regex("lootId:([^;]*)")
        val match = pattern.find(itemData) ?: return null
        return match.groupValues[1]
    }
}

