package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.entity.LivingEntity
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack
import java.util.concurrent.ConcurrentHashMap

/**
 * 蓄能词条
 * 效果：受到伤害时存储部分伤害，追加到下次的攻击
 * 每级储存伤害的3%（12级36%），下次攻击附加（最多15点）
 */
class Capacitor : com.sky4th.equipment.modifier.ConfiguredModifier("capacitor") {

    companion object {
        // 每级储存伤害的百分比
        private const val STORAGE_PERCENTAGE_PER_LEVEL = 0.03

        // 最大等级
        private const val MAX_LEVEL = 12

        // 最大附加伤害
        private const val MAX_STORED_DAMAGE = 15.0

        // 存储每个玩家的蓄能量
        // Key: 玩家UUID
        // Value: 蓄能量
        private val storedDamageMap = ConcurrentHashMap<java.util.UUID, Double>()
    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(EntityDamageEvent::class.java, EntityDamageByEntityEvent::class.java)

    override fun handle(
        event: Event,
        player: org.bukkit.entity.Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 限制等级不超过最大等级
        val effectiveLevel = level.coerceAtMost(MAX_LEVEL)

        // 计算储存百分比
        val storagePercentage = effectiveLevel * STORAGE_PERCENTAGE_PER_LEVEL

        // 根据玩家角色处理不同逻辑
        when (playerRole) {
            PlayerRole.DEFENDER -> {
                // 处理玩家受到伤害的事件
                // 存储部分受到的伤害
                val damage = (event as? EntityDamageEvent)?.damage ?: return
                val storedDamage = damage * storagePercentage

                // 获取当前蓄能量并累加
                val currentStored = storedDamageMap.getOrDefault(player.uniqueId, 0.0)
                val newStored = (currentStored + storedDamage).coerceAtMost(MAX_STORED_DAMAGE)
                storedDamageMap[player.uniqueId] = newStored
            }

            PlayerRole.ATTACKER -> {
                // 获取当前蓄能量
                val storedDamage = storedDamageMap.getOrDefault(player.uniqueId, 0.0)

                // 如果没有蓄能量，直接返回
                if (storedDamage <= 0) {
                    return
                }

                // 获取目标实体
                val damageEvent = event as? EntityDamageByEntityEvent ?: return
                val target = damageEvent.entity
                if (target !is LivingEntity) {
                    return
                }

                // 追加蓄能伤害
                val originalDamage = damageEvent.damage
                damageEvent.damage = originalDamage + storedDamage

                // 清空蓄能量
                storedDamageMap.remove(player.uniqueId)
            }

            else -> {}
        }
    }

    override fun onRemove(player: org.bukkit.entity.Player) {
    }
}
