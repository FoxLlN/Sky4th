package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import com.sky4th.equipment.util.DamageTypeUtil
import com.sky4th.equipment.util.PlayereffectUtil
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack

/**
 * 威压词条
 * 效果：周围5格内的敌对生物造成的所有伤害降低8%
 */
class Coercion : com.sky4th.equipment.modifier.ConfiguredModifier("coercion") {

    override fun getEventTypes(): List<Class<out org.bukkit.event.Event>> =
        listOf(EntityDamageByEntityEvent::class.java)

    override fun handle(
        event: org.bukkit.event.Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: com.sky4th.equipment.modifier.config.PlayerRole
    ) {
        // 检查是否是玩家受到伤害
        if (event !is EntityDamageByEntityEvent || playerRole != PlayerRole.DEFENDER) {
            return
        }

        // 检查伤害是否有来源
        val damager = event.damager

        // 检查敌对生物是否在威压范围内
        val distance = damager.location.distance(player.location)
        if (distance > 5.0) {
            return
        }

        // 获取当前伤害
        val damage = event.damage

        // 计算减伤后的伤害（降低8%）
        val newDamage = damage * (1 - 0.08)

        // 设置伤害
        event.damage = newDamage
    }
}
