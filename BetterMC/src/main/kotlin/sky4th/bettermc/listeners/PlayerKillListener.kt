package sky4th.bettermc.listeners

import sky4th.bettermc.command.FeatureManager
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import java.util.UUID

/**
 * 玩家击杀监听器
 * 
 * 用于追踪哪些生物被玩家攻击过，只有被玩家攻击过的生物才会掉落物品和经验
 */
class PlayerKillListener : Listener {

    // 存储被玩家攻击过的实体ID
    private val attackedEntities = mutableSetOf<UUID>()

    @EventHandler
    fun onEntityDamage(event: EntityDamageByEntityEvent) {
        // 检查攻击者是否是玩家
        if (event.damager is Player && event.entity is LivingEntity) {
            // 将被攻击的实体ID添加到集合中
            attackedEntities.add(event.entity.uniqueId)
        }
    }

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        val entity = event.entity

        // 如果实体没有被玩家攻击过，清除所有掉落物和经验
        if (!attackedEntities.contains(entity.uniqueId)) {
            if (!FeatureManager.isFeatureEnabled("player-kill")) return
            event.drops.clear()
            event.droppedExp = 0
        }

        // 从集合中移除已死亡的实体ID
        attackedEntities.remove(entity.uniqueId)
    }
}
