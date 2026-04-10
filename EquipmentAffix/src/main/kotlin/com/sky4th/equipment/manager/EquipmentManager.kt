package com.sky4th.equipment.manager

import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.NamespacedKey
import com.sky4th.equipment.attributes.EquipmentAttributes
import com.sky4th.equipment.attributes.EquipmentCategory
import com.sky4th.equipment.registry.EquipmentRegistry
import com.sky4th.equipment.data.NBTEquipmentDataManager
import com.sky4th.equipment.manager.LoreDisplayManager
import com.sky4th.equipment.util.EquipmentModifierUtil
import com.sky4th.equipment.registry.EquipmentType

/**
 * 装备管理器
 * 整合所有装备相关功能，提供统一的接口
 * 使用NBT标签存储装备数据
 */
object EquipmentManager {

    /**
     * 创建装备
     */
    fun createEquipment(equipmentId: String): ItemStack? {
        val equipmentType = EquipmentRegistry.getEquipmentType(equipmentId) ?: return null
        val item = ItemStack(equipmentType.material)

        // 设置装备类型ID到NBT
        NBTEquipmentDataManager.setEquipmentId(item, equipmentId)
        // 清除缓存（新创建的装备不需要缓存，但为了保险起见）
        com.sky4th.equipment.cache.EquipmentAttributesCache.invalidate(item)

        // 初始化槽位（根据初始熟练度等级计算）
        SlotManager.updateSlots(item, 0)

        // 应用默认属性
        val attributes = equipmentType.defaultAttributes
        val resultItem = attributes.applyToItemStack(item, equipmentType.displayName, equipmentType.maxDurability)

        EquipmentModifierUtil.initEquipmentModifier(resultItem, equipmentType)
        
        // 使用LoreDisplayManager生成简单描述（包含切换提示）
        val finalItem = LoreDisplayManager.modifyItemLore(resultItem, false)

        return finalItem
    }

    /**
     * 创建装备并添加自定义属性
     */
    fun createEquipment(
        equipmentId: String,
        proficiencyLevel: Int = 0,
        proficiency: Int = 0,
        affixes: Map<String, Int> = emptyMap(),
        enchantments: Map<Enchantment, Int> = emptyMap(),
        dodgeChance: Double = 0.0,
        knockbackResistance: Double = 0.0,
        hungerPenalty: Double = 0.0,
        movementPenalty: Double = 0.0,
        materialEffect: String
    ): ItemStack? {
        val equipmentType = EquipmentRegistry.getEquipmentType(equipmentId) ?: return null

        // 创建属性
        val attributes = EquipmentAttributes(
            material = equipmentType.material,
            proficiencyLevel = proficiencyLevel,
            proficiency = proficiency,
            affixes = affixes,
            enchantments = enchantments,
            dodgeChance = dodgeChance,
            knockbackResistance = knockbackResistance,
            hungerPenalty = hungerPenalty,
            movementPenalty = movementPenalty,
            materialEffect = materialEffect
        )

        val item = ItemStack(equipmentType.material)

        // 设置装备类型ID到NBT
        NBTEquipmentDataManager.setEquipmentId(item, equipmentId)
        // 清除缓存（新创建的装备不需要缓存，但为了保险起见）
        com.sky4th.equipment.cache.EquipmentAttributesCache.invalidate(item)

        // 初始化槽位（根据初始熟练度等级计算）
        SlotManager.updateSlots(item, proficiencyLevel)
        
        val resultItem = attributes.applyToItemStack(item, equipmentType.displayName, equipmentType.maxDurability)

        EquipmentModifierUtil.initEquipmentModifier(resultItem, equipmentType)

        return resultItem
    }

    /**
     * 获取装备属性
     */
    fun getEquipmentAttributes(item: ItemStack): EquipmentAttributes {
        return EquipmentAttributes.fromItemStack(item)
    }

    /**
     * 应用装备属性
     */
    fun applyEquipmentAttributes(item: ItemStack, attributes: EquipmentAttributes): ItemStack {
        return attributes.applyToItemStack(item)
    }

    /**
     * 添加词条
     */
    fun addAffix(item: ItemStack, affixId: String, level: Int = 1): ItemStack {
        val attributes = getEquipmentAttributes(item)
        val newAttributes = attributes.addAffix(affixId, level)
        return applyEquipmentAttributes(item, newAttributes)
    }

    /**
     * 移除词条
     */
    fun removeAffix(item: ItemStack, affixId: String): ItemStack {
        val attributes = getEquipmentAttributes(item)
        val newAttributes = attributes.removeAffix(affixId)
        return applyEquipmentAttributes(item, newAttributes)
    }

    /**
     * 检查是否有词条
     */
    fun hasAffix(item: ItemStack, affixId: String): Boolean {
        val attributes = getEquipmentAttributes(item)
        return attributes.hasAffix(affixId)
    }

    /**
     * 获取所有词条
     */
    fun getAffixes(item: ItemStack): Map<String, Int> {
        val attributes = getEquipmentAttributes(item)
        return attributes.affixes
    }

    /**
     * 添加附魔
     */
    fun addEnchantment(item: ItemStack, enchantment: org.bukkit.enchantments.Enchantment, level: Int): ItemStack {
        val attributes = getEquipmentAttributes(item)
        val newAttributes = attributes.addEnchantment(enchantment, level)
        return applyEquipmentAttributes(item, newAttributes)
    }

    /**
     * 移除附魔
     */
    fun removeEnchantment(item: ItemStack, enchantment: org.bukkit.enchantments.Enchantment): ItemStack {
        val attributes = getEquipmentAttributes(item)
        val newAttributes = attributes.removeEnchantment(enchantment)
        return applyEquipmentAttributes(item, newAttributes)
    }

    /**
     * 检查是否有附魔
     */
    fun hasEnchantment(item: ItemStack, enchantment: org.bukkit.enchantments.Enchantment): Boolean {
        val attributes = getEquipmentAttributes(item)
        return attributes.hasEnchantment(enchantment)
    }

    /**
     * 获取所有附魔
     */
    fun getEnchantments(item: ItemStack): Map<org.bukkit.enchantments.Enchantment, Int> {
        val attributes = getEquipmentAttributes(item)
        return attributes.enchantments
    }

    /**
     * 记录装备使用/受伤
     * 武器使用一次或装备抗伤一次增加一次计数
     * 熟练度绑定在具体的武器/装备上
     */
    fun recordUsage(item: ItemStack): ItemStack {
        val attributes = getEquipmentAttributes(item)
        val newAttributes = attributes.increaseProficiency()
        return applyEquipmentAttributes(item, newAttributes)
    }

    /**
     * 获取装备的使用/受伤次数（熟练度）
     */
    fun getUsageCount(item: ItemStack): Int {
        val attributes = getEquipmentAttributes(item)
        return attributes.proficiency
    }

    /**
     * 获取熟练度等级
     */
    fun getProficiencyLevel(item: ItemStack): Int {
        val attributes = getEquipmentAttributes(item)
        return attributes.proficiencyLevel
    }

    /**
     * 获取当前等级的进度（0.0 - 1.0）
     */
    fun getLevelProgress(item: ItemStack): Double {
        val attributes = getEquipmentAttributes(item)
        return attributes.getLevelProgress()
    }

    /**
     * 获取下一级所需的使用/受伤次数
     */
    fun getRequiredUsageForNextLevel(item: ItemStack): Int {
        val attributes = getEquipmentAttributes(item)
        return attributes.getRequiredUsageForNextLevel()
    }
}