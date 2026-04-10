
package sky4th.bettermc.listeners

import org.bukkit.Material
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import sky4th.bettermc.command.FeatureManager

/**
 * 穿草攻击监听器
 * 
 * 允许玩家手持武器右键点击草类植物时，攻击其中的实体
 */
class GrassAttackListener : Listener {

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        // 检查功能是否启用
        if (!FeatureManager.isFeatureEnabled("grass-attack")) return

        // 只处理左键点击方块
        if (event.action != Action.LEFT_CLICK_BLOCK) return
        // 防止双击触发：只处理主手点击
        if (event.hand != org.bukkit.inventory.EquipmentSlot.HAND) {
            return
        }

        val block = event.clickedBlock ?: return
        val material = block.type

        // 检查是否是草类植物（高草、蕨类等）
        if (!isGrassMaterial(material)) return

        val player = event.player

        // 使用玩家视线方向检测实体
        val target = player.getTargetEntity(5, true) as? LivingEntity

        // 检查目标实体是否在点击的草方块内
        if (target != null) {
            // 获取草方块的坐标范围
            val blockX = block.x.toDouble()
            val blockZ = block.z.toDouble()
            
            // 获取实体位置
            val entityLoc = target.location
            
            // 检查实体是否在草方块的坐标范围内（X和Z在0-1之间）
            if (entityLoc.x < blockX || entityLoc.x >= blockX + 1 ||
                entityLoc.z < blockZ || entityLoc.z >= blockZ + 1 ) {
                return
            }
            // 取消原事件，防止破坏草或放置方块
            event.isCancelled = true

            // 模拟原版攻击：使用 player.attack(target) 会触发伤害事件、击退、音效等
            player.attack(target)

        }
    }

    private fun isGrassMaterial(material: Material): Boolean {
        return when (material) {
            Material.TALL_GRASS,
            Material.SHORT_GRASS,
            Material.FERN,
            Material.LARGE_FERN,
            Material.TALL_SEAGRASS,
            Material.SEAGRASS -> true
            else -> false
        }
    }
}
