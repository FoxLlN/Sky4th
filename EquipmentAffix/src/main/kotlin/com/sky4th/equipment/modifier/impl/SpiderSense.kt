package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import com.sky4th.equipment.util.GlowingEntityUtil
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityTargetEvent
import org.bukkit.inventory.ItemStack
import java.util.concurrent.ConcurrentHashMap

/**
 * 蜘蛛感应词条
 * 效果：当玩家被怪物锁定为攻击目标时，使怪物发光
 */
class SpiderSense : com.sky4th.equipment.modifier.ConfiguredModifier("spider_sense") {

    companion object {
        // 存储每个实体上次触发蜘蛛感应的时间戳
        // Key: 实体ID
        // Value: 上次触发时间戳（毫秒）
        private val cooldownMap = ConcurrentHashMap<Int, Long>()

        // 发光持续时间（5秒）
        private const val GLOW_DURATION = 5L

        // 冷却时间（毫秒）- 10秒
        private const val COOLDOWN_TIME = 10 * 1000L

    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(EntityTargetEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 只处理实体目标事件
        if (event !is EntityTargetEvent) {
            return
        }

        // 只处理玩家被锁定为目标的情况
        if (event.target != player) {
            return
        }

        // 获取锁定玩家的实体
        val attacker = event.entity
        // 排除标记系统的显示实体
        if (attacker !is LivingEntity || attacker is org.bukkit.entity.ItemDisplay || attacker is org.bukkit.entity.TextDisplay) {
            return
        }

        // 检查冷却时间
        val currentTime = System.currentTimeMillis()
        val lastTriggerTime = cooldownMap[attacker.entityId]

        // 如果在冷却时间内，不触发效果
        if (lastTriggerTime != null && currentTime - lastTriggerTime < COOLDOWN_TIME) {
            return
        }

        // 更新冷却时间
        cooldownMap[attacker.entityId] = currentTime

        // 使用GlowingEntityUtil设置发光效果（仅对玩家可见）
        // 红色发光，持续2秒
        GlowingEntityUtil.setGlowForPlayer(
            attacker,
            player,
            GLOW_DURATION,
            org.bukkit.Color.RED
        )
    }

    override fun onRemove(player: Player) {
    }
}
