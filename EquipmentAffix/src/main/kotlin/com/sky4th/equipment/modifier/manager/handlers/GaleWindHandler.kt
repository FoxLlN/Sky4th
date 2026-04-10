package com.sky4th.equipment.modifier.manager.handlers

import com.sky4th.equipment.data.NBTEquipmentDataManager
import com.sky4th.equipment.modifier.manager.AffixHandler
import com.sky4th.equipment.modifier.manager.AffixData
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.lang.ref.WeakReference

/**
 * 疾风词条处理器
 * 负责处理疾风层数的衰减逻辑
 *
 * 功能：
 * 1. 停止移动1秒后开始掉层数
 * 2. 每秒掉1层
 *
 * 注意：
 * - 配置数据（DAMAGE_BONUS_PER_LAYER、MOVEMENT_CONFIG）由GaleWind词条类管理
 * - 玩家数据由UnifiedModifierManager管理
 * - 本处理器只负责衰减逻辑
 */
class GaleWindHandler : AffixHandler {

    /**
     * 疾风词条数据
     */
    class GaleWindData(
        val uuid: java.util.UUID,
        var lastMoveTime: Long = System.currentTimeMillis(),  // 上次移动时间
        var currentLayers: Int = 0,  // 当前层数
        var lastDecayTime: Long = System.currentTimeMillis(),  // 上次衰减时间
        var totalDistance: Double = 0.0,  // 累计移动距离
        var item: org.bukkit.inventory.ItemStack  // 直接存储物品引用
    ) : AffixData() {

        override val interval: Int = 5  // 每5 tick检查一次

        /**
         * 检查是否需要衰减层数
         * @return 如果需要衰减返回true，否则返回false
         */
        fun shouldDecay(): Boolean {
            val currentTime = System.currentTimeMillis()
            val timeSinceLastMove = currentTime - lastMoveTime
            
            if (currentLayers <= 0) {
                return false
            }
            // 如果停止移动超过1秒，检查是否需要衰减
            if (timeSinceLastMove >= 1000) {
                val timeSinceLastDecay = currentTime - lastDecayTime
                // 如果距离上次衰减超过1秒，需要衰减
                if (timeSinceLastDecay >= 1000) {
                    lastDecayTime = currentTime
                    return true
                }
            }
            return false
        }

        /**
         * 更新移动时间
         */
        fun updateMoveTime() {
            lastMoveTime = System.currentTimeMillis()
            lastDecayTime = System.currentTimeMillis()  // 重置衰减时间
        }
    }

    override fun process(player: Player, data: AffixData) {
        val galeWindData = data as? GaleWindData ?: return
        
        // 检查是否需要衰减
        if (!galeWindData.shouldDecay()) {
            return
        }

        if (galeWindData.totalDistance != 0.0){
            galeWindData.totalDistance = 0.0
        }

        // 衰减层数
        if (galeWindData.currentLayers > 0) {
            galeWindData.currentLayers--

            // 获取玩家裤腿上的疾风词条物品
            // 更新NBT数据
            NBTEquipmentDataManager.setAffixResource(galeWindData.item, "gale_wind", galeWindData.currentLayers)
        }
    }
}
