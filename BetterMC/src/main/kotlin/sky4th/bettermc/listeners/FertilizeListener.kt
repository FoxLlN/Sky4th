package sky4th.bettermc.listeners

import sky4th.bettermc.command.FeatureManager
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockFertilizeEvent

/**
 * 骨粉催熟事件监听器
 *
 * 用于限制骨粉对特定方块的催熟效果
 */
class FertilizeListener : Listener {

    @EventHandler
    fun onBoneMeal(event: BlockFertilizeEvent) {
        if (!FeatureManager.isFeatureEnabled("fertilize")) return
        // 检查被施肥的方块是否是苔藓块
        if (event.block.type == Material.MOSS_BLOCK) {
            // 取消事件，禁止催熟苔藓块
            event.isCancelled = true
        }
    }
}
