package com.sky4th.equipment.listener

import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import com.sky4th.equipment.manager.LoreDisplayManager

/**
 * Lore显示监听器
 * 监听玩家双击物品事件，用于切换物品描述的详细/简单模式
 * 双击事件会在任何容器（背包、箱子等）中触发，物品会被收集到光标上
 */
class LoreDisplayListener(
    private val plugin: com.sky4th.equipment.EquipmentAffix
) : Listener {

    // NBT键：存储物品是否处于详细描述模式（1=详细，0=简单）
    private val KEY_DETAILED_LORE = NamespacedKey("sky_equipment", "detailed_lore")

    // NBT键：标识物品是否属于本插件的装备系统（可选，用于过滤）
    private val KEY_IS_EQUIPMENT = NamespacedKey("sky_equipment", "is_equipment")

    /**
     * 处理双击左键事件
     * 双击左键物品后，将物品从光标放到格子并切换描述
     */
    @EventHandler
    fun onInventoryDoubleClick(event: InventoryClickEvent) {
        // 仅处理双击类型事件
        if (event.click != ClickType.DOUBLE_CLICK) return

        val player = event.whoClicked as? Player ?: return
        val cursorItem = player.itemOnCursor
        val currentItem = event.currentItem

        // 如果光标上没有物品，则不处理
        if (cursorItem.type.isAir) return

        // 检查点击槽位是否为空，防止删除物品
        if (currentItem?.type?.isAir == false) return

        // 检查物品是否属于我们的装备系统
        if (!isOurEquipment(cursorItem)) return

        // 获取当前显示模式并切换
        val isDetailed = isDetailedDescription(cursorItem)
        val newDetailed = !isDetailed

        // 设置新的模式到NBT
        setDetailedDescription(cursorItem, newDetailed)

        // 根据新模式生成新的Lore
        LoreDisplayManager.modifyItemLore(cursorItem, newDetailed)

        // 将修改后的物品放回点击的槽位
        event.currentItem = cursorItem

        // 清空光标上的物品
        player.setItemOnCursor(null)
        
        // 触发词条更新（延迟一tick，确保物品已经放回槽位）
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            com.sky4th.equipment.modifier.ModifierManager.instance.updatePlayerModifiers(player)
        }, 1L)
    }

    /**
     * 判断物品是否属于本插件的装备系统
     * 通过检查NBT中是否有装备ID来判断
     */
    private fun isOurEquipment(item: ItemStack): Boolean {
        return com.sky4th.equipment.data.NBTEquipmentDataManager.isEquipment(item)
    }

    /**
     * 获取物品当前的详细描述模式
     * @return true = 详细模式，false = 简单模式
     */
    private fun isDetailedDescription(item: ItemStack): Boolean {
        val meta = item.itemMeta ?: return false
        val container = meta.persistentDataContainer
        return container.getOrDefault(KEY_DETAILED_LORE, PersistentDataType.BYTE, 0) == 1.toByte()
    }

    /**
     * 设置物品的详细描述模式
     */
    private fun setDetailedDescription(item: ItemStack, detailed: Boolean) {
        val meta = item.itemMeta ?: return
        val container = meta.persistentDataContainer
        container.set(KEY_DETAILED_LORE, PersistentDataType.BYTE, if (detailed) 1 else 0)
        item.itemMeta = meta
    }
}