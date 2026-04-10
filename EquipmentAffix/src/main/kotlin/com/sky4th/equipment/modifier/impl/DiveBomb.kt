package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack

/**
 * 急袭词条
 * 效果：降落时可以撞击敌人造成伤害
 *
 * 1级：造成50%坠落伤害的范围伤害
 * 2级：造成60%坠落伤害的范围伤害
 * 3级：造成70%坠落伤害的范围伤害
 */
class DiveBomb : com.sky4th.equipment.modifier.ConfiguredModifier("dive_bomb") {

    companion object {
        // 每级伤害倍率（50%, 60%, 70%）
        private val DAMAGE_MULTIPLIER = arrayOf(0.5, 0.6, 0.7)

        // 影响范围（方块）
        private const val IMPACT_RADIUS = 2.0
    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(EntityDamageEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 只处理玩家受到伤害的事件
        if (event !is EntityDamageEvent || playerRole != PlayerRole.DEFENDER) {
            return
        }
        // 只处理坠落伤害
        if (event.cause != EntityDamageEvent.DamageCause.FLY_INTO_WALL) {
            return
        }

        // 获取当前等级的伤害倍率
        val damageMultiplier = DAMAGE_MULTIPLIER.getOrNull(level - 1) ?: return

        // 计算撞击伤害
        val impactDamage = event.damage * damageMultiplier

        // 获取玩家周围的所有实体
        val nearbyEntities = player.world.getNearbyEntities(
            player.location,
            IMPACT_RADIUS,
            IMPACT_RADIUS,
            IMPACT_RADIUS
        )

        // 对每个敌人造成伤害
        var hitCount = 0
        for (entity in nearbyEntities) {
            // 跳过玩家自己
            if (entity === player) continue

            // 只对生物实体造成伤害
            if (entity is LivingEntity) {
                // 造成伤害
                entity.damage(impactDamage, player)
                hitCount++
            }
        }

        // 如果击中了敌人，播放特效
        if (hitCount > 0) {
            // 播放撞击音效
            player.world.playSound(
                player.location,
                Sound.ENTITY_GENERIC_EXPLODE,
                1.0f,
                1.0f
            )
            // 播放爆炸粒子效果
            player.world.spawnParticle(
                Particle.EXPLOSION,
                player.location,
                10 * level,
                0.8,
                0.8,
                0.8,
                0.2
            )
        }
    }

    override fun onRemove(player: Player) {
        // 词条移除时不需要特殊处理
    }

    override fun onInit(player: Player, item: ItemStack, level: Int) {
        // 初始化时不需要特殊处理
    }
}
