package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID

/**
 * 锁骨词条
 * 效果：钓鱼竿钩住实体时持续1/2/3秒扣血，每秒扣1滴血
 */
class BoneLock : com.sky4th.equipment.modifier.ConfiguredModifier("bone_lock") {

    // 存储正在流血的实体及其任务
    private val bleedingEntities = mutableMapOf<UUID, BukkitRunnable>()

    // 存储玩家上次触发时间
    private val playerCooldowns = mutableMapOf<UUID, Long>()

    // 冷却时间（秒）
    private val COOLDOWN_TIME = 8L
    
    // 每级持续时间（秒，每秒伤害）
    private val CONFIG = arrayOf(
        3.0 to 1.0, 
        4.0 to 1.5, 
        5.0 to 2.0
    )

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(
            PlayerFishEvent::class.java
        )

    override fun handle(
        event: Event,
        player: Player,
        item: org.bukkit.inventory.ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 检查是否是玩家自己的事件
        if (playerRole != PlayerRole.SELF) {
            return
        }

        // 检查冷却
        val currentTime = System.currentTimeMillis()
        val lastTriggerTime = playerCooldowns[player.uniqueId] ?: 0L
        val cooldownMs = COOLDOWN_TIME * 1000L
        val timeSinceLastTrigger = currentTime - lastTriggerTime

        if (timeSinceLastTrigger < cooldownMs) {
            return
        }

        // 检查是否是 PlayerFishEvent
        if (event !is PlayerFishEvent) {
            return
        }

        // 只在钩住实体时触发
        if (event.state != PlayerFishEvent.State.CAUGHT_ENTITY) {
            return
        }

        // 获取被钩住的实体
        val target = event.caught

        if (target == null) {
            return
        }

        // 检查目标是否是生物实体
        if (target !is LivingEntity) {
            return
        }

        // 获取持续时间（秒）
        val (duration, damagePerSecond) = CONFIG.getOrNull(level - 1) ?: return

        // 如果目标已经在流血，先取消之前的任务
        if (bleedingEntities.containsKey(target.uniqueId)) {
            bleedingEntities[target.uniqueId]?.cancel()
        }

        // 创建流血任务
        val bleedingTask = object : BukkitRunnable() {
            private var ticks = 0
            private val totalTicks = duration * 20L // 每秒20tick

            override fun run() {
                // 检查目标是否还活着
                if (!target.isValid || target.isDead) {
                    cancel()
                    bleedingEntities.remove(target.uniqueId)
                    return
                }

                // 每秒扣1滴血（每20tick）
                if (ticks % 20 == 0 && ticks > 0) {
                    target.damage(damagePerSecond, player)
                }

                ticks++

                // 检查是否达到持续时间
                if (ticks > totalTicks) {
                    cancel()
                    bleedingEntities.remove(target.uniqueId)
                }
            }
        }

        // 启动流血任务
        bleedingTask.runTaskTimer(com.sky4th.equipment.EquipmentAffix.instance, 0L, 1L)
        bleedingEntities[target.uniqueId] = bleedingTask

        // 更新触发时间
        playerCooldowns[player.uniqueId] = System.currentTimeMillis()
    }
}
