package sky4th.core.scoreboard

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

/**
 * 计分板监听器
 * 处理玩家加入和退出事件
 */
class ScoreboardListener : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        // 为玩家创建计分板
        ScoreboardManager.createScoreboard(player)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        // 移除玩家的计分板
        ScoreboardManager.removeScoreboard(player)
    }
}
