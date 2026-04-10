package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.MaterialEffectModifier
import com.sky4th.equipment.util.AttributeModifierUtil
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack

/**
 * 致密词条
 * 效果：装备持有者的移动速度降低10%
 */
class Dense : com.sky4th.equipment.modifier.ConfiguredModifier("dense"), MaterialEffectModifier {

    companion object {
        private val DENSE_MODIFIER_KEY = NamespacedKey("equipment_affix", "dense")
    }

    // 该词条不监听任何事件，效果通过属性修饰器实现
    override fun getEventTypes(): List<Class<out Event>> = emptyList()

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: com.sky4th.equipment.modifier.config.PlayerRole
    ) {
        // 不处理任何事件
    }

    /**
     * 当词条进入活跃状态时调用，应用移动速度降低效果
     * 如果物品已经有该词条的效果，则不重复应用
     */
    override fun onInit(player: Player, item: ItemStack, level: Int) {
        // 如果物品没有该词条的效果，则应用移动速度降低
        if (!hasDenseModifier(item)) {
            // 应用移动速度降低效果
            AttributeModifierUtil.updateItemAttribute(
                item,
                Attribute.GENERIC_MOVEMENT_SPEED,
                DENSE_MODIFIER_KEY,
                -0.1,
                org.bukkit.attribute.AttributeModifier.Operation.ADD_SCALAR,
                org.bukkit.inventory.EquipmentSlotGroup.MAINHAND
            )
        }
    }

    /**
     * 检查物品是否已经有该词条的效果
     */
    private fun hasDenseModifier(item: ItemStack): Boolean {
        val meta = item.itemMeta ?: return false
        val attributeModifiers = meta.getAttributeModifiers(Attribute.GENERIC_MOVEMENT_SPEED) ?: return false
        return attributeModifiers.any { it.key == DENSE_MODIFIER_KEY }
    }

    /**
     * 获取词条ID
     */
    override fun getMaterialEffectId(): String = "dense"
}
