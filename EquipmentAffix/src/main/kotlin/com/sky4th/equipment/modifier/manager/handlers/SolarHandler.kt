package com.sky4th.equipment.modifier.manager.handlers

import com.sky4th.equipment.data.NBTEquipmentDataManager
import com.sky4th.equipment.modifier.manager.AffixData
import com.sky4th.equipment.modifier.manager.AffixHandler
import org.bukkit.entity.Player

/**
 * 太阳能词条处理器
 * 负责处理太阳能词条的耐久恢复逻辑
 *
 * 功能：
 * 1. 每5秒检查一次是否满足恢复条件
 * 2. 白天且头顶无遮挡时恢复耐久
 * 3. 每个物品单独计时，每30秒恢复1点耐久，最多恢复到60%
 */
class SolarHandler : AffixHandler {

    // 恢复间隔（毫秒），30秒 = 30000 毫秒
    private val RECOVERY_INTERVAL_MS = 30 * 1000L

    // 每次恢复的耐久度
    private val RECOVERY_AMOUNT = 1

    // 最大恢复百分比（60%）
    private val MAX_RECOVERY_PERCENTAGE = 0.6

    /**
     * 物品恢复信息
     */
    data class ItemRecoveryInfo(
        val item: org.bukkit.inventory.ItemStack,
        var lastRecoveryTime: Long = System.currentTimeMillis()
    )

    /**
     * 太阳能词条数据
     */
    class SolarData(
        val uuid: java.util.UUID,
        var level: Int,
        val items: MutableList<ItemRecoveryInfo> = mutableListOf()
    ) : AffixData() {
        override val interval: Int = 20  // 每20 tick（1秒）执行一次
    }

    override fun process(player: Player, data: AffixData) {
        val solarData = data as? SolarData ?: return

        // 更新玩家的太阳能物品列表
        updatePlayerItems(player, solarData)

        // 如果玩家没有任何太阳能物品，移除玩家
        if (solarData.items.isEmpty()) {
            return
        }

        // 检查是否满足恢复条件：白天且头顶无遮挡
        if (!canRecoverDurability(player)) {
            return
        }

        val currentTime = System.currentTimeMillis()

        // 恢复所有物品的耐久（每个物品单独计时）
        solarData.items.forEach { itemInfo ->
            val timeSinceLastRecovery = currentTime - itemInfo.lastRecoveryTime
            if (timeSinceLastRecovery >= RECOVERY_INTERVAL_MS) {
                recoverDurability(itemInfo.item)
                itemInfo.lastRecoveryTime = currentTime
            }
        }
    }

    /**
     * 更新玩家的太阳能物品列表
     * @param player 玩家
     * @param solarData 太阳能数据
     */
    private fun updatePlayerItems(player: Player, solarData: SolarData) {
        val currentSolarItems = mutableListOf<org.bukkit.inventory.ItemStack>()

        // 检查主手物品
        val mainHandItem = player.inventory.itemInMainHand
        if (containsSolarModifier(mainHandItem)) {
            currentSolarItems.add(mainHandItem)
        }

        // 检查副手物品
        val offHandItem = player.inventory.itemInOffHand
        if (containsSolarModifier(offHandItem)) {
            currentSolarItems.add(offHandItem)
        }

        // 检查护甲物品
        player.inventory.armorContents.forEach { item ->
            if (item != null && containsSolarModifier(item)) {
                currentSolarItems.add(item)
            }
        }

        // 更新物品列表，保留已存在的物品的恢复时间
        val newItems = mutableListOf<ItemRecoveryInfo>()
        currentSolarItems.forEach { currentItem ->
            // 查找是否已存在该物品
            val existingInfo = solarData.items.find { it.item == currentItem }
            if (existingInfo != null) {
                // 保留已有的恢复时间
                newItems.add(existingInfo)
            } else {
                // 新物品，使用当前时间
                newItems.add(ItemRecoveryInfo(currentItem))
            }
        }

        solarData.items.clear()
        solarData.items.addAll(newItems)
    }

    /**
     * 检查物品是否含有太阳能词条
     * @param item 物品
     * @return 如果物品含有太阳能词条返回true，否则返回false
     */
    private fun containsSolarModifier(item: org.bukkit.inventory.ItemStack?): Boolean {
        if (item == null || item.type.isAir) {
            return false
        }

        // 检查物品是否是装备
        if (!NBTEquipmentDataManager.isEquipment(item)) {
            return false
        }

        // 检查材质效果是否是太阳能
        val materialEffect = NBTEquipmentDataManager.getMaterialEffect(item)
        if (materialEffect == "solar") {
            return true
        }

        // 检查词条是否包含太阳能
        val affixes = NBTEquipmentDataManager.getAffixes(item)
        return affixes.containsKey("solar")
    }

    /**
     * 检查是否满足恢复条件：白天且头顶无遮挡
     * @param player 玩家
     * @return 如果满足条件返回true，否则返回false
     */
    private fun canRecoverDurability(player: Player): Boolean {
        // 检查是否是白天
        if (player.world.time !in 0..12000) {
            return false
        }

        // 检查头顶是否有遮挡
        val location = player.location
        val highestY = location.world.getHighestBlockYAt(location)
        if (location.blockY < highestY) {
            return false
        }

        return true
    }

    /**
     * 恢复物品耐久
     * @param item 物品
     */
    private fun recoverDurability(item: org.bukkit.inventory.ItemStack) {
        // 获取物品最大耐久度
        val maxDurability = item.type.maxDurability
        if (maxDurability == 0.toShort()) {
            return // 物品没有耐久度
        }

        // 获取物品的 ItemMeta
        val itemMeta = item.itemMeta ?: return
        val damageable = itemMeta as? org.bukkit.inventory.meta.Damageable ?: return

        // 获取当前耐久度
        val currentDamage = damageable.damage
        if (currentDamage <= 0) {
            return // 物品耐久已满
        }

        // 计算最大可恢复的耐久度（60%）
        val maxRecoverable = (maxDurability * MAX_RECOVERY_PERCENTAGE).toInt()
        val currentLostDurability = maxDurability - currentDamage

        // 计算实际恢复的耐久度
        val recoverAmount = minOf(RECOVERY_AMOUNT, currentLostDurability, maxRecoverable)

        // 应用恢复
        damageable.damage = currentDamage - recoverAmount
        item.itemMeta = itemMeta
    }
}
