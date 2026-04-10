
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack

/**
 * 导热词条
 * 效果：自身点燃时，造成的伤害+15%/20%/25%
 * 1级：额外15%伤害
 * 2级：额外20%伤害
 * 3级：额外25%伤害
 */
class ThermalConduct : com.sky4th.equipment.modifier.ConfiguredModifier("thermal_conduct") {

    companion object {
        // 每级的额外伤害百分比
        private val CONFIG = doubleArrayOf(0.15, 0.20, 0.25)
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

        // 检查玩家是否处于点燃状态
        if (player.fireTicks <= 0) {
            return
        }

        // 获取受击者
        val victim = event.entity
        if (victim !is LivingEntity) {
            return
        }

        // 获取当前等级的伤害加成
        val damageBonus = if (level - 1 in 0..2) CONFIG[level - 1] else return

        // 计算新伤害
        val originalDamage = event.damage
        val newDamage = originalDamage * (1 + damageBonus)
        event.damage = newDamage
    }
}
