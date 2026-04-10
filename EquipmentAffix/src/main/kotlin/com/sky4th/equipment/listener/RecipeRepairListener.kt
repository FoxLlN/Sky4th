package com.sky4th.equipment.listener

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.inventory.ItemStack
import com.sky4th.equipment.data.NBTEquipmentDataManager

/**
 * 配方修复监听器
 * 负责阻止玩家在合成台中合并相同物品来修复耐久度
 */
class RecipeRepairListener : Listener {

    @EventHandler
    fun onPrepareItemCraft(event: PrepareItemCraftEvent) {
        val inventory = event.inventory
        val matrix = inventory.matrix ?: return

        // 统计非空物品槽
        val nonNullItems = matrix.filterNotNull()

        // 如果只有两个物品且类型相同，则认为是尝试修复
        if (nonNullItems.size == 2) {
            val firstItem = nonNullItems[0]
            val secondItem = nonNullItems[1]

            // 检查是否为相同类型的物品
            if (firstItem.type == secondItem.type) {
                // 检查是否为装备系统物品
                val isFirstEquipment = NBTEquipmentDataManager.isEquipment(firstItem)
                val isSecondEquipment = NBTEquipmentDataManager.isEquipment(secondItem)

                // 如果任一物品是装备系统物品，则阻止合成
                if (isFirstEquipment || isSecondEquipment) {
                    event.inventory.result = null
                    return
                }

                // 检查物品是否有耐久度（可修复物品）
                if (firstItem.type.maxDurability > 0) {
                    // 阻止所有装备修复（包括合成台和背包）
                    event.inventory.result = null
                }
            }
        }
    }
}
