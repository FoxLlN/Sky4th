package com.sky4th.equipment.util

import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.damage.DamageType

/**
 * 伤害原因映射工具类
 * 用于将 EntityDamageEvent.DamageCause 映射到原版的 DamageType
 */
object DamageCauseMapper {

    /**
     * 根据伤害原因获取原版伤害类型
     * @param cause 伤害原因
     * @return 伤害类型
     */
    fun mapDamageType(cause: EntityDamageEvent.DamageCause): DamageType {
        return when (cause) {
            // 物理攻击类
            EntityDamageEvent.DamageCause.ENTITY_ATTACK,
            EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK,
            EntityDamageEvent.DamageCause.PROJECTILE -> DamageType.GENERIC
            
            // 魔法/特殊伤害类
            EntityDamageEvent.DamageCause.MAGIC -> DamageType.MAGIC
            EntityDamageEvent.DamageCause.DRAGON_BREATH -> DamageType.DRAGON_BREATH
            EntityDamageEvent.DamageCause.SONIC_BOOM -> DamageType.SONIC_BOOM
            EntityDamageEvent.DamageCause.THORNS -> DamageType.THORNS
            
            // 爆炸类
            EntityDamageEvent.DamageCause.BLOCK_EXPLOSION,
            EntityDamageEvent.DamageCause.ENTITY_EXPLOSION -> DamageType.GENERIC
            
            // 火焰类
            EntityDamageEvent.DamageCause.CAMPFIRE,
            EntityDamageEvent.DamageCause.FIRE,
            EntityDamageEvent.DamageCause.FIRE_TICK,
            EntityDamageEvent.DamageCause.LAVA,
            EntityDamageEvent.DamageCause.HOT_FLOOR -> DamageType.ON_FIRE
            
            // 环境/物理类
            EntityDamageEvent.DamageCause.LIGHTNING -> DamageType.LIGHTNING_BOLT
            EntityDamageEvent.DamageCause.FREEZE -> DamageType.FREEZE
            EntityDamageEvent.DamageCause.POISON -> DamageType.MAGIC
            EntityDamageEvent.DamageCause.WITHER -> DamageType.MAGIC
            EntityDamageEvent.DamageCause.CONTACT -> DamageType.CACTUS
            EntityDamageEvent.DamageCause.FALL -> DamageType.FALL
            EntityDamageEvent.DamageCause.FALLING_BLOCK -> DamageType.FALLING_BLOCK
            EntityDamageEvent.DamageCause.FLY_INTO_WALL -> DamageType.FLY_INTO_WALL
            EntityDamageEvent.DamageCause.CRAMMING -> DamageType.CRAMMING
            
            // 窒息/溺水类（需要根据实际 DamageType 调整）
            EntityDamageEvent.DamageCause.SUFFOCATION -> DamageType.IN_WALL
            EntityDamageEvent.DamageCause.DROWNING -> DamageType.DROWN
            EntityDamageEvent.DamageCause.DRYOUT -> DamageType.DRY_OUT
            
            // 边界/虚空类
            EntityDamageEvent.DamageCause.VOID -> DamageType.GENERIC
            EntityDamageEvent.DamageCause.WORLD_BORDER -> DamageType.GENERIC
            
            // 其他
            EntityDamageEvent.DamageCause.STARVATION -> DamageType.STARVE
            EntityDamageEvent.DamageCause.SUICIDE,
            EntityDamageEvent.DamageCause.MELTING,
            EntityDamageEvent.DamageCause.KILL,
            EntityDamageEvent.DamageCause.CUSTOM -> DamageType.GENERIC
        }
    }
}
