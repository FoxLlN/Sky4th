
package com.sky4th.equipment.cache

import com.sky4th.equipment.attributes.EquipmentAttributes
import com.sky4th.equipment.data.NBTEquipmentDataManager
import org.bukkit.inventory.ItemStack
import java.util.WeakHashMap

/**
 * 装备属性缓存
 * 使用WeakHashMap实现自动GC回收，无需手动清理
 */
object EquipmentAttributesCache {

    // 使用WeakHashMap实现自动GC回收
    // 当instance_id字符串不再被强引用时，缓存条目会自动被GC回收
    private val cache = WeakHashMap<String, EquipmentAttributes>()

    /**
     * 获取装备属性
     * @param item 装备物品
     * @return 装备属性
     */
    fun getAttributes(item: ItemStack): EquipmentAttributes {
        val instanceId = NBTEquipmentDataManager.getInstanceId(item)

        if (instanceId != null) {
            cache[instanceId]?.let {
                return it
            }
        }

        val attributes = readAttributesFromNBT(item)

        if (instanceId != null) {
            cache[instanceId] = attributes
        }

        return attributes
    }

    /**
     * 手动失效（当物品属性变化时调用）
     * @param item 装备物品
     */
    fun invalidate(item: ItemStack) {
        val instanceId = NBTEquipmentDataManager.getInstanceId(item)

        if (instanceId != null) {
            cache.remove(instanceId)
        }
    }

    /**
     * 手动失效物品id（当物品属性变化时调用）
     * @param instanceId 实例id
     */
    fun invalidateId(instanceId: String) {
        cache.remove(instanceId)
    }

    

    /**
     * 使所有缓存失效
     */
    fun invalidateAll() {
        cache.clear()
    }

    /**
     * 从NBT读取装备属性
     * @param item 装备物品
     * @return 装备属性
     */
    private fun readAttributesFromNBT(item: ItemStack): EquipmentAttributes {
        val data = NBTEquipmentDataManager.readAllAttributes(item)
        val enchantments = item.enchantments.toMap()
        return EquipmentAttributes(
            material = item.type,
            proficiencyLevel = data.proficiencyLevel,
            proficiency = data.proficiency,
            affixes = data.affixes,
            enchantments = enchantments,
            dodgeChance = data.dodgeChance,
            knockbackResistance = data.knockbackResistance,
            hungerPenalty = data.hungerPenalty,
            movementPenalty = data.movementPenalty,
            materialEffect = data.materialEffect
        )
    }

    /**
     * 获取缓存大小
     * @return 缓存中的条目数
     */
    fun size(): Int = cache.size
}
