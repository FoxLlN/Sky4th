package sky4th.economy.listener

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import sky4th.core.event.PlayTimeHourEvent
import sky4th.economy.config.EconomyConfig
import sky4th.economy.manager.EconomyManager

/**
 * 游玩时间经济监听器
 * 
 * 监听 PlayTimeHourEvent 事件，处理基于游玩时间的经济变动
 * 包括：
 * - 存活时长扣费
 * - 每日时长奖励
 */
class PlayTimeEconomyListener : Listener {

    @EventHandler
    fun onPlayTimeHour(event: PlayTimeHourEvent) {
        // 通过 PlayTimeProcessor 处理游玩时间事件
        val player = event.player
        val hours = event.toHour

        when (event.timeType) {
            PlayTimeHourEvent.TimeType.DAILY -> {
                // 处理每日时长奖励
                // 检查是否有奖励
                if (!EconomyConfig.hasDailyReward(hours)) {
                    return
                }
                // 计算奖励金额
                val rewardAmount = EconomyConfig.getDailyReward(hours)

                // 获取奖励原因
                val reason = "${hours} 时长奖励"

                // 发放奖励
                EconomyManager.reward(player, rewardAmount, reason)
            }  
            else -> {
                return
            }
        }
    }
}
