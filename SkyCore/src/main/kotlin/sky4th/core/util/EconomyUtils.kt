package sky4th.core.util

import sky4th.core.economy.EconomyService
import sky4th.core.model.PlayerData
import org.bukkit.Location
import org.bukkit.entity.Player

/**
 * 经济系统工具类
 * 提供便捷的经济操作方法
 */
object EconomyUtils {

    /**
     * 计算传送费用
     * @param from 起始位置
     * @param to 目标位置
     * @param isCrossDimension 是否跨维度
     * @return 传送费用
     */
    fun calculateTeleportCost(
        from: Location,
        to: Location,
        isCrossDimension: Boolean
    ): Double {
        val distance = if (isCrossDimension) {
            // 跨维度传送
            when {
                from.world?.name == "world" && to.world?.name == "world_nether" -> {
                    // 主世界到地狱：使用主世界距离
                    from.distance(to) * 8.0 // 地狱坐标是主世界的1/8
                }
                from.world?.name == "world" && to.world?.name == "world_the_end" -> {
                    // 主世界到末地：距离为0与目标距离
                    to.distance(to.world?.spawnLocation ?: to)
                }
                else -> from.distance(to)
            }
        } else {
            // 同维度传送
            from.distance(to)
        }

        return if (isCrossDimension) {
            10.0 + distance * 0.03
        } else {
            5.0 + distance * 0.01
        }
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
        val discount = when {
            memberCount == 2 -> 0.9  // 2人9折
            memberCount in 3..4 -> 0.8  // 3-4人8折
            memberCount >= 5 -> 0.7  // 5人+7折
            else -> 1.0
        }

        return baseCost * memberCount * discount
    }

    /**
     * 尝试扣除费用（如果余额足够）
     * @param player 玩家
     * @param amount 金额
     * @return 是否成功扣除
     */
    fun tryCharge(player: Player, amount: Double): Boolean {
        if (!EconomyService.hasEnough(player, amount)) {
            return false
        }
        return EconomyService.withdraw(player, amount)
    }

    /**
     * 给玩家奖励信用点（考虑每日上限）
     * @param player 玩家
     * @param amount 金额
     * @return 实际奖励的金额
     */
    fun reward(player: Player, amount: Double): Double {
        return EconomyService.deposit(player, amount)
    }
}
