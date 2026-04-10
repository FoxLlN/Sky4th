
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack

/**
 * 渴血词条
 * 效果：攻击时恢复造成伤害的百分比生命值
 * 1级：恢复造成伤害的5%
 * 2级：恢复造成伤害的10%
 * 3级：恢复造成伤害的15%
 */
class Bloodthirst : com.sky4th.equipment.modifier.ConfiguredModifier("bloodthirst") {

    companion object {
        // 每级的生命恢复百分比
        private val CONFIG = doubleArrayOf(0.05, 0.10, 0.15)
    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(EntityDamageByEntityEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 只处理实体伤害事件，且玩家必须是攻击者
        if (event !is EntityDamageByEntityEvent || playerRole != PlayerRole.ATTACKER) {
            return
        }

        // 获取受击者
        val victim = event.entity
        if (victim !is LivingEntity) {
            return
        }

        // 获取当前等级的生命恢复百分比
        val healPercent = if (level - 1 in 0..2) CONFIG[level - 1] else return

        // 计算恢复的生命值
        val healAmount = event.damage * healPercent

        // 恢复玩家生命值（不超过最大生命值）
        val newHealth = (player.health + healAmount).coerceAtMost(player.maxHealth)
        player.health = newHealth
    }

    override fun onRemove(player: Player) {}
}
