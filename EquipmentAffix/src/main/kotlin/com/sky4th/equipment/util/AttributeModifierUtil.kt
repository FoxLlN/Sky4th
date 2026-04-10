package com.sky4th.equipment.util

import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.meta.Damageable
import org.bukkit.persistence.PersistentDataType
import com.sky4th.equipment.registry.EquipmentType

/**
 * 属性修饰符工具类
 * 提供统一的属性修改方法，用于管理AttributeModifier的添加、替换和移除
 */
object AttributeModifierUtil {

    /**
     * 通用的属性更新方法
     * 统一处理AttributeModifier的添加和替换
     * 
     * @param player 玩家对象
     * @param attribute 要修改的属性
     * @param modifierKey 修饰符的键（用于标识和替换）
     * @param value 修饰符的值
     * @param operation 修饰符的操作类型
     * @param slotGroup 适用的装备槽位组
     * @param nameKey 命名空间键的前缀（用于标识命名空间）
     */
    fun updateAttribute(
        player: Player,
        attribute: Attribute,
        modifierKey: String,
        value: Double,
        operation: AttributeModifier.Operation,
        slotGroup: EquipmentSlotGroup,
        nameKey: String = "skycore_world"
    ) {
        val attr = player.getAttribute(attribute) ?: return
        // 使用 NamespacedKey 来确保修饰符可以被正确替换
        val key = NamespacedKey(nameKey, modifierKey)
        val existingModifier = attr.modifiers.find { it.key == key }

        if (existingModifier != null) {
            attr.removeModifier(existingModifier)
        }

        // 添加新的修饰符
        val modifier = AttributeModifier(
            key,
            value,
            operation,
            slotGroup
        )
        attr.addModifier(modifier)
    }

    /**
     * 移除属性修饰符
     * 统一处理AttributeModifier的移除
     * 
     * @param player 玩家对象
     * @param attribute 要修改的属性
     * @param modifierKey 修饰符的键（用于标识和移除）
     * @param nameKey 命名空间键的前缀（用于标识命名空间）
     */
    fun removeAttributeModifier(
        player: Player,
        attribute: Attribute,
        modifierKey: String,
        nameKey: String = "skycore_world"
    ) {
        val attr = player.getAttribute(attribute) ?: return

        // 使用 NamespacedKey 来查找并移除修饰符
        val key = NamespacedKey(nameKey, modifierKey)
        val existingModifier = attr.modifiers.find { it.key == key }

        if (existingModifier != null) {
            attr.removeModifier(existingModifier)
        }
    }

    /**
     * 更新物品的属性修饰符
     * 统一处理物品ItemMeta上的AttributeModifier的添加和替换
     *
     * @param item 物品实例
     * @param attribute 要修改的属性
     * @param modifierKey 修饰符的命名空间键（用于标识和替换）
     * @param value 修饰符的值
     * @param operation 修饰符的操作类型
     * @param slotGroup 适用的装备槽位组
     */
    fun updateItemAttribute(
        item: ItemStack,
        attribute: Attribute,
        modifierKey: NamespacedKey,
        value: Double,
        operation: AttributeModifier.Operation,
        slotGroup: EquipmentSlotGroup
    ) {
        val meta = item.itemMeta ?: return
        val damageable = meta as? Damageable ?: return

        // 移除旧的修饰符
        val modifiers = damageable.getAttributeModifiers(attribute)
        if (modifiers != null) {
            modifiers.forEach { modifier ->
                if (modifier.key == modifierKey) {
                    damageable.removeAttributeModifier(attribute, modifier)
                }
            }
        }

        // 添加新修饰符
        val modifier = AttributeModifier(
            modifierKey,
            value,
            operation,
            slotGroup
        )
        damageable.addAttributeModifier(attribute, modifier)
        // 更新物品的元数据
        item.itemMeta = meta
    }

    /**
     * 移除物品的属性修饰符
     * 统一处理物品ItemMeta上的AttributeModifier的移除
     *
     * @param item 物品实例
     * @param attribute 要修改的属性
     * @param modifierKey 修饰符的命名空间键（用于标识和移除）
     */
    fun removeItemAttributeModifier(
        item: ItemStack,
        attribute: Attribute,
        modifierKey: NamespacedKey
    ) {
        val meta = item.itemMeta ?: return
        val damageable = meta as? Damageable ?: return

        // 移除指定的修饰符
        val modifiers = damageable.getAttributeModifiers(attribute)
        if (modifiers != null) {
            modifiers.forEach { modifier ->
                if (modifier.key == modifierKey) {
                    damageable.removeAttributeModifier(attribute, modifier)
                }
            }
        }

        // 更新物品的元数据
        item.itemMeta = meta
    }
}
