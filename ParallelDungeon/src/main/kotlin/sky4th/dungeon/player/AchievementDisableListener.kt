
package sky4th.dungeon.player

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerAdvancementDoneEvent
import org.bukkit.event.player.PlayerStatisticIncrementEvent

/**
 * 玩家在地牢世界中不会获得成就/进度也不会触发成就/进度
 */
class AchievementDisableListener(
    private val playerManager: PlayerManager
) : Listener {

    /**
     * 阻止玩家在地牢世界中获得成就/进度
     * 通过撤销已获得的成就/进度来实现
     */
    @EventHandler
    fun onPlayerAdvancementDone(event: PlayerAdvancementDoneEvent) {
        val player = event.player
        if (playerManager.isPlayerInDungeon(player)) {
            // 撤销已获得的成就/进度
            val advancement = event.advancement
            // 移除成就/进度
            player.getAdvancementProgress(advancement).awardedCriteria.forEach { criteria ->
                player.getAdvancementProgress(advancement).revokeCriteria(criteria)
            }
        }
    }

    /**
     * 阻止玩家在地牢世界中触发统计数据的增加
     */
    @EventHandler
    fun onPlayerStatisticIncrement(event: PlayerStatisticIncrementEvent) {
        val player = event.player
        if (playerManager.isPlayerInDungeon(player)) {
            // 取消事件，阻止统计数据增加
            event.isCancelled = true
        }
    }
}
