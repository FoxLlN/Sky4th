
package sky4th.dungeon.monster.spawn

import org.bukkit.Bukkit
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityTargetEvent
import org.bukkit.event.entity.EntityTeleportEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.event.world.WorldUnloadEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import sky4th.dungeon.monster.core.MonsterDataKeys
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 怪物仇恨管理器（事件驱动版本）
 * 
 * 功能：
 * 1. 监听怪物目标变化事件，动态调整怪物速度
 * 2. 怪物生成时速度为0，等待仇恨触发
 * 3. 获得仇恨后恢复正常速度，追击玩家
 * 4. 失去仇恨后保持正常速度，允许自然游荡
 * 5. 自动清理无效缓存和边界情况
 */
class MonsterAggroManager(
    private val plugin: JavaPlugin,
    private val downedPlayerManager: sky4th.dungeon.player.DownedPlayerManager
) : Listener {

    companion object {
        // 怪物默认速度映射
        private val DEFAULT_SPEEDS = mapOf(
            "zombie" to 0.23,
            "husk" to 0.23,
            "skeleton" to 0.25,
            "stray" to 0.25,
            "pillager" to 0.35,
            "vindicator" to 0.30,
            "ravager" to 0.30,
            "witch" to 0.25,
            "spider" to 0.30,
            "cave_spider" to 0.30
        )

        /**
         * 获取怪物类型的默认速度
         */
        private fun getDefaultSpeed(entityType: String): Double {
            return DEFAULT_SPEEDS[entityType.lowercase()] ?: 0.25
        }
    }

    // 怪物状态缓存（用于快速查询和清理）
    private val monsterStates: ConcurrentHashMap<UUID, MonsterState> = ConcurrentHashMap()

    // 实例到怪物集合的映射（用于实例卸载时清理）
    private val instanceMonsters: ConcurrentHashMap<String, MutableSet<UUID>> = ConcurrentHashMap()

    // 需要清理的怪物集合（延迟清理）
    private val pendingCleanup: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    /**
     * 怪物状态数据类
     */
    private data class MonsterState(
        val uuid: UUID,
        val instanceId: String,
        val spawnPoint: String,
        var hasAggro: Boolean,
        val entityType: String,
        val worldName: String,
        val chunkX: Int,
        val chunkZ: Int
    )

    /**
     * 初始化管理器
     */
    fun init() {
        Bukkit.getPluginManager().registerEvents(this, plugin)

        // 启动定期清理任务（每5秒清理一次）
        Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            performPeriodicCleanup()
        }, 100L, 100L)
    }

    /**
     * 关闭管理器
     */
    fun shutdown() {
        // 清理所有缓存
        monsterStates.clear()
        instanceMonsters.clear()
        pendingCleanup.clear()
    }

    /**
     * 清理指定实例的所有怪物状态
     */
    fun clearForInstance(instanceFullId: String) {
        val monsterIds = instanceMonsters.remove(instanceFullId) ?: return

        monsterIds.forEach { uuid ->
            monsterStates.remove(uuid)
        }
    }

    /**
     * 监听怪物目标变化事件
     */
    @EventHandler()
    fun onEntityTarget(event: EntityTargetEvent) {
        val entity = event.entity

        // 只处理LivingEntity类型的实体
        if (entity !is LivingEntity) return

        val uuid = entity.uniqueId
        val state = monsterStates[uuid] ?: run {
            recoverMonsterState(entity) ?: return
        }

        // 检查实体是否仍然有效
        if (!entity.isValid) {
            scheduleCleanup(uuid)
            return
        }

        val hasAggro = event.target != null

        // 如果目标不是玩家，则取消目标
        if (hasAggro && event.target !is Player) {
            event.isCancelled = true
            if (entity is Mob) {
                entity.target = null
            }
            return
        }

        // 如果目标是倒地玩家，则取消目标
        if (hasAggro && event.target is Player) {
            val player = event.target as Player
            if (isPlayerDowned(player)) {
                event.isCancelled = true
                if (entity is Mob) {
                    entity.target = null
                }
                return
            }
        }

        // 更新怪物速度和目标
        if (hasAggro) {
            // 获得仇恨，恢复正常速度
            restoreNormalSpeed(entity)

            // 如果实体是Mob类型，设置目标
            if (entity is Mob && event.target is LivingEntity) {
                entity.target = event.target as LivingEntity
            }

            // 更新状态
            state.hasAggro = hasAggro
            monsterStates[uuid] = state
        } else {
            // 失去仇恨，保持正常速度，允许游荡
            restoreNormalSpeed(entity)

            // 更新状态
            state.hasAggro = hasAggro
            monsterStates[uuid] = state
        }
    }

    /**
     * 检查玩家是否倒地
     */
    private fun isPlayerDowned(player: Player): Boolean {
        return downedPlayerManager.isPlayerDowned(player.uniqueId)
    }

    /**
     * 监听怪物死亡事件
     */
    @EventHandler()
    fun onEntityDeath(event: EntityDeathEvent) {
        val entity = event.entity

        val uuid = entity.uniqueId
        scheduleCleanup(uuid)
    }

    /**
     * 监听怪物传送事件（防止怪物被传送出地牢）
     */
    @EventHandler()
    fun onEntityTeleport(event: EntityTeleportEvent) {
        val entity = event.entity
        
        // 只处理LivingEntity类型的实体
        if (entity !is LivingEntity) return

        val from = event.from
        val to = event.to

        // 如果怪物被传送到其他世界，清理状态
        if (from.world != to?.world) {
            scheduleCleanup(entity.uniqueId)
        }
    }

    /**
     * 监听区块卸载事件
     */
    @EventHandler()
    fun onChunkUnload(event: ChunkUnloadEvent) {
        val chunk = event.chunk
        val worldName = chunk.world.name

        // 查找该区块中的所有怪物
        monsterStates.values.filter { state ->
            state.worldName == worldName &&
            state.chunkX == chunk.x &&
            state.chunkZ == chunk.z
        }.forEach { state ->
            // 标记为需要清理
            scheduleCleanup(state.uuid)
        }
    }

    /**
     * 监听世界卸载事件
     */
    @EventHandler()
    fun onWorldUnload(event: WorldUnloadEvent) {
        val worldName = event.world.name

        // 查找该世界中的所有实例
        val instancesToRemove = instanceMonsters.keys.filter { instanceId ->
            instanceId.startsWith(worldName)
        }

        // 清理这些实例的所有怪物
        instancesToRemove.forEach { instanceId ->
            clearForInstance(instanceId)
        }
    }

    /**
     * 恢复怪物的正常速度
     */
    private fun restoreNormalSpeed(entity: LivingEntity) {
        val entityType = entity.type.name.lowercase()
        val defaultSpeed = getDefaultSpeed(entityType)

        // 获取移动速度属性
        val speedAttribute = entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)
        if (speedAttribute != null) {
            // 设置基础速度值
            speedAttribute.baseValue = defaultSpeed

            // 确保实体是Mob类型且AI已启用
            if (entity is org.bukkit.entity.Mob) {
                entity.setAI(true)

                // 如果有目标，确保目标被设置
                if (entity.target != null) {
                    // 目标已经存在，不需要额外设置
                }
            }
        }
    }

    /**
     * 从实体数据恢复怪物状态
     */
    private fun recoverMonsterState(entity: LivingEntity): MonsterState? {
        val pdc = entity.persistentDataContainer

        val spawnPoint = pdc.get(MonsterDataKeys.SPAWN_POINT_KEY, PersistentDataType.STRING) ?: return null
        val instanceId = pdc.get(MonsterDataKeys.INSTANCE_ID_KEY, PersistentDataType.STRING) ?: return null
        val hasAggro = pdc.get(MonsterDataKeys.HAS_AGGRO_KEY, PersistentDataType.BYTE) == 1.toByte()

        val uuid = entity.uniqueId
        val location = entity.location
        val worldName = location.world?.name ?: return null

        // 创建状态对象
        val state = MonsterState(
            uuid = uuid,
            instanceId = instanceId,
            spawnPoint = spawnPoint,
            hasAggro = hasAggro,
            entityType = entity.type.name,
            worldName = worldName,
            chunkX = location.blockX shr 4,
            chunkZ = location.blockZ shr 4
        )

        // 添加到缓存
        monsterStates[uuid] = state

        // 添加到实例索引
        instanceMonsters.getOrPut(instanceId) { ConcurrentHashMap.newKeySet() }.add(uuid)

        return state
    }

    /**
     * 调度清理任务
     */
    private fun scheduleCleanup(uuid: UUID) {
        pendingCleanup.add(uuid)
    }

    /**
     * 执行定期清理
     */
    private fun performPeriodicCleanup() {
        if (pendingCleanup.isEmpty()) return

        // 创建副本以避免并发修改
        val toCleanup = pendingCleanup.toList()
        pendingCleanup.clear()

        toCleanup.forEach { uuid ->
            val state = monsterStates.remove(uuid) ?: return@forEach

            // 从实例索引中移除
            instanceMonsters[state.instanceId]?.remove(uuid)

            // 如果实例没有怪物了，清理实例条目
            if (instanceMonsters[state.instanceId]?.isEmpty() == true) {
                instanceMonsters.remove(state.instanceId)
            }
        }
    }

    /**
     * 获取当前缓存的怪物数量
     */
    fun getCachedMonsterCount(): Int = monsterStates.size

    /**
     * 获取当前待清理的怪物数量
     */
    fun getPendingCleanupCount(): Int = pendingCleanup.size
}
