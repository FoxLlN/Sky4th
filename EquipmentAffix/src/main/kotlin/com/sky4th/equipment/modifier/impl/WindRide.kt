package com.sky4th.equipment.modifier.impl

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityToggleGlideEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector

/**
 * 乘风词条
 * 效果：进入滑翔阶段时给予初始动力
 * 
 * 1级：给予80%的初始速度加成
 * 2级：给予100%的初始速度加成
 * 3级：给予120%的初始速度加成
 */
class WindRide : com.sky4th.equipment.modifier.ConfiguredModifier("wind_ride") {

    companion object {
        // 每级速度加成
        private val SPEED_BONUS = arrayOf(0.80, 1.00, 1.20)
    }

    override fun getEventTypes(): List<Class<out Event>> = listOf(
        EntityToggleGlideEvent::class.java
    )

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: com.sky4th.equipment.modifier.config.PlayerRole
    ) {
        if (event !is EntityToggleGlideEvent) return
        if (event.entity !is Player) return

        val glidingPlayer = event.entity as Player

        // 只在开始滑翔时给予初始动力
        if (event.isGliding) {
            applyInitialGlideSpeed(glidingPlayer, level)
        }
    }

    override fun onRemove(player: Player) {
        // 词条移除时不需要特殊处理
    }

    override fun onInit(player: Player, item: ItemStack, level: Int) {
        // 初始化时不需要特殊处理
    }

    /**
     * 应用初始滑行速度加成
     * 通过直接修改玩家速度向量来实现
     */
    private fun applyInitialGlideSpeed(player: Player, level: Int) {
        val speedBonus = SPEED_BONUS.getOrNull(level - 1) ?: return

        // 获取玩家当前速度
        val velocity = player.velocity
        
        // 获取玩家当前朝向的方向向量（水平方向，忽略上下）
        val direction = player.location.direction.clone()
        direction.y = 0.0
        direction.normalize()

        val currentVel = player.velocity
        val newVel = currentVel.clone()
        newVel.x += direction.x * speedBonus
        newVel.z += direction.z * speedBonus
        // 限制最大速度（可选）
        val maxSpeed = 3.0
        if (newVel.length() > maxSpeed) {
            newVel.multiply(maxSpeed / newVel.length())
        }
        player.velocity = newVel
    }
}
