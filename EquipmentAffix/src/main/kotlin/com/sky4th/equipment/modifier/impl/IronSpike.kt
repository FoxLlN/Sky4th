package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import com.sky4th.equipment.util.DamageTypeUtil
import org.bukkit.entity.LivingEntity
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack

/**
 * 铁刺词条
 * 效果：受到近战攻击时，将部分伤害反弹给攻击者
 * 1级：反弹8%伤害
 * 2级：反弹12%伤害
 * 3级：反弹16%伤害
 */
class IronSpike : com.sky4th.equipment.modifier.ConfiguredModifier("iron_spike") {

    companion object {
        // 每级伤害反弹百分比
        private const val REFLECTION_PERCENTAGE_PER_LEVEL = 0.03
        
        // 最大等级
        private const val MAX_LEVEL = 3
    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(EntityDamageByEntityEvent::class.java)

    override fun handle(
        event: Event,
        player: org.bukkit.entity.Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 只处理玩家受到近战攻击的事件
        if (event !is EntityDamageByEntityEvent || playerRole != PlayerRole.DEFENDER) {
            return
        }

        // 检查伤害类型是否为物理伤害（近战）
        if (!DamageTypeUtil.isPhysicalDamage(event)) {
            return
        }

        // 限制等级不超过最大等级
        val effectiveLevel = level.coerceAtMost(MAX_LEVEL)
        
        // 获取当前等级的伤害反弹百分比
        val reflectionPercent = effectiveLevel * REFLECTION_PERCENTAGE_PER_LEVEL

        // 获取攻击者
        val attacker = event.damager
        if (attacker !is LivingEntity) {
            return
        }

        // 计算反弹伤害
        val originalDamage = event.damage
        val reflectedDamage = originalDamage * reflectionPercent

        // 对攻击者造成反弹伤害
        attacker.damage(reflectedDamage, player)
    }

    override fun onRemove(player: org.bukkit.entity.Player) {}
}
