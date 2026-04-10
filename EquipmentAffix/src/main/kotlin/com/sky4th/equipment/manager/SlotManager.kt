
package com.sky4th.equipment.manager

import com.sky4th.equipment.data.NBTEquipmentDataManager
import org.bukkit.inventory.ItemStack

/**
 * 槽位管理器
 * 负责管理装备的锻造词条槽位和附魔槽位
 * 
 * 规则：
 * - 1级：+1 词条槽位
 * - 2级：+1 附魔槽位
 * - 3级：+1 词条槽位
 * - 4级：+1 附魔槽位
 * 
 * 满级（5级）共：2个词条槽位，2个附魔槽位
 */
object SlotManager {

    /**
     * 根据熟练度等级计算锻造词条槽位上限
     * 1级：1个，3级：2个
     */
    fun calculateAffixSlots(proficiencyLevel: Int): Int {
        return when {
            proficiencyLevel >= 3 -> 2
            proficiencyLevel >= 1 -> 1
            else -> 0
        }
    }

    /**
     * 根据熟练度等级计算附魔槽位上限
     * 2级：1个，4级：2个
     */
    fun calculateEnchantmentSlots(proficiencyLevel: Int): Int {
        return when {
            proficiencyLevel >= 4 -> 2
            proficiencyLevel >= 2 -> 1
            else -> 0
        }
    }

    /**
     * 更新装备的槽位上限
     * 根据熟练度等级自动更新锻造词条和附魔槽位
     * 槽位 = 装备类型的基础槽位 + 根据熟练度等级计算的额外槽位
     */
    fun updateSlots(item: ItemStack, proficiencyLevel: Int) {
        val equipmentId = NBTEquipmentDataManager.getEquipmentId(item) ?: return
        val equipmentType = com.sky4th.equipment.registry.EquipmentRegistry.getEquipmentType(equipmentId) ?: return

        // 获取装备类型的基础槽位
        val baseAffixSlots = equipmentType.maxAffixSlots
        val baseEnchantmentSlots = equipmentType.maxEnchantmentSlots

        // 计算根据熟练度等级的额外槽位
        val extraAffixSlots = calculateAffixSlots(proficiencyLevel)
        val extraEnchantmentSlots = calculateEnchantmentSlots(proficiencyLevel)

        // 最终槽位 = 基础槽位 + 额外槽位
        val finalAffixSlots = baseAffixSlots + extraAffixSlots
        val finalEnchantmentSlots = baseEnchantmentSlots + extraEnchantmentSlots

        NBTEquipmentDataManager.setMaxAffixSlots(item, finalAffixSlots)
        NBTEquipmentDataManager.setMaxEnchantmentSlots(item, finalEnchantmentSlots)
        // 清除缓存
        com.sky4th.equipment.cache.EquipmentAttributesCache.invalidate(item)
        com.sky4th.equipment.cache.LoreDisplayCache.invalidate(item)
    }

    /**
     * 获取装备的锻造词条槽位上限
     */
    fun getAffixSlots(item: ItemStack): Int {
        val equipmentId = NBTEquipmentDataManager.getEquipmentId(item) ?: return 0
        val equipmentType = com.sky4th.equipment.registry.EquipmentRegistry.getEquipmentType(equipmentId) ?: return 0
        val proficiencyLevel = NBTEquipmentDataManager.getProficiencyLevel(item)
        val baseSlots = equipmentType.maxAffixSlots
        val extraSlots = calculateAffixSlots(proficiencyLevel)
        return baseSlots + extraSlots
    }

    /**
     * 获取装备的附魔槽位上限
     */
    fun getEnchantmentSlots(item: ItemStack): Int {
        val equipmentId = NBTEquipmentDataManager.getEquipmentId(item) ?: return 0
        val equipmentType = com.sky4th.equipment.registry.EquipmentRegistry.getEquipmentType(equipmentId) ?: return 0
        val proficiencyLevel = NBTEquipmentDataManager.getProficiencyLevel(item)
        val baseSlots = equipmentType.maxEnchantmentSlots
        val extraSlots = calculateEnchantmentSlots(proficiencyLevel)
        return baseSlots + extraSlots
    }
}
