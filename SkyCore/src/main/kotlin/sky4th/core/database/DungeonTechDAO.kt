package sky4th.core.database

import java.sql.SQLException
import java.util.*

/**
 * 地牢科技树等级数据访问对象。
 * 读写 dungeon_tech 表，仅存储玩家当前科技树等级（进地牢时由地牢插件按 config 应用对应属性）。
 */
class DungeonTechDAO(private val databaseManager: DatabaseManager) {

    /**
     * 获取玩家科技树等级，无记录时视为 0 级返回 0。
     */
    fun getLevel(uuid: UUID): Int {
        return databaseManager.getConnection().use { conn ->
            conn.prepareStatement("SELECT tech_level FROM dungeon_tech WHERE uuid = ?").use { stmt ->
                stmt.setString(1, uuid.toString())
                stmt.executeQuery().use { rs ->
                    if (rs.next()) rs.getInt("tech_level") else DEFAULT_LEVEL
                }
            }
        }
    }

    /**
     * 设置玩家科技树等级（等级从 0 开始，不低于 0）。
     * 若玩家尚未在 player_identity 中存在，插入可能因外键约束失败，调用方需保证先有身份记录。
     */
    @Throws(SQLException::class)
    fun setLevel(uuid: UUID, level: Int) {
        val safeLevel = level.coerceAtLeast(0)
        databaseManager.getConnection().use { conn ->
            conn.prepareStatement("""
                INSERT INTO dungeon_tech (uuid, tech_level)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE tech_level = VALUES(tech_level)
            """.trimIndent()).use { stmt ->
                stmt.setString(1, uuid.toString())
                stmt.setInt(2, safeLevel)
                stmt.executeUpdate()
            }
        }
    }

    companion object {
        /** 无记录时视为的默认等级（0 级） */
        const val DEFAULT_LEVEL = 0
    }
}
