
package sky4th.bettermc.listeners

import sky4th.bettermc.command.FeatureManager
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.vehicle.VehicleEnterEvent

/**
 * 交通工具事件监听器
 * 
 * 用于限制交通工具只能被玩家乘坐，阻止其他生物进入
 */
class VehicleListener : Listener {

    @EventHandler
    fun onVehicleEnter(event: VehicleEnterEvent) {
        if (!FeatureManager.isFeatureEnabled("vehicle")) return
        val entity = event.entered

        // 如果进入的不是玩家，则取消事件
        if (entity !is Player) {
            event.isCancelled = true
        }
    }
}
