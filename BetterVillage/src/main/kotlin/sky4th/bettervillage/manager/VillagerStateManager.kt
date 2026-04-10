
package sky4th.bettervillage.manager

import org.bukkit.Location
import org.bukkit.entity.Villager
import sky4th.bettervillage.config.ConfigManager
import sky4th.core.model.VillageData
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 村民状态管理器
 * 负责维护村民的状态信息（在村庄内/接近边界/在村庄外）
 */
object VillagerStateManager {

    // 村民状态缓存：UUID -> VillagerState
    private val villagerStates = ConcurrentHashMap<UUID, VillagerState>()

    /**
     * 村民状态数据类
     */
    data class VillagerState(
        val villageId: UUID,           // 所属村庄ID
        val status: VillagerStatus,     // 当前状态
        val lastCheckTime: Long,        // 上次检查时间
        val lastLocation: Location      // 上次位置
    )

    /**
     * 村民状态枚举
     */
    enum class VillagerStatus {
        IN_VILLAGE,      // 在村庄内（安全区）
        OUTSIDE_VILLAGE  // 在村庄外（离开）
    }

    /**
     * 更新村民状态
     * @param villager 要检查的村民
     * @return 新状态（如果状态变化），否则返回null
     */
    fun updateVillagerStatus(villager: Villager): VillagerStatus? {
        val currentState = villagerStates[villager.uniqueId]
        val village = VillageManager.getVillageByLocation(villager.location)

        // 如果村民不在任何村庄内，且没有历史状态，则不处理
        if (village == null && currentState == null) {
            return null
        }

        // 获取新状态
        val newStatus = determineVillagerStatus(villager, village)

        // 如果没有状态变化，返回null
        if (currentState?.status == newStatus) {
            // 更新检查时间和位置
            villagerStates[villager.uniqueId] = currentState.copy(
                lastCheckTime = System.currentTimeMillis(),
                lastLocation = villager.location.clone()
            )
            return null
        }

        // 更新状态
        val newState = VillagerState(
            villageId = village?.id ?: currentState?.villageId ?: return null,
            status = newStatus,
            lastCheckTime = System.currentTimeMillis(),
            lastLocation = villager.location.clone()
        )

        villagerStates[villager.uniqueId] = newState

        // 返回状态变化
        return newStatus
    }

    /**
     * 判断村民状态
     * @param villager 要检查的村民
     * @param village 村民所在的村庄（可为null）
     * @return 村民状态
     */
    private fun determineVillagerStatus(villager: Villager, village: VillageData?): VillagerStatus {
        if (village == null) {
            return VillagerStatus.OUTSIDE_VILLAGE
        }

        val x = villager.location.blockX
        val z = villager.location.blockZ

        return when {
            x < village.minX || x > village.maxX ||
            z < village.minZ || z > village.maxZ ->
                VillagerStatus.OUTSIDE_VILLAGE
            else ->
                VillagerStatus.IN_VILLAGE
        }
    }

    /**
     * 获取村民当前状态
     * @param villagerId 村民UUID
     * @return 村民状态（如果存在）
     */
    fun getVillagerState(villagerId: UUID): VillagerState? {
        return villagerStates[villagerId]
    }

    /**
     * 移除村民状态
     * @param villagerId 村民UUID
     */
    fun removeVillagerState(villagerId: UUID) {
        villagerStates.remove(villagerId)
    }

    /**
     * 清空所有状态
     */
    fun clearAllStates() {
        villagerStates.clear()
    }

    /**
     * 获取所有状态为指定值的村民
     * @param status 要查询的状态
     * @return 村民UUID列表
     */
    fun getVillagersByStatus(status: VillagerStatus): List<UUID> {
        return villagerStates.filter { it.value.status == status }.keys.toList()
    }

    /**
     * 获取村庄内的所有村民
     * @param villageId 村庄ID
     * @return 村民UUID列表
     */
    fun getVillagersInVillage(villageId: UUID): List<UUID> {
        return villagerStates.filter { it.value.villageId == villageId }.keys.toList()
    }
}
