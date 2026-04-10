package sky4th.core.service

import sky4th.core.database.DatabaseManager
import sky4th.core.database.DungeonTechDAO
import java.util.*

/**
 * 地牢科技树等级服务。
 * 供地牢插件等调用，仅读写当前等级；等级对应属性由地牢 config 配置，进地牢时按等级应用。
 */
class DungeonTechService(private val databaseManager: DatabaseManager) {

    private val dao = DungeonTechDAO(databaseManager)

    /**
     * 获取玩家科技树等级，无记录时视为 0 级返回 0。
     */
    fun getTechLevel(uuid: UUID): Int = dao.getLevel(uuid)

    /**
     * 设置玩家科技树等级（等级从 0 开始，不低于 0）。
     * 玩家需已在 Core 中有身份记录（已登录过），否则可能因外键约束写入失败。
     */
    fun setTechLevel(uuid: UUID, level: Int) {
        dao.setLevel(uuid, level.coerceAtLeast(0))
    }

    /**
     * 增加玩家科技树等级。无记录时按 0 级为基准加上增量。
     * @param amount 增加的等级数（可为负，结果不低于 0）
     * @return 增加后的等级；写入失败时返回 -1
     */
    fun addTechLevel(uuid: UUID, amount: Int): Int {
        val current = dao.getLevel(uuid)
        val newLevel = (current + amount).coerceAtLeast(0)
        return try {
            dao.setLevel(uuid, newLevel)
            newLevel
        } catch (e: Exception) {
            -1
        }
    }
}
