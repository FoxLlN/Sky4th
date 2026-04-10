
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import com.sky4th.equipment.util.PlayereffectUtil
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack

/**
 * 下界克星词条
 * 效果：对下界生物造成额外伤害
 * 1级：额外15%伤害
 * 2级：额外20%伤害
 * 3级：额外25%伤害
 */
class NetherBane : com.sky4th.equipment.modifier.ConfiguredModifier("nether_bane") {

    companion object {
        // 每级的额外伤害百分比
        private val CONFIG = doubleArrayOf(0.20, 0.25, 0.30)

        // 下界生物类型
        private val NETHER_MOBS = setOf(
            // 下界原版生物
            org.bukkit.entity.EntityType.BLAZE,
            org.bukkit.entity.EntityType.GHAST,
            org.bukkit.entity.EntityType.MAGMA_CUBE,
            org.bukkit.entity.EntityType.PIGLIN,
            org.bukkit.entity.EntityType.PIGLIN_BRUTE,
            org.bukkit.entity.EntityType.HOGLIN,
            org.bukkit.entity.EntityType.ZOGLIN,
            org.bukkit.entity.EntityType.STRIDER,
            org.bukkit.entity.EntityType.WITHER_SKELETON,
            org.bukkit.entity.EntityType.WITHER
        )
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
        if (victim !is LivingEntity) {
            return
        }

        // 检查受击者是否为下界生物
        if (!NETHER_MOBS.contains(victim.type)) {
            return
        }

        // 获取当前等级的伤害加成
        val damageBonus = if (level - 1 in 0..2) CONFIG[level - 1] else return

        // 计算新伤害
        val originalDamage = event.damage
        val newDamage = originalDamage * (1 + damageBonus)
        // 设置新伤害
        event.damage = newDamage
    }
}
