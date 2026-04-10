package sky4th.core.api

import org.bukkit.entity.Player
import sky4th.core.SkyCore
import java.util.UUID

/**
 * 地牢对外 API（科技树等级等）。
 * 等级从 0 开始，无记录视为 0 级；服务不可用时返回 -1。
 */
object DungeonAPI {

    /** 地牢科技树服务是否可用 */
    fun isAvailable(): Boolean = SkyCore.getDungeonTechService() != null

    /**
     * 读取玩家当前科技树等级。无记录视为 0 级；服务不可用时返回 -1。
     */
    fun getTechLevel(uuid: UUID): Int = SkyCore.getDungeonTechService()?.getTechLevel(uuid) ?: -1

    /**
     * 读取玩家当前科技树等级（按玩家对象）。无记录视为 0 级；服务不可用时返回 -1。
     */
    fun getTechLevel(player: Player): Int = getTechLevel(player.uniqueId)

    /**
     * 增加玩家科技树等级。无记录时按 0 级为基准加上增量，结果不低于 0。
     * @param amount 增加的等级数（可为负，最终等级不低于 0）
     * @return 增加后的等级；服务不可用或写入失败时返回 -1
     */
    fun addTechLevel(uuid: UUID, amount: Int): Int =
        SkyCore.getDungeonTechService()?.addTechLevel(uuid, amount) ?: -1

    /**
     * 增加玩家科技树等级（按玩家对象）。
     */
    fun addTechLevel(player: Player, amount: Int): Int = addTechLevel(player.uniqueId, amount)
}
