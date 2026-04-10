package sky4th.core.database

import sky4th.core.model.StorageEntry
import sky4th.core.model.StorageEntryType
import java.sql.Types
import java.util.*

/**
 * 玩家仓库数据访问：按槽位存储配装（id+数量+耐久）或搜打撤物品（序列化）。
 */
class PlayerStorageDAO(private val databaseManager: DatabaseManager) {

    /**
     * 获取玩家仓库全部槽位。
     * 返回 List 按 slot_index 排序，空槽为 null，长度由最大 slot_index 决定（可约定如 36 格则 0..35）。
     */
    fun getStorage(uuid: UUID, maxSlots: Int = 36): List<StorageEntry?> {
        return databaseManager.getConnection().use { conn ->
            conn.prepareStatement("""
                SELECT slot_index, entry_type, loadout_id, count, durability, item_data
                FROM player_storage WHERE uuid = ? ORDER BY slot_index
            """.trimIndent()).use { stmt ->
                stmt.setString(1, uuid.toString())
                stmt.executeQuery().use { rs ->
                    val list = MutableList<StorageEntry?>(maxSlots) { null }
                    while (rs.next()) {
                        val slot = rs.getInt("slot_index")
                        if (slot in 0 until maxSlots) {
                            list[slot] = rowToEntry(rs)
                        }
                    }
                    list
                }
            }
        }
    }

    /**
     * 设置某一槽位：entry 为 null 时删除该槽。
     */
    fun setSlot(uuid: UUID, slotIndex: Int, entry: StorageEntry?) {
        databaseManager.getConnection().use { conn ->
            if (entry == null) {
                conn.prepareStatement("DELETE FROM player_storage WHERE uuid = ? AND slot_index = ?").use { stmt ->
                    stmt.setString(1, uuid.toString())
                    stmt.setInt(2, slotIndex)
                    stmt.executeUpdate()
                }
            } else {
                conn.prepareStatement("""
                    INSERT INTO player_storage (uuid, slot_index, entry_type, loadout_id, count, durability, item_data)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        entry_type = VALUES(entry_type),
                        loadout_id = VALUES(loadout_id),
                        count = VALUES(count),
                        durability = VALUES(durability),
                        item_data = VALUES(item_data)
                """.trimIndent()).use { stmt ->
                    stmt.setString(1, uuid.toString())
                    stmt.setInt(2, slotIndex)
                    stmt.setString(3, entry.type.name)
                    if (entry.loadoutId != null) stmt.setString(4, entry.loadoutId) else stmt.setNull(4, Types.VARCHAR)
                    stmt.setInt(5, entry.count)
                    if (entry.durability != null) stmt.setInt(6, entry.durability) else stmt.setNull(6, Types.INTEGER)
                    if (entry.itemData != null) stmt.setString(7, entry.itemData) else stmt.setNull(7, Types.LONGVARCHAR)
                    stmt.executeUpdate()
                }
            }
        }
    }

    /**
     * 批量写入玩家仓库（先清空该玩家再按槽位写入，空槽不写）。
     */
    fun setStorage(uuid: UUID, entries: List<StorageEntry?>) {
        databaseManager.getConnection().use { conn ->
            conn.autoCommit = false
            try {
                conn.prepareStatement("DELETE FROM player_storage WHERE uuid = ?").use { stmt ->
                    stmt.setString(1, uuid.toString())
                    stmt.executeUpdate()
                }
                conn.prepareStatement("""
                    INSERT INTO player_storage (uuid, slot_index, entry_type, loadout_id, count, durability, item_data)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()).use { insert ->
                    entries.forEachIndexed { slotIndex, entry ->
                        if (entry != null) {
                            insert.setString(1, uuid.toString())
                            insert.setInt(2, slotIndex)
                            insert.setString(3, entry.type.name)
                            if (entry.loadoutId != null) insert.setString(4, entry.loadoutId) else insert.setNull(4, Types.VARCHAR)
                            insert.setInt(5, entry.count)
                            if (entry.durability != null) insert.setInt(6, entry.durability) else insert.setNull(6, Types.INTEGER)
                            if (entry.itemData != null) insert.setString(7, entry.itemData) else insert.setNull(7, Types.LONGVARCHAR)
                            insert.addBatch()
                        }
                    }
                    insert.executeBatch()
                }
                conn.commit()
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
    }

    /**
     * 清空该玩家所有仓库槽位。
     */
    fun clearPlayer(uuid: UUID) {
        databaseManager.getConnection().use { conn ->
            conn.prepareStatement("DELETE FROM player_storage WHERE uuid = ?").use { stmt ->
                stmt.setString(1, uuid.toString())
                stmt.executeUpdate()
            }
        }
    }

    private fun rowToEntry(rs: java.sql.ResultSet): StorageEntry {
        val type = StorageEntryType.valueOf(rs.getString("entry_type"))
        val count = rs.getInt("count")
        val loadoutId = rs.getString("loadout_id")?.takeIf { it.isNotBlank() }
        val durability = rs.getInt("durability").let { if (rs.wasNull()) null else it }
        val itemData = rs.getString("item_data")?.takeIf { it.isNotBlank() }
        return StorageEntry(
            type = type,
            count = count,
            loadoutId = loadoutId,
            durability = durability,
            itemData = itemData
        )
    }
}
