package sky4th.dungeon.monster.core

import org.bukkit.entity.Arrow
import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageByEntityEvent
import sky4th.dungeon.monster.core.MonsterMetadata

/**
 * 怪物特殊机制注册表
 *
 * 用于管理所有怪物的特殊机制，统一处理事件分发
 */
object MonsterMechanicRegistry {

    private val mechanics: MutableMap<String, MonsterMechanic> = mutableMapOf()

    /**
     * 注册怪物的特殊机制
     */
    fun register(mechanic: MonsterMechanic) {
        mechanics[mechanic.monsterId.lowercase()] = mechanic
    }

    /**
     * 获取怪物的特殊机制
     */
    fun get(monsterId: String): MonsterMechanic? {
        return mechanics[monsterId.lowercase()]
    }

    /**
     * 处理怪物受到伤害的事件
     */
    fun handleDamaged(monster: LivingEntity, event: EntityDamageByEntityEvent) {
        val monsterId = MonsterMetadata.getMonsterId(monster) ?: return
        val mechanic = get(monsterId) ?: return
        mechanic.onDamaged(monster, event)
    }

    /**
     * 处理怪物近战命中目标的事件
     */
    fun handleMeleeHit(monster: LivingEntity, victim: LivingEntity, event: EntityDamageByEntityEvent) {
        val monsterId = MonsterMetadata.getMonsterId(monster) ?: return
        val mechanic = get(monsterId) ?: return
        mechanic.onMeleeHit(monster, victim, event)
    }

    /**
     * 处理怪物远程命中目标的事件
     */
    fun handleArrowHit(arrow: Arrow, victim: LivingEntity, event: EntityDamageByEntityEvent) {
        val shooter = arrow.shooter as? LivingEntity ?: return
        val monsterId = MonsterMetadata.getMonsterId(shooter) ?: return
        val mechanic = get(monsterId) ?: return
        mechanic.onArrowHit(shooter, arrow, victim, event)
    }
}
