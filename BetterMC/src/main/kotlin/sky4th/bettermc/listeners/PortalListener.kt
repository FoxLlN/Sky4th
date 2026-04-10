
package sky4th.bettermc.listeners

import sky4th.bettermc.command.FeatureManager
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityPortalEvent
import org.bukkit.event.world.PortalCreateEvent

/**
 * 传送门事件监听器
 * 
 * 用于限制：
 * 1. 只有玩家才能通过传送门
 * 2. 无法创建地狱传送门
 */
class PortalListener : Listener {

    @EventHandler
    fun onEntityPortal(event: EntityPortalEvent) {
        if (!FeatureManager.isFeatureEnabled("portal")) return
        val entity = event.entity

        // 如果通过传送门的不是玩家，则取消事件
        if (entity !is Player) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPortalCreate(event: PortalCreateEvent) {
        if (!FeatureManager.isFeatureEnabled("portal")) return
        // 取消所有传送门的创建
        event.isCancelled = true
    }
}
