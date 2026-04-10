package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import com.sky4th.equipment.util.DamageTypeUtil
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack

/**
 * 减震词条
 * 效果：受到动能/爆炸伤害时，减少一定伤害
 * 1级：减少8%伤害
 * 2级：减少10%伤害
 * 3级：减少12%伤害
 */
class Damping : com.sky4th.equipment.modifier.ConfiguredModifier("damping") {

    companion object {
        // 每级伤害减免百分比
        private const val DAMAGE_REDUCTION_PER_LEVEL = 0.04
        
        // 最大等级
        private const val MAX_LEVEL = 3
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

        // 检查伤害类型是否为动能或爆炸
        val damageType = DamageTypeUtil.getDamageType(event)
        if (damageType != DamageTypeUtil.DamageType.KINETIC && 
            damageType != DamageTypeUtil.DamageType.EXPLOSION) {
            return
        }

        // 限制等级不超过最大等级
        val effectiveLevel = level.coerceAtMost(MAX_LEVEL)
        
        // 获取当前等级的伤害减免百分比
        val reduction = effectiveLevel * DAMAGE_REDUCTION_PER_LEVEL

        // 计算减免后的伤害
        val originalDamage = event.damage
        val reducedDamage = originalDamage * (1 - reduction)

        // 应用减免后的伤害
        event.damage = reducedDamage
    }

    override fun onRemove(player: org.bukkit.entity.Player) {}
}
