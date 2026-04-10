
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.MaterialEffectModifier
import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack

/**
 * 水生材质词条
 * 效果：受到水生生物攻击时，伤害降低15%
 * 水生生物包括：守卫者、远古守卫者、溺尸、各类鱼类等
 */
class Aquatic : com.sky4th.equipment.modifier.ConfiguredModifier("aquatic"), MaterialEffectModifier {

    companion object {
        // 水生生物类型
        private val AQUATIC_MOBS = setOf(
            // 守卫者类型
            org.bukkit.entity.EntityType.GUARDIAN,
            org.bukkit.entity.EntityType.ELDER_GUARDIAN,
            // 溺尸
            org.bukkit.entity.EntityType.DROWNED,
            // 鱼类
            org.bukkit.entity.EntityType.COD,
            org.bukkit.entity.EntityType.SALMON,
            org.bukkit.entity.EntityType.PUFFERFISH,
            org.bukkit.entity.EntityType.TROPICAL_FISH,
            // 其他水生生物
            org.bukkit.entity.EntityType.TURTLE,
            org.bukkit.entity.EntityType.DOLPHIN,
            org.bukkit.entity.EntityType.SQUID,
            org.bukkit.entity.EntityType.GLOW_SQUID
        )

        // 伤害减免比例（15%）
        private const val DAMAGE_REDUCTION = 0.15
    }

    override fun getMaterialEffectId(): String = "aquatic"

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(EntityDamageEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 只处理玩家受到伤害的事件
        if (event !is EntityDamageEvent || playerRole != PlayerRole.DEFENDER) {
            return
        }

        // 检查是否是实体攻击实体事件
        if (event !is org.bukkit.event.entity.EntityDamageByEntityEvent) {
            return
        }

        // 获取攻击者
        val attacker = event.damager
        if (attacker !is org.bukkit.entity.LivingEntity) {
            return
        }

        // 检查攻击者是否为水生生物
        if (!AQUATIC_MOBS.contains(attacker.type)) {
            return
        }

        // 计算减免后的伤害
        val originalDamage = event.damage
        val reducedDamage = originalDamage * (1 - DAMAGE_REDUCTION)

        // 设置新伤害
        event.damage = reducedDamage
    }
}
