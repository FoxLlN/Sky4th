
package sky4th.bettervillage.manager

import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.World
import sky4th.bettervillage.BetterVillage
import sky4th.bettervillage.config.ConfigManager
import sky4th.core.api.VillageAPI
import sky4th.core.model.VillageData
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 村庄管理器
 * 负责村庄数据的缓存和管理
 */
object VillageManager {

    // 村庄缓存：ID -> VillageData
    private val villageCache = ConcurrentHashMap<UUID, VillageData>()

    // 区块到村庄的映射：worldName -> (chunkKey -> villageId)
    private val chunkToVillageMap = ConcurrentHashMap<String, ConcurrentHashMap<Long, UUID>>()

    // 异步保存队列
    private val saveQueue = LinkedBlockingQueue<VillageData>()
    private val saverStarted = AtomicBoolean(false)
    private val shutdownRequested = AtomicBoolean(false)

    /**
     * 初始化管理器
     */
    fun initialize() {
        if (!VillageAPI.isAvailable()) {
            BetterVillage.instance.logger.warning("VillageAPI 不可用，村庄管理器无法正常工作")
            return
        }

        // 加载所有村庄到缓存
        loadAllVillages()
        BetterVillage.instance.logger.info("村庄管理器已初始化，已加载 ${villageCache.size} 个村庄")
    }

    /**
     * 加载所有村庄到缓存
     */
    private fun loadAllVillages() {
        villageCache.clear()
        chunkToVillageMap.clear()

        val villages = VillageAPI.getAllVillages()
        villages.forEach { village ->
            villageCache[village.id] = village
            updateChunkMapping(village)
        }
    }

    /**
     * 遍历村庄覆盖的所有区块
     */
    private fun forEachChunkInVillage(village: VillageData, action: (chunkX: Int, chunkZ: Int) -> Unit) {
        val minX = village.minX shr 4
        val maxX = village.maxX shr 4
        val minZ = village.minZ shr 4
        val maxZ = village.maxZ shr 4

        for (x in minX..maxX) {
            for (z in minZ..maxZ) {
                action(x, z)
            }
        }
    }

    /**
     * 更新区块映射
     */
    private fun updateChunkMapping(village: VillageData) {
        val worldMap = chunkToVillageMap.computeIfAbsent(village.worldName) { ConcurrentHashMap() }

        forEachChunkInVillage(village) { x, z ->
            worldMap.put(getChunkKey(x, z), village.id)
        }
    }

    /**
     * 创建新村庄（异步保存到数据库）
     */
    fun createVillage(village: VillageData): Boolean {
        if (!VillageAPI.isAvailable()) return false

        // 先放入缓存（立即生效）
        villageCache[village.id] = village
        updateChunkMapping(village)

        // 加入保存队列
        saveQueue.offer(village)
        ensureSaverRunning()

        return true
    }

    /**
     * 确保异步保存线程正在运行
     */
    private fun ensureSaverRunning() {
        if (saverStarted.compareAndSet(false, true)) {
            Bukkit.getScheduler().runTaskAsynchronously(BetterVillage.instance, Runnable {
                try {
                    while (!shutdownRequested.get()) {
                        val village = saveQueue.poll(1, TimeUnit.SECONDS)
                        if (village == null) {
                            // 队列空，但可能还有数据正在加入？等待片刻再检查
                            if (shutdownRequested.get()) break
                            Thread.sleep(100) // 避免空转
                            continue
                        }
                        try {
                            VillageAPI.createVillage(village)
                        } catch (e: Exception) {
                            // 可选：重新放回队列或记录失败日志
                        }
                    }
                } finally {
                    saverStarted.set(false)
                    // 如果队列中还有数据且未关闭，重启 saver
                    if (!shutdownRequested.get() && !saveQueue.isEmpty()) {
                        ensureSaverRunning()
                    }
                }
            })
        }
    }

