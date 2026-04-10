package com.sky4th.equipment.listener

import com.sky4th.equipment.data.NBTEquipmentDataManager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.inventory.ItemStack

/**
 * 装备实例ID监听器
 * 监听装备的创建和获取事件，自动初始化实例ID
 */
class EquipmentInstanceListener : Listener {

    /**
     * 监听物品合成事件
     * 当玩家合成出我们的自定义装备时，自动初始化实例ID
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onCraftItem(event: CraftItemEvent) {
        val result = event.recipe.result

        // 检查是否是自定义装备
        if (!NBTEquipmentDataManager.isEquipment(result)) {
            return
        }

        // 检查是否已有实例ID
        if (NBTEquipmentDataManager.getInstanceId(result) != null) {
            return
        }

        // 初始化实例ID
        NBTEquipmentDataManager.setInstanceId(result)
        println("[装备实例] 合成装备 - 初始化实例ID: ${NBTEquipmentDataManager.getInstanceId(result)}")
    }

    /**
     * 监听物品点击事件
     * 处理从容器中获取装备的情况
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onInventoryClick(event: InventoryClickEvent) {
        val currentItem = event.currentItem ?: return

        // 检查是否是自定义装备
        if (!NBTEquipmentDataManager.isEquipment(currentItem)) {
            return
        }

        // 检查是否已有实例ID
        if (NBTEquipmentDataManager.getInstanceId(currentItem) != null) {
            return
        }

        // 初始化实例ID
        NBTEquipmentDataManager.setInstanceId(currentItem)
        println("[装备实例] 获取装备 - 初始化实例ID: ${NBTEquipmentDataManager.getInstanceId(currentItem)}")
    }
}
