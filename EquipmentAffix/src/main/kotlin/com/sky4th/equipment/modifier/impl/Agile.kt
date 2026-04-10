package com.sky4th.equipment.modifier.impl

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack

/**
 * 灵巧词条
 * 效果：闪避+2%/3%/4%
 * 闪避加成由 EquipmentAttributesManager 统一计算和应用
 */
class Agile : com.sky4th.equipment.modifier.ConfiguredModifier("agile") {

    override fun getEventTypes(): List<Class<out Event>> = emptyList()

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: com.sky4th.equipment.modifier.config.PlayerRole
    ) {
        // 不需要处理事件，闪避加成由 EquipmentAttributesManager 统一计算和应用
    }
}
