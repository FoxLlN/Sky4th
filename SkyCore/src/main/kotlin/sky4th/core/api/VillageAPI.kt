
package sky4th.core.api

import sky4th.core.SkyCore
import sky4th.core.model.VillageData
import java.util.*

/**
 * 村庄 API
 * 提供便捷的村庄数据访问接口
 */
object VillageAPI {

    /**
     * 获取村庄服务
     */
    private fun getService() = SkyCore.getVillageService()

    /**
     * 检查村庄服务是否可用
     */
    fun isAvailable(): Boolean = getService() != null

    /**
     * 创建新村庄
     * @param village 村庄数据
     * @return 是否创建成功
     */
    fun createVillage(village: VillageData): Boolean {
        return getService()?.createVillage(village) ?: false
    }

    /**
     * 获取村庄数据
     * @param id 村庄 ID
     * @return 村庄数据，如果不存在则返回 null
     */
    fun getVillage(id: UUID): VillageData? {
        return getService()?.getVillage(id)
    }

    /**
     * 根据区块坐标获取村庄
     * @param worldName 世界名称
     * @param chunkX 区块 X 坐标
     * @param chunkZ 区块 Z 坐标
     * @return 村庄数据，如果不存在则返回 null
     */
    fun getVillageByChunk(worldName: String, chunkX: Int, chunkZ: Int): VillageData? {
        return getService()?.getVillageByChunk(worldName, chunkX, chunkZ)
    }

    /**
     * 获取指定世界的所有村庄
     * @param worldName 世界名称
     * @return 村庄列表
     */
    fun getVillagesByWorld(worldName: String): List<VillageData> {
        return getService()?.getVillagesByWorld(worldName) ?: emptyList()
    }

    /**
     * 获取所有村庄
     * @return 村庄列表
     */
    fun getAllVillages(): List<VillageData> {
        return getService()?.getAllVillages() ?: emptyList()
    }

    /**
     * 更新村庄数据
     * @param village 村庄数据
     * @return 是否更新成功
     */
    fun updateVillage(village: VillageData): Boolean {
        return getService()?.updateVillage(village) ?: false
    }

    /**
     * 删除村庄
     * @param id 村庄 ID
     * @return 是否删除成功
     */
    fun deleteVillage(id: UUID): Boolean {
        return getService()?.deleteVillage(id) ?: false
    }

    /**
     * 更新村庄等级
     * @param id 村庄 ID
     * @param level 新等级
     * @return 是否更新成功
     */
    fun updateVillageLevel(id: UUID, level: Int): Boolean {
        return getService()?.updateVillageLevel(id, level) ?: false
    }

    /**
     * 更新劫掠时间
     * @param id 村庄 ID
     * @param timestamp 时间戳（秒），默认为当前时间
     * @return 是否更新成功
     */
    fun updateRaidTime(id: UUID, timestamp: Long = System.currentTimeMillis() / 1000): Boolean {
        return getService()?.updateRaidTime(id, timestamp) ?: false
    }

    /**
     * 更新抢夺时间
     * @param id 村庄 ID
     * @param timestamp 时间戳（秒），默认为当前时间
     * @return 是否更新成功
     */
    fun updateLootTime(id: UUID, timestamp: Long = System.currentTimeMillis() / 1000): Boolean {
        return getService()?.updateLootTime(id, timestamp) ?: false
    }

    /**
     * 添加建交队伍
     * @param villageId 村庄 ID
     * @param teamId 队伍 ID
     * @return 是否添加成功
     */
    fun addAlliedTeam(villageId: UUID, teamId: UUID): Boolean {
        return getService()?.addAlliedTeam(villageId, teamId) ?: false
    }

    /**
     * 移除建交队伍
     * @param villageId 村庄 ID
     * @param teamId 队伍 ID
     * @return 是否移除成功
     */
    fun removeAlliedTeam(villageId: UUID, teamId: UUID): Boolean {
        return getService()?.removeAlliedTeam(villageId, teamId) ?: false
    }

    /**
     * 添加敌对队伍
     * @param villageId 村庄 ID
     * @param teamId 队伍 ID
     * @return 是否添加成功
     */
    fun addHostileTeam(villageId: UUID, teamId: UUID): Boolean {
        return getService()?.addHostileTeam(villageId, teamId) ?: false
    }

    /**
     * 移除敌对队伍
     * @param villageId 村庄 ID
     * @param teamId 队伍 ID
     * @return 是否移除成功
     */
    fun removeHostileTeam(villageId: UUID, teamId: UUID): Boolean {
        return getService()?.removeHostileTeam(villageId, teamId) ?: false
    }

    /**
     * 更新单个村民统计字段
     * @param villageId 村庄 ID
     * @param field 字段名：baby_villager_count, unemployed_villager_count, level1_villager_count 等
     * @param value 新值
     * @return 是否更新成功
     */
    fun updateVillagerCountField(villageId: UUID, field: String, value: Int): Boolean {
        return getService()?.updateVillagerCountField(villageId, field, value) ?: false
    }

    /**
     * 批量更新村民统计字段
     * @param villageId 村庄 ID
     * @param babyCount 幼年体村民人数（可选）
     * @param unemployedCount 无职业村民人数（可选）
     * @param level1Count 1级职业村民人数（可选）
     * @param level2Count 2级职业村民人数（可选）
     * @param level3Count 3级职业村民人数（可选）
     * @param level4Count 4级职业村民人数（可选）
     * @param level5Count 5级职业村民人数（可选）
     * @return 是否更新成功
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
        return getService()?.updateVillagerStats(
            villageId, babyCount, unemployedCount,
            level1Count, level2Count, level3Count, level4Count, level5Count
        ) ?: false
    }
}
