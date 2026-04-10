package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import com.sky4th.equipment.util.DamageTypeUtil
import org.bukkit.entity.LivingEntity
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * 反击词条
 * 效果：格挡近战攻击后，有概率将部分伤害反弹给攻击者
 * 1级：15%概率反弹30%伤害
 * 2级：20%概率反弹40%伤害
 * 3级：25%概率反弹50%伤害
 */
class Counterattack : com.sky4th.equipment.modifier.ConfiguredModifier("counterattack") {

    companion object {
        // 每级配置：(触发概率, 反弹伤害百分比)
        private val CONFIG = arrayOf(
            0.15 to 0.30,  // 1级：15%概率，反弹30%伤害
            0.20 to 0.40,  // 2级：20%概率，反弹40%伤害
            0.25 to 0.50   // 3级：25%概率，反弹50%伤害
        )
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

        // 检查玩家是否成功格挡
        val blockDamage = event.getDamage(DamageModifier.BLOCKING)
        if (blockDamage == 0.0) {
            return
        }

        // 获取当前等级的配置
        val (triggerChance, reflectionPercent) = CONFIG.getOrNull(level - 1) ?: return

        // 检查是否触发反击
        if (Random.nextDouble() > triggerChance) return

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
