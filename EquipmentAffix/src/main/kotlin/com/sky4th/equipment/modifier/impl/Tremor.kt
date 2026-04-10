
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector

/**
 * 震地词条
 * 效果：重锤砸地时产生冲击波
 * 1级：冲击波半径3格，造成30%武器伤害
 * 2级：冲击波半径3格，造成35%武器伤害
 * 3级：冲击波半径3格，造成40%武器伤害
 */
class Tremor : com.sky4th.equipment.modifier.ConfiguredModifier("tremor") {

    companion object {
        // 每级击退距离（格）
        // 每级配置：(半径, 伤害百分比, 最大伤害)
        private val CONFIG = arrayOf(
            0.30 to 6.0,  // 1级：30%伤害，最大6
            0.35 to 8.0,  // 2级：35%伤害，最大8
            0.40 to 10.0  // 3级：40%伤害，最大10
        )

        private val RADIUS = 3.0 // 冲击波半径

        // 跟踪正在处理冲击波的实体，防止无限循环
        private val processingEntities = mutableSetOf<org.bukkit.entity.Entity>()
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

        // 检查玩家是否在下坠状态（fallDistance > 0）
        // 这表示玩家正在下落攻击
        if (player.fallDistance <= 4.0) {
            return
        }
        val targetEntity = event.entity
        if (targetEntity !is LivingEntity) {
            return
        }

        // 获取当前等级的配置
        val (damagePercent, maxDamage) = CONFIG.getOrNull(level - 1) ?: return

        // 获取玩家位置
        val playerLoc = player.location

        // 获取原始伤害
        val baseDamage = event.damage
        val shockwaveDamage = minOf(baseDamage * damagePercent, maxDamage)

        // 播放砸地声音
        player.world.playSound(playerLoc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.5f, 0.5f)
        player.world.playSound(playerLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.5f)

        createShockwave(player, targetEntity, shockwaveDamage, level)
    }

    /**
     * 创建冲击波效果
     */
    private fun createShockwave(player: Player, targetEntity: LivingEntity, damage: Double, level: Int) {
        val center = targetEntity.location
        val world = center.world
        val affectedEntities = mutableListOf<Entity>()


        // 获取范围内的所有实体
        val nearbyEntities = world.getNearbyEntities(center, RADIUS, 2.0, RADIUS    )

        nearbyEntities.forEach { entity ->
            // 只对活体实体生效，排除玩家自己和被攻击实体
            if (entity is LivingEntity && entity != player && entity != targetEntity) {
                // 检查是否正在处理该实体，防止无限循环
                if (entity in processingEntities) {
                    return@forEach
                }

                // 添加到处理列表
                processingEntities.add(entity)
                
                // 使用延迟任务造成伤害，避免在当前事件处理中再次触发词条
                org.bukkit.Bukkit.getScheduler().runTaskLater(
                    com.sky4th.equipment.EquipmentAffix.instance,
                    Runnable {
                        entity.damage(damage, player)
                        // 从处理列表中移除
                        processingEntities.remove(entity)
                    },
                    1L  // 延迟1 tick
                )

                affectedEntities.add(entity)
            }
        }
    }
}
