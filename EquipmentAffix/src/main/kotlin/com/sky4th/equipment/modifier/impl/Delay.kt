package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import com.sky4th.equipment.util.DamageTypeUtil
import com.sky4th.equipment.util.DamageCauseMapper
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap


/**
 * 延付词条
 * 效果：受到伤害时，将部分伤害延迟3秒后触发
 * 1级：将10%伤害延迟到3秒后触发
 * 2级：将15%伤害延迟到3秒后触发
 * 3级：将20%伤害延迟到3秒后触发
 */
class Delay : com.sky4th.equipment.modifier.ConfiguredModifier("delay") {

    companion object {
        // 每级的延迟伤害百分比
        private val DELAY_PERCENTAGE = doubleArrayOf(0.10, 0.15, 0.20)

        // 延迟时间（秒）
        private const val DELAY_SECONDS = 3L

        // 延迟伤害标记的基础键
        private val DELAY_DAMAGE_BASE_KEY = NamespacedKey("equipment_affix", "delay_damage")

        // 存储每个玩家的延迟伤害任务ID
        private val playerDelayTasks = ConcurrentHashMap<UUID, MutableSet<Long>>()

        // 存储延迟伤害的数据
        private val delayDamageData = ConcurrentHashMap<Long, DelayDamageInfo>()

        // 延迟伤害信息数据类
        data class DelayDamageInfo(
            val damageCause: EntityDamageEvent.DamageCause,
            val damager: org.bukkit.entity.Entity?
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

        // 检查是否是延迟伤害，如果是则跳过处理，防止无限循环
        val delayTasks = playerDelayTasks[player.uniqueId]

        if (delayTasks != null && delayTasks.isNotEmpty()) {
            // 检查玩家是否有延迟伤害标记
            val hasDelayMark = player.persistentDataContainer.has(
                DELAY_DAMAGE_BASE_KEY,
                PersistentDataType.LONG
            )

            if (hasDelayMark) {
                // 获取任务ID
                val taskId = player.persistentDataContainer.get(
                    DELAY_DAMAGE_BASE_KEY,
                    PersistentDataType.LONG
                )

                // 检查这个任务ID是否在玩家的延迟任务列表中
                if (taskId != null && taskId in delayTasks) {
                    // 清除标记
                    player.persistentDataContainer.remove(DELAY_DAMAGE_BASE_KEY)
                    // 从任务列表中移除
                    delayTasks.remove(taskId)
                    return
                }
            }
        }

        // 获取当前等级的延迟百分比
        val delayPercent = if (level - 1 in 0..2) DELAY_PERCENTAGE[level - 1] else return

        // 检查伤害类型是否可以延迟
        val damageType = DamageTypeUtil.getDamageType(event)
        val canDelay = when (damageType) {
            DamageTypeUtil.DamageType.PHYSICAL,
            DamageTypeUtil.DamageType.PROJECTILE,
            DamageTypeUtil.DamageType.MAGIC,
            DamageTypeUtil.DamageType.EXPLOSION,
            DamageTypeUtil.DamageType.ENVIRONMENT -> true
            else -> false
        }

        // 只有特定类型的伤害才会延迟
        if (!canDelay) return

        // 获取原始伤害
        val originalDamage = event.damage

        // 计算延迟伤害和即时伤害
        val delayedDamage = originalDamage * delayPercent
        val immediateDamage = originalDamage - delayedDamage

        // 将即时伤害应用到事件
        event.damage = immediateDamage

        // 保存伤害原因和来源
        val damageCause = event.cause
        val damager = if (event is EntityDamageByEntityEvent) event.damager else null

        // 生成唯一的任务ID
        val taskId = System.currentTimeMillis()
        val playerUuid = player.uniqueId

        // 将任务ID添加到玩家的延迟任务列表
        playerDelayTasks.computeIfAbsent(playerUuid) { mutableSetOf() }.add(taskId)

        // 保存延迟伤害数据
        delayDamageData[taskId] = DelayDamageInfo(damageCause, damager)

        // 调度延迟伤害任务
        org.bukkit.Bukkit.getScheduler().runTaskLater(
            com.sky4th.equipment.EquipmentAffix.instance,
            Runnable {
                // 检查玩家是否在线且存活
                if (!player.isOnline || player.isDead) {
                    // 清理任务ID
                    playerDelayTasks[playerUuid]?.remove(taskId)
                    return@Runnable
                }

                // 设置延迟伤害标记，使用任务ID
                player.persistentDataContainer.set(
                    DELAY_DAMAGE_BASE_KEY,
                    PersistentDataType.LONG,
                    taskId
                )

                try {
                    // 获取保存的延迟伤害数据
                    val damageInfo = delayDamageData[taskId]

                    if (damageInfo != null) {
                        // 使用 DamageSource.builder() 创建伤害源
                        val damageType = DamageCauseMapper.mapDamageType(damageInfo.damageCause)
                        val damageSource = if (damageInfo.damager != null) {
                            DamageSource.builder(damageType)
                                .withDirectEntity(damageInfo.damager)
                                .build()
                        } else {
                            DamageSource.builder(damageType).build()
                        }

                        // 使用伤害源造成伤害
                        player.damage(delayedDamage, damageSource)
                    } else {
                        // 如果没有保存的数据，使用默认方式
                        player.damage(delayedDamage)
                    }
                } finally {
                    // 如果伤害没有被处理（例如事件被取消），清除标记
                    if (player.persistentDataContainer.has(DELAY_DAMAGE_BASE_KEY, PersistentDataType.LONG)) {
                        player.persistentDataContainer.remove(DELAY_DAMAGE_BASE_KEY)
                    }
                    // 从任务列表中移除
                    playerDelayTasks[playerUuid]?.remove(taskId)
                    // 清理延迟伤害数据
                    delayDamageData.remove(taskId)
                }
            },
            DELAY_SECONDS * 20L  // 转换为tick（20 tick = 1秒）
        )
    }
}
