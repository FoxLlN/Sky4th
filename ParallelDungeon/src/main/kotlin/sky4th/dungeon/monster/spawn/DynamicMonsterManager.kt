package sky4th.dungeon.monster.spawn

import sky4th.dungeon.monster.core.MonsterDataKeys
import sky4th.dungeon.monster.core.MonsterMetadata
import sky4th.dungeon.monster.core.MonsterRegistry
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.*

/**
 * 动态怪物管理器
 * 根据玩家位置动态生成怪物，避免怪物因玩家不在附近而消失
 */
class DynamicMonsterManager(
    private val plugin: JavaPlugin,
    private val configManager: sky4th.dungeon.config.ConfigManager,
    private val playerManager: sky4th.dungeon.player.PlayerManager,
    private val downedPlayerManager: sky4th.dungeon.player.DownedPlayerManager
) {
    // 仇恨管理器（事件驱动）
    private val aggroManager: MonsterAggroManager = MonsterAggroManager(plugin, downedPlayerManager)
    
    // 初始化标志
    private var initialized = false
    
    // 怪物死亡事件监听器
    private val entityDeathListener = object : org.bukkit.event.Listener {
        @org.bukkit.event.EventHandler
        fun onEntityDeath(event: org.bukkit.event.entity.EntityDeathEvent) {
            val entity = event.entity
            // 检查是否是自定义怪物
            if (MonsterMetadata.getMonsterId(entity) != null) {
                // 检查是否有生成点信息
                if (entity.persistentDataContainer.has(MonsterDataKeys.SPAWN_POINT_KEY, org.bukkit.persistence.PersistentDataType.STRING)) {
                    val spawnPointStr = entity.persistentDataContainer.get(MonsterDataKeys.SPAWN_POINT_KEY, org.bukkit.persistence.PersistentDataType.STRING)
                    if (spawnPointStr != null) {
                        // 查找对应的生成点
                        val spawnPointLocation = parseLocation(spawnPointStr, entity.world)
                        // 获取实例ID
                        val instanceFullId = entity.persistentDataContainer.get(MonsterDataKeys.INSTANCE_ID_KEY, org.bukkit.persistence.PersistentDataType.STRING) ?: return
                        val instanceSpawnPoints = spawnPoints[instanceFullId] ?: return

                        instanceSpawnPoints.values.forEach { state ->
                            val stateLocation = state.config.toLocation(entity.world)
                            if (stateLocation.distance(spawnPointLocation) < 1.0) {
                                // 检查是否是被玩家击杀
                                if (entity is org.bukkit.entity.Mob && entity.killer is Player) {
                                    // 标记为被玩家击杀
                                    state.killedByPlayer = true
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // 记录每个怪物生成点的状态
    private data class SpawnPointState(
        val config: sky4th.dungeon.config.MonsterSpawnConfig,
        var spawnedCount: Int = 0,
        var lastSpawnTime: Long = 0L,
        var hasSpawned: Boolean = false,  // 标记是否已经生成过怪物
        var killedByPlayer: Boolean = false  // 标记怪物是否被玩家击杀
    )

    // ========== 复合键辅助方法 ==========
    /**
     * 创建复合键：instanceFullId_spawnPointId
     */
    private fun makeFullKey(instanceFullId: String, spawnPointId: String): String {
        return "${instanceFullId}_${spawnPointId}"
    }
    
    /**
     * 从复合键中提取实例ID
     */
    private fun extractInstanceId(fullKey: String): String {
        return fullKey.substringBeforeLast("_")
    }
    
    /**
     * 从复合键中提取生成点ID
     */
    private fun extractSpawnPointId(fullKey: String): String {
        return fullKey.substringAfterLast("_")
    }

    // ========== 数据存储（使用复合键优化） ==========
    // 怪物生成点状态（按实例隔离：instanceFullId -> spawnPointId -> state）
    private val spawnPoints: MutableMap<String, MutableMap<String, SpawnPointState>> = mutableMapOf()
    // 每个实例的检查任务
    private val checkTasks: MutableMap<String, BukkitTask> = mutableMapOf()
    // 每个实例的运行状态
    private val isRunning: MutableMap<String, Boolean> = mutableMapOf()

    // 玩家附近的怪物生成距离（方块）
    private val SPAWN_DISTANCE = 30

    // 检查间隔（tick）
    private val CHECK_INTERVAL = 20L // 1秒
    
    // 怪物活动范围限制（方块）
    private val MONSTER_MOVE_RANGE = 16.0

    /**
     * 初始化怪物生成点
     * @param world 世界
     * @param instanceFullId 实例完整ID
     */
    fun initSpawnPoints(world: World, instanceFullId: String) {

        // 初始化该实例的数据结构
        spawnPoints[instanceFullId] = mutableMapOf()
        isRunning[instanceFullId] = false
        
        // 从实例完整ID中提取dungeonId
        val dungeonId = instanceFullId.substringBefore("_")
        // 获取所有地牢配置并找到对应的配置
        val dungeonConfigs = configManager.loadDungeonConfigs()
        val dungeonConfig = dungeonConfigs[dungeonId]
        if (dungeonConfig != null) {
            dungeonConfig.monsterSpawns.forEach { config ->
                val fullKey = makeFullKey(instanceFullId, config.id)
                spawnPoints[instanceFullId]?.set(fullKey, SpawnPointState(config))
            }
        } else {
            plugin.logger.warning("未找到地牢配置: $dungeonId")
        }
    }

    /**
     * 启动动态怪物生成
     * @param world 世界
     * @param instanceFullId 实例完整ID
     */
    fun start(world: World, instanceFullId: String) {
        // 先停止之前的任务（如果存在）
        stop(instanceFullId)

        // 只初始化一次
        if (!initialized) {
            aggroManager.init()
            // 注册怪物死亡事件监听器（只注册一次）
            Bukkit.getPluginManager().registerEvents(entityDeathListener, plugin)
            initialized = true
        }

        isRunning[instanceFullId] = true
        checkTasks[instanceFullId] = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            checkAndSpawnMonsters(world, instanceFullId)
        }, CHECK_INTERVAL, CHECK_INTERVAL)
    }

    /**
     * 停止动态怪物生成
     * @param instanceFullId 实例完整ID
     */
    fun stop(instanceFullId: String) {
        if (isRunning[instanceFullId] != true) return
        
        checkTasks[instanceFullId]?.cancel()
        checkTasks.remove(instanceFullId)
        isRunning[instanceFullId] = false
        
        // 清理该实例的怪物状态
        aggroManager.clearForInstance(instanceFullId)
    }

    /**
     * 获取怪物仇恨管理器
     * @return 怪物仇恨管理器
     */
    fun getMonsterAggroManager(): MonsterAggroManager = aggroManager

    /**
     * 重置所有怪物生成点状态并清除已生成的怪物
     * @param world 世界
     * @param instanceFullId 实例完整ID
     */
    fun reset(world: World, instanceFullId: String) {
        val instanceSpawnPoints = spawnPoints[instanceFullId] ?: return
        
        // 重置所有生成点状态
        instanceSpawnPoints.values.forEach { state ->
            state.spawnedCount = 0
            state.lastSpawnTime = 0L
            state.hasSpawned = false
            state.killedByPlayer = false
        }

        // 清除所有已生成的怪物
        instanceSpawnPoints.values.forEach { state ->
            val location = state.config.toLocation(world)
            clearMonstersAtLocation(world, location)
        }
        
        // 清理怪物状态缓存
        aggroManager.clearForInstance(instanceFullId)
    }

    /**
     * 检查并生成怪物
     * @param world 世界
     * @param instanceFullId 实例完整ID
     */
    private fun checkAndSpawnMonsters(world: World, instanceFullId: String) {
        val players = world.players
        val instanceSpawnPoints = spawnPoints[instanceFullId] ?: return
        
        instanceSpawnPoints.values.forEach { state ->
            val location = state.config.toLocation(world)

            // 检查是否有玩家在附近
            val hasNearbyPlayer = players.any { player ->
                player.location.distance(location) <= SPAWN_DISTANCE
            }

            // 如果已经生成过怪物
            if (state.hasSpawned) {
                if (!hasNearbyPlayer && !state.killedByPlayer) {
                    // 没有玩家在附近且怪物未被击杀，检查是否还有怪物存在
                    val monsterCount = countMonstersAtLocation(world, location)
                    if (monsterCount == 0) {
                        // 没有怪物了，重置生成点状态，允许再次生成
                        state.hasSpawned = false
                        state.spawnedCount = 0
                    }
                }
                return@forEach
            }

            if (!hasNearbyPlayer) {
                // 没有玩家在附近，不生成怪物
                return@forEach
            }

            // 有玩家在附近且未生成过，生成怪物
            var spawnedCount = 0
            repeat(state.config.count) {
                val entity = MonsterRegistry.spawn(state.config.monsterId, location)
                if (entity != null) {
                    state.spawnedCount++
                    state.lastSpawnTime = System.currentTimeMillis()
                    // 限制怪物的活动范围
                    spawnedCount++
                    entity.setAI(true)  // 保持AI，让怪物可以攻击玩家
                    // 初始速度设为0，等待仇恨触发
                    entity.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED)?.baseValue = 0.0
                    // 记录怪物的生成点和原始速度
                    entity.persistentDataContainer.set(
                        MonsterDataKeys.SPAWN_POINT_KEY,
                        org.bukkit.persistence.PersistentDataType.STRING,
                        "${location.x},${location.y},${location.z}"
                    )
                    // 记录实例ID
                    entity.persistentDataContainer.set(
                        MonsterDataKeys.INSTANCE_ID_KEY,
                        org.bukkit.persistence.PersistentDataType.STRING,
                        instanceFullId
                    )
                    // 标记怪物是否有仇恨
                    entity.persistentDataContainer.set(
                        MonsterDataKeys.HAS_AGGRO_KEY,
                        org.bukkit.persistence.PersistentDataType.BYTE,
                        0.toByte()
                    )
                }
            }
            
            // 只有成功生成至少一只怪物时，才标记为已生成
            if (spawnedCount > 0) {
                state.hasSpawned = true
            } 
        }
    }
    
    /**
     * 计算指定位置的怪物数量
     */
    private fun countMonstersAtLocation(world: World, location: Location): Int {
        return world.entities.count { entity ->
            entity is org.bukkit.entity.LivingEntity &&
            entity.location.distance(location) <= 5.0 &&
            MonsterMetadata.getMonsterId(entity) != null
        }
    }

    /**
     * 清除指定位置的怪物
     */
    private fun clearMonstersAtLocation(world: World, location: Location) {
        world.entities.filter { entity ->
            entity is org.bukkit.entity.LivingEntity &&
            entity.location.distance(location) <= 5.0 &&
            MonsterMetadata.getMonsterId(entity) != null
        }.forEach { it.remove() }
    }

    /**
     * 解析位置字符串
     */
    private fun parseLocation(locationStr: String, world: World): Location {
        val parts = locationStr.split(",")
        return Location(world, parts[0].toDouble(), parts[1].toDouble(), parts[2].toDouble())
    }
}
