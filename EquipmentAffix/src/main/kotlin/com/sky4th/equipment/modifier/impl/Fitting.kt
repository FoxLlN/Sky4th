package com.sky4th.equipment.modifier.impl

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack

/**
 * 贴合词条
 * 效果：移动速度+10%
 */
class Fitting : com.sky4th.equipment.modifier.ConfiguredModifier("fitting") {

    override fun getEventTypes(): List<Class<out Event>> = emptyList()

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: com.sky4th.equipment.modifier.config.PlayerRole
    ) {
        // 不需要处理事件，速度加成由 EquipmentAttributesManager 统一计算和应用
    }
}
