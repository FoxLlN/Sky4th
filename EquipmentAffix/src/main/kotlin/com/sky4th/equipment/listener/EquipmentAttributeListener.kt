package com.sky4th.equipment.listener

import com.sky4th.equipment.EquipmentAffix
import com.sky4th.equipment.manager.EquipmentAttributesManager
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.persistence.PersistentDataType
import sky4th.core.api.PlayerAttributesAPI

/**
 * 装备属性监听器
 * 处理装备属性（饥饿惩罚等）的应用
 * 使用SkyCore的PlayerAttributesAPI管理玩家属性
 * 注意：装备变更监听已移至UnifiedEquipmentChangeListener
 */
class EquipmentAttributeListener(
    private val plugin: EquipmentAffix,
    private val attributesManager: EquipmentAttributesManager
) : Listener {

    /**
     * 监听饥饿度变化事件
     * 根据装备的饥饿惩罚调整饥饿度消耗
     * 从 PlayerAttributes 缓存中读取饥饿消耗倍率并应用
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onFoodLevelChange(event: FoodLevelChangeEvent) {
        val player = event.entity as? Player ?: return

        // 从SkyCore的PlayerAttributes缓存中获取饥饿消耗倍率
        val attributes = PlayerAttributesAPI.getAttributes(player.uniqueId)
        val multiplier = attributes.hungerConsumptionMultiplier

        if (multiplier <= 1.0) return

        val originalDiff = event.foodLevel - player.foodLevel
        if (originalDiff >= 0) return // 只处理消耗

        val key = NamespacedKey(plugin, "hunger_fraction")
        val currentFraction = player.persistentDataContainer.getOrDefault(key, PersistentDataType.DOUBLE, 0.0)

        val exactDiff = originalDiff * multiplier
        val totalDiff = exactDiff + currentFraction

        val newDiff = totalDiff.toInt()
        val newFraction = totalDiff - newDiff

        // 存储小数部分
        player.persistentDataContainer.set(key, PersistentDataType.DOUBLE, newFraction)

        event.foodLevel = player.foodLevel + newDiff
    }
}
