
package com.sky4th.equipment.modifier.impl

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable

/**
 * 镀层词条
 * 效果：每级增加100点耐久度上限（最多5级）
 * 适用于：所有装备
 */
class Sturdy : com.sky4th.equipment.modifier.ConfiguredModifier("sturdy") {

    companion object {
        // 每级增加的耐久度
        private const val DURABILITY_BONUS_PER_LEVEL = 100
        // 最大等级
        private const val MAX_LEVEL = 5
    }

    override fun getEventTypes(): List<Class<out Event>> = emptyList()
    
    override fun handle(
        event: Event,
        player: org.bukkit.entity.Player,
        item: ItemStack,
        level: Int,
        playerRole: com.sky4th.equipment.modifier.config.PlayerRole
    ) {
        // 不需要处理事件
    }

    /**
     * 当词条被锻造到装备上时调用
     */
    override fun onSmithing(item: ItemStack, level: Int, isInit: Boolean) {
        // 限制等级不超过最大等级
        val effectiveLevel = level.coerceAtMost(MAX_LEVEL)

        // 获取物品的元数据
        val itemMeta = item.itemMeta ?: return
        if (itemMeta !is Damageable) {
            return
        }

        // 检查物品是否有耐久度
        var currentMaxDurability = 0

        if (itemMeta.hasMaxDamage()) {
            // 如果元数据中有耐久度信息，直接使用
            currentMaxDurability = itemMeta.maxDamage
        } else {
            // 如果元数据中没有耐久度信息，尝试从物品类型获取默认耐久度
            val material = item.type
            if (material.isItem && material.maxDurability > 0) {
                currentMaxDurability = material.maxDurability.toInt()
            } else {
                return // 物品没有耐久度，无需处理
            }
        }
        if (currentMaxDurability == 0) {
            return // 物品没有耐久度，无需处理
        }

        var bonusDurability = when (isInit) {
            true -> DURABILITY_BONUS_PER_LEVEL * effectiveLevel // 初始化时直接根据等级计算总加成
            false -> DURABILITY_BONUS_PER_LEVEL
        }

        // 设置新的耐久度上限
        val newMaxDurability = currentMaxDurability + bonusDurability
        // 应用新的耐久度
        itemMeta.setMaxDamage(newMaxDurability) 
        item.itemMeta = itemMeta
    }

    override fun onRemove(player: Player) {
        // 移除时无需特殊处理
    }
}
