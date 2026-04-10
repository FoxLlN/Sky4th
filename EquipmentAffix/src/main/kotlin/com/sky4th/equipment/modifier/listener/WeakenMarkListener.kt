
package com.sky4th.equipment.modifier.listener

import com.sky4th.equipment.modifier.impl.Weaken
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent

/**
 * 虚弱标记监听器
 * 处理虚弱标记的减伤效果
 * I级：-10%伤害
 * II级：-15%伤害
 * III级：-20%伤害
 */
class WeakenMarkListener : Listener {

    companion object {
        // 标记名称常量（支持1/2/3级）
        const val MARK_ID_1 = "weaken_mark_1"
        const val MARK_ID_2 = "weaken_mark_2"
        const val MARK_ID_3 = "weaken_mark_3"
    }

    /**
     * 处理实体造成伤害事件
     * 检查攻击者是否带有虚弱标记，如果有则应用减伤效果
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onEntityDamage(event: EntityDamageByEntityEvent) {
        val damager = event.damager

        // 检查攻击者是否是LivingEntity
        if (damager !is org.bukkit.entity.LivingEntity) {
            return
        }

        // 根据标记等级确定减伤比例
        val damageReduction = when {
            sky4th.core.api.MarkAPI.hasMark(damager, MARK_ID_3) -> 0.20  // 3级：-20%
            sky4th.core.api.MarkAPI.hasMark(damager, MARK_ID_2) -> 0.15  // 2级：-15%
            sky4th.core.api.MarkAPI.hasMark(damager, MARK_ID_1) -> 0.10  // 1级：-10%
            else -> return  // 没有虚弱标记，直接返回
        }

        // 应用减伤效果
        val originalDamage = event.damage
        val reducedDamage = originalDamage * damageReduction
        event.damage = originalDamage - reducedDamage
    }
}
