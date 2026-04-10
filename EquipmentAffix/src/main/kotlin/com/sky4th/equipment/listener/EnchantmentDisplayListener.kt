package com.sky4th.equipment.listener

import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.enchantment.PrepareItemEnchantEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.inventory.PrepareGrindstoneEvent
import org.bukkit.inventory.AnvilInventory
import org.bukkit.inventory.ItemStack
import com.sky4th.equipment.EquipmentAffix
import com.sky4th.equipment.data.NBTEquipmentDataManager
import com.sky4th.equipment.manager.LoreDisplayManager
import com.sky4th.equipment.util.EnchantmentUtil

/**
 * 附魔显示监听器
 * 监听附魔事件，自动将附魔转换成自定义描述
 */
class EnchantmentDisplayListener(private val plugin: EquipmentAffix) : Listener {

    /**
     * 监听附魔台准备事件
     * 在显示附魔选项时进行验证和限制
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPrepareItemEnchant(event: PrepareItemEnchantEvent) {
        val item = event.item

        // 检查物品是否是装备
        if (!NBTEquipmentDataManager.isEquipment(item)) {
            return
        }

        // 获取剩余槽位
        val availableSlots = EnchantmentUtil.getAvailableEnchantmentSlots(item)

        // 如果没有剩余槽位，将所有选项消耗设为0（不可用）
        if (availableSlots <= 0) {
            event.setCancelled(true)
            return
        }

        // 检查每个附魔选项
        for (i in event.offers.indices) {
            val enchantment = event.offers[i]?.enchantment ?: continue

            // 检查附魔与词条的冲突
            if (EnchantmentUtil.hasEnchantmentConflict(item, enchantment)) {
                // 存在冲突，将该选项设为不可用
                // 将对应位置的附魔选项置为 null（需要 Paper API）
                val offers = event.offers
                offers[i] = null
                continue
            }
        }
    }

    /**
     * 监听附魔台附魔完成事件
     * 在附魔完成后更新物品描述
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEnchantItem(event: EnchantItemEvent) {
        val item = event.item

        // 检查物品是否是装备
        if (!NBTEquipmentDataManager.isEquipment(item)) {
            return
        }
        val availableSlots = EnchantmentUtil.getAvailableEnchantmentSlots(item)
        // 如果要添加的附魔数量超过剩余槽位，只保留与剩余槽位数量相等的附魔
        if (event.enchantsToAdd.size > availableSlots) {
            val enchantmentsToKeep = event.enchantsToAdd.entries.take(availableSlots).toList()
            event.enchantsToAdd.clear()
            enchantmentsToKeep.forEach { (enchant, level) ->
                event.enchantsToAdd[enchant] = level
            }
        }

        // 延迟执行，确保附魔已经应用到物品上
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            // 清除缓存
            com.sky4th.equipment.cache.EquipmentAttributesCache.invalidate(item)
            com.sky4th.equipment.cache.LoreDisplayCache.invalidate(item)
            EnchantmentUtil.updateEnchantmentDisplay(item)
        }, 1L)
    }

    /**
     * 监听铁砧准备事件
     * 在铁砧中显示结果时更新描述
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPrepareAnvil(event: PrepareAnvilEvent) {
        val resultItem = event.result ?: return

        // 检查物品是否是装备
        if (!NBTEquipmentDataManager.isEquipment(resultItem)) {
            return
        }

        // 清除instance_id，使物品被视为新实例
        NBTEquipmentDataManager.clearInstanceId(resultItem)

        // 获取铁砧中的输入物品
        val inventory = event.inventory
        val firstItem = inventory.getItem(0)
        val secondItem = inventory.getItem(1)

        // 获取输入物品的附魔
        val inputEnchantments = mutableMapOf<Enchantment, Int>()
        firstItem?.enchantments?.forEach { (enchant, level) ->
            inputEnchantments[enchant] = level
        }
        secondItem?.enchantments?.forEach { (enchant, level) ->
            inputEnchantments[enchant] = level
        }

        // 获取结果物品的附魔
        val resultEnchantments = resultItem.enchantments

        // 计算新增的附魔（结果中有但输入中没有的）
        val newEnchantments = mutableMapOf<Enchantment, Int>()
        resultEnchantments.forEach { (enchant, level) ->
            val inputLevel = inputEnchantments[enchant] ?: 0
            if (level > inputLevel) {
                // 这是一个新增的或升级的附魔
                newEnchantments[enchant] = level
            }
        }

        // 检查新增的附魔是否与词条冲突
        for ((enchant, _) in newEnchantments) {
            if (EnchantmentUtil.hasEnchantmentConflict(resultItem, enchant)) {
                // 检查输入物品是否已有该附魔
                val firstHasEnchant = firstItem?.enchantments?.containsKey(enchant) == true
                val secondHasEnchant = secondItem?.enchantments?.containsKey(enchant) == true
                // 如果是新添加的附魔且与词条冲突，则取消铁砧操作
                if (!firstHasEnchant && !secondHasEnchant) {
                    event.result = null
                    return
                }
            }
        }

        // 检查槽位限制
        val maxSlots = NBTEquipmentDataManager.getMaxEnchantmentSlots(resultItem)
        val firstItemEnchantmentCount = firstItem?.enchantments?.size ?: 0
        
        // 如果原本物品槽位已满
        if (firstItemEnchantmentCount >= maxSlots) {
            event.result = null
            return
        }

        // 计算结果物品的附魔数量
        val resultEnchantmentCount = resultEnchantments.size

        // 如果附魔数量超过最大槽位，按顺序保留前N个附魔
        if (resultEnchantmentCount > maxSlots) {
            // 获取所有附魔并按顺序排列
            val sortedEnchantments = resultItem.enchantments.entries.toList()
            // 创建物品副本
            val modifiedItem = resultItem.clone()

            // 移除所有附魔
            for (enchant in modifiedItem.enchantments.keys) {
                modifiedItem.removeEnchantment(enchant)
            }
            // 只保留前maxSlots个附魔
            sortedEnchantments.take(maxSlots).forEach { (enchant, level) ->
                modifiedItem.addUnsafeEnchantment(enchant, level)
            }

            // 更新附魔显示
            EnchantmentUtil.updateEnchantmentDisplay(modifiedItem)

            // 更新结果物品
            event.result = modifiedItem
            return
        }
        
        // 创建物品副本，避免直接修改原始物品
        val modifiedItem = resultItem.clone()

        // 更新附魔显示
        EnchantmentUtil.updateEnchantmentDisplay(modifiedItem)

        // 更新铁砧结果
        event.result = modifiedItem
    }

    /**
     * 监听砂轮准备事件
     * 在砂轮中显示结果时更新描述（祛魔后）
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPrepareGrindstone(event: PrepareGrindstoneEvent) {
        val resultItem = event.result ?: return

        // 检查物品是否是装备
        if (!NBTEquipmentDataManager.isEquipment(resultItem)) {
            return
        }

        // 清除instance_id，使物品被视为新实例
        NBTEquipmentDataManager.clearInstanceId(resultItem)

        // 更新附魔显示（祛魔后附魔为空）
        EnchantmentUtil.updateEnchantmentDisplay(resultItem)

        // 更新砂轮结果
        event.result = resultItem
    }
}
