package com.sky4th.equipment.modifier.manager.handlers

import com.sky4th.equipment.modifier.manager.AffixHandler
import com.sky4th.equipment.modifier.manager.AffixData
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import java.lang.ref.WeakReference

/**
 * 磁力词条处理器
 * 负责处理磁力词条的吸引掉落物逻辑
 *
 * 功能：
 * 1. 根据词条等级吸引周围掉落物
 * 2. 等级越高，吸引范围和速度越大
 *
 * 注意：
 * - 配置数据由磁力词条类管理
 * - 玩家数据由UnifiedModifierManager管理
 * - 本处理器只负责吸引逻辑
 */
class MagneticHandler : AffixHandler {

    // 吸引范围（格）
    private val BASE_ATTRACT_RANGE = 3.0

    // 吸引速度
    private val BASE_ATTRACT_SPEED = 0.07

    // 最小吸引距离（避免抖动）
    private val MIN_DISTANCE = 0.5

    /**
     * 磁力词条数据
     */
    class MagneticData(
        val uuid: java.util.UUID,
        var level: Int,
        var item: org.bukkit.inventory.ItemStack
    ) : AffixData() {
        override val interval: Int = 1  // 每1 tick执行一次
    }

    override fun process(player: Player, data: AffixData) {
        val magneticData = data as? MagneticData ?: return

        // 根据等级计算吸引范围和速度
        val range = BASE_ATTRACT_RANGE + (magneticData.level * 0.5)
        val speed = BASE_ATTRACT_SPEED + (magneticData.level * 0.05)

        // 获取玩家周围范围内的所有掉落物
        val nearbyItems = player.world.getNearbyEntities(
            player.location,
            range,
            range,
            range
        ).filterIsInstance<Item>()

        // 对每个掉落物施加吸引力
        nearbyItems.forEach { drop ->
            // 计算从掉落物到玩家的方向向量
            val direction = player.location.clone().subtract(drop.location).toVector()
            val distance = direction.length()

            // 只对一定距离外的掉落物施加吸引力，避免抖动
            if (distance > MIN_DISTANCE) {
                // 归一化方向向量并乘以吸引速度
                direction.normalize().multiply(speed)

                // 应用速度到掉落物
                drop.velocity = drop.velocity.add(direction)
            }
        }
    }
}
