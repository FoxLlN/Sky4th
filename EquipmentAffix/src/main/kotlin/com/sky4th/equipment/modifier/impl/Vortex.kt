package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Trident
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector

/**
 * 漩涡词条
 * 效果：命中目标后牵引目标周围的生物到命中点
 * 1级：牵引目标周围3格内的生物
 * 2级：牵引目标周围4格内的生物
 * 3级：牵引目标周围5格内的生物
 */
class Vortex : com.sky4th.equipment.modifier.ConfiguredModifier("vortex") {

    companion object {
        // 每级的牵引范围
        private val PULL_RADIUS = doubleArrayOf(3.0, 4.0, 5.0)

        // 牵引力度
        private const val PULL_STRENGTH = 0.5
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
        when (event) {
            is EntityDamageByEntityEvent -> {
                // 处理三叉戟命中
                if (event.damager is Trident && event.entity is LivingEntity) {
                    val trident = event.damager as Trident

                    // 检查是否是玩家投掷的三叉戟
                    if (trident.shooter !is Player || trident.shooter != player) return

                    // 检查是否是持有该词条的装备
                    if (trident.itemStack != item) return

                    // 获取当前等级的牵引范围
                    val radius = if (level - 1 in 0..2) PULL_RADIUS[level - 1] else 3.0

                    // 获取命中位置
                    val hitLocation = event.entity.location

                    // 播放命中效果
                    playVortexHitEffect(hitLocation)

                    // 牵引周围的生物
                    pullNearbyEntities(hitLocation, player, radius)
                }
            }
        }
    }

    /**
     * 牵引周围的生物到命中点
     */
    private fun pullNearbyEntities(hitLocation: org.bukkit.Location, player: Player, radius: Double) {
        // 获取范围内的所有实体
        val nearbyEntities = hitLocation.world?.getNearbyEntities(
            hitLocation,
            radius,
            radius,
            radius
        ) ?: return

        for (entity in nearbyEntities) {
            // 只处理活体生物
            if (entity !is LivingEntity) continue

            // 跳过投掷者自己
            if (entity.uniqueId == player.uniqueId) continue

            // 计算从实体到命中点的向量
            val toHitLocation = hitLocation.toVector().subtract(entity.location.toVector())

            // 检查向量长度是否为0，避免归一化导致的非有限值错误
            if (toHitLocation.lengthSquared() < 0.0001) continue

            // 设置牵引速度（归一化向量 * 力度）
            val pullVelocity = toHitLocation.normalize().multiply(PULL_STRENGTH)

            // 应用牵引速度
            entity.velocity = pullVelocity

            // 播放粒子效果
            playVortexPullEffect(entity.location)
        }
    }

    /**
     * 播放漩涡命中效果
     */
    private fun playVortexHitEffect(location: org.bukkit.Location) {
        // 播放漩涡粒子效果 - 使用水滴粒子
        location.world?.spawnParticle(
            Particle.END_ROD,
            location,
            20,
            1.0,
            1.0,
            1.0,
            0.1
        )

        // 播放额外的气泡效果
        location.world?.spawnParticle(
            Particle.BUBBLE,
            location,
            15,
            0.8,
            0.8,
            0.8,
            0.1
        )

        // 播放声音
        location.world?.playSound(
            location,
            Sound.ENTITY_PLAYER_SPLASH,
            1.0f,
            0.5f
        )
    }

    /**
     * 播放漩涡牵引效果
     */
    private fun playVortexPullEffect(location: org.bukkit.Location) {
        // 播放气泡效果
        location.world?.spawnParticle(
            Particle.BUBBLE,
            location,
            5,
            0.2,
            0.2,
            0.2,
            0.05
        )
    }
}
