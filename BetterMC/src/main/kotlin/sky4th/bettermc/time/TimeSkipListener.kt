package sky4th.bettermc.time

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.TimeSkipEvent

class TimeSkipListener : Listener {

    @EventHandler
    fun onTimeSkip(event: TimeSkipEvent) {
        // 只处理主世界
        if (event.world.environment != org.bukkit.World.Environment.NORMAL) return

        // 取消原版的夜间跳过事件
        if (event.skipReason == TimeSkipEvent.SkipReason.NIGHT_SKIP) {
            event.isCancelled = true
        }
    }
}
