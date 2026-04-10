package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.util.DamageTypeUtil
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack

/**
 * 坚毅词条
 * 效果：生命值大于80%时，获得5%伤害减免
 */
class Resilience : com.sky4th.equipment.modifier.ConfiguredModifier("resilience") {

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(EntityDamageEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: com.sky4th.equipment.modifier.config.PlayerRole
    ) {
        // 检查是否是玩家受到伤害
        if (event !is EntityDamageEvent || playerRole != com.sky4th.equipment.modifier.config.PlayerRole.DEFENDER) {
            return
        }

        // 检查伤害类型是否可以基础减伤
        if (!DamageTypeUtil.isBasicReduction(event)) {
            return
        }

        // 获取当前血量和最大血量
        val currentHealth = player.health
        val maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH)?.value ?: player.health

        // 计算血量百分比
        val healthPercent = currentHealth / maxHealth

        // 检查血量是否大于80%
        if (healthPercent <= 0.8) return
        
        // 获取当前基础伤害
        val damage = event.damage

        // 计算减伤后的伤害（减少5%）
        val newDamage = damage * 0.95

        // 设置基础伤害
        event.damage = newDamage
    }
}
