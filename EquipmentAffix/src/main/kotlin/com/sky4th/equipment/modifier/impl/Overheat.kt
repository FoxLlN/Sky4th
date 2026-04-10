
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import com.sky4th.equipment.util.PlayereffectUtil
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack

/**
 * 过热词条
 * 效果：对处于点燃状态下的实体造成更高伤害
 * 1级：额外10%伤害
 * 2级：额外15%伤害
 * 3级：额外20%伤害
 */
class Overheat : com.sky4th.equipment.modifier.ConfiguredModifier("overheat") {

    companion object {
        // 每级的额外伤害百分比
        private val CONFIG = doubleArrayOf(0.10, 0.15, 0.20)
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

        // 检查受击者是否处于点燃状态
        if (victim.fireTicks <= 0) {
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
