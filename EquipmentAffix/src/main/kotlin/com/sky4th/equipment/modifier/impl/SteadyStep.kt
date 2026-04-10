package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.util.AttributeModifierUtil
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack

/**
 * 稳行词条
 * 效果：在水中行走不受水中阻力影响
 */
class SteadyStep : com.sky4th.equipment.modifier.ConfiguredModifier("steady_step") {

    companion object {
        private val STEADY_STEP_MODIFIER_KEY = NamespacedKey("equipment_affix", "steady_step")
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
     * 一次性初始化水中移动效率属性
     */
    override fun onSmithing(item: ItemStack, level: Int, isInit: Boolean) {
        AttributeModifierUtil.updateItemAttribute(
            item,
            Attribute.GENERIC_WATER_MOVEMENT_EFFICIENCY,
            STEADY_STEP_MODIFIER_KEY,
            1.0,
            AttributeModifier.Operation.ADD_NUMBER,
            org.bukkit.inventory.EquipmentSlotGroup.FEET
        )
    }

    /**
     * 当词条从装备上移除时调用
     * 清理水中移动效率属性
     */
    override fun onUnsmithing(item: ItemStack, level: Int) {
        AttributeModifierUtil.removeItemAttributeModifier(
            item,
            Attribute.GENERIC_WATER_MOVEMENT_EFFICIENCY,
            STEADY_STEP_MODIFIER_KEY
        )
    }
}
