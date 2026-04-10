package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack

/**
 * 汲取词条
 * 效果：弓/弩命中目标后恢复生命值
 * 1级：恢复造成伤害的5%
 * 2级：恢复造成伤害的8%
 * 3级：恢复造成伤害的10%
 */
class Leech : com.sky4th.equipment.modifier.ConfiguredModifier("leech") {

    private val CONFIG = doubleArrayOf(0.05, 0.08, 0.10)

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(EntityDamageByEntityEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 只处理实体攻击实体事件
        if (event !is EntityDamageByEntityEvent) {
            return
        }

        // 检查是否是弹射物造成的伤害
        val damager = event.damager
        if (!org.bukkit.entity.Projectile::class.java.isInstance(damager)) {
            return
        }
        val projectile = damager as org.bukkit.entity.Projectile

        // 确保弹射物的射击者是玩家
        if (projectile.shooter !is Player || projectile.shooter != player) {
            return
        }

        // 检查是否被盾牌完全格挡（伤害为0）
        if (event.damage <= 0) {
            return
        }

        // 获取受害者
        val victim = event.entity
        if (victim !is LivingEntity) {
            return
        }

        // 根据词条等级计算恢复比例
        val healPercentage = CONFIG.getOrElse(level - 1) { CONFIG[0] }

        // 计算恢复的生命值
        val healAmount = event.damage * healPercentage

        // 恢复玩家生命值
        player.health = minOf(player.health + healAmount, player.maxHealth)
    }

    override fun onRemove(player: Player) {
    }
}
