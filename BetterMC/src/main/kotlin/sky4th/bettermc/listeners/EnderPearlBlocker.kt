package sky4th.bettermc.listeners

import sky4th.bettermc.command.FeatureManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerTeleportEvent

/**
 * 防止玩家通过末影珍珠传送到地狱基岩上层
 */
class EnderPearlBlocker : Listener {

    @EventHandler
    fun onPlayerTeleport(event: PlayerTeleportEvent) {
        if (!FeatureManager.isFeatureEnabled("ender-pearl")) return
        // 1. 仅拦截由末影珍珠引起的传送
        if (event.cause != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            return
        }

        val to = event.to
        val from = event.from

        // 2. 检查目标地点是否在地狱维度 (World.Environment.NETHER)
        if (to.world?.environment != org.bukkit.World.Environment.NETHER) {
            return
        }

        // 3. 检查传送目标是否过高 (比如 Y > 126)。这是地狱顶层基岩的常见高度。
        if (to.y > 126) {
            // 额外的安全检查：确保玩家不是从基岩上层合法下来的。
            // 如果起点和终点都在基岩上层，则允许传送。
            if (from.world?.environment == org.bukkit.World.Environment.NETHER && from.y > 126) {
                return
            }
            // 如果是从下方（Y <= 126）传送到上方（Y > 126），则判定为违规穿越。
            event.isCancelled = true
        }
    }
}
