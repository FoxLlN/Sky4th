package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import com.sky4th.equipment.util.BlockTypeUtil
import com.sky4th.equipment.util.PlayereffectUtil
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * 锐利词条
 * 效果：攻击时10%概率伤害+30%
 */
class Sharp : com.sky4th.equipment.modifier.ConfiguredModifier("sharp") {

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(EntityDamageEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 处理实体伤害事件
        if (event !is EntityDamageEvent || playerRole != PlayerRole.ATTACKER) {
            return
        }

        // 10%概率触发
        if (Random.nextDouble() >= 0.1) return

        // 获取原始伤害
        val damage = event.damage

        // 增加30%伤害
        val newDamage = damage * 1.3

        // 设置新伤害
        event.damage = newDamage

        // 获取受击者（被攻击的实体）
        val victim = when (event) {
            is org.bukkit.event.entity.EntityDamageByEntityEvent -> event.entity
            else -> return
        }

        // 播放特效（在受击者身上）
        if (victim is LivingEntity) {
            PlayereffectUtil.playCircleParticle(victim, Particle.CRIT, 5)
        }
    }
}
