
package sky4th.core.model

import org.bukkit.generator.structure.GeneratedStructure
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import java.util.*
import org.bukkit.util.BoundingBox

/**
 * 村庄数据模型
 * 包含村庄的所有信息，包括位置、等级、劫掠状态、队伍关系等
 */
data class VillageData(
    val id: UUID,
    val worldName: String,
    val chunkX: Int,
    val chunkZ: Int,
    val minX: Int,
    val minY: Int,
    val minZ: Int,
    val maxX: Int,
    val maxY: Int,
    val maxZ: Int,
    var level: Int = 1,
    var lastRaidTime: Long = 0L,
    var lastLootTime: Long = 0L,
    var alliedTeams: Set<UUID> = emptySet(),
    var hostileTeams: Set<UUID> = emptySet(),
    // 村民人数统计
    var babyVillagerCount: Int = 0,
    var unemployedVillagerCount: Int = 0,
    var level1VillagerCount: Int = 0,
    var level2VillagerCount: Int = 0,
    var level3VillagerCount: Int = 0,
    var level4VillagerCount: Int = 0,
    var level5VillagerCount: Int = 0
) {
    companion object {
        /**
         * 创建新的村庄数据
         */
        fun createNew(
            worldName: String,
            chunkX: Int,
            chunkZ: Int,
            minX: Int,
            minY: Int,
            minZ: Int,
            maxX: Int,
            maxY: Int,
            maxZ: Int
        ): VillageData {
            return VillageData(
                id = UUID.randomUUID(),
                worldName = worldName,
                chunkX = chunkX,
                chunkZ = chunkZ,
                minX = minX,
                minY = minY,
                minZ = minZ,
                maxX = maxX,
                maxY = maxY,
                maxZ = maxZ,
                level = 1,
                lastRaidTime = 0L,
                lastLootTime = 0L,
                alliedTeams = emptySet(),
                hostileTeams = emptySet(),
                babyVillagerCount = 0,
                unemployedVillagerCount = 0,
                level1VillagerCount = 0,
                level2VillagerCount = 0,
                level3VillagerCount = 0,
                level4VillagerCount = 0,
                level5VillagerCount = 0
            )
        }

        /**
         * 从 Bukkit Location 创建村庄数据
         */
        fun fromLocation(location: Location, radius: Int = 32): VillageData {
            val world = location.world ?: throw IllegalArgumentException("Location must have a world")
            val chunk = location.chunk

            return VillageData(
                id = UUID.randomUUID(),
                worldName = world.name,
                chunkX = chunk.x,
                chunkZ = chunk.z,
                minX = location.blockX - radius,
                minY = location.blockY - radius,
                minZ = location.blockZ - radius,
                maxX = location.blockX + radius,
                maxY = location.blockY + radius,
                maxZ = location.blockZ + radius,
                level = 1,
                lastRaidTime = 0L,
                lastLootTime = 0L,
                alliedTeams = emptySet(),
                hostileTeams = emptySet(),
                babyVillagerCount = 0,
                unemployedVillagerCount = 0,
                level1VillagerCount = 0,
                level2VillagerCount = 0,
                level3VillagerCount = 0,
                level4VillagerCount = 0,
                level5VillagerCount = 0
            )
        }

        fun fromStructure(generated: GeneratedStructure, world: World): VillageData {
            val box = generated.boundingBox   // 实际调用 getBoundingBox()
            return VillageData(
                id = UUID.randomUUID(),
                worldName = world.name,
                chunkX = box.minX.toInt() shr 4,
                chunkZ = box.minZ.toInt() shr 4,
                minX = box.minX.toInt(),
                minY = box.minY.toInt(),
                minZ = box.minZ.toInt(),
                maxX = box.maxX.toInt(),
                maxY = box.maxY.toInt(),
                maxZ = box.maxZ.toInt(),
                level = 1,
                lastRaidTime = 0L,
                lastLootTime = 0L,
                alliedTeams = emptySet(),
                hostileTeams = emptySet(),
                babyVillagerCount = 0,
                unemployedVillagerCount = 0,
                level1VillagerCount = 0,
                level2VillagerCount = 0,
                level3VillagerCount = 0,
                level4VillagerCount = 0,
                level5VillagerCount = 0
            )
        }
    }

    /**
     * 获取 Bukkit World 对象
     */
    fun getWorld(): World? = Bukkit.getWorld(worldName)

    /**
     * 获取村庄中心位置
     */
    fun getCenterLocation(): Location? {
        val world = getWorld() ?: return null
        return Location(
            world,
            (minX + maxX).toDouble() / 2,
            (minY + maxY).toDouble() / 2,
            (minZ + maxZ).toDouble() / 2
        )
    }

    /**
     * 检查指定位置是否在村庄边界内
     */
    fun contains(location: Location): Boolean {
        if (location.world?.name != worldName) return false
        val x = location.blockX
        val y = location.blockY
        val z = location.blockZ
        return x in minX..maxX && y in minY..maxY && z in minZ..maxZ
    }

    /**
     * 检查指定区块是否在村庄范围内
     */
    fun containsChunk(chunkX: Int, chunkZ: Int): Boolean {
        val chunkMinX = minX shr 4
        val chunkMaxX = maxX shr 4
        val chunkMinZ = minZ shr 4
        val chunkMaxZ = maxZ shr 4
        return chunkX in chunkMinX..chunkMaxX && chunkZ in chunkMinZ..chunkMaxZ
    }

    /**
     * 添加建交队伍
     */
    fun addAlliedTeam(teamId: UUID) {
        alliedTeams = alliedTeams + teamId
    }

    /**
     * 移除建交队伍
     */
    fun removeAlliedTeam(teamId: UUID) {
        alliedTeams = alliedTeams - teamId
    }

    /**
     * 添加敌对队伍
     */
    fun addHostileTeam(teamId: UUID) {
        hostileTeams = hostileTeams + teamId
    }

    /**
     * 移除敌对队伍
     */
    fun removeHostileTeam(teamId: UUID) {
        hostileTeams = hostileTeams - teamId
    }

    /**
     * 检查队伍是否是建交队伍
     */
    fun isAlliedTeam(teamId: UUID): Boolean = teamId in alliedTeams

    /**
     * 检查队伍是否是敌对队伍
     */
    fun isHostileTeam(teamId: UUID): Boolean = teamId in hostileTeams

    /**
     * 更新劫掠时间
     */
    fun updateRaidTime() {
        lastRaidTime = System.currentTimeMillis() / 1000
    }

    /**
     * 更新抢夺时间
     */
    fun updateLootTime() {
        lastLootTime = System.currentTimeMillis() / 1000
    }

    /**
     * 检查劫掠冷却是否结束
     */
    fun isRaidCooldownOver(cooldownSeconds: Long): Boolean {
        val now = System.currentTimeMillis() / 1000
        return now - lastRaidTime >= cooldownSeconds
    }

    /**
     * 检查抢夺冷却是否结束
     */
    fun isLootCooldownOver(cooldownSeconds: Long): Boolean {
        val now = System.currentTimeMillis() / 1000
        return now - lastLootTime >= cooldownSeconds
    }

    /**
     * 更新村民统计信息
     * @param babyCount 幼年体村民人数
     * @param unemployedCount 无职业村民人数
     * @param levelCounts 各级职业村民人数（索引0对应1级，索引4对应5级）
     */
    fun updateVillagerStats(
        babyCount: Int,
        unemployedCount: Int,
        levelCounts: Map<Int, Int>
    ) {
        babyVillagerCount = babyCount
        unemployedVillagerCount = unemployedCount
        level1VillagerCount = levelCounts[1] ?: 0
        level2VillagerCount = levelCounts[2] ?: 0
        level3VillagerCount = levelCounts[3] ?: 0
        level4VillagerCount = levelCounts[4] ?: 0
        level5VillagerCount = levelCounts[5] ?: 0
    }

    /**
     * 增加指定类型的村民数量
     * @param villagerType 村民类型：baby(幼年), unemployed(无职业), level1-5(职业等级)
     * @param amount 增加的数量（默认为1）
     */
    fun addVillager(villagerType: String, amount: Int = 1) {
        when (villagerType.lowercase()) {
            "baby" -> babyVillagerCount += amount
            "unemployed" -> unemployedVillagerCount += amount
            "level1" -> level1VillagerCount += amount
            "level2" -> level2VillagerCount += amount
            "level3" -> level3VillagerCount += amount
            "level4" -> level4VillagerCount += amount
            "level5" -> level5VillagerCount += amount
        }
    }

    /**
     * 减少指定类型的村民数量
     * @param villagerType 村民类型：baby(幼年), unemployed(无职业), level1-5(职业等级)
     * @param amount 减少的数量（默认为1）
     */
    fun removeVillager(villagerType: String, amount: Int = 1) {
        when (villagerType.lowercase()) {
            "baby" -> babyVillagerCount = maxOf(0, babyVillagerCount - amount)
            "unemployed" -> unemployedVillagerCount = maxOf(0, unemployedVillagerCount - amount)
            "level1" -> level1VillagerCount = maxOf(0, level1VillagerCount - amount)
            "level2" -> level2VillagerCount = maxOf(0, level2VillagerCount - amount)
            "level3" -> level3VillagerCount = maxOf(0, level3VillagerCount - amount)
            "level4" -> level4VillagerCount = maxOf(0, level4VillagerCount - amount)
            "level5" -> level5VillagerCount = maxOf(0, level5VillagerCount - amount)
        }
    }

    /**
     * 获取村民总数
     */
    fun getTotalVillagerCount(): Int {
        return babyVillagerCount + unemployedVillagerCount +
                level1VillagerCount + level2VillagerCount + level3VillagerCount +
                level4VillagerCount + level5VillagerCount
    }

    /**
     * 获取成年村民总数
     */
    fun getAdultVillagerCount(): Int {
        return unemployedVillagerCount +
                level1VillagerCount + level2VillagerCount + level3VillagerCount +
                level4VillagerCount + level5VillagerCount
    }

    /**
     * 获取职业村民总数
     */
    fun getEmployedVillagerCount(): Int {
        return level1VillagerCount + level2VillagerCount + level3VillagerCount +
                level4VillagerCount + level5VillagerCount
    }
}
