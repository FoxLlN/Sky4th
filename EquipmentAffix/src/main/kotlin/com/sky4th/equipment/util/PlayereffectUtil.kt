package com.sky4th.equipment.util

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import kotlin.random.Random

/**
 * 玩家效果工具类
 */
object PlayereffectUtil {
    /**
     * 播放环绕粒子效果
     */
    fun playCircleParticle(entity: LivingEntity, particle: Particle, count: Int) {
        val world = entity.world
        val location = entity.location

        for (i in 0 until count) {
            val angle = Random.nextDouble() * Math.PI * 2
            val height = Random.nextDouble(0.0, 1.8)
            val radius = Random.nextDouble(0.3, 0.6)

            val x = Math.cos(angle) * radius
            val z = Math.sin(angle) * radius

            world.spawnParticle(
                particle,
                location.clone().add(x, height, z),
                1
            )
        }
    }

    /**
     * 播放音效（使用自定义音量和音高范围）
     * @param entity 目标实体
     * @param sound 音效类型
     * @param minVolume 最小音量（默认0.5）
     * @param maxVolume 最大音量（默认1.0）
     * @param minPitch 最小音高（默认1.0）
     * @param maxPitch 最大音高（默认1.2）
     */
    fun playSound(
        entity: LivingEntity,
        sound: Sound,
        minVolume: Double = 0.5,
        maxVolume: Double = 1.0,
        minPitch: Double = 1.0,
        maxPitch: Double = 1.2
    ) {
        val volume = Random.nextDouble(minVolume, maxVolume).toFloat()
        val pitch = Random.nextDouble(minPitch, maxPitch).toFloat()
        entity.world.playSound(entity.location, sound, volume, pitch)
    }
}