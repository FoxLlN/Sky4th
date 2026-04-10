package sky4th.core.model

import java.util.*

/**
 * 玩家属性数据模型
 * 只存储需要持久化的核心属性
 * 其他属性直接从MC服务器获取
 */
data class PlayerAttributes(
    val uuid: UUID,
    val dodge: Double = 0.0,                   // 闪避率 (0.0-1.0)
    val hungerConsumptionMultiplier: Double = 0.0,  // 饥饿消耗倍率
    val tradeDiscount: Double = 0.0,               // 交易折扣 (0.0-1.0)
    val forgingSuccessRate: Double = 0.0,          // 锻造成功率 (0.0-1.0)
    val expGainMultiplier: Double = 0.0,           // 经验获取加成
    val talents: List<String> = emptyList()        // 天赋列表
    // 以下属性已移除，直接从MC服务器获取
    // val maxHealth: Double = 0.0              // 最大生命值
    // val armor: Double = 0.0                   // 护甲值
    // val knockbackResistance: Double = 0.0     // 击退抗性
    // val movementSpeedMultiplier: Double = 0.0      // 移动速度倍率
    // val attackDamage: Double = 0.0                // 攻击伤害
    // val attackSpeed: Double = 0.0                 // 攻击速度
    // val armorToughness: Double = 0.0             // 护甲韧性
    // val luck: Double = 0.0                        // 幸运值
    // val fallDamageMultiplier: Double = 0.0        // 坠落伤害倍率
    // val maxAbsorption: Double = 0.0               // 最大吸收生命值
    // val safeFallDistance: Double = 0.0            // 安全坠落距离
    // val scale: Double = 0.0                       // 模型缩放
    // val stepHeight: Double = 0.0                  // 跨越高度
    // val gravity: Double = 0.0                    // 重力
    // val jumpStrength: Double = 0.0                // 跳跃力度
    // val burningTime: Double = 0.0                  // 燃烧时间
    // val explosionKnockbackResistance: Double = 0.0  // 爆炸击退抗性
    // val movementEfficiency: Double = 0.0           // 移动效率
    // val oxygenBonus: Double = 0.0                  // 氧气加成
    // val waterMovementEfficiency: Double = 0.0       // 水中移动效率
    // val blockBreakSpeed: Double = 0.0              // 方块破坏速度
    // val blockInteractionRange: Double = 0.0       // 方块交互范围
    // val entityInteractionRange: Double = 0.0       // 实体交互范围
    // val sneakingSpeed: Double = 0.0                // 潜行速度
    // val submergedMiningSpeed: Double = 0.0          // 水下挖掘速度
    // val sweepingDamageRatio: Double = 0.0          // 横扫伤害比例
) {
    companion object {
        /**
         * 创建新玩家的默认属性
         */
        fun createDefault(uuid: UUID): PlayerAttributes {
            return PlayerAttributes(
                uuid = uuid,
                dodge = 0.0,
                hungerConsumptionMultiplier = 0.0,
                tradeDiscount = 0.0,
                forgingSuccessRate = 0.0,
                expGainMultiplier = 0.0,
                talents = emptyList()
            )
        }
    }
}
