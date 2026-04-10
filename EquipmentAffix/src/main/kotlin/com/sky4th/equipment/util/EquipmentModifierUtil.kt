package com.sky4th.equipment.util

import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.EquipmentSlotGroup
import com.sky4th.equipment.registry.EquipmentType

/**
 * 属性修饰符工具类
 * 提供统一的装备属性修改方法，用于管理EquipmentModifier的添加、替换和移除
 */
object EquipmentModifierUtil {
    /**
     * 初始化装备修饰符
     * 
     **/
    fun initEquipmentModifier(item: ItemStack, equipmentType: EquipmentType) {
        val meta = item.itemMeta ?: return
        val material = equipmentType.material
        when {
            // 护甲类物品
            material.name.endsWith("HELMET") -> {
                updateEquipment(meta, equipmentType, org.bukkit.inventory.EquipmentSlotGroup.HEAD)
            }
            material.name.endsWith("CHESTPLATE") -> {
                updateEquipment(meta, equipmentType, org.bukkit.inventory.EquipmentSlotGroup.CHEST)
            }
            material.name.endsWith("LEGGINGS") -> {
                updateEquipment(meta, equipmentType, org.bukkit.inventory.EquipmentSlotGroup.LEGS)
            }
            material.name.endsWith("BOOTS") -> {
                updateEquipment(meta, equipmentType, org.bukkit.inventory.EquipmentSlotGroup.FEET)
            }
            // 武器类物品
            material.name.endsWith("AXE") ||
            material.name.endsWith("HOE")  ||
            material.name.endsWith("PICKAXE")  ||
            material.name.endsWith("SHOVEL")  ||
            material.name.endsWith("SWORD") ||
            material.name == "TRIDENT" ||
            material.name == "MACE"-> {
                // 应用攻击伤害
                if (equipmentType.attackDamage > 0) {
                    val damageModifier = AttributeModifier(
                        NamespacedKey("sky_equipment", "attack_damage"),
                        equipmentType.attackDamage,
                        AttributeModifier.Operation.ADD_NUMBER,
                        org.bukkit.inventory.EquipmentSlotGroup.MAINHAND
                    )
                    meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, damageModifier)
                }
                // 应用攻击速度
                if (equipmentType.attackSpeed > 0) {
                    val speed = equipmentType.attackSpeed - 4
                    val speedModifier = AttributeModifier(
                        NamespacedKey("sky_equipment", "attack_speed"),
                        speed,
                        AttributeModifier.Operation.ADD_NUMBER,
                        org.bukkit.inventory.EquipmentSlotGroup.MAINHAND
                    )
                    meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, speedModifier)
                }
            }
        }
        // 隐藏物品上的属性提示
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES)
        item.itemMeta = meta
    }
    
    /** 
     * 更新装备修饰符
     **/
    private fun updateEquipment(meta: ItemMeta, equipmentType: EquipmentType, slotGroup: EquipmentSlotGroup) {
        val slot = when (slotGroup) {
            org.bukkit.inventory.EquipmentSlotGroup.HEAD -> "heat"
            org.bukkit.inventory.EquipmentSlotGroup.CHEST -> "chest"
            org.bukkit.inventory.EquipmentSlotGroup.LEGS -> "legs"
            org.bukkit.inventory.EquipmentSlotGroup.FEET -> "feet"
            else -> "else"
        }
        // 应用护甲值
        if (equipmentType.armor > 0) {
            val armorModifier = AttributeModifier(
                NamespacedKey("sky_equipment", "armor_$slot"),
                equipmentType.armor,
                AttributeModifier.Operation.ADD_NUMBER,
                slotGroup
            )
            meta.addAttributeModifier(Attribute.GENERIC_ARMOR, armorModifier)
        }
        // 应用护甲韧性
        if (equipmentType.toughness > 0) {
            val toughnessModifier = AttributeModifier(
                NamespacedKey("sky_equipment", "toughness_$slot"),
                equipmentType.toughness,
                AttributeModifier.Operation.ADD_NUMBER,
                slotGroup
            )
            meta.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, toughnessModifier)
        }
        var speed = 0.0
        if (equipmentType.defaultAttributes.materialEffect == "fitting") {
                speed += 0.025
            }
        if (equipmentType.defaultAttributes.movementPenalty > 0) {
            speed -= equipmentType.defaultAttributes.movementPenalty
        }
        if (speed != 0.0) {   
            val speedModifier = AttributeModifier(
                NamespacedKey("sky_equipment", "movement_penalty_$slot"),
                speed,
                AttributeModifier.Operation.ADD_SCALAR,
                slotGroup
            )
            meta.addAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED, speedModifier)
        }
        if (equipmentType.defaultAttributes.knockbackResistance > 0) {
            val knockbackModifier = AttributeModifier(
                NamespacedKey("sky_equipment", "knockback_resistance_$slot"),
                equipmentType.defaultAttributes.knockbackResistance,
                AttributeModifier.Operation.ADD_NUMBER,
                slotGroup
            )
            meta.addAttributeModifier(Attribute.GENERIC_KNOCKBACK_RESISTANCE, knockbackModifier)
        }
    }
}
