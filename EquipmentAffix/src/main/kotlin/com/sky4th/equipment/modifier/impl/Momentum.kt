package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.util.AttributeModifierUtil
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack

/**
 * 动力词条
 * 效果：增加武器击退距离，1级+1格，2级+2格，3级+3格
 */
class Momentum : com.sky4th.equipment.modifier.ConfiguredModifier("momentum") {

    companion object {
        private val MOMENTUM_MODIFIER_KEY = NamespacedKey("equipment_affix", "momentum")
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
     * 一次性初始化击退距离属性
     */
    override fun onSmithing(item: ItemStack, level: Int, isInit: Boolean) {
        // 根据等级计算击退距离加成：1级+1格，2级+2格，3级+3格（0.4/0.8/1.2)
        val knockbackBonus = level.toDouble() * 0.4

        AttributeModifierUtil.updateItemAttribute(
            item,
            Attribute.GENERIC_ATTACK_KNOCKBACK,
            MOMENTUM_MODIFIER_KEY,
            knockbackBonus,
            AttributeModifier.Operation.ADD_NUMBER,
            org.bukkit.inventory.EquipmentSlotGroup.MAINHAND
        )
    }

    /**
     * 当词条从装备上移除时调用
     * 清理击退距离属性
     */
    override fun onUnsmithing(item: ItemStack, level: Int) {
        AttributeModifierUtil.removeItemAttributeModifier(
            item,
            Attribute.GENERIC_ATTACK_KNOCKBACK,
            MOMENTUM_MODIFIER_KEY
        )
    }
}