    /**
     * 获取村庄数据
     */
    fun getVillage(id: UUID): VillageData? {
        // 先从缓存获取
        villageCache[id]?.let { return it }

        // 缓存中不存在，从数据库加载
        if (!VillageAPI.isAvailable()) return null

        val village = VillageAPI.getVillage(id)
        if (village != null) {
            villageCache[id] = village
            updateChunkMapping(village)
        }
        return village
    }

    /**
     * 根据区块坐标获取村庄
     */
    fun getVillageByChunk(worldName: String, chunkX: Int, chunkZ: Int): VillageData? {
        val worldMap = chunkToVillageMap[worldName] ?: return null
        val chunkKey = getChunkKey(chunkX, chunkZ)
        val villageId = worldMap.get(chunkKey) ?: return null

        return getVillage(villageId)
    }

    /**
     * 检查区块是否在村庄缓存中
     * 仅检查缓存，不查询数据库，避免阻塞
     * @return true表示该区块已被某个村庄覆盖
     */
    fun isVillageInCache(worldName: String, chunkX: Int, chunkZ: Int): Boolean {
        val worldMap = chunkToVillageMap[worldName] ?: return false
        val chunkKey = getChunkKey(chunkX, chunkZ)
        return worldMap.containsKey(chunkKey)
    }

    /**
     * 根据区块对象获取村庄
     */
    fun getVillageByChunk(chunk: Chunk): VillageData? {
        return getVillageByChunk(chunk.world.name, chunk.x, chunk.z)
    }

    /**
     * 根据位置获取村庄
     */
    fun getVillageByLocation(location: Location): VillageData? {
        return getVillageByChunk(location.world?.name ?: return null, location.blockX shr 4, location.blockZ shr 4)
    }

    /**
     * 获取指定世界的所有村庄
     */
    fun getVillagesByWorld(worldName: String): List<VillageData> {
        return villageCache.values.filter { it.worldName == worldName }
    }

    /**
     * 获取所有村庄
     */
    fun getAllVillages(): List<VillageData> {
        return villageCache.values.toList()
    }

    /**
     * 更新村庄数据
     */
    fun updateVillage(village: VillageData): Boolean {
        if (!VillageAPI.isAvailable()) return false

        val oldVillage = villageCache[village.id]
        val success = VillageAPI.updateVillage(village)
        if (success) {
            villageCache[village.id] = village
            val worldMap = chunkToVillageMap[village.worldName]
            if (worldMap != null) {
                // 如果旧村庄存在且范围不同，先清除旧映射
                if (oldVillage != null && (oldVillage.minX != village.minX || oldVillage.maxX != village.maxX ||
                        oldVillage.minZ != village.minZ || oldVillage.maxZ != village.maxZ)) {
                    forEachChunkInVillage(oldVillage) { x, z ->
                        val key = getChunkKey(x, z)
                        if (worldMap.get(key) == village.id) {
                            worldMap.remove(key)
                        }
                    }
                }
                // 添加新区块映射
                forEachChunkInVillage(village) { x, z ->
                    worldMap.put(getChunkKey(x, z), village.id)
                }
            }
        }
        return success
    }

    /**
     * 删除村庄
     */
    fun deleteVillage(id: UUID): Boolean {
        if (!VillageAPI.isAvailable()) return false

        val village = villageCache[id] ?: return false
        val success = VillageAPI.deleteVillage(id)

        if (success) {
            villageCache.remove(id)
            // 清除区块映射
            val worldMap = chunkToVillageMap[village.worldName]
            if (worldMap != null) {
                forEachChunkInVillage(village) { x, z ->
                    val chunkKey = getChunkKey(x, z)
                    if (worldMap.get(chunkKey) == id) {
                        worldMap.remove(chunkKey)
                    }
                }
            }
        }
        return success
    }

    /**
     * 更新村庄等级
     */
    fun updateVillageLevel(id: UUID, level: Int): Boolean {
        val village = getVillage(id) ?: return false
        village.level = level
        return updateVillage(village)
    }

