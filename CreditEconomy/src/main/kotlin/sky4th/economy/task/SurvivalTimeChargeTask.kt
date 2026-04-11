package sky4th.economy.task

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import sky4th.core.api.PlayerAPI
import sky4th.economy.CreditEconomy
import sky4th.economy.config.EconomyConfig
import sky4th.economy.manager.EconomyManager

/**
 * 存活时长扣费任务
 * 
 * 每分钟执行一次，对所有在线玩家进行存活时长扣费
 */
class SurvivalTimeChargeTask : BukkitRunnable() {

    companion object {
        /**
         * 启动每分钟扣费任务
         * 
         * @param plugin 插件实例
         * @return 任务ID
         */
        fun start(plugin: CreditEconomy): Int {
            val task = SurvivalTimeChargeTask()
            return task.runTaskTimer(plugin, 20L, 1200L).taskId // 20 ticks = 1秒延迟开始，1200 ticks = 60秒间隔
        }
    }

    override fun run() {
        // 遍历所有在线玩家
        Bukkit.getOnlinePlayers().forEach { player ->
            try {
                // 获取玩家当前存活小时数
                val hours = PlayerAPI.getPlayerData(player)
                    ?.identity
                    ?.currentLifePlayTime
                    ?.toMinutes()
                    ?.div(60)
                    ?.toInt() ?: 0
                // 执行每分钟扣费
                handleSurvivalTimeMinuteCharge(player, hours)
            } catch (e: Exception) {
                // 记录错误，但不影响其他玩家
                e.printStackTrace()
            }
        }
    }

    
    /**
     * 处理存活时长扣费（每分钟扣费）
     * 注意：这个方法应该由每分钟执行的任务调用
     *
     * @param player 玩家对象
     * @param hours 当前存活小时数
     */
    private fun handleSurvivalTimeMinuteCharge(player: Player, hours: Int) {
        // 检查是否应该扣费
        //if (!EconomyConfig.shouldCharge(hours)) return

        // 计算扣费比例
        val rate = EconomyConfig.getChargeRate(hours)

        // 计算每分钟基础扣费金额（不乘rate，避免重复计算）
        val minuteChargeAmount = EconomyConfig.getMinuteRate(hours)

        // 获取扣费原因
        val reason = "存活时长扣费（第${hours}小时，每分钟${minuteChargeAmount}信用点）"
        println(reason)
        // 执行强制扣费（不检查余额）
        EconomyManager.forceCharge(
            player = player,
            amount = minuteChargeAmount,
            reason = reason,
            rate = rate
        )
    }
}
