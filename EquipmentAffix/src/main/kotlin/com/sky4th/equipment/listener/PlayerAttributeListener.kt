
package com.sky4th.equipment.listener

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import com.sky4th.equipment.EquipmentAffix
import com.sky4th.equipment.event.PlayerDodgeEvent
import com.sky4th.equipment.util.DamageTypeUtil
import sky4th.core.api.PlayerAttributesAPI
import kotlin.random.Random

/**
 * 玩家属性监听器
 * 处理玩家常驻属性的应用，如闪避、抗击退等
 * 使用SkyCore的PlayerAttributesAPI管理玩家属性
 */
class PlayerAttributeListener(
    private val plugin: EquipmentAffix
) : Listener {

    /**
     * 监听实体受伤事件
     * 在HIGHEST优先级处理，确保在其他伤害计算之前进行闪避判定
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityDamage(event: EntityDamageEvent) {
        // 检查事件是否已取消，如果已取消则跳过闪避判定
        if (event.isCancelled) return
        if (event.damage <= 0.0) return

        val entity = event.entity

        // 只处理玩家受伤
        if (entity !is Player) return
        // 判断伤害类型是否可闪避
        if (!DamageTypeUtil.isEvadable(event)) return
        // 从SkyCore获取玩家的闪避率
        val attributes = PlayerAttributesAPI.getAttributes(entity.uniqueId)
        val dodgeChance = attributes.dodge

        // 如果闪避率为0，直接返回
        if (dodgeChance <= 0.0) {
            return
        }
        // 进行闪避判定
        if (Random.nextDouble() < dodgeChance) {
            // 创建并触发闪避成功事件
            val dodgeEvent = PlayerDodgeEvent(entity, event, dodgeChance)
            plugin.server.pluginManager.callEvent(dodgeEvent)
            
            // 如果闪避事件未被取消，则继续闪避处理
            if (!dodgeEvent.isCancelled) {
                // 闪避成功，取消伤害
                event.damage = 0.0

                // 播放闪避成功的音效
                val sound = org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP
                val volume = Random.nextDouble(0.5, 1.0).toFloat()
                val pitch = Random.nextDouble(1.0, 1.2).toFloat()
                entity.world.playSound(entity.location, sound, volume, pitch)

                // 在玩家周围生成环绕的云雾粒子效果
                val particle = org.bukkit.Particle.CLOUD
                val particleCount = Random.nextInt(8, 13) // 8-13个

                // 在玩家身体周围随机位置生成粒子
                for (i in 0 until particleCount) {
                    // 随机角度和高度
                    val angle = Random.nextDouble() * Math.PI * 2
                    val height = Random.nextDouble(0.0, 1.8) // 玩家高度范围内随机
                    val radius = Random.nextDouble(0.3, 0.6) // 环绕半径

                    // 计算粒子位置
                    val x = Math.cos(angle) * radius
                    val z = Math.sin(angle) * radius

                    entity.world.spawnParticle(
                        particle,
                        entity.location.add(x, height, z),
                        1 // 每个位置生成1个粒子
                    )
                }
            }
        }
    }
}
