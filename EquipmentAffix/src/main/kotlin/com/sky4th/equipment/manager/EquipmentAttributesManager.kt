package com.sky4th.equipment.manager

import com.sky4th.equipment.EquipmentAffix
import com.sky4th.equipment.data.NBTEquipmentDataManager
import org.bukkit.entity.Player
import sky4th.core.api.PlayerAttributesAPI

/**
 * 装备属性管理器
 * 负责计算装备属性并更新到SkyCore的PlayerAttributes中
 */
class EquipmentAttributesManager(private val plugin: EquipmentAffix) {

    companion object {
        // 闪避词条ID
        private const val DODGE_AFFIX_ID = "agile"

        // 闪避词条各等级加成
        private val dodgeAffixChances: Map<Int, Double> = mapOf(
            1 to 0.02,  // 1级: 2%
            2 to 0.03,  // 2级: 3%
            3 to 0.04   // 3级: 4%
        )
    }

    /**
     * 更新玩家的装备属性
     * 优化版本：添加性能监控
     * 注意：大部分属性现在直接在物品上应用，这里只处理闪避和饥饿惩罚等特殊属性
     */
    fun updatePlayerEquipmentAttributes(player: Player) {
        com.sky4th.equipment.monitor.PerformanceMonitorHelper.monitor("attribute_update") {
            val uuid = player.uniqueId

            // 获取SkyCore中的玩家属性
            val dbAttributes = PlayerAttributesAPI.getAttributes(uuid)

            // 计算装备属性（只计算需要通过SkyCore处理的属性）
            val equipmentDodge = calculateEquipmentDodge(player)
            val equipmentHungerPenalty = calculateEquipmentHungerPenalty(player)

            // 计算最终的属性值
            // 注意：这里直接修改SkyCore缓存中的属性对象
            // 这些属性在保存时会被持久化到数据库
            val hungerConsumptionMultiplier = (1.0 + equipmentHungerPenalty).coerceAtMost(3.0)

            val updatedAttributes = dbAttributes.copy(
                dodge = equipmentDodge,
                hungerConsumptionMultiplier = hungerConsumptionMultiplier
            )

            // 保存到SkyCore缓存
            PlayerAttributesAPI.saveAttributes(updatedAttributes)
        }
    }

    /**
     * 计算装备提供的总闪避率
     * 优化版本：使用批量读取方法，减少NBT访问次数
     */
    private fun calculateEquipmentDodge(player: Player): Double {
        var totalDodge = 0.0

        val equipmentSlots = listOf(
            player.inventory.helmet,
            player.inventory.chestplate,
            player.inventory.leggings,
            player.inventory.boots,
            player.inventory.itemInMainHand,
            player.inventory.itemInOffHand
        )

        equipmentSlots.forEach { item ->
            if (item != null && !item.type.isAir && NBTEquipmentDataManager.isEquipment(item)) {
                // 使用批量读取方法一次性获取所有属性
                val data = NBTEquipmentDataManager.readAllAttributes(item)

                // 装备基础闪避率
                totalDodge += data.dodgeChance

                // 闪避词条加成 - 使用等级对应的加成
                val dodgeLevel = data.affixes[DODGE_AFFIX_ID] ?: 0
                if (dodgeLevel > 0) {
                    // 从本地配置获取对应等级的加成
                    val dodgeChance = dodgeAffixChances[dodgeLevel] ?: 0.0
                    totalDodge += dodgeChance
                }
            }
        }

        // 闪避率上限
        return totalDodge.coerceAtMost(com.sky4th.equipment.config.ConfigManager.maxDodgeChance)
    }

    /**
     * 计算装备提供的饥饿惩罚
     * 只从护甲槽位获取
     */
    private fun calculateEquipmentHungerPenalty(player: Player): Double {
        var totalPenalty = 0.0

        val armorSlots = listOf(
            player.inventory.helmet,
            player.inventory.chestplate,
            player.inventory.leggings,
            player.inventory.boots
        )

        armorSlots.forEach { item ->
            if (item != null && !item.type.isAir && NBTEquipmentDataManager.isEquipment(item)) {
                totalPenalty += NBTEquipmentDataManager.getHungerPenalty(item)
            }
        }

        // 饥饿惩罚上限
        return totalPenalty.coerceIn(0.0, com.sky4th.equipment.config.ConfigManager.maxHungerPenalty)
    }

}
