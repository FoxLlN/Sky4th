
package sky4th.dungeon.loadout.repair.animation

import org.bukkit.Material

/**
 * 维修动画配置类
 * 定义每种维修物品的动画属性
 */
data class RepairAnimationConfig(
    val repairItem: Material,              // 维修物品类型
    val targetTypes: Set<RepairTargetType>, // 可维修的目标物品类型
    val durationTicks: Int,                // 维修时长（tick，20 tick = 1秒）
    val animationClass: Class<out RepairAnimation> // 动画实现类
) {
    /**
     * 检查是否可以维修指定类型的物品
     */
    fun canRepair(targetType: RepairTargetType): Boolean {
        return targetType in targetTypes
    }
}
