package com.sky4th.equipment.util

import org.bukkit.event.entity.EntityDamageEvent

/**
 * 伤害类型工具类
 * 用于区分不同类型的伤害
 * 
 */
object DamageTypeUtil {

    /**
     * 伤害类型枚举
     */
    enum class DamageType {
        /** 物理攻击 - 近战攻击（可闪避） */
        PHYSICAL,
        /** 弹射物攻击 - 远程攻击（可闪避） */
        PROJECTILE,
        /** 魔法伤害 */
        MAGIC,
        /** 爆炸伤害 */
        EXPLOSION,
        /** 环境伤害 - 火、岩浆等 */
        ENVIRONMENT,
        /** 状态效果伤害 - 中毒、凋零等 */
        STATUS_EFFECT,
        /** 接触伤害 **/
        CONTACT,
        /** 动能伤害 **/
        KINETIC,
        /** 其他不可闪避伤害 */
        OTHER
    }

    /**
     * 获取伤害类型
     * @param event 伤害事件
     * @return 伤害类型
     */
    fun getDamageType(event: EntityDamageEvent): DamageType {
        val cause = event.cause

        return when (cause) {
            // 物理攻击 - 可闪避
            EntityDamageEvent.DamageCause.ENTITY_ATTACK,          // 实体近战攻击（玩家、怪物）
            EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK ->  // 横扫攻击（剑的横扫）
                DamageType.PHYSICAL

            // 弹射物攻击 - 可闪避
            EntityDamageEvent.DamageCause.PROJECTILE ->           // 弹射物（箭、雪球、火球撞击等）
                DamageType.PROJECTILE

            // 魔法伤害
            EntityDamageEvent.DamageCause.MAGIC,                  // 魔法伤害（药水、唤魔者尖牙等）
            EntityDamageEvent.DamageCause.DRAGON_BREATH,          // 末影龙龙息
            EntityDamageEvent.DamageCause.SONIC_BOOM,             // 监守者音爆攻击（无视护甲，不可格挡）
            EntityDamageEvent.DamageCause.THORNS ->               // 荆棘附魔反弹的伤害
                DamageType.MAGIC

            // 爆炸伤害
            EntityDamageEvent.DamageCause.BLOCK_EXPLOSION,        // 方块爆炸（如TNT、床、重生锚等）
            EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ->     // 实体爆炸（苦力怕、恶魂火球等）
                DamageType.EXPLOSION

            // 环境伤害
            EntityDamageEvent.DamageCause.CAMPFIRE,               // 营火（包括灵魂营火）烫伤
            EntityDamageEvent.DamageCause.FIRE,                   // 直接着火（被点燃）
            EntityDamageEvent.DamageCause.FIRE_TICK,              // 持续火焰灼烧
            EntityDamageEvent.DamageCause.LAVA,                   // 接触岩浆
            EntityDamageEvent.DamageCause.HOT_FLOOR,              // 岩浆块烫伤
            EntityDamageEvent.DamageCause.LIGHTNING,              // 闪电击中
            EntityDamageEvent.DamageCause.FREEZE ->               // 细雪冻伤
                DamageType.ENVIRONMENT

            // 状态效果伤害
            EntityDamageEvent.DamageCause.POISON,                 // 中毒效果
            EntityDamageEvent.DamageCause.WITHER ->               // 凋零效果
                DamageType.STATUS_EFFECT

            // 接触伤害
            EntityDamageEvent.DamageCause.CONTACT ->                // 接触伤害（仙人掌、甜浆果丛、滴水石锥等）
                DamageType.CONTACT

            // 动能伤害
            EntityDamageEvent.DamageCause.FALL,                   // 摔落伤害
            EntityDamageEvent.DamageCause.FALLING_BLOCK,          // 下落的方块（沙子、铁砧等）砸伤
            EntityDamageEvent.DamageCause.FLY_INTO_WALL ->        // 鞘翅高速撞墙
                DamageType.KINETIC

            // 其他伤害
            EntityDamageEvent.DamageCause.CRAMMING,               // 实体挤压（因maxEntityCramming规则）
            EntityDamageEvent.DamageCause.CUSTOM,                 // 插件自定义伤害
            EntityDamageEvent.DamageCause.DROWNING,               // 溺水
            EntityDamageEvent.DamageCause.DRYOUT,                 // 脱水（如守卫者离开水）
            EntityDamageEvent.DamageCause.KILL,                   // /kill指令
            EntityDamageEvent.DamageCause.MELTING,                // 雪人融化（已弃用）
            EntityDamageEvent.DamageCause.STARVATION,             // 饥饿
            EntityDamageEvent.DamageCause.SUFFOCATION,            // 窒息（卡在方块内）
            EntityDamageEvent.DamageCause.SUICIDE,                // 自杀
            EntityDamageEvent.DamageCause.VOID,                   // 虚空伤害
            EntityDamageEvent.DamageCause.WORLD_BORDER ->         // 世界边界挤压
                DamageType.OTHER

        }
    }

    /**
     * 判断是否为物理伤害
     */
    fun isPhysicalDamage(event: EntityDamageEvent): Boolean {
        return getDamageType(event) == DamageType.PHYSICAL
    }

    /**
     * 判断是否为弹射物伤害
     */
    fun isProjectileDamage(event: EntityDamageEvent): Boolean {
        return getDamageType(event) == DamageType.PROJECTILE
    }

    /**
     * 判断是否为魔法伤害
     */
    fun isMagicDamage(event: EntityDamageEvent): Boolean {
        return getDamageType(event) == DamageType.MAGIC
    }

    /**
     * 判断是否为爆炸伤害
     */
    fun isExplosionDamage(event: EntityDamageEvent): Boolean {
        return getDamageType(event) == DamageType.EXPLOSION
    }

    /**
     * 判断是否为环境伤害
     */
    fun isEnvironmentDamage(event: EntityDamageEvent): Boolean {
        return getDamageType(event) == DamageType.ENVIRONMENT
    }

    /**
     * 判断是否为状态效果伤害
     */
    fun isStatusEffectDamage(event: EntityDamageEvent): Boolean {
        return getDamageType(event) == DamageType.STATUS_EFFECT
    }

    /**
     * 判断是否为接触伤害
     */
    fun isContactDamage(event: EntityDamageEvent): Boolean {
        return getDamageType(event) == DamageType.CONTACT
    }

    /**
     * 判断是否为动能伤害
     */
    fun isKineticDamage(event: EntityDamageEvent): Boolean {
        return getDamageType(event) == DamageType.KINETIC
    }

    /**
     * 判断是否为其他类型伤害
     */
    fun isOtherDamage(event: EntityDamageEvent): Boolean {
        return getDamageType(event) == DamageType.OTHER
    }

    /**
     * 判断伤害类型是否可闪避
     * @param event 伤害事件
     * @return 是否可闪避
     */
    fun isEvadable(event: EntityDamageEvent): Boolean {
        return when (getDamageType(event)) {
            DamageType.PHYSICAL, DamageType.PROJECTILE -> true
            else -> false
        }
    }

    /**
     * 判断伤害类型是否可以基础减伤
     * @param event 伤害事件
     * @return 是否可减伤
     */
    fun isBasicReduction(event: EntityDamageEvent): Boolean {
        return when (getDamageType(event)) {
            DamageType.PHYSICAL, DamageType.PROJECTILE, DamageType.KINETIC -> true
            else -> false
        }
    }
}
