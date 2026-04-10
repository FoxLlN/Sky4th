package sky4th.core.api

import sky4th.core.model.PlayerData
import sky4th.core.util.EconomyUtils
import org.bukkit.Location
import org.bukkit.entity.Player

/**
 * 费用计算 API
 *
 * 提供各种游戏内费用计算的统一接口
 *
 * **设计原则：**
 * - 只负责计算费用（纯函数，不涉及实际扣除）
 * - 不依赖具体的经济实现
 * - 实际的经济操作（扣除、添加）通过 EconomyAPI 完成
 *
 * **费用规则来源：**
 * - 基础费用规则：EconomyUtils
 * - 完整计算逻辑：PlayerData（考虑折扣、保护等）
 */
object CostCalculationAPI {

    /**
     * 计算传送费用
     * @param from 起始位置
     * @param to 目标位置
     * @param isCrossDimension 是否跨维度
     * @return 传送费用
     *
     * 费用规则：
     * - 普通传送：5 + 距离 * 0.01
     * - 跨维度传送：10 + 距离 * 0.03
     */
    fun calculateTeleportCost(
        from: Location,
        to: Location,
        isCrossDimension: Boolean
    ): Double {
        return EconomyUtils.calculateTeleportCost(from, to, isCrossDimension)
    }

    /**
     * 计算传送费用（自动判断是否跨维度）
     * @param from 起始位置
     * @param to 目标位置
     * @return 传送费用
     */
    fun calculateTeleportCost(from: Location, to: Location): Double {
        val isCrossDimension = from.world != to.world
        return calculateTeleportCost(from, to, isCrossDimension)
    }

    /**
     * 计算团队传送费用
     * @param baseCost 基础费用
     * @param memberCount 团队成员数量
     * @return 总费用
     *
     * 折扣规则：
     * - 2人：9折
     * - 3-4人：8折
     * - 5人+：7折
     */
    fun calculateTeamTeleportCost(baseCost: Double, memberCount: Int): Double {
        return EconomyUtils.calculateTeamTeleportCost(baseCost, memberCount)
    }

    /**
     * 计算并尝试扣除传送费用（便捷方法）
     * 注意：实际扣除操作通过 EconomyAPI 完成
     *
     * @param player 玩家对象
     * @param from 起始位置
     * @param to 目标位置
     * @return 是否成功扣除
     */
    fun tryChargeTeleport(player: Player, from: Location, to: Location): Boolean {
        val cost = calculateTeleportCost(from, to)
        return EconomyAPI.withdraw(player, cost)
    }
}