    /**
     * 更新劫掠时间
     */
    fun updateRaidTime(id: UUID): Boolean {
        val village = getVillage(id) ?: return false
        village.updateRaidTime()
        return updateVillage(village)
    }

    /**
     * 更新抢夺时间
     */
    fun updateLootTime(id: UUID): Boolean {
        val village = getVillage(id) ?: return false
        village.updateLootTime()
        return updateVillage(village)
    }

    /**
     * 添加建交队伍
     */
    fun addAlliedTeam(villageId: UUID, teamId: UUID): Boolean {
        val village = getVillage(villageId) ?: return false
        village.addAlliedTeam(teamId)
        return updateVillage(village)
    }

    /**
     * 移除建交队伍
     */
    fun removeAlliedTeam(villageId: UUID, teamId: UUID): Boolean {
        val village = getVillage(villageId) ?: return false
        village.removeAlliedTeam(teamId)
        return updateVillage(village)
    }

    /**
     * 添加敌对队伍
     */
    fun addHostileTeam(villageId: UUID, teamId: UUID): Boolean {
        val village = getVillage(villageId) ?: return false
        village.addHostileTeam(teamId)
        return updateVillage(village)
    }

    /**
     * 移除敌对队伍
     */
    fun removeHostileTeam(villageId: UUID, teamId: UUID): Boolean {
        val village = getVillage(villageId) ?: return false
        village.removeHostileTeam(teamId)
        return updateVillage(village)
    }

    /**
     * 从缓存中移除村庄
     */
    fun removeFromCache(id: UUID) {
        val village = villageCache.remove(id) ?: return

        // 清除区块映射
        val worldMap = chunkToVillageMap[village.worldName]
        if (worldMap != null) {
            forEachChunkInVillage(village) { x, z ->
                val chunkKey = getChunkKey(x, z)
                if (worldMap.get(chunkKey) == id) {
                    worldMap.remove(chunkKey)
                }
            }
        }
    }

    /**
     * 清空所有缓存
     */
    fun clearAll() {
        villageCache.clear()
        chunkToVillageMap.clear()
    }

