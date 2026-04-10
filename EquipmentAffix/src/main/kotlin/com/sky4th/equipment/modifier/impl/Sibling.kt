
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack

/**
 * 手足词条
 * 效果：增加对玩家造成伤害
 * 1级：对玩家造成额外40%伤害
 * 2级：对玩家造成额外60%伤害
 * 3级：对玩家造成额外80%伤害
 */
class Sibling : com.sky4th.equipment.modifier.ConfiguredModifier("sibling") {

    companion object {
        // 每级的额外伤害百分比
        private val CONFIG = doubleArrayOf(0.40, 0.60, 0.80)
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
        // 只对玩家生效
        if (victim !is Player) {
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

    override fun onRemove(player: Player) {}
}
