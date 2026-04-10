package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.util.AttributeModifierUtil
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack

/**
 * 屹立不倒词条
 * 效果：增加重甲抗击退属性
 * 1级：抗击退+2%
 * 2级：抗击退+3%
 * 3级：抗击退+4%
 */
class Unyielding : com.sky4th.equipment.modifier.ConfiguredModifier("unyielding") {

    companion object {
        private val UNYIELDING_MODIFIER_KEY = NamespacedKey("equipment_affix", "unyielding")

        // 各等级的抗击退加成（百分比）
        private val KNOCKBACK_RESISTANCE_BONUS = doubleArrayOf(0.02, 0.03, 0.04)
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
     * 增加抗击退属性
     */
    override fun onSmithing(item: ItemStack, level: Int, isInit: Boolean) {
        val bonus = if (level - 1 in KNOCKBACK_RESISTANCE_BONUS.indices) KNOCKBACK_RESISTANCE_BONUS[level - 1] else return

        AttributeModifierUtil.updateItemAttribute(
            item,
            Attribute.GENERIC_KNOCKBACK_RESISTANCE,
            UNYIELDING_MODIFIER_KEY,
            bonus,
            AttributeModifier.Operation.ADD_NUMBER,
            org.bukkit.inventory.EquipmentSlotGroup.ARMOR
        )
    }

    /**
     * 当词条从装备上移除时调用
     * 移除抗击退属性
     */
    override fun onUnsmithing(item: ItemStack, level: Int) {
        AttributeModifierUtil.removeItemAttributeModifier(
            item,
            Attribute.GENERIC_KNOCKBACK_RESISTANCE,
            UNYIELDING_MODIFIER_KEY
        )
    }
}
