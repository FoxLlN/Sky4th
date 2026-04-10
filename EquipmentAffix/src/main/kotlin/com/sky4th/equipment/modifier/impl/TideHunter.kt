
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack

/**
 * 猎潮词条
 * 效果：对水生生物造成额外伤害
 * 1级：额外30%伤害
 * 2级：额外40%伤害
 * 3级：额外50%伤害
 */
class TideHunter : com.sky4th.equipment.modifier.ConfiguredModifier("tide_hunter") {

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

        // 每级的额外伤害百分比
        private val DAMAGE_BONUS = doubleArrayOf(0.30, 0.40, 0.50)
    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(EntityDamageEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 只处理实体伤害事件，且玩家必须是攻击者
        if (event !is EntityDamageEvent || playerRole != PlayerRole.ATTACKER) {
            return
        }

        // 检查是否是实体攻击实体事件
        if (event !is org.bukkit.event.entity.EntityDamageByEntityEvent) {
            return
        }

        // 获取受击者
        val victim = event.entity
        if (victim !is org.bukkit.entity.LivingEntity) {
            return
        }

        // 检查受击者是否为水生生物
        if (!AQUATIC_MOBS.contains(victim.type)) {
            return
        }

        // 获取当前等级的伤害加成
        val damageBonus = if (level - 1 in 0..2) DAMAGE_BONUS[level - 1] else return

        // 计算新伤害
        val originalDamage = event.damage
        val newDamage = originalDamage * (1 + damageBonus)

        // 设置新伤害
        event.damage = newDamage
    }
}
