package sky4th.bettervillage.listeners

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.raid.RaidFinishEvent
import org.bukkit.event.raid.RaidTriggerEvent
import sky4th.bettervillage.manager.VillageManager

/**
 * 劫掠事件监听器
 * 
 * 确保劫掠只会在村庄结构中产生,不会在其他地方触发
 */
class RaidListener : Listener {

    /**
     * 监听劫掠触发事件
     * 
     * 当劫掠被触发时,检查触发位置是否在村庄结构内
     * 如果不在村庄结构内,则取消该事件
     */
    @EventHandler
    fun onRaidTrigger(event: RaidTriggerEvent) {
        val location = event.raid.location
        // 获取村庄，如果不存在则取消劫掠事件
        val village = VillageManager.getVillageByLocation(location)
        if (village == null) {
            event.isCancelled = true
            return
        }
        VillageManager.updateRaidTime(village.id)
    }

    /**
     * 监听劫掠结束事件
     *
     * 当劫掠结束时,更新村庄的劫掠时间
     */
    @EventHandler
    fun onRaidFinish(event: RaidFinishEvent) {
        val location = event.raid.location
        val village = VillageManager.getVillageByLocation(location)
        if (village != null) {
            VillageManager.updateRaidTime(village.id)
        }
    }
}
