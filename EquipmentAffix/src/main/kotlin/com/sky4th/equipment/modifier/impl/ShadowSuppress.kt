package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.Material
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack
import sky4th.core.api.MarkAPI

/**
 * 抑影词条
 * 效果：命中目标时，目标3秒内无法传送
 * 1级：命中目标时，目标3秒内无法传送
 */
class ShadowSuppress : com.sky4th.equipment.modifier.ConfiguredModifier("shadow_suppress") {
    
    companion object {
        // 抑影持续时间（秒）
        private const val DURATION = 3.0
        
        // 标记显示物品（末影珍珠）
        private val MARK_ITEM = ItemStack(Material.ENDER_PEARL)
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
        if (event !is EntityDamageByEntityEvent) {
            return
        }
        
        // 只处理实体伤害事件，且玩家必须是攻击者
        if (playerRole != PlayerRole.ATTACKER) {
            return
        }
        
        // 获取受击者
        val victim = event.entity
        if (victim !is LivingEntity) {
            return
        }
        
        // 创建抑影标记，持续3秒
        MarkAPI.createMark(victim, getAffixId(), MARK_ITEM, showToAllPlayers = true, duration = DURATION.toLong())
    }
}
