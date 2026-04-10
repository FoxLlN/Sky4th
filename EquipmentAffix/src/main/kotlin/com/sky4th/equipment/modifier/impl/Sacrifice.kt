
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType

/**
 * 献祭词条
 * 效果：受到伤害增加，但是部分伤害转化为对周围敌人的火焰伤害
 * 1级：受到10%的额外伤害，将受到的15%伤害转化为对周围2格内所有敌人的火焰伤害
 * 2级：受到15%的额外伤害，将受到的20%伤害转化为对周围2.5格内所有敌人的火焰伤害
 * 3级：受到20%的额外伤害，将受到的25%伤害转化为对周围3格内所有敌人的火焰伤害
 */
class Sacrifice : com.sky4th.equipment.modifier.ConfiguredModifier("sacrifice") {

    companion object {
        // 每级配置：(额外伤害百分比, 反射伤害百分比, 爆炸范围格数)
        private val CONFIG = arrayOf(
            Triple(0.10, 0.15, 2.0),  // 1级：10%额外伤害，15%反射，2格范围
            Triple(0.15, 0.20, 2.5),  // 2级：15%额外伤害，20%反射，2.5格范围
            Triple(0.20, 0.25, 3.0)   // 3级：20%额外伤害，25%反射，3格范围
        )
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

        // 获取当前等级的配置
        val (extraDamagePercent, reflectDamagePercent, radius) = CONFIG.getOrNull(level - 1) ?: return

        // 获取原始伤害
        val originalDamage = event.damage

        // 计算额外伤害
        val extraDamage = originalDamage * extraDamagePercent

        // 增加玩家受到的伤害
        event.damage = originalDamage + extraDamage

        // 计算反射伤害
        val reflectDamage = originalDamage * reflectDamagePercent

        // 对周围敌人造成火焰伤害
        if (reflectDamage > 0) {
            createFlameDamage(player.location, radius, reflectDamage, player)
        }
    }

    /**
     * 对周围敌人造成火焰伤害
     * @param location 中心位置
     * @param radius 伤害范围
     * @param damage 火焰伤害
     * @param player 伤害来源玩家
     */
    private fun createFlameDamage(location: Location, radius: Double, damage: Double, player: Player) {
        // 播放火焰音效，音量根据伤害调整
        val volume = (damage / 10.0).coerceIn(0.3, 1.5).toFloat()
        location.world?.playSound(location, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, volume, 0.8f)

        // 播放火焰粒子效果，粒子数量根据伤害调整
        val particleCount = (damage / 3.0).toInt().coerceIn(5, 30)
        location.world?.spawnParticle(Particle.FLAME, location.x, location.y + 0.5, location.z, particleCount, radius, 0.5, radius, 0.05)
        location.world?.spawnParticle(Particle.LAVA, location.x, location.y, location.z, (particleCount / 3).coerceAtLeast(1), 0.3, 0.3, 0.3, 0.02)

        // 获取范围内的所有实体
        val nearbyEntities = location.world?.getNearbyEntities(location, radius, radius, radius) ?: return

        // 对范围内的每个生物实体造成火焰伤害
        for (entity in nearbyEntities) {
            if (entity is LivingEntity && entity != player) {
                val damageSource = DamageSource.builder(DamageType.ON_FIRE)
                    .build()

                entity.damage(damage, damageSource)

                // 给予短暂的燃烧效果
                entity.fireTicks = (damage * 10).toInt().coerceAtMost(100)
            }
        }
    }

    override fun onRemove(player: Player) {}
}
