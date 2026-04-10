package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.util.AttributeModifierUtil
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack

/**
 * 迅捷词条
 * 效果：攻击速度+10%
 */
class Swift : com.sky4th.equipment.modifier.ConfiguredModifier("swift") {

    companion object {
        // 词条修饰符的命名空间键
        private val SWIFT_MODIFIER_KEY = NamespacedKey("equipment_affix", "swift")
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
     * 一次性初始化攻击速度属性
     */
    override fun onSmithing(item: ItemStack, level: Int, isInit: Boolean) {
        // 攻击速度 +10%
        AttributeModifierUtil.updateItemAttribute(
            item,
            Attribute.GENERIC_ATTACK_SPEED,
            SWIFT_MODIFIER_KEY,
            0.1,
            AttributeModifier.Operation.ADD_SCALAR,
            org.bukkit.inventory.EquipmentSlotGroup.MAINHAND
        )
    }

    /**
     * 当词条从装备上移除时调用
     * 清理攻击速度属性
     */
    override fun onUnsmithing(item: ItemStack, level: Int) {
        AttributeModifierUtil.removeItemAttributeModifier(
            item,
            Attribute.GENERIC_ATTACK_SPEED,
            SWIFT_MODIFIER_KEY
        )
    }
}
