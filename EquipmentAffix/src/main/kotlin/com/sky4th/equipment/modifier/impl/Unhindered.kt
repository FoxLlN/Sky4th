package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Arrow
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.inventory.ItemStack
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 无阻词条
 * 效果：箭矢射出后不受重力影响
 * 1级：0.5秒（10 ticks）
 * 2级：1.0秒（20 ticks）
 * 3级：1.5秒（30 ticks）
 */
class Unhindered : com.sky4th.equipment.modifier.ConfiguredModifier("unhindered") {

    companion object {
        // 每级的持续时间（ticks）
        private val DURATION = intArrayOf(10, 20, 30)  // 0.5s/1s/1.5s

        // 存储每个箭矢的重力恢复任务ID
        private val arrowTasks = ConcurrentHashMap<UUID, Long>()

        // 存储每个箭矢的无阻状态
        private val arrowStates = ConcurrentHashMap<UUID, Arrow>()
    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(
            EntityShootBowEvent::class.java,
            ProjectileHitEvent::class.java
        )

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        when (event) {
            is EntityShootBowEvent -> handleShoot(event, player, level, playerRole)
            is ProjectileHitEvent -> handleHit(event)
        }
    }

    /**
     * 处理射箭事件
     */
    private fun handleShoot(event: EntityShootBowEvent, player: Player, level: Int, playerRole: PlayerRole) {
        // 只处理玩家自己射出的箭
        if (playerRole != PlayerRole.ATTACKER) return

        // 检查是否是箭矢
        val projectile = event.entity
        if (projectile !is Arrow) return

        // 获取当前等级的持续时间
        val duration = DURATION.getOrNull(level - 1) ?: return

        // 设置箭矢不受重力影响
        projectile.setGravity(false)

        // 存储箭矢状态
        arrowStates[projectile.uniqueId] = projectile

        // 生成唯一的任务ID
        val taskId = System.currentTimeMillis()
        arrowTasks[projectile.uniqueId] = taskId

        // 使用调度器在持续时间后恢复重力
        org.bukkit.Bukkit.getScheduler().runTaskLater(
            com.sky4th.equipment.EquipmentAffix.instance,
            Runnable {
                // 检查箭矢是否还存在且未死亡
                val arrow = arrowStates.remove(projectile.uniqueId)
                if (arrow != null && !arrow.isDead) {
                    arrow.setGravity(true)
                }
                // 清理任务记录
                arrowTasks.remove(projectile.uniqueId)
            },
            duration.toLong()
        )
    }

    /**
     * 处理箭矢命中事件
     * 清理未执行的重力恢复任务
     */
    private fun handleHit(event: ProjectileHitEvent) {
        val projectile = event.entity
        if (projectile !is Arrow) return

        // 取消未执行的重力恢复任务
        val taskId = arrowTasks.remove(projectile.uniqueId)
        if (taskId != null) {
            // 恢复箭矢的重力
            projectile.setGravity(true)
            // 清理箭矢状态
            arrowStates.remove(projectile.uniqueId)
        }
    }
}
