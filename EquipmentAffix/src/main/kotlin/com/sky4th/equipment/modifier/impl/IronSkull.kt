
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import com.sky4th.equipment.util.HitPart
import com.sky4th.equipment.util.HitPartUtil
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack

/**
 * 铁颅词条
 * 效果：减少头部受到的伤害
 * 1级：头部受到的伤害减少10%
 * 2级：头部受到的伤害减少15%
 * 3级：头部受到的伤害减少20%
 * 仅适用于重型头盔
 */
class IronSkull : com.sky4th.equipment.modifier.ConfiguredModifier("iron_skull") {

    companion object {
        // 每级减伤比例 (索引0对应1级，索引1对应2级，索引2对应3级)
        private val CONFIG = doubleArrayOf(0.10, 0.15, 0.20)
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
        // 检查是否是玩家受到伤害
        if (event !is EntityDamageByEntityEvent || playerRole != PlayerRole.DEFENDER) {
            return
        }

        // 获取命中部位
        val hitPart = HitPartUtil.getHitPart(event)

        // 只处理头部伤害
        if (hitPart != HitPart.HEAD) {
            return
        }

        // 获取当前等级的减伤比例
        val reduction = CONFIG.getOrNull(level - 1) ?: return

        // 获取当前伤害
        val damage = event.damage

        // 计算减伤后的伤害
        val newDamage = damage * (1 - reduction)

        // 设置新伤害
        event.damage = newDamage
    }
}
