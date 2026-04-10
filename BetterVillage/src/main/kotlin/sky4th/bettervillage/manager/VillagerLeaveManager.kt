
package sky4th.bettervillage.manager

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Villager
import sky4th.bettervillage.BetterVillage
import sky4th.bettervillage.config.ConfigManager
import sky4th.bettervillage.util.LanguageUtil.sendLang
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 村民离开信息管理器
 * 负责记录村民离开村庄的信息，并提供查询接口
 */
object VillagerLeaveManager {

    // 离开记录缓存：村民UUID -> 离开信息
    private val leaveRecords = ConcurrentHashMap<UUID, VillagerLeaveInfo>()

    /**
     * 村民离开信息数据类
     */
    data class VillagerLeaveInfo(
        val villagerId: UUID,           // 村民UUID
        val villageId: UUID,            // 村庄ID
        val leaveTime: Long,            // 离开时间
        val leaveLocation: Location,    // 离开位置
        var returnTime: Long = 0,      // 返回时间
        var hasReturned: Boolean = false, // 是否已返回
        var actionTaken: VillagerAction = VillagerAction.NONE, // 已采取的操作
        var beingTransported: Boolean = false // 是否正在被玩家搬运
    )

    /**
     * 村民操作枚举
     */
    enum class VillagerAction {
        NONE,           // 无操作
        RETURNED        // 已传送回村庄
    }

    /**
     * 记录村民离开
     * @param villager 离开的村民
     * @param villageId 村庄ID
     */
    fun recordLeave(villager: Villager, villageId: UUID) {
        // 如果村民正在被搬运，不记录离开
        if (isBeingTransported(villager.uniqueId)) {
            return
        }

        val info = VillagerLeaveInfo(
            villagerId = villager.uniqueId,
            villageId = villageId,
            leaveTime = System.currentTimeMillis(),
            leaveLocation = villager.location.clone()
        )

        leaveRecords[villager.uniqueId] = info

        // 直接送回村庄
        returnVillagerToVillage(villager, info.villageId)
    }

    /**
     * 将村民传送回村庄
     * @param villager 要传送的村民
     * @param villageId 村庄ID
     */
    private fun returnVillagerToVillage(villager: Villager, villageId: UUID) {
        val village = VillageManager.getVillage(villageId) ?: return

        // 计算村庄中心位置
        val centerX = (village.minX + village.maxX) / 2.0
        val centerZ = (village.minZ + village.maxZ) / 2.0
        val centerY = villager.location.world?.getHighestBlockYAt(centerX.toInt(), centerZ.toInt())?.toDouble() ?: villager.location.y

        val centerLocation = Location(
            villager.location.world,
            centerX,
            centerY,
            centerZ
        )

        // 传送村民
        villager.teleport(centerLocation)

        // 更新记录
        leaveRecords[villager.uniqueId]?.let {
            it.returnTime = System.currentTimeMillis()
            it.hasReturned = true
            it.actionTaken = VillagerAction.RETURNED
        }
    }

    /**
     * 标记村民正在被搬运
     * @param villagerId 村民UUID
     * @param beingTransported 是否正在被搬运
     */
    fun setTransporting(villagerId: UUID, beingTransported: Boolean) {
        leaveRecords[villagerId]?.beingTransported = beingTransported
    }

    /**
     * 检查村民是否正在被搬运
     * @param villagerId 村民UUID
     * @return 是否正在被搬运
     */
    fun isBeingTransported(villagerId: UUID): Boolean {
        return leaveRecords[villagerId]?.beingTransported == true
    }

    /**
     * 标记村民已返回
     * @param villagerId 村民UUID
     */
    fun markReturned(villagerId: UUID) {
        leaveRecords[villagerId]?.let {
            it.returnTime = System.currentTimeMillis()
            it.hasReturned = true
        }
    }

    /**
     * 获取离开信息
     * @param villagerId 村民UUID
     * @return 离开信息（如果存在）
     */
    fun getLeaveInfo(villagerId: UUID): VillagerLeaveInfo? {
        return leaveRecords[villagerId]
    }

    /**
     * 清理过期记录
     */
    fun cleanupOldRecords() {
        val expireTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        leaveRecords.entries.removeIf { 
            it.value.leaveTime < expireTime && it.value.hasReturned 
        }
    }

    /**
     * 清空所有记录
     */
    fun clearAllRecords() {
        leaveRecords.clear()
    }
}
