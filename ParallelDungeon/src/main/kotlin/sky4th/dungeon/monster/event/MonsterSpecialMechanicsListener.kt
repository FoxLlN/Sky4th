package sky4th.dungeon.monster.event

import sky4th.dungeon.monster.core.MonsterMechanicRegistry
import org.bukkit.entity.Arrow
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.plugin.Plugin

/**
 * 怪物特殊机制监听器
 * 
 * 负责监听实体伤害事件，并将事件分发到对应怪物的特殊机制处理器
 */
class MonsterSpecialMechanicsListener(
    private val plugin: Plugin
) : Listener {

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val victim = event.entity as? LivingEntity ?: return

        // 处理受击方的特殊机制
        MonsterMechanicRegistry.handleDamaged(victim, event)

        // 处理攻击方的特殊机制
        when (val damager = event.damager) {
            is LivingEntity -> MonsterMechanicRegistry.handleMeleeHit(damager, victim, event)
            is Arrow -> MonsterMechanicRegistry.handleArrowHit(damager, victim, event)
        }
    }
}
