package sky4th.bettervillage.util

import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.generator.structure.GeneratedStructure
import org.bukkit.generator.structure.Structure
import sky4th.bettervillage.BetterVillage
import sky4th.bettervillage.manager.VillageManager
import sky4th.core.model.VillageData
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object VillageStructureChecker : Listener {

    private val villageStructures = setOf(
        Structure.VILLAGE_PLAINS,
        Structure.VILLAGE_DESERT,
        Structure.VILLAGE_SAVANNA,
        Structure.VILLAGE_SNOWY,
        Structure.VILLAGE_TAIGA
    )

    // 已检查过的结构缓存：结构中心区块坐标 -> 检查时间戳
    private val checkedStructures = ConcurrentHashMap<String, Long>()
    // 缓存过期时间（5分钟）
    private const val CACHE_EXPIRY_MS = 5 * 60 * 1000L

    // 当前正在处理的村庄创建请求数量
    private val pendingVillageCreations = AtomicInteger(0)
    // 每tick最多处理的村庄创建请求数
    private const val MAX_VILLAGE_CREATIONS_PER_TICK = 3

    fun isInVillage(location: Location): Boolean {
        return VillageManager.getVillageByLocation(location) != null
    }

    /**
     * 检查区块是否包含村庄结构
     * 仅执行轻量级操作，将耗时操作异步化
     */
    private fun checkChunkForVillage(chunk: Chunk) {
        val structures = chunk.getStructures()
        val villageStructuresHere = structures.filter { it.structure in villageStructures }
        if (villageStructuresHere.isEmpty()) return

        for (generated in villageStructuresHere) {
            // 检查缓存，避免重复处理同一结构
            val structureKey = "${chunk.world.name}_${generated.boundingBox.minX.toInt() shr 4}_${generated.boundingBox.minZ.toInt() shr 4}"
            val now = System.currentTimeMillis()

            val lastChecked = checkedStructures[structureKey]
            if (lastChecked != null && now - lastChecked < CACHE_EXPIRY_MS) {
                continue // 已在近期检查过，跳过
            }

            // 限制并发创建数量
            if (pendingVillageCreations.get() >= MAX_VILLAGE_CREATIONS_PER_TICK) {
                continue
            }

            // 异步处理村庄创建
            pendingVillageCreations.incrementAndGet()
            Bukkit.getScheduler().runTaskAsynchronously(BetterVillage.instance, Runnable {
                try {
                    processVillageStructureAsync(generated, chunk.world, structureKey)
                } finally {
                    pendingVillageCreations.decrementAndGet()
                }
            })
        }
    }

    /**
     * 异步处理村庄结构
     * 仅使用缓存进行存在性检查，不强制加载区块
     */
    private fun processVillageStructureAsync(generated: GeneratedStructure, world: World, structureKey: String) {
        val box = generated.boundingBox

        val minChunkX = box.minX.toInt() shr 4
        val maxChunkX = box.maxX.toInt() shr 4
        val minChunkZ = box.minZ.toInt() shr 4
        val maxChunkZ = box.maxZ.toInt() shr 4

        // 使用缓存检查村庄是否存在，避免数据库查询和区块加载
        var exists = false
        run loop@{
            for (cx in minChunkX..maxChunkX) {
                for (cz in minChunkZ..maxChunkZ) {
                    if (VillageManager.isVillageInCache(world.name, cx, cz)) {
                        exists = true
                        return@loop
                    }
                }
            }
        }

        // 先标记结构已检查，防止重复处理
        checkedStructures[structureKey] = System.currentTimeMillis()

        if (!exists) {
            // 创建村庄数据（不统计村民）
            val newVillage = VillageData.fromStructure(generated, world)

            // 在主线程更新缓存
            Bukkit.getScheduler().runTask(BetterVillage.instance, Runnable {
                // 再次检查，确保没有其他线程已经创建了村庄
                if (!VillageManager.isVillageInCache(world.name, minChunkX, minChunkZ)) {
                    VillageManager.createVillage(newVillage)
                    // 延迟5秒后初始化村民统计，给服务器一些缓冲时间
                    Bukkit.getScheduler().runTaskLater(BetterVillage.instance, Runnable {
                        VillageManager.updateVillageVillagerStats(newVillage.id)
                    }, 100L) // 5秒 = 100 ticks
                }
            })
        }
    }

    @EventHandler
    fun onChunkLoad(event: ChunkLoadEvent) {
        checkChunkForVillage(event.chunk)
    }

    /**
     * 扫描已加载的区块
     * 使用分批处理避免卡顿
     */
    fun scanLoadedChunks() {
        val worlds = Bukkit.getWorlds().iterator()
        var chunksInWorld: Iterator<Chunk>? = null
        val task = object : Runnable {
            override fun run() {
                if (chunksInWorld == null || !chunksInWorld!!.hasNext()) {
                    if (!worlds.hasNext()) {
                        // 任务完成，取消任务
                        return
                    }
                    val world = worlds.next()
                    chunksInWorld = world.loadedChunks.iterator()
                }
                repeat(50) { // 每 tick 处理 50 个区块
                    if (chunksInWorld.hasNext()) {
                        checkChunkForVillage(chunksInWorld.next())
                    }
                }
            }
        }
        // 使用 scheduleSyncRepeatingTask 获取任务 ID
        val taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(BetterVillage.instance, task, 0L, 1L)

        // 修改 task 使其能够访问 taskId
        val taskWithCancel = object : Runnable {
            override fun run() {
                task.run()
                if (!worlds.hasNext() && (chunksInWorld == null || !chunksInWorld.hasNext())) {
                    Bukkit.getScheduler().cancelTask(taskId)
                }
            }
        }
        // 重新调度使用带取消功能的任务
        Bukkit.getScheduler().cancelTask(taskId)
        Bukkit.getScheduler().scheduleSyncRepeatingTask(BetterVillage.instance, taskWithCancel, 0L, 1L)
    }

    /**
     * 清理过期的缓存条目
     */
    fun cleanupCache() {
        val now = System.currentTimeMillis()
        val expiredKeys = checkedStructures.filter { (key, timestamp) ->
            now - timestamp > CACHE_EXPIRY_MS
        }.keys

        expiredKeys.forEach { key ->
            checkedStructures.remove(key)
        }
    }
}