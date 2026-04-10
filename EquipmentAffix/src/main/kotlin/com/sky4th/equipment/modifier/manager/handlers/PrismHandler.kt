package com.sky4th.equipment.modifier.manager.handlers

import com.sky4th.equipment.data.NBTEquipmentDataManager
import com.sky4th.equipment.modifier.manager.AffixHandler
import com.sky4th.equipment.modifier.manager.AffixData
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * 棱镜词条处理器
 * 负责处理棱镜词条的护盾累计逻辑
 *
 * 功能：
 * 1. 根据等级定期累计护盾层数
 * 2. 1级：25秒累计1层，上限1层
 * 3. 2级：20秒累计1层，上限2层
 * 4. 3级：15秒累计1层，上限3层
 *
 * 注意：
 * - 配置数据由棱镜词条类管理
 * - 玩家数据由UnifiedModifierManager管理
 * - 本处理器只负责护盾累计逻辑
 */
class PrismHandler : AffixHandler {

    // 每级配置：(累计间隔tick, 最大层数)
    private val CONFIG = arrayOf(
        25L to 1,  // 1级：25秒，1层
        20L to 2,   // 2级：20秒，2层
        15L to 3,   // 3级：15秒，3层
    )

    /**
     * 棱镜词条数据
     */
    class PrismData(
        val uuid: java.util.UUID,
        var level: Int,
        var item: org.bukkit.inventory.ItemStack,
        var lastAccumulateTick: Long = System.currentTimeMillis()  // 上次累计时间（tick）
    ) : AffixData() {
        override val interval: Int = 5  // 每5 tick（0.25秒）执行一次
    }

    override fun process(player: Player, data: AffixData) {
        val prismData = data as? PrismData ?: return

        // 获取当前等级配置
        val config = CONFIG.getOrNull(prismData.level - 1) ?: return
        val (accumulateInterval, maxLayers) = config

        // 使用存储的物品
        val item = prismData.item

        // 使用系统的资源充能系统获取当前层数
        val currentLayers = NBTEquipmentDataManager.getAffixResource(item, "prism")

        // 计算距离上次累计的时间差
        val currentTick = System.currentTimeMillis()
        val tickDiff = currentTick - prismData.lastAccumulateTick

        // 如果达到累计间隔，增加一层（将秒转换为毫秒）
        if (tickDiff >= accumulateInterval * 1000) {
            // 如果已经达到最大层数，只重置累计时间
            if (currentLayers >= maxLayers) {
                prismData.lastAccumulateTick = currentTick
                return
            }

            // 增加一层
            val newLayers = currentLayers + 1
            NBTEquipmentDataManager.setAffixResource(item, "prism", newLayers)

            // 更新最后累计时间
            prismData.lastAccumulateTick = currentTick
        }
    }
}
