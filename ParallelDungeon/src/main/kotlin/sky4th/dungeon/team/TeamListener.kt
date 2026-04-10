
package sky4th.dungeon.team

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import sky4th.dungeon.player.PlayerManager

/**
 * 队伍事件监听器
 * 处理玩家退出地牢时的队伍数据清理
 */
class TeamListener(
    private val teamManager: TeamManager,
    private val playerManager: PlayerManager
) : Listener {

    /**
     * 玩家退出事件
     * 清理玩家的队伍数据
     */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player

        // 检查玩家是否在地牢中
        if (!playerManager.isPlayerInDungeon(player)) {
            return
        }

        // 清理玩家的队伍数据
        teamManager.clearPlayerData(player.uniqueId)
    }
}
