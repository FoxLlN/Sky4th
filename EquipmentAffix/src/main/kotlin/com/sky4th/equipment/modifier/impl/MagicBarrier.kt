package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import com.sky4th.equipment.util.DamageTypeUtil
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack

/**
 * 魔法屏障词条
 * 效果：受到魔法攻击时，获得魔法减免
 * 每级减免4%伤害（12级48%）
 */
class MagicBarrier : com.sky4th.equipment.modifier.ConfiguredModifier("magic_barrier") {

    companion object {
        // 每级减免4%伤害
        private const val DAMAGE_REDUCTION_PER_LEVEL = 0.04

        // 最大等级
        private const val MAX_LEVEL = 12
    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(EntityDamageEvent::class.java)

    override fun handle(
        event: Event,
        player: org.bukkit.entity.Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 只处理玩家受到伤害的事件
        if (event !is EntityDamageEvent || playerRole != PlayerRole.DEFENDER) {
            return
        }

        // 检查伤害类型是否为魔法伤害
        if (!DamageTypeUtil.isMagicDamage(event)) {
            return
        }

        // 限制等级不超过最大等级
        val effectiveLevel = level.coerceAtMost(MAX_LEVEL)

        // 计算伤害减免百分比
        val reduction = effectiveLevel * DAMAGE_REDUCTION_PER_LEVEL

        // 计算减免后的伤害
        val originalDamage = event.damage
        val reducedDamage = originalDamage * (1 - reduction)

        // 应用减免后的伤害
        event.damage = reducedDamage
    }

    override fun onRemove(player: org.bukkit.entity.Player) {}
}
