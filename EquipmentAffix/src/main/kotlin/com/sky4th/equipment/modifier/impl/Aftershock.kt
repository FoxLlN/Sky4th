package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack

/**
 * 余震词条
 * 效果：猛击落地后，地面留下裂纹，3秒后再次爆发小范围冲击
 * 1级：冲击波半径3格，造成25%武器伤害，最大8点
 * 2级：冲击波半径3格，造成30%武器伤害，最大10点
 * 3级：冲击波半径3格，造成35%武器伤害，最大12点
 */
class Aftershock : com.sky4th.equipment.modifier.ConfiguredModifier("aftershock") {

    companion object {
        // 每级配置：(伤害百分比, 最大伤害)
        private val CONFIG = arrayOf(
            0.25 to 10.0,  // 1级：25%伤害，最大10
            0.30 to 12.0, // 2级：30%伤害，最大12
            0.35 to 14.0  // 3级：35%伤害，最大14
        )

        private val RADIUS = 3.0 // 冲击波半径
        private val DELAY_TICKS = 3 * 20L // 延迟3秒（20 ticks）
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
        // 只处理实体伤害事件，且玩家必须是攻击者
        if (event !is EntityDamageByEntityEvent || playerRole != PlayerRole.ATTACKER) {
            return
        }

        // 伤害类型
        if (event.cause != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            return
        }

        // 检查玩家是否在下坠状态（fallDistance > 0）
        // 这表示玩家正在下落攻击
        if (player.fallDistance <= 3.0) {
            return
        }

        val targetEntity = event.entity
        if (targetEntity !is LivingEntity) {
            return
        }

        // 获取当前等级的配置
        val (damagePercent, maxDamage) = CONFIG.getOrNull(level - 1) ?: return

        // 获取原始伤害
        val baseDamage = event.damage
        val aftershockDamage = minOf(baseDamage * damagePercent, maxDamage)

        // 获取落点位置（被攻击实体的位置）
        val impactLocation = targetEntity.location

        // 创建持续3秒的地面裂纹效果
        scheduleCrackEffect(impactLocation, DELAY_TICKS)

        // 播放延迟爆炸效果
        scheduleAftershock(player, impactLocation, aftershockDamage)
    }

    /**
     * 创建地面裂纹效果
     */
    private fun createCrackEffect(location: Location) {
        val world = location.world
        val random = java.util.Random()

        // 在圆形区域内随机创建裂纹粒子效果
        for (i in 0 until 30) {
            // 随机角度
            val angle = random.nextDouble() * 2 * Math.PI
            // 随机距离（使用平方根分布使粒子均匀分布在圆内）
            val distance = Math.sqrt(random.nextDouble()) * 2.0

            val x = location.x + Math.cos(angle) * distance
            val z = location.z + Math.sin(angle) * distance

            world.spawnParticle(
                Particle.BLOCK,
                Location(world, x, location.y + 0.1, z),
                5,
                0.0, 0.0, 0.0,
                0.0,
                org.bukkit.Material.STONE.createBlockData()
            )
        }
    }

    /**
     * 安排持续的裂纹效果
     */
    private fun scheduleCrackEffect(location: Location, durationTicks: Long) {
        val intervalTicks = 5L // 每5个tick（0.25秒）刷新一次裂纹
        val iterations = (durationTicks / intervalTicks).toInt()

        for (i in 0 until iterations) {
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                com.sky4th.equipment.EquipmentAffix.instance,
                Runnable {
                    createCrackEffect(location)
                },
                i * intervalTicks
            )
        }
    }

    /**
     * 安排延迟余震爆炸
     */
    private fun scheduleAftershock(player: Player, center: Location, damage: Double) {
        // 延迟1秒后触发余震
        org.bukkit.Bukkit.getScheduler().runTaskLater(
            com.sky4th.equipment.EquipmentAffix.instance,
            Runnable {
                // 创建爆炸粒子效果
                createExplosionParticles(center, damage)

                // 对范围内的实体造成伤害
                applyAftershockDamage(player, center, damage)
            },
            DELAY_TICKS
        )
    }

    /**
     * 创建爆炸粒子效果
     */
    private fun createExplosionParticles(center: Location, damage: Double) {
        val world = center.world

        // 播放爆炸音效
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2.0f, 1f)

        // 播放爆炸粒子效果
        world.spawnParticle(Particle.EXPLOSION, center.x, center.y, center.z, 30, 0.8, 0.8, 0.8, 0.2)
    }

    /**
     * 应用余震伤害
     */
    private fun applyAftershockDamage(player: Player, center: Location, damage: Double) {
        val world = center.world
        val nearbyEntities = world.getNearbyEntities(center, RADIUS, 1.0, RADIUS)
        nearbyEntities.forEach { entity ->
            // 只对活体实体生效
            if (entity is LivingEntity && entity != player) {

                // 使用爆炸类型伤害
                val damageSource = DamageSource.builder(DamageType.EXPLOSION)
                    .withDirectEntity(player)
                    .build()

                entity.damage(damage, damageSource)
            }
        }
    }
}
