
package sky4th.core.service

import sky4th.core.database.VillageDataDAO
import sky4th.core.model.VillageData
import java.util.*

/**
 * 村庄服务
 * 提供村庄数据的业务逻辑操作
 */
class VillageService(private val villageDataDAO: VillageDataDAO) {

    /**
     * 初始化服务
     */
    fun initialize() {
        // 表的初始化现在由DatabaseManager统一管理
    }

    /**
     * 创建新村庄
     */
    fun createVillage(village: VillageData): Boolean {
        return try {
            villageDataDAO.saveVillage(village)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 获取村庄数据
     */
    fun getVillage(id: UUID): VillageData? {
        return villageDataDAO.loadVillage(id)
    }

    /**
     * 根据区块坐标获取村庄
     */
    fun getVillageByChunk(worldName: String, chunkX: Int, chunkZ: Int): VillageData? {
        return villageDataDAO.loadVillageByChunk(worldName, chunkX, chunkZ)
    }

    /**
     * 获取指定世界的所有村庄
     */
    fun getVillagesByWorld(worldName: String): List<VillageData> {
        return villageDataDAO.loadVillagesByWorld(worldName)
    }

    /**
     * 获取所有村庄
     */
    fun getAllVillages(): List<VillageData> {
        return villageDataDAO.loadAllVillages()
    }

    /**
     * 更新村庄数据
     */
    fun updateVillage(village: VillageData): Boolean {
        return try {
            villageDataDAO.saveVillage(village)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 删除村庄
     */
    fun deleteVillage(id: UUID): Boolean {
        return villageDataDAO.deleteVillage(id)
    }

    /**
     * 更新村庄等级
     */
    fun updateVillageLevel(id: UUID, level: Int): Boolean {
        return villageDataDAO.updateVillageLevel(id, level)
    }

    /**
     * 更新劫掠时间
     */
    fun updateRaidTime(id: UUID, timestamp: Long = System.currentTimeMillis() / 1000): Boolean {
        return villageDataDAO.updateRaidTime(id, timestamp)
    }

    /**
     * 更新抢夺时间
     */
    fun updateLootTime(id: UUID, timestamp: Long = System.currentTimeMillis() / 1000): Boolean {
        return villageDataDAO.updateLootTime(id, timestamp)
    }

    /**
     * 添加建交队伍
     */
    fun addAlliedTeam(villageId: UUID, teamId: UUID): Boolean {
        return villageDataDAO.addAlliedTeam(villageId, teamId)
    }

    /**
     * 移除建交队伍
     */
    fun removeAlliedTeam(villageId: UUID, teamId: UUID): Boolean {
        return villageDataDAO.removeAlliedTeam(villageId, teamId)
    }

    /**
     * 添加敌对队伍
     */
    fun addHostileTeam(villageId: UUID, teamId: UUID): Boolean {
        return villageDataDAO.addHostileTeam(villageId, teamId)
    }

    /**
     * 移除敌对队伍
     */
    fun removeHostileTeam(villageId: UUID, teamId: UUID): Boolean {
        return villageDataDAO.removeHostileTeam(villageId, teamId)
    }

    /**
     * 更新单个村民统计字段
     */
    fun updateVillagerCountField(villageId: UUID, field: String, value: Int): Boolean {
        return villageDataDAO.updateVillagerCountField(villageId, field, value)
    }

    /**
     * 批量更新村民统计字段
     */
    fun updateVillagerStats(
        villageId: UUID,
        babyCount: Int? = null,
        unemployedCount: Int? = null,
        level1Count: Int? = null,
        level2Count: Int? = null,
        level3Count: Int? = null,
        level4Count: Int? = null,
        level5Count: Int? = null
    ): Boolean {
        return villageDataDAO.updateVillagerStats(
            villageId, babyCount, unemployedCount,
            level1Count, level2Count, level3Count, level4Count, level5Count
        )
    }
}
