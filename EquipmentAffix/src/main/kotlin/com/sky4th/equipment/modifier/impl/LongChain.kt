package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.util.AttributeModifierUtil
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack

/**
 * 长链词条
 * 效果：增加1格实体交互距离
 */
class LongChain : com.sky4th.equipment.modifier.ConfiguredModifier("long_chain") {

    companion object {
        private val LONG_CHAIN_MODIFIER_KEY = NamespacedKey("equipment_affix", "long_chain")
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
     * 一次性初始化实体交互距离属性
     */
    override fun onSmithing(item: ItemStack, level: Int, isInit: Boolean) {
        AttributeModifierUtil.updateItemAttribute(
            item,
            Attribute.PLAYER_ENTITY_INTERACTION_RANGE,
            LONG_CHAIN_MODIFIER_KEY,
            1.0,
            AttributeModifier.Operation.ADD_NUMBER,
            org.bukkit.inventory.EquipmentSlotGroup.MAINHAND
        )
    }

    /**
     * 当词条从装备上移除时调用
     * 清理实体交互距离属性
     */
    override fun onUnsmithing(item: ItemStack, level: Int) {
        AttributeModifierUtil.removeItemAttributeModifier(
            item,
            Attribute.PLAYER_ENTITY_INTERACTION_RANGE,
            LONG_CHAIN_MODIFIER_KEY
        )
    }
}
