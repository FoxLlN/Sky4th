
package com.sky4th.equipment.cache

import net.kyori.adventure.text.Component
import org.bukkit.inventory.ItemStack
import java.util.WeakHashMap

/**
 * Lore显示缓存
 * 缓存装备的Lore显示，减少重复计算
 * 使用WeakHashMap实现自动GC回收，无需手动清理
 */
object LoreDisplayCache {

    // 使用WeakHashMap实现自动GC回收
    // 当instance_id字符串不再被强引用时，缓存条目会自动被GC回收
    // 使用嵌套Map：外层是instance_id，内层是detailed模式
    private val cache = WeakHashMap<String, MutableMap<Boolean, List<Component>>>()

    /**
     * 获取缓存的Lore
     * @param item 装备物品
     * @param detailed 是否详细模式
     * @return 缓存的Lore，如果不存在则返回null
     */
    fun getLore(item: ItemStack, detailed: Boolean): List<Component>? {
        val instanceId = com.sky4th.equipment.data.NBTEquipmentDataManager.getInstanceId(item)

        if (instanceId != null) {
            cache[instanceId]?.get(detailed)?.let {
                return it
            }
        }

        return null
    }

    /**
     * 缓存Lore
     * @param item 装备物品
     * @param detailed 是否详细模式
     * @param lore Lore列表
     */
    fun cacheLore(item: ItemStack, detailed: Boolean, lore: List<Component>) {
        val instanceId = com.sky4th.equipment.data.NBTEquipmentDataManager.getInstanceId(item)

        if (instanceId != null) {
            cache.getOrPut(instanceId) { HashMap() }[detailed] = lore
        }
    }

    /**
     * 使缓存失效
     * @param item 装备物品
     */
    fun invalidate(item: ItemStack) {
        val instanceId = com.sky4th.equipment.data.NBTEquipmentDataManager.getInstanceId(item)

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
     * 获取缓存大小
     * @return 缓存中的条目数
     */
    fun size(): Int {
        return cache.size
    }
}
