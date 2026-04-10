
package sky4th.dungeon.model

import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.plugin.java.JavaPlugin
import sky4th.dungeon.config.ConfigManager
import sky4th.dungeon.world.TemplateWorldManager
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 地牢实例管理器
 * 负责管理所有地牢配置和实例
 *
 * @property plugin 插件实例
 * @property configManager 配置管理器
 * @property templateWorldManager 模板世界管理器
 * @property playerManager 玩家管理器
 */
class DungeonInstanceManager(
    private val plugin: JavaPlugin,
    private val configManager: ConfigManager,
    private val templateWorldManager: TemplateWorldManager,
    private var playerManager: sky4th.dungeon.player.PlayerManager?
) {

    /**
     * 设置玩家管理器
     * @param manager 玩家管理器实例
     */
    fun setPlayerManager(manager: sky4th.dungeon.player.PlayerManager) {
        this.playerManager = manager
    }
    // 地牢配置映射（地牢ID -> 配置）
    private val dungeonConfigs: ConcurrentHashMap<String, DungeonConfig> = ConcurrentHashMap()

    // 地牢实例映射（完整实例ID -> 实例）
    private val dungeonInstances: ConcurrentHashMap<String, DungeonInstance> = ConcurrentHashMap()

    // 地牢实例计数器（地牢ID -> 下一个实例ID）
    private val instanceCounters: ConcurrentHashMap<String, AtomicInteger> = ConcurrentHashMap()

    // 地牢倒计时任务（按实例分组）
    private val dungeonCountdownTasks: ConcurrentHashMap<String, org.bukkit.scheduler.BukkitTask> = ConcurrentHashMap()

    // 定期检查任务
    private var checkTask: org.bukkit.scheduler.BukkitTask? = null

    /**
     * 初始化地牢配置
     * 从ConfigManager加载所有地牢配置
     */
    fun initialize() {
        val configs = configManager.loadDungeonConfigs()
        dungeonConfigs.clear()
        configs.forEach { (id, config) ->
            dungeonConfigs[id] = config
            instanceCounters[id] = AtomicInteger(1)
        }
    }

    /**
     * 获取所有地牢配置
     * @return 地牢配置映射
     */
    fun getAllDungeonConfigs(): Map<String, DungeonConfig> = dungeonConfigs.toMap()

    /**
     * 获取地牢配置
     * @param id 地牢ID
     * @return 地牢配置，不存在则返回null
     */
    fun getDungeonConfig(id: String): DungeonConfig? = dungeonConfigs[id]

    /**
     * 检查地牢配置是否存在
     * @param id 地牢ID
     * @return 配置是否存在
     */
    fun hasDungeonConfig(id: String): Boolean = dungeonConfigs.containsKey(id)

    /**
     * 创建新的地牢实例（异步）
     * @param dungeonId 地牢ID
     * @param callback 完成回调，参数为创建的实例，失败返回null
     */
    fun createDungeonInstance(dungeonId: String, callback: (DungeonInstance?) -> Unit) {
        val config = dungeonConfigs[dungeonId] ?: run {
            plugin.logger.warning("尝试创建不存在的地牢实例: $dungeonId")
            callback(null)
            return
        }

        // 检查是否已达到最大实例数
        val currentInstances = dungeonInstances.values.count { it.config.id == dungeonId }
        if (currentInstances >= config.maxInstances) {
            plugin.logger.warning("地牢 $dungeonId 已达到最大实例数 ${config.maxInstances}")
            callback(null)
            return
        }

        // 生成新的实例ID（只保留个位数，循环使用1-9）
        val counter = instanceCounters[dungeonId]?.getAndIncrement() ?: 0
        val instanceId = ((counter - 1) % 9 + 1).toString()
        val worldName = config.getInstanceWorldName(instanceId)

        // 创建实例世界
        templateWorldManager.createInstanceWorld(config, instanceId) { world ->
            if (world == null) {
                plugin.logger.severe("创建地牢实例世界失败: $worldName")
                callback(null)
                return@createInstanceWorld
            }

            // 创建实例
            val instance = DungeonInstance(instanceId, config, world)
            dungeonInstances[instance.getFullId()] = instance

            // 启动倒计时任务
            ensureDungeonCountdownTask(instance.getFullId())

            callback(instance)
        }
    }

    /**
     * 获取可用的地牢实例（异步）
     * 优先返回玩家数量未满的现有实例，如果没有则创建新实例
     * @param dungeonId 地牢ID
     * @param callback 完成回调，参数为可用实例，失败返回null
     */
    fun getAvailableInstance(dungeonId: String, callback: (DungeonInstance?) -> Unit) {
        // 查找未满且未关闭的现有实例
        val availableInstance = dungeonInstances.values
            .filter { it.config.id == dungeonId && !it.isFull() && !it.isClosed() }
            .minByOrNull { it.getPlayerCount() }

        if (availableInstance != null) {
            callback(availableInstance)
            return
        }

        // 没有可用实例，尝试创建新实例
        createDungeonInstance(dungeonId, callback)
    }

    /**
     * 获取地牢实例
     * @param fullId 完整实例ID（格式：{dungeonId}_{instanceId}）
     * @return 地牢实例，不存在返回null
     */
    fun getInstance(fullId: String): DungeonInstance? = dungeonInstances[fullId]

    /**
     * 通过世界对象获取地牢实例
     * @param world Bukkit世界对象
     * @return 地牢实例，不存在返回null
     */
    fun getInstanceByWorld(world: World): DungeonInstance? {
        return dungeonInstances.values.find { it.world == world }
    }

    /**
     * 重置地牢实例
     * @param fullId 完整实例ID
     * @return 是否成功重置
     */
    fun resetInstance(fullId: String): Boolean {
        val instance = dungeonInstances[fullId] ?: return false

        // 将所有玩家传送出地牢
        instance.getPlayers().forEach { uuid ->
            val player = Bukkit.getPlayer(uuid)
            if (player != null && playerManager != null) {
                playerManager!!.teleportFromDungeon(player, success = false)
            }
        }

        // 等待所有玩家传送完成
        var attempts = 0
        while (!instance.isEmpty() && attempts < 20) {
            Thread.sleep(50)
            attempts++
        }

        // 不清除玩家进入记录，防止玩家重复进入同一个实例
        // 只有在实例销毁时才会清除所有玩家在该实例的进入记录

        // 清理实例数据
        instance.clearAllInstanceData()

        // 重置世界
        val success = templateWorldManager.resetInstanceWorld(instance)

        return success
    }

    /**
     * 重置实例世界（异步调用）
     * @param instance 地牢实例
     * @return 是否成功重置
     */
    fun resetInstanceWorld(instance: DungeonInstance): Boolean {
        return templateWorldManager.resetInstanceWorld(instance)
    }

    /**
     * 销毁地牢实例
     * @param fullId 实例完整ID
     * @return 是否成功销毁
     */
    fun destroyInstance(fullId: String): Boolean {
        val instance = dungeonInstances.remove(fullId) ?: return false

        // 停止该实例的倒计时任务
        dungeonCountdownTasks.remove(fullId)?.cancel()

        // 将所有玩家传送出地牢
        instance.getPlayers().forEach { uuid ->
            val player = Bukkit.getPlayer(uuid)
            if (player != null && playerManager != null) {
                playerManager!!.teleportFromDungeon(player, success = false)
            }
        }

        // 等待所有玩家传送完成
        var attempts = 0
        while (!instance.isEmpty() && attempts < 20) {
            Thread.sleep(50)
            attempts++
        }

        // 清理实例数据
        instance.clearAllInstanceData()

        // 获取上下文
        val context = sky4th.dungeon.command.DungeonContext.get() ?: run {
            // 如果上下文不存在，至少清理队伍数据
            val teamManager = sky4th.dungeon.command.DungeonContext.get()?.teamManager
            teamManager?.clearInstanceData(fullId)
            templateWorldManager.unloadInstanceWorld(instance)
            return false
        }

        // 清理队伍数据
        context.teamManager?.clearInstanceData(fullId)

        // 清理死亡UI数据
        context.playerDeathUI.clearForInstance(fullId)

        // 清理怪物搜索数据
        context.monsterSearchManager.clearForInstance(fullId)

        // 清理容器搜索数据
        context.containerSearchManager.clearForInstance(fullId)

        // 清理怪物仇恨数据
        context.dynamicMonsterManager?.getMonsterAggroManager()?.clearForInstance(fullId)

        // 清理玩家进入记录
        context.playerManager.clearPlayerEntryForInstance(fullId)

        // 清理倒地玩家数据
        context.downedPlayerManager?.clearForInstance(fullId)

        // 清理死亡玩家数据
        context.playerDeathListener.clearForInstance(fullId)

        // 清理侧边栏数据
        context.sidebarManager.clearForInstance(fullId)

        // 清理数据存储
        context.dataStorage.clearForInstance(fullId)

        // 卸载世界
        val success = templateWorldManager.unloadInstanceWorld(instance)

        return success
    }

    /**
     * 确保地牢倒计时任务已启动（如果未启动则启动）
     * @param instanceFullId 实例完整ID
     */
    fun ensureDungeonCountdownTask(instanceFullId: String) {
        // 如果已经存在该实例的倒计时任务，直接返回
        if (dungeonCountdownTasks.containsKey(instanceFullId)) {
            return
        }
        startDungeonCountdownTask(instanceFullId)
    }

    /**
     * 启动地牢倒计时任务
     * @param instanceFullId 实例完整ID
     */
    private fun startDungeonCountdownTask(instanceFullId: String) {
        // 如果已经存在该实例的倒计时任务，先停止
        stopDungeonCountdownTask(instanceFullId)

        // 在异步线程中执行倒计时检查，避免阻塞主线程
        dungeonCountdownTasks[instanceFullId] = plugin.server.scheduler.runTaskTimerAsynchronously(plugin, Runnable {
            val instance = dungeonInstances[instanceFullId] ?: run {
                stopDungeonCountdownTask(instanceFullId)
                return@Runnable
            }

            // 检查实例是否已关闭
            if (instance.isClosed()) {
                stopDungeonCountdownTask(instanceFullId)
                return@Runnable
            }

            // 检查剩余时间是否到0秒
            val remainingSeconds = instance.getRemainingSeconds()
            if (remainingSeconds == 0) {
                // 先标记实例为已关闭，防止新玩家进入
                instance.close()

                // 停止倒计时任务
                stopDungeonCountdownTask(instanceFullId)

                // 保存地牢ID，用于创建新实例
                val dungeonId = instance.config.id

                // 在主线程杀死所有玩家，通过死亡事件自动传送
                plugin.server.scheduler.runTask(plugin, Runnable {
                    instance.getPlayers().forEach { uuid ->
                        val player = Bukkit.getPlayer(uuid)
                        if (player != null && player.isOnline) {
                            // 使用forceKillAndKick方法强制杀死玩家，跳过倒地逻辑
                            val downedPlayerManager = sky4th.dungeon.command.DungeonContext.get()?.downedPlayerManager
                            if (downedPlayerManager != null) {
                                downedPlayerManager.forceKillAndKick(player)
                            }
                        }
                    }

                    // 延迟一段时间后销毁旧实例并创建新实例，确保所有玩家都已死亡并传送
                    plugin.server.scheduler.runTaskLater(plugin, Runnable {
                        // 销毁旧实例（会自动卸载世界）
                        destroyInstance(instanceFullId)

                        // 创建新的实例
                        createDungeonInstance(dungeonId) { newInstance ->
                            if (newInstance != null) {
                                // 新实例创建成功，启动倒计时任务
                                ensureDungeonCountdownTask(newInstance.getFullId())
                            }
                        }
                    }, 60L) // 延迟3秒
                })
                return@Runnable
            }

            // 获取实例中的所有玩家
            val players = playerManager?.getPlayersInInstance(instance) ?: return@Runnable

            // 在主线程更新所有玩家的地牢倒计时
            plugin.server.scheduler.runTask(plugin, Runnable {
                val sidebarManager = sky4th.dungeon.command.DungeonContext.get()?.sidebarManager
                players.forEach { player ->
                    if (player.isOnline) {
                        sidebarManager?.updateSection(player, sky4th.dungeon.player.SidebarManager.Section.DUNGEON_TIME)
                    }
                }
            })
        }, 20L, 20L) // 每秒更新一次
    }

    /**
     * 停止地牢倒计时任务
     * @param instanceFullId 实例完整ID
     */
    private fun stopDungeonCountdownTask(instanceFullId: String) {
        dungeonCountdownTasks.remove(instanceFullId)?.cancel()
    }

    /**
     * 获取地牢的所有实例
     * @param dungeonId 地牢ID
     * @return 实例列表
     */
    fun getDungeonInstances(dungeonId: String): List<DungeonInstance> {
        return dungeonInstances.values.filter { it.config.id == dungeonId }
    }

    /**
     * 获取所有实例
     * @return 所有实例列表
     */
    fun getAllInstances(): List<DungeonInstance> = dungeonInstances.values.toList()

    /**
     * 清理空闲实例
     * @param idleThresholdMs 空闲阈值（毫秒）
     * @return 清理的实例数量
     */
    fun cleanupIdleInstances(idleThresholdMs: Long): Int {
        val now = System.currentTimeMillis()
        var cleaned = 0

        dungeonInstances.values
            .filter { it.isEmpty() && (now - it.createdAt) > idleThresholdMs }
            .forEach { instance ->
                if (destroyInstance(instance.getFullId())) {
                    cleaned++
                }
            }

        return cleaned
    }

    /**
     * 关闭插件时清理
     */
    fun shutdown() {
        // 停止所有倒计时任务
        dungeonCountdownTasks.values.forEach { it.cancel() }
        dungeonCountdownTasks.clear()

        // 传送所有玩家出地牢
        dungeonInstances.values.forEach { instance ->
            if (!instance.isEmpty()) {
                instance.getPlayers().forEach { uuid ->
                    val player = Bukkit.getPlayer(uuid)
                    if (player != null && playerManager != null) {
                        playerManager!!.teleportFromDungeon(player, success = false)
                    }
                }
            }
        }

        // 卸载所有世界
        dungeonInstances.values.forEach { instance ->
            // 清理实例数据
            instance.clearAllInstanceData()
            templateWorldManager.unloadInstanceWorld(instance)
        }

        // 清空实例
        dungeonInstances.clear()
    }
}
