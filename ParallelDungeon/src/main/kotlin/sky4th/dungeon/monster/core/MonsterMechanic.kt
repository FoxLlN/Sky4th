package sky4th.dungeon.monster.core

import org.bukkit.entity.Arrow
import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageByEntityEvent

/**
 * 怪物特殊机制接口
 *
 * 每个怪物可以实现自己的特殊机制，包括：
 * - 受到伤害时的特殊处理
 * - 造成伤害时的特殊处理（近战）
 * - 造成伤害时的特殊处理（远程）
 */
interface MonsterMechanic {
    /**
     * 怪物ID
     */
    val monsterId: String

    /**
     * 当怪物受到伤害时调用
     * @param monster 受伤的怪物
     * @param event 伤害事件
     */
    fun onDamaged(monster: LivingEntity, event: EntityDamageByEntityEvent) {}

    /**
     * 当怪物近战命中目标时调用
     * @param monster 攻击的怪物
     * @param victim 被攻击的目标
     * @param event 伤害事件
     */
    fun onMeleeHit(monster: LivingEntity, victim: LivingEntity, event: EntityDamageByEntityEvent) {}

    /**
     * 当怪物远程命中目标时调用
     * @param monster 攻击的怪物
     * @param arrow 射出的箭
     * @param victim 被攻击的目标
     * @param event 伤害事件
     */
    fun onArrowHit(monster: LivingEntity, arrow: Arrow, victim: LivingEntity, event: EntityDamageByEntityEvent) {}
}
