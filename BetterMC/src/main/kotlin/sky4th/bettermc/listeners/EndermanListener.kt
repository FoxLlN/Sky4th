
package sky4th.bettermc.listeners

import sky4th.bettermc.command.FeatureManager
import org.bukkit.entity.Endermite
import org.bukkit.entity.Enderman
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityTargetEvent

/**
 * 末影人事件监听器
 * 
 * 用于限制：
 * 末影人不对末影螨产生仇恨
 */
class EndermanListener : Listener {

    @EventHandler
    fun onEntityTarget(event: EntityTargetEvent) {
        if (!FeatureManager.isFeatureEnabled("enderman")) return
        
        val entity = event.entity

        // 如果目标是末影螨，且攻击者是末影人，则取消目标
        if (entity is Enderman && event.target is Endermite) {
            event.isCancelled = true
        }
    }
}
