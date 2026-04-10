package sky4th.bettermc.drops

import sky4th.bettermc.command.FeatureManager
import org.bukkit.entity.Ageable
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent

/**
 * 动物掉落物事件监听器
 * 
 * 用于在动物死亡时添加额外掉落物
 * 额外掉落物必定掉落，不受附魔影响
 */
class DropListener : Listener {

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH)
    fun onEntityDeath(event: EntityDeathEvent) {
        if (!FeatureManager.isFeatureEnabled("drops")) return
        val entity = event.entity

        // 检查是否是可成长的生物（动物）
        if (entity !is Ageable) return
        // 获取实体类型
        val entityType = entity.type

        // 获取额外掉落物（必定掉落，不受附魔影响）
        val extraDrops = DropManager.getExtraDrops(entityType)

        // 直接添加额外掉落物到掉落列表
        // 这些掉落物必定掉落，不受任何附魔影响
        extraDrops.forEach { drop ->
            event.drops.add(drop.clone())
        }
    }
}
