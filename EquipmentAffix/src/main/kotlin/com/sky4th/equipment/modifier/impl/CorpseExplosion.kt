
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType

/**
 * 尸爆词条
 * 效果：击杀目标时，引发爆炸，对周围敌人造成伤害（不破坏方块）
 * 1级：爆炸范围2格，造成15%伤害
 * 2级：爆炸范围3格，造成20%伤害
 * 3级：爆炸范围3格，造成25%伤害
 */
class CorpseExplosion : com.sky4th.equipment.modifier.ConfiguredModifier("corpse_explosion") {

    companion object {
        // 每级配置：(爆炸范围格数, 伤害百分比)
        private val CONFIG = arrayOf(
            1.0 to 0.15,       // 1格范围，15%伤害
            2.0 to 0.20,       // 2格范围，20%伤害
            3.0 to 0.25        // 3格范围，25伤害
        )
    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(EntityDeathEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 只处理实体死亡事件
        if (event !is EntityDeathEvent) {
            return
        }

        // 获取死亡实体
        val entity = event.entity

        // 检查是否有击杀者
        val killer = entity.killer ?: return

        // 只对持有该词条装备的玩家生效
        if (killer != player) {
            return
        }

        // 获取当前等级的配置
        val (radius, damagePercent) = CONFIG.getOrNull(level - 1) ?: return

        // 获取击杀时的伤害（使用实体最大生命值作为基准）
        val baseDamage = entity.maxHealth * damagePercent
        // 创建爆炸效果
        createExplosion(entity.location, radius, baseDamage, player, entity)
    }

    /**
     * 创建爆炸效果
     * @param location 爆炸中心位置
     * @param radius 爆炸范围
     * @param damage 爆炸伤害
     * @param player 爆炸来源玩家
     */
    private fun createExplosion(location: Location, radius: Double, damage: Double, player: Player, deathEntity: LivingEntity) {
        // 播放爆炸音效，音量根据伤害调整
        val volume = (damage / 20.0).coerceIn(0.5, 2.0).toFloat()
        location.world?.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, volume, 1f)

        // 播放爆炸粒子效果，粒子数量根据伤害调整
        val particleCount = (damage / 5.0).toInt().coerceIn(1, 20)
        location.world?.spawnParticle(Particle.EXPLOSION, location.x, location.y, location.z, particleCount, 0.5, 0.5, 0.5, 0.1)

        // 获取爆炸范围内的所有实体
        val nearbyEntities = location.world?.getNearbyEntities(location, radius, radius, radius) ?: return

        // 对范围内的每个生物实体造成伤害
        for (entity in nearbyEntities) {
            if (entity is LivingEntity && entity != player) {
                val damageSource = DamageSource.builder(DamageType.EXPLOSION)
                    .withDirectEntity(deathEntity)
                    .build()
                
                entity.damage(damage, damageSource)
            }
        }
    }

    override fun onRemove(player: Player) {}
}
