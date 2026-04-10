
package sky4th.core.database

import sky4th.core.model.VillageData
import java.sql.*
import java.util.*

/**
 * 村庄数据访问对象（DAO）
 * 负责所有村庄数据的数据库操作
 */
class VillageDataDAO(private val databaseManager: DatabaseManager) {
    /**
     * 保存村庄数据
     */
    fun saveVillage(village: VillageData) {
        databaseManager.getConnection().use { conn ->
            val sql = """
                INSERT INTO village_data (
                    id, world, chunk_x, chunk_z,
                    min_x, min_y, min_z, max_x, max_y, max_z,
                    level, last_raid_time, last_loot_time,
                    allied_teams, hostile_teams,
                    baby_villager_count, unemployed_villager_count,
                    level1_villager_count, level2_villager_count,
                    level3_villager_count, level4_villager_count,
                    level5_villager_count
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    world = VALUES(world),
                    chunk_x = VALUES(chunk_x),
                    chunk_z = VALUES(chunk_z),
                    min_x = VALUES(min_x),
                    min_y = VALUES(min_y),
                    min_z = VALUES(min_z),
                    max_x = VALUES(max_x),
                    max_y = VALUES(max_y),
                    max_z = VALUES(max_z),
                    level = VALUES(level),
                    last_raid_time = VALUES(last_raid_time),
                    last_loot_time = VALUES(last_loot_time),
                    allied_teams = VALUES(allied_teams),
                    hostile_teams = VALUES(hostile_teams),
                    baby_villager_count = VALUES(baby_villager_count),
                    unemployed_villager_count = VALUES(unemployed_villager_count),
                    level1_villager_count = VALUES(level1_villager_count),
                    level2_villager_count = VALUES(level2_villager_count),
                    level3_villager_count = VALUES(level3_villager_count),
                    level4_villager_count = VALUES(level4_villager_count),
                    level5_villager_count = VALUES(level5_villager_count)
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, village.id.toString())
                stmt.setString(2, village.worldName)
                stmt.setInt(3, village.chunkX)
                stmt.setInt(4, village.chunkZ)
                stmt.setInt(5, village.minX)
                stmt.setInt(6, village.minY)
                stmt.setInt(7, village.minZ)
                stmt.setInt(8, village.maxX)
                stmt.setInt(9, village.maxY)
                stmt.setInt(10, village.maxZ)
                stmt.setInt(11, village.level)
                stmt.setLong(12, village.lastRaidTime)
                stmt.setLong(13, village.lastLootTime)
                stmt.setString(14, serializeUUIDSet(village.alliedTeams))
                stmt.setString(15, serializeUUIDSet(village.hostileTeams))
                stmt.setInt(16, village.babyVillagerCount)
                stmt.setInt(17, village.unemployedVillagerCount)
                stmt.setInt(18, village.level1VillagerCount)
                stmt.setInt(19, village.level2VillagerCount)
                stmt.setInt(20, village.level3VillagerCount)
                stmt.setInt(21, village.level4VillagerCount)
                stmt.setInt(22, village.level5VillagerCount)
                stmt.executeUpdate()
            }
        }
    }

    /**
     * 根据 ID 加载村庄数据
     */
    fun loadVillage(id: UUID): VillageData? {
        val sql = "SELECT * FROM village_data WHERE id = ?"

        return databaseManager.getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, id.toString())
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        mapRowToVillage(rs)
                    } else {
                        null
                    }
                }
            }
        }
    }

    /**
     * 根据世界和区块坐标加载村庄数据
     */
    fun loadVillageByChunk(worldName: String, chunkX: Int, chunkZ: Int): VillageData? {
        val sql = "SELECT * FROM village_data WHERE world = ? AND chunk_x = ? AND chunk_z = ?"

        return databaseManager.getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, worldName)
                stmt.setInt(2, chunkX)
                stmt.setInt(3, chunkZ)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        mapRowToVillage(rs)
                    } else {
                        null
                    }
                }
            }
        }
    }

    /**
     * 加载指定世界的所有村庄
     */
    fun loadVillagesByWorld(worldName: String): List<VillageData> {
        val villages = mutableListOf<VillageData>()
        val sql = "SELECT * FROM village_data WHERE world = ?"

        databaseManager.getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, worldName)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        villages.add(mapRowToVillage(rs))
                    }
                }
            }
        }
        return villages
    }

    /**
     * 加载所有村庄
     */
    fun loadAllVillages(): List<VillageData> {
        val villages = mutableListOf<VillageData>()
        val sql = "SELECT * FROM village_data"

        databaseManager.getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeQuery(sql).use { rs ->
                    while (rs.next()) {
                        villages.add(mapRowToVillage(rs))
                    }
                }
            }
        }
        return villages
    }

    /**
     * 删除村庄数据
     */
    fun deleteVillage(id: UUID): Boolean {
        val sql = "DELETE FROM village_data WHERE id = ?"

        return databaseManager.getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, id.toString())
                stmt.executeUpdate() > 0
            }
        }
    }

    /**
     * 更新村庄等级
     */
    fun updateVillageLevel(id: UUID, level: Int): Boolean {
        val sql = "UPDATE village_data SET level = ? WHERE id = ?"

        return databaseManager.getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, level)
                stmt.setString(2, id.toString())
                stmt.executeUpdate() > 0
            }
        }
    }

    /**
     * 更新劫掠时间
     */
    fun updateRaidTime(id: UUID, timestamp: Long): Boolean {
        val sql = "UPDATE village_data SET last_raid_time = ? WHERE id = ?"

        return databaseManager.getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, timestamp)
                stmt.setString(2, id.toString())
                stmt.executeUpdate() > 0
            }
        }
    }

    /**
     * 更新抢夺时间
     */
    fun updateLootTime(id: UUID, timestamp: Long): Boolean {
        val sql = "UPDATE village_data SET last_loot_time = ? WHERE id = ?"

        return databaseManager.getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, timestamp)
                stmt.setString(2, id.toString())
                stmt.executeUpdate() > 0
            }
        }
    }

    /**
     * 更新单个村民统计字段
     */
    fun updateVillagerCountField(id: UUID, field: String, value: Int): Boolean {
        val sql = "UPDATE village_data SET $field = ? WHERE id = ?"

        return databaseManager.getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, value)
                stmt.setString(2, id.toString())
                stmt.executeUpdate() > 0
            }
        }
    }

    /**
     * 批量更新村民统计字段
     */
    fun updateVillagerStats(
        id: UUID,
        babyCount: Int? = null,
        unemployedCount: Int? = null,
        level1Count: Int? = null,
        level2Count: Int? = null,
        level3Count: Int? = null,
        level4Count: Int? = null,
        level5Count: Int? = null
    ): Boolean {
        val updates = mutableListOf<String>()
        val params = mutableListOf<Any>()

        babyCount?.let {
            updates.add("baby_villager_count = ?")
            params.add(it)
        }
        unemployedCount?.let {
            updates.add("unemployed_villager_count = ?")
            params.add(it)
        }
        level1Count?.let {
            updates.add("level1_villager_count = ?")
            params.add(it)
        }
        level2Count?.let {
            updates.add("level2_villager_count = ?")
            params.add(it)
        }
        level3Count?.let {
            updates.add("level3_villager_count = ?")
            params.add(it)
        }
        level4Count?.let {
            updates.add("level4_villager_count = ?")
            params.add(it)
        }
        level5Count?.let {
            updates.add("level5_villager_count = ?")
            params.add(it)
        }

        if (updates.isEmpty()) return false

        val sql = "UPDATE village_data SET ${updates.joinToString(", ")} WHERE id = ?"
        params.add(id.toString())

        return databaseManager.getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                params.forEachIndexed { index, param ->
                    when (param) {
                        is Int -> stmt.setInt(index + 1, param)
                        is String -> stmt.setString(index + 1, param)
                    }
                }
                stmt.executeUpdate() > 0
            }
        }
    }

    /**
     * 添加建交队伍
     */
    fun addAlliedTeam(villageId: UUID, teamId: UUID): Boolean {
        val village = loadVillage(villageId) ?: return false
        village.addAlliedTeam(teamId)
        saveVillage(village)
        return true
    }

    /**
     * 移除建交队伍
     */
    fun removeAlliedTeam(villageId: UUID, teamId: UUID): Boolean {
        val village = loadVillage(villageId) ?: return false
        village.removeAlliedTeam(teamId)
        saveVillage(village)
        return true
    }

    /**
     * 添加敌对队伍
     */
    fun addHostileTeam(villageId: UUID, teamId: UUID): Boolean {
        val village = loadVillage(villageId) ?: return false
        village.addHostileTeam(teamId)
        saveVillage(village)
        return true
    }

    /**
     * 移除敌对队伍
     */
    fun removeHostileTeam(villageId: UUID, teamId: UUID): Boolean {
        val village = loadVillage(villageId) ?: return false
        village.removeHostileTeam(teamId)
        saveVillage(village)
        return true
    }

    /**
     * 将数据库行映射为 VillageData 对象
     */
    private fun mapRowToVillage(rs: ResultSet): VillageData {
        return VillageData(
            id = UUID.fromString(rs.getString("id")),
            worldName = rs.getString("world"),
            chunkX = rs.getInt("chunk_x"),
            chunkZ = rs.getInt("chunk_z"),
            minX = rs.getInt("min_x"),
            minY = rs.getInt("min_y"),
            minZ = rs.getInt("min_z"),
            maxX = rs.getInt("max_x"),
            maxY = rs.getInt("max_y"),
            maxZ = rs.getInt("max_z"),
            level = rs.getInt("level"),
            lastRaidTime = rs.getLong("last_raid_time"),
            lastLootTime = rs.getLong("last_loot_time"),
            alliedTeams = deserializeUUIDSet(rs.getString("allied_teams")),
            hostileTeams = deserializeUUIDSet(rs.getString("hostile_teams")),
            babyVillagerCount = rs.getInt("baby_villager_count"),
            unemployedVillagerCount = rs.getInt("unemployed_villager_count"),
            level1VillagerCount = rs.getInt("level1_villager_count"),
            level2VillagerCount = rs.getInt("level2_villager_count"),
            level3VillagerCount = rs.getInt("level3_villager_count"),
            level4VillagerCount = rs.getInt("level4_villager_count"),
            level5VillagerCount = rs.getInt("level5_villager_count")
        )
    }

    /**
     * 序列化 UUID Set 为 JSON 字符串
     */
    private fun serializeUUIDSet(set: Set<UUID>): String {
        if (set.isEmpty()) return "[]"
        return set.joinToString(",", "[", "]") { "\"$it\"" }
    }

    /**
     * 反序列化 JSON 字符串为 UUID Set
     */
    private fun deserializeUUIDSet(json: String?): Set<UUID> {
        if (json.isNullOrBlank() || json == "[]") return emptySet()
        return try {
            json.removeSurrounding("[", "]")
                .split(",")
                .mapNotNull { it.trim().removeSurrounding("\"").takeIf { s -> s.isNotEmpty() } }
                .map { UUID.fromString(it) }
                .toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }
}
