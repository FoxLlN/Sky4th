package sky4th.core.model

import org.bukkit.Location

/**
 * 玩家位置信息
 */
data class PlayerLocations(
    var lastLocation: Location? = null,      // 最后位置（用于死亡恢复）
    var baseLocation: Location? = null       // 基地核心位置
) {
    /**
     * 检查是否在基地中（可以根据需要扩展判断逻辑）
     */
    fun isInBase(location: Location?): Boolean {
        if (baseLocation == null || location == null) return false
        
        // 简单的距离判断（可以根据需要扩展为区域判断）
        val distance = baseLocation!!.distance(location)
        return distance <= 50.0 // 假设基地范围为50格
    }
}
