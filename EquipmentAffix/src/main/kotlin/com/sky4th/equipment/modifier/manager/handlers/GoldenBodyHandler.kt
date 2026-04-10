package com.sky4th.equipment.modifier.manager.handlers

import com.sky4th.equipment.modifier.manager.AffixHandler
import com.sky4th.equipment.modifier.manager.AffixData
import org.bukkit.entity.Player
import java.lang.ref.WeakReference

/**
 * 金身词条处理器
 * 负责处理金身词条的伤害吸收增加逻辑
 *
 * 功能：
 * 1. 玩家受伤时重置计时器
 * 2. 60秒未受伤后，开始增加伤害吸收
 * 3. 每次增加2点伤害吸收，直到达到等级上限
 *
 * 注意：
 * - 配置数据由金身词条类管理
 * - 玩家数据由UnifiedModifierManager管理
 * - 本处理器只负责伤害吸收逻辑
 */
class GoldenBodyHandler : AffixHandler {

    // 增加伤害吸收的周期（tick），60秒 = 1200 tick
    private val BONUS_CYCLE_TICKS = 1200L

    // 每次增加的伤害吸收值
    private val BONUS_PER_CYCLE = 2

    // 最大伤害吸收值
    private val MAX_ABSORPTION = doubleArrayOf(4.0, 6.0, 8.0) // 根据等级分别为4/6/8点

    /**
     * 金身词条数据
     */
    class GoldenBodyData(
    val uuid: java.util.UUID,
    var level: Int,
    var currentAbsorption: Double = 0.0,
    var startTime: Long = System.currentTimeMillis(),
    var item: org.bukkit.inventory.ItemStack  // 直接存储物品引用
    ) : AffixData() {
        override val interval: Int = 20  // 每20 tick（1秒）执行一次
    }

    override fun process(player: Player, data: AffixData) {
        val goldenBodyData = data as? GoldenBodyData ?: return

        val currentTime = System.currentTimeMillis()

        // 检查距离开始时间是否超过60秒
        val elapsedTime = currentTime - goldenBodyData.startTime
        if (elapsedTime >= BONUS_CYCLE_TICKS * 50) { // 50毫秒 * 1200 = 60秒
            // 计算当前等级的最大伤害吸收值
            val maxAbsorption = MAX_ABSORPTION.getOrNull(goldenBodyData.level - 1) ?: 4.0
            // 获取当前实际伤害吸收值
            val currentActualAbsorption = player.absorptionAmount
            // 如果还未达到上限，增加伤害吸收
            if (currentActualAbsorption < maxAbsorption) {
                val newAbsorption = minOf(currentActualAbsorption + BONUS_PER_CYCLE, maxAbsorption)

                // 增加伤害吸收
                player.absorptionAmount = newAbsorption
                // 更新当前伤害吸收值
                goldenBodyData.currentAbsorption = newAbsorption
            }

            // 更新开始时间为当前时间
            goldenBodyData.startTime = currentTime
        }
    }
}
