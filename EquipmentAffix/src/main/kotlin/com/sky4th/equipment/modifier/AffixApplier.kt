
package com.sky4th.equipment.modifier

import com.sky4th.equipment.data.NBTEquipmentDataManager
import org.bukkit.inventory.ItemStack

/**
 * 词条应用器
 * 负责将词条应用到物品上
 */
object AffixApplier {

    /**
     * 应用词条到物品
     * @param item 物品
     * @param affixId 词条ID
     * @param level 词条等级
     * @return 是否应用成功
     */
    fun applyAffix(item: ItemStack, affixId: String, level: Int = 1): Boolean {
        // 检查词条是否存在
        if (!ModifierManager.instance.hasModifier(affixId)) {
            return false
        }

        // 获取词条实现
        val modifier = ModifierManager.instance.getModifier(affixId) ?: return false

        // 检查等级是否有效（默认最大等级为1，实际应从配置读取）
        if (level < 1) {
            return false
        }

        // 应用词条到物品
        NBTEquipmentDataManager.setAffix(item, affixId, level)
        // 清除缓存
        com.sky4th.equipment.cache.EquipmentAttributesCache.invalidate(item)

        return true
    }

    /**
     * 从物品移除词条
     * @param item 物品
     * @param affixId 词条ID
     * @return 是否移除成功
     */
    fun removeAffix(item: ItemStack, affixId: String): Boolean {
        // 检查词条是否存在
        if (!ModifierManager.instance.hasModifier(affixId)) {
            return false
        }

        // 从物品移除词条
        NBTEquipmentDataManager.removeAffix(item, affixId)
        // 清除缓存
        com.sky4th.equipment.cache.EquipmentAttributesCache.invalidate(item)

        return true
    }

    /**
     * 更新物品上的词条等级
     * @param item 物品
     * @param affixId 词条ID
     * @param newLevel 新等级
     * @return 是否更新成功
     */
    fun updateAffixLevel(item: ItemStack, affixId: String, newLevel: Int): Boolean {
        // 检查词条是否存在
        if (!ModifierManager.instance.hasModifier(affixId)) {
            return false
        }

        // 获取词条实现
        val modifier = ModifierManager.instance.getModifier(affixId) ?: return false

        // 检查等级是否有效（默认最大等级为1，实际应从配置读取）
        if (newLevel < 1) {
            return false
        }

        // 更新词条等级
        NBTEquipmentDataManager.setAffix(item, affixId, newLevel)
        // 清除缓存
        com.sky4th.equipment.cache.EquipmentAttributesCache.invalidate(item)

        return true
    }

    /**
     * 清除物品上的所有词条
     * @param item 物品
     * @return 是否清除成功
     */
    fun clearAllAffixes(item: ItemStack): Boolean {
        // 清除所有词条
        NBTEquipmentDataManager.clearAffixes(item)
        // 清除缓存
        com.sky4th.equipment.cache.EquipmentAttributesCache.invalidate(item)

        return true
    }

    /**
     * 获取物品上的所有词条
     * @param item 物品
     * @return 词条ID到等级的映射
     */
    fun getAffixes(item: ItemStack): Map<String, Int> {
        return NBTEquipmentDataManager.getAffixes(item)
    }

    /**
     * 检查物品是否具有指定词条
     * @param item 物品
     * @param affixId 词条ID
     * @return 是否具有该词条
     */
    fun hasAffix(item: ItemStack, affixId: String): Boolean {
        return NBTEquipmentDataManager.getAffixes(item).containsKey(affixId)
    }

    /**
     * 获取物品上指定词条的等级
     * @param item 物品
     * @param affixId 词条ID
     * @return 词条等级，如果不存在则返回0
     */
    fun getAffixLevel(item: ItemStack, affixId: String): Int {
        return NBTEquipmentDataManager.getAffixes(item).getOrDefault(affixId, 0)
    }
}
