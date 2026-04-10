
package com.sky4th.equipment.listener

import com.sky4th.equipment.data.NBTEquipmentDataManager
import com.sky4th.equipment.modifier.ModifierManager
import com.sky4th.equipment.modifier.AffixConfigManager
import com.sky4th.equipment.util.LanguageUtil.sendLang
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.ItemStack

/**
 * 资源存储监听器
 * 处理Shift+F快捷键，将副手物品作为资源存储到主手武器
 */
class ResourceChargeListener : Listener {

    @EventHandler
    fun onPlayerSwapHandItems(event: PlayerSwapHandItemsEvent) {
        val player = event.player

        // 检查是否按下了Shift
        if (!player.isSneaking) {
            return
        }

        // 获取主手和副手物品
        val mainHandItem = player.inventory.itemInMainHand
        val offHandItem = player.inventory.itemInOffHand

        // 检查主手是否是装备
        if (!NBTEquipmentDataManager.isEquipment(mainHandItem)) {
            return
        }

        // 检查副手是否是物品
        if (offHandItem.type.isAir) {
            return
        }

        // 获取主手装备的所有词条
        val affixes = NBTEquipmentDataManager.getAffixes(mainHandItem)

        if (affixes.isEmpty()) {
            return
        }

        // 取消原版切换副手事件
        event.isCancelled = true

        // 遍历所有词条，查找支持充能的词条
        var charged = false
        for ((affixId, level) in affixes) {
            // 获取词条配置
            val affixConfig = AffixConfigManager.getAffixConfig(affixId)
            
            // 检查词条是否支持充能
            if (affixConfig != null && affixConfig.isChargeable) {
                // 检查副手物品是否是该词条需要的资源
                val resourceMaterial = Material.matchMaterial(affixConfig.chargeResource ?: "")
                if (resourceMaterial != null && offHandItem.type == resourceMaterial) {
                    // 获取当前存储量
                    val currentAmount = NBTEquipmentDataManager.getAffixResource(mainHandItem, affixId)
                    val maxAmount = affixConfig.chargeMaxStorage[level] ?: 0

                    // 如果已经满了
                    if (currentAmount >= maxAmount) {
                        player.sendLang(com.sky4th.equipment.EquipmentAffix.instance, "charge.charge-full",
                            "current" to currentAmount,
                            "max" to maxAmount)
                        continue
                    }

                    // 计算可以充能的数量
                    val chargeAmount = minOf(offHandItem.amount, maxAmount - currentAmount)

                    if (chargeAmount > 0) {
                        // 执行充能
                        val newAmount = NBTEquipmentDataManager.addAffixResource(mainHandItem, affixId, chargeAmount)

                        // 减少副手物品数量
                        offHandItem.amount -= chargeAmount
                        if (offHandItem.amount <= 0) {
                            player.inventory.setItemInOffHand(null)
                        }

                        player.sendLang(com.sky4th.equipment.EquipmentAffix.instance, "charge.charge-success",
                            "current" to newAmount,
                            "max" to maxAmount)
                        charged = true

                        // 如果副手物品用完了，停止处理
                        if (player.inventory.itemInOffHand.type.isAir) {
                            break
                        }
                    }
                }
            }
        }

    }
}
