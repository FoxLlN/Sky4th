package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import com.sky4th.equipment.util.PlayereffectUtil
import org.bukkit.Particle
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * 碎片词条
 * 效果：使用时消耗双倍耐久，有概率额外造成伤害
 * 等级1：20%概率额外造成40%伤害
 * 等级2：25%概率额外造成50%伤害
 * 等级3：30%概率额外造成60%伤害
 */
class Shrapnel : com.sky4th.equipment.modifier.ConfiguredModifier("shrapnel") {
    companion object {
        // 每级配置：(触发概率, 额外伤害倍率)
        private val CONFIG = arrayOf(
            0.20 to 0.40,       // 1级: 20%, 40%
            0.25 to 0.50,       // 2级: 25%, 50%
            0.30 to 0.60        // 3级: 30%, 60%
        )
    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(EntityDamageEvent::class.java, PlayerItemDamageEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 处理耐久度消耗事件
        if (event is PlayerItemDamageEvent && playerRole == PlayerRole.SELF) {
            // 确保事件涉及的物品是当前装备
            if (event.item != item) {
                return
            }
            // 双倍消耗耐久度
            event.damage = event.damage * 2
            return
        }

        // 处理实体伤害事件
        if (event !is EntityDamageEvent || playerRole != PlayerRole.ATTACKER) {
            return
        }

        // 根据等级获取概率和额外伤害倍率
        val (probability, extraDamageMultiplier) = CONFIG.getOrNull(level - 1) ?: return

        // 概率触发额外伤害
        if (Random.nextDouble() >= probability) return

        // 获取原始伤害
        val damage = event.damage

        // 计算额外伤害
        val extraDamage = damage * extraDamageMultiplier

        // 设置新伤害
        event.damage = damage + extraDamage

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
