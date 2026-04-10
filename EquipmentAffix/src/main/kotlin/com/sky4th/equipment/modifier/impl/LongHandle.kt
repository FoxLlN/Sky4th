package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.util.AttributeModifierUtil
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack

/**
 * 长柄词条
 * 效果：增加1格方块交互距离
 */
class LongHandle : com.sky4th.equipment.modifier.ConfiguredModifier("long_handle") {

    companion object {
        private val LONG_HANDLE_MODIFIER_KEY = NamespacedKey("equipment_affix", "long_handle")
    }

    // 不监听任何事件，只依赖onSmithing和onRemove
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
     * 一次性初始化方块交互距离属性
     */
    override fun onSmithing(item: ItemStack, level: Int, isInit: Boolean) {
        AttributeModifierUtil.updateItemAttribute(
            item,
            Attribute.PLAYER_BLOCK_INTERACTION_RANGE,
            LONG_HANDLE_MODIFIER_KEY,
            1.0,
            AttributeModifier.Operation.ADD_NUMBER,
            org.bukkit.inventory.EquipmentSlotGroup.MAINHAND
        )
    }

    /**
     * 当词条从装备上移除时调用
     * 清理方块交互距离属性
     */
    override fun onUnsmithing(item: ItemStack, level: Int) {
        AttributeModifierUtil.removeItemAttributeModifier(
            item,
            Attribute.PLAYER_BLOCK_INTERACTION_RANGE,
            LONG_HANDLE_MODIFIER_KEY
        )
    }
}
