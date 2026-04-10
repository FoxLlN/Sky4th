package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.inventory.ItemStack

/**
 * 旋风词条
 * 效果：弓/弩命中后在命中位置生成一个风弹
 */
class Whirlwind : com.sky4th.equipment.modifier.ConfiguredModifier("whirlwind") {

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(ProjectileHitEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 只处理弹射物命中事件
        if (event !is ProjectileHitEvent) {
            return
        }

        // 获取弹射物
        val projectile = event.entity

        // 确保弹射物的射击者是玩家
        if (projectile.shooter !is Player || projectile.shooter != player) {
            return
        }

        // 获取命中位置（优先使用击中的方块位置，否则使用击中的实体位置，最后使用弹射物位置）
        val center = event.hitBlock?.location?.add(0.5, 0.5, 0.5)
            ?: event.hitEntity?.location
            ?: projectile.location

        val world = center.world ?: return
        // 1.21 风弹碰撞特效：粒子 + 音效
        playWindBurstEffect(world, center)
        val radius = 4.0
        val strength = 1.4
        for (e in world.getNearbyEntities(center, radius, radius, radius)) {
            if (e !is LivingEntity) continue
            val dir = e.location.toVector().subtract(center.toVector()).normalize()
            val dist = e.location.distance(center)
            if (dist < 0.1) continue
            val falloff = 1.0 - (dist / radius) * 0.5
            e.velocity = dir.multiply(strength * falloff)
        }
    }

    /** 1.21 风弹命中时的粒子与音效（与原版风弹碰撞一致） */
    private fun playWindBurstEffect(world: org.bukkit.World, center: org.bukkit.Location) {
        val x = center.x
        val y = center.y
        val z = center.z
        world.spawnParticle(Particle.GUST_EMITTER_SMALL, x, y, z, 1, 0.0, 0.0, 0.0, 0.0)
        world.spawnParticle(Particle.GUST, x, y, z, 12, 0.5, 0.5, 0.5, 0.15)
        try {
            world.playSound(center, Sound.ENTITY_WIND_CHARGE_WIND_BURST, SoundCategory.PLAYERS, 1f, (0.7 + Math.random() * 0.35).toFloat())
        } catch (_: Throwable) {
            // 低版本无此音效时忽略
        }
    }

    override fun onRemove(player: Player) {
    }
}