package sky4th.dungeon.player

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent

/**
 * 处理玩家进出服务器时与副本相关的逻辑
 */
class PlayerListener(
    private val playerManager: PlayerManager
) : Listener {

    /**
     * 玩家主动离开服务器时：
     * 如果处在副本世界中，先送回原来的世界位置，
     * 确保下次上线不会还卡在副本里。
     */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        if (playerManager.isPlayerInDungeon(player)) {
            playerManager.teleportFromDungeon(player, false)
        }
    }

    /**
     * 被踢出服务器的情况也一并处理，逻辑同上。
     */
    @EventHandler
    fun onPlayerKick(event: PlayerKickEvent) {
        val player = event.player
        if (playerManager.isPlayerInDungeon(player)) {
            playerManager.teleportFromDungeon(player, false)
        }
    }
}
