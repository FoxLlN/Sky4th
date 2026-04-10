package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.util.AttributeModifierUtil
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack

/**
 * 轻盈词条
 * 效果：削弱重甲惩罚，增加移动速度
 * 1级：增加1%移动速度
 * 2级：增加1.5%移动速度
 * 3级：增加2%移动速度
 */
class Lightweight : com.sky4th.equipment.modifier.ConfiguredModifier("lightweight") {

    companion object {
        private val LIGHTWEIGHT_MODIFIER_KEY = NamespacedKey("equipment_affix", "lightweight")

        // 各等级的移动速度加成（百分比）
        private val SPEED_BONUS = doubleArrayOf(0.01, 0.015, 0.02)
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
     * 增加移动速度属性
     */
    override fun onSmithing(item: ItemStack, level: Int, isInit: Boolean) {
        val bonus = if (level - 1 in SPEED_BONUS.indices) SPEED_BONUS[level - 1] else return

        AttributeModifierUtil.updateItemAttribute(
            item,
            Attribute.GENERIC_MOVEMENT_SPEED,
            LIGHTWEIGHT_MODIFIER_KEY,
            bonus,
            AttributeModifier.Operation.ADD_SCALAR,
            org.bukkit.inventory.EquipmentSlotGroup.ARMOR
        )
    }

    /**
     * 当词条从装备上移除时调用
     * 移除移动速度属性
     */
    override fun onUnsmithing(item: ItemStack, level: Int) {
        AttributeModifierUtil.removeItemAttributeModifier(
            item,
            Attribute.GENERIC_MOVEMENT_SPEED,
            LIGHTWEIGHT_MODIFIER_KEY
        )
    }
}
