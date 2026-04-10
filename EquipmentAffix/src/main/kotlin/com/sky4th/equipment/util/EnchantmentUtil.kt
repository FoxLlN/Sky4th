package com.sky4th.equipment.util

import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemFlag
import com.sky4th.equipment.data.NBTEquipmentDataManager
import com.sky4th.equipment.manager.LoreDisplayManager
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType

/**
 * 附魔工具类
 * 提供附魔相关的通用方法
 */
object EnchantmentUtil {

    private val DETAILED_LORE_KEY = NamespacedKey("sky_equipment", "detailed_lore")

    /**
     * 更新物品的附魔显示
     * 隐藏原版附魔，显示自定义描述
     * @param item 要更新的物品
     */
    fun updateEnchantmentDisplay(item: ItemStack) {
        val meta = item.itemMeta ?: return

        // 隐藏原版附魔显示
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)

        // 获取当前是否处于详细模式
        val isDetailed = isDetailedDescription(item)

        // 更新物品meta
        item.itemMeta = meta

        // 使用LoreDisplayManager更新描述
        val modifiedItem = LoreDisplayManager.modifyItemLore(item, isDetailed)

        // 直接使用修改后的物品的所有属性
        item.itemMeta = modifiedItem.itemMeta
        item.amount = modifiedItem.amount
    }

    /**
     * 获取物品当前的详细描述模式
     * @param item 物品
     * @return true = 详细模式，false = 简单模式
     */
    fun isDetailedDescription(item: ItemStack): Boolean {
        val meta = item.itemMeta ?: return false
        val container = meta.persistentDataContainer
        return container.getOrDefault(DETAILED_LORE_KEY, PersistentDataType.BYTE, 0) == 1.toByte()
    }

    /**
     * 设置物品的详细描述模式
     * @param item 物品
     * @param isDetailed 是否为详细模式
     */
    fun setDetailedDescription(item: ItemStack, isDetailed: Boolean) {
        val meta = item.itemMeta ?: return
        val container = meta.persistentDataContainer
        container.set(DETAILED_LORE_KEY, PersistentDataType.BYTE, if (isDetailed) 1 else 0)
        item.itemMeta = meta
    }

    /**
     * 检查附魔是否与装备的词条冲突
     * @param item 装备
     * @param enchantment 要检查的附魔
     * @return true 如果存在冲突，false 如果不存在冲突
     */
    fun hasEnchantmentConflict(item: ItemStack, enchantment: org.bukkit.enchantments.Enchantment): Boolean {
        val existingAffixes = NBTEquipmentDataManager.getAffixes(item)
        if (existingAffixes.isEmpty()) return false

        for ((affixId, _) in existingAffixes) {
            val modifier = com.sky4th.equipment.modifier.ModifierManager.instance.getModifier(affixId)
            if (modifier != null) {
                val conflictingEnchantments = modifier.getConflictingEnchantments()
                if (enchantment in conflictingEnchantments) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * 获取装备可用的附魔槽位数量
     * @param item 装备
     * @return 可用的附魔槽位数量，如果无法获取则返回 0
     */
    fun getAvailableEnchantmentSlots(item: ItemStack): Int {
        val maxSlots = NBTEquipmentDataManager.getMaxEnchantmentSlots(item)
        if (maxSlots <= 0) return 0

        val currentEnchantments = item.enchantments.size
        return maxSlots - currentEnchantments
    }

    /**
     * 检查装备是否可以添加附魔
     * @param item 装备
     * @param enchantment 要添加的附魔
     * @return true 如果可以添加，false 如果不能添加
     */
    fun canAddEnchantment(item: ItemStack, enchantment: org.bukkit.enchantments.Enchantment): Boolean {
        // 检查是否有可用槽位
        if (getAvailableEnchantmentSlots(item) <= 0) {
            return false
        }

        // 检查是否与现有词条冲突
        if (hasEnchantmentConflict(item, enchantment)) {
            return false
        }

        return true
    }

    /**
     * 尝试给装备添加附魔
     * 自动检查槽位、词条冲突等所有限制
     * @param item 装备
     * @param enchantment 要添加的附魔
     * @param level 附魔等级
     * @param updateDisplay 是否更新附魔显示（默认为 true）
     * @return true 如果添加成功，false 如果添加失败
     */
    fun tryAddEnchantment(
        item: ItemStack,
        enchantment: org.bukkit.enchantments.Enchantment,
        level: Int,
        updateDisplay: Boolean = true
    ): Boolean {
        // 检查是否可以添加附魔
        if (!canAddEnchantment(item, enchantment)) {
            return false
        }

        // 添加附魔
        item.addUnsafeEnchantment(enchantment, level)

        // 更新附魔显示
        if (updateDisplay) {
            updateEnchantmentDisplay(item)
        }

        return true
    }

    /**
     * 批量尝试给装备添加附魔
     * 自动检查槽位、词条冲突等所有限制
     * 按照传入的顺序尝试添加，直到槽位用完或所有附魔尝试完毕
     * @param item 装备
     * @param enchantments 要添加的附魔列表（附魔 -> 等级）
     * @param updateDisplay 是否更新附魔显示（默认为 true）
     * @return 成功添加的附魔数量
     */
    fun tryAddEnchantments(
        item: ItemStack,
        enchantments: Map<org.bukkit.enchantments.Enchantment, Int>,
        updateDisplay: Boolean = true
    ): Int {
        var successCount = 0

        for ((enchantment, level) in enchantments) {
            if (tryAddEnchantment(item, enchantment, level, false)) {
                successCount++
            }
        }

        // 最后统一更新显示
        if (updateDisplay && successCount > 0) {
            updateEnchantmentDisplay(item)
        }

        return successCount
    }

    /**
     * 从源装备转移附魔到目标装备
     * 自动检查槽位、词条冲突等所有限制
     * 按照源装备的附魔顺序转移
     * @param source 源装备
     * @param target 目标装备
     * @param updateDisplay 是否更新附魔显示（默认为 true）
     * @return 成功转移的附魔数量
     */
    fun transferEnchantments(
        source: ItemStack,
        target: ItemStack,
        updateDisplay: Boolean = true
    ): Int {
        val sourceEnchantments = source.enchantments
        if (sourceEnchantments.isEmpty()) return 0

        return tryAddEnchantments(target, sourceEnchantments, updateDisplay)
    }

    /**
     * 从源装备转移附魔到目标装备（忽略槽位限制）
     * 完全保留所有附魔，不受槽位限制
     * 仍然会检查附魔是否适用于装备类型和词条冲突
     * 按照源装备的附魔顺序转移
     * @param source 源装备
     * @param target 目标装备
     * @param updateDisplay 是否更新附魔显示（默认为 true）
     * @return 成功转移的附魔数量
     */
    fun transferEnchantmentsIgnoreSlotLimit(
        source: ItemStack,
        target: ItemStack,
        updateDisplay: Boolean = true
    ): Int {
        val sourceEnchantments = source.enchantments
        if (sourceEnchantments.isEmpty()) return 0

        var successCount = 0

        for ((enchantment, level) in sourceEnchantments) {
            // 检查是否与现有词条冲突
            if (hasEnchantmentConflict(target, enchantment)) {
                continue
            }

            // 添加附魔（忽略槽位限制）
            target.addUnsafeEnchantment(enchantment, level)
            successCount++
        }

        // 更新附魔显示
        if (updateDisplay && successCount > 0) {
            updateEnchantmentDisplay(target)
        }

        return successCount
    }
}