    /**
     * 关闭管理器,停止所有异步任务
     */
    fun shutdown() {
        // 请求关闭异步保存任务
        shutdownRequested.set(true)
        
        // 等待保存队列清空
        try {
            val timeout = 5L // 5秒超时
            val startTime = System.currentTimeMillis()
            while (!saveQueue.isEmpty() && System.currentTimeMillis() - startTime < timeout * 1000) {
                Thread.sleep(100)
            }

            if (!saveQueue.isEmpty()) {
                BetterVillage.instance.logger.warning("插件关闭时仍有 ${saveQueue.size} 个村庄未保存")
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        // 清空缓存
        clearAll()

        // 清理村民状态管理器
        VillagerStateManager.clearAllStates()

        // 清理村民离开记录管理器
        VillagerLeaveManager.clearAllRecords()
    }

    /**
     * 获取区块键
     */
    private fun getChunkKey(chunkX: Int, chunkZ: Int): Long {
        return (chunkX.toLong() shl 32) xor (chunkZ.toLong() and 0xffffffff)
    }

    /**
     * 更新村庄的村民统计信息
     * 注意：此方法会遍历村庄范围内的所有区块和实体，非常耗时
     * 建议仅在必要时调用，平时使用增量更新方法
     * @param villageId 村庄ID
     * @return 是否更新成功
     */
    fun updateVillageVillagerStats(villageId: UUID, chunksPerTick: Int = 4): Boolean {
        val village = getVillage(villageId) ?: return false
        println("开始分批更新村庄 $villageId 的村民统计信息")

        // 收集所有区块位置
        val chunkPositions = mutableListOf<Pair<Int, Int>>()
        forEachChunkInVillage(village) { chunkX, chunkZ ->
            chunkPositions.add(chunkX to chunkZ)
        }

        // 如果没有区块，直接返回
        if (chunkPositions.isEmpty()) {
            println("村庄 $villageId 没有区块需要统计")
            return false
        }

        println("村庄 $villageId 共有 ${chunkPositions.size} 个区块需要统计")

        // 初始化统计
        val stats = mutableMapOf(
            "baby" to 0,
            "unemployed" to 0,
            "level1" to 0,
            "level2" to 0,
            "level3" to 0,
            "level4" to 0,
            "level5" to 0
        )

        // 分批处理区块
        var currentIndex = 0
        val totalChunks = chunkPositions.size

        // 创建一个任务来分批处理
        val processChunks = object : Runnable {
            override fun run() {
                val world = village.getWorld() ?: return

                // 处理当前批次的区块
                val endIndex = minOf(currentIndex + chunksPerTick, totalChunks)
                for (i in currentIndex until endIndex) {
                    val (chunkX, chunkZ) = chunkPositions[i]

                    // 加载区块
                    val chunk = if (world.isChunkLoaded(chunkX, chunkZ)) {
                        world.getChunkAt(chunkX, chunkZ)
                    } else {
                        world.loadChunk(chunkX, chunkZ)
                        world.getChunkAt(chunkX, chunkZ)
                    }

                    // 统计村民
                    chunk.entities.forEach { entity ->
                        if (entity is org.bukkit.entity.Villager) {
                            when {
                                entity.isAdult.not() -> stats["baby"] = stats["baby"]!! + 1
                                // 无职业和傻子都统计为无职业
                                entity.profession == org.bukkit.entity.Villager.Profession.NONE ||
                                entity.profession == org.bukkit.entity.Villager.Profession.NITWIT ->
                                    stats["unemployed"] = stats["unemployed"]!! + 1
                                else -> {
                                    // 只有有职业的村民才统计到对应等级
                                    val level = entity.villagerLevel
                                    if (level in 1..5) {
                                        val levelKey = "level$level"
                                        stats[levelKey] = stats[levelKey]!! + 1
                                    }
                                }
                            }
                        }
                    }
                }

                println("已处理 ${endIndex}/${totalChunks} 个区块")

                currentIndex = endIndex

                // 如果还有区块未处理，继续下一批
                if (currentIndex < totalChunks) {
                    Bukkit.getScheduler().runTaskLater(BetterVillage.instance, this, 1L)
                } else {
                    // 所有区块处理完成，更新缓存和数据库
                    println("村庄 $villageId 统计完成: baby=${stats["baby"]}, unemployed=${stats["unemployed"]}, level1=${stats["level1"]}, level2=${stats["level2"]}, level3=${stats["level3"]}, level4=${stats["level4"]}, level5=${stats["level5"]}")

                    village.updateVillagerStats(
                        babyCount = stats["baby"] ?: 0,
                        unemployedCount = stats["unemployed"] ?: 0,
                        levelCounts = mapOf(
                            1 to (stats["level1"] ?: 0),
                            2 to (stats["level2"] ?: 0),
                            3 to (stats["level3"] ?: 0),
                            4 to (stats["level4"] ?: 0),
                            5 to (stats["level5"] ?: 0)
                        )
                    )

                    // 异步更新数据库
                    Bukkit.getScheduler().runTaskAsynchronously(BetterVillage.instance, Runnable {
                        VillageAPI.updateVillagerStats(
                            villageId,
                            babyCount = stats["baby"],
                            unemployedCount = stats["unemployed"],
                            level1Count = stats["level1"],
                            level2Count = stats["level2"],
                            level3Count = stats["level3"],
                            level4Count = stats["level4"],
                            level5Count = stats["level5"]
                        )
                    })
                }
            }
        }

        // 开始处理第一批区块
        Bukkit.getScheduler().runTask(BetterVillage.instance, processChunks)
        return true
    }

    /**
     * 村民转职时更新统计
     * 此方法线程安全，可在异步线程调用
     * @param villageId 村庄ID
     * @param fromLevel 原等级（0表示无职业）
     * @param toLevel 新等级（0表示无职业）
     * @return 是否更新成功
     */
    fun updateVillagerProfession(villageId: UUID, fromLevel: Int, toLevel: Int): Boolean {
        val village = villageCache[villageId] ?: return false

        // 减少原等级数量
        val fromField = when (fromLevel) {
            0 -> "unemployed_villager_count"
            in 1..5 -> "level${fromLevel}_villager_count"
            else -> return false
        }

        // 增加新等级数量
        val toField = when (toLevel) {
            0 -> "unemployed_villager_count"
            in 1..5 -> "level${toLevel}_villager_count"
            else -> return false
        }

        // 更新缓存（线程安全）
        when (fromLevel) {
            0 -> village.unemployedVillagerCount--
            in 1..5 -> {
                when (fromLevel) {
                    1 -> village.level1VillagerCount--
                    2 -> village.level2VillagerCount--
                    3 -> village.level3VillagerCount--
                    4 -> village.level4VillagerCount--
                    5 -> village.level5VillagerCount--
                }
            }
        }

        when (toLevel) {
            0 -> village.unemployedVillagerCount++
            in 1..5 -> {
                when (toLevel) {
                    1 -> village.level1VillagerCount++
                    2 -> village.level2VillagerCount++
                    3 -> village.level3VillagerCount++
                    4 -> village.level4VillagerCount++
                    5 -> village.level5VillagerCount++
                }
            }
        }

        // 使用API批量更新数据库中的两个字段
        return VillageAPI.updateVillagerStats(
            villageId,
            unemployedCount = if (fromLevel == 0 || toLevel == 0) village.unemployedVillagerCount else null,
            level1Count = if (fromLevel == 1 || toLevel == 1) village.level1VillagerCount else null,
            level2Count = if (fromLevel == 2 || toLevel == 2) village.level2VillagerCount else null,
            level3Count = if (fromLevel == 3 || toLevel == 3) village.level3VillagerCount else null,
            level4Count = if (fromLevel == 4 || toLevel == 4) village.level4VillagerCount else null,
            level5Count = if (fromLevel == 5 || toLevel == 5) village.level5VillagerCount else null
        )
    }

    /**
     * 村民升级时更新统计
     * @param villageId 村庄ID
     * @param fromLevel 原等级
     * @param toLevel 新等级
     * @return 是否更新成功
     */
    fun updateVillagerLevel(villageId: UUID, fromLevel: Int, toLevel: Int): Boolean {
        val village = getVillage(villageId) ?: return false

        // 减少原等级数量
        when (fromLevel) {
            in 1..5 -> {
                when (fromLevel) {
                    1 -> village.level1VillagerCount--
                    2 -> village.level2VillagerCount--
                    3 -> village.level3VillagerCount--
                    4 -> village.level4VillagerCount--
                    5 -> village.level5VillagerCount--
                }
            }
        }

        // 增加新等级数量
        when (toLevel) {
            in 1..5 -> {
                when (toLevel) {
                    1 -> village.level1VillagerCount++
                    2 -> village.level2VillagerCount++
                    3 -> village.level3VillagerCount++
                    4 -> village.level4VillagerCount++
                    5 -> village.level5VillagerCount++
                }
            }
        }

        // 使用API批量更新数据库中的两个字段
        return VillageAPI.updateVillagerStats(
            villageId,
            level1Count = if (fromLevel == 1 || toLevel == 1) village.level1VillagerCount else null,
            level2Count = if (fromLevel == 2 || toLevel == 2) village.level2VillagerCount else null,
            level3Count = if (fromLevel == 3 || toLevel == 3) village.level3VillagerCount else null,
            level4Count = if (fromLevel == 4 || toLevel == 4) village.level4VillagerCount else null,
            level5Count = if (fromLevel == 5 || toLevel == 5) village.level5VillagerCount else null
        )
    }

    /**
     * 村民繁衍时更新统计
     * 此方法线程安全，可在异步线程调用
     * @param villageId 村庄ID
     * @param isBaby 是否为幼年体
     * @return 是否更新成功
     */
    fun updateVillagerBreeding(villageId: UUID, isBaby: Boolean): Boolean {
        val village = villageCache[villageId] ?: return false

        if (isBaby) {
            village.babyVillagerCount++
            // 使用API只更新幼年体村民数量
            return VillageAPI.updateVillagerStats(
                villageId,
                babyCount = village.babyVillagerCount
            )
        } else {
            village.unemployedVillagerCount++
            // 使用API只更新无职业村民数量
            return VillageAPI.updateVillagerStats(
                villageId,
                unemployedCount = village.unemployedVillagerCount
            )
        }
    }

    /**
     * 村民死亡时更新统计
     * 此方法线程安全，可在异步线程调用
     * @param villageId 村庄ID
     * @param isBaby 是否为幼年体
     * @param isUnemployed 是否为无职业村民
     * @param level 村民等级（1-5，仅当isUnemployed为false时有效）
     * @return 是否更新成功
     */
    fun updateVillagerDeath(villageId: UUID, isBaby: Boolean, isUnemployed: Boolean, level: Int = 0): Boolean {
        val village = villageCache[villageId] ?: return false

        if (isBaby) {
            // 幼年体村民
            village.babyVillagerCount--
            return VillageAPI.updateVillagerStats(
                villageId,
                babyCount = village.babyVillagerCount
            )
        } else if (isUnemployed) {
            // 无职业村民
            village.unemployedVillagerCount--
            return VillageAPI.updateVillagerStats(
                villageId,
                unemployedCount = village.unemployedVillagerCount
            )
        } else {
            // 有职业村民，减少对应等级统计
            when (level) {
                1 -> village.level1VillagerCount--
                2 -> village.level2VillagerCount--
                3 -> village.level3VillagerCount--
                4 -> village.level4VillagerCount--
                5 -> village.level5VillagerCount--
            }
            return VillageAPI.updateVillagerStats(
                villageId,
                level1Count = if (level == 1) village.level1VillagerCount else null,
                level2Count = if (level == 2) village.level2VillagerCount else null,
                level3Count = if (level == 3) village.level3VillagerCount else null,
                level4Count = if (level == 4) village.level4VillagerCount else null,
                level5Count = if (level == 5) village.level5VillagerCount else null
            )
        }
    }

    /**
     * 村民成长时更新统计（幼年→成年）
     * 此方法线程安全，可在异步线程调用
     * @param villageId 村庄ID
     * @return 是否更新成功
     */
    fun updateVillagerGrowth(villageId: UUID): Boolean {
        val village = villageCache[villageId] ?: return false

        // 减少幼年体村民数量
        village.babyVillagerCount--
        // 增加无职业村民数量（刚长大的村民默认无职业）
        village.unemployedVillagerCount++

        // 使用API批量更新数据库中的两个字段
        return VillageAPI.updateVillagerStats(
            villageId,
            babyCount = village.babyVillagerCount,
            unemployedCount = village.unemployedVillagerCount
        )
    }

    /**
     * 计算村庄的最大村民数量
     * 公式：basePopulation + floor(area / populationPerArea)
     * @param village 村庄数据
     * @return 最大村民数量
     */
    fun calculateMaxVillagers(village: VillageData): Int {
        val area = (village.maxX - village.minX) * (village.maxZ - village.minZ)
        val basePopulation = ConfigManager.getBasePopulation()
        val populationPerArea = ConfigManager.getPopulationPerArea()
        val populationPerAreaValue = area.toDouble() / populationPerArea
        println("村庄区域：$area，基础村民数量：$basePopulation，每区域村民数量：$populationPerArea，每区域村民数量值：$populationPerAreaValue")
        return basePopulation + kotlin.math.floor(populationPerAreaValue).toInt()
    }
}

