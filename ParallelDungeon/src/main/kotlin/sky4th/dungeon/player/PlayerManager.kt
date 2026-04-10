
package sky4th.dungeon.player

import sky4th.dungeon.Dungeon
import sky4th.dungeon.config.ConfigManager
import sky4th.dungeon.spawn.SpawnManager
import sky4th.dungeon.util.LanguageUtil.sendLang
import sky4th.dungeon.command.DungeonContext
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

class PlayerManager(
    private val plugin: JavaPlugin,
    private val configManager: ConfigManager,
    private val spawnManager: SpawnManager,
    private val backpackManager: BackpackManager,
    private val techLevelBonusHandler: TechLevelBonusHandler,
    private val dungeonInstanceManager: sky4th.dungeon.model.DungeonInstanceManager,
    private val dataStorage: DungeonDataStorage,
    private val teamManager: sky4th.dungeon.team.TeamManager
) {

    // 玩家到地牢实例的映射
    private val playerInstances: MutableMap<UUID, sky4th.dungeon.model.DungeonInstance> = mutableMapOf()

    /**
     * 获取队伍管理器
     */
    fun getTeamManager(): sky4th.dungeon.team.TeamManager = teamManager
    private val playerOriginalLocations: MutableMap<UUID, Location> = mutableMapOf()
    private val playerOriginalGameModes: MutableMap<UUID, GameMode> = mutableMapOf()
    // 玩家实例进入记录（UUID -> 实例完整ID -> 进入时间戳）
    private val playerInstanceEntries: MutableMap<UUID, MutableMap<String, Long>> = mutableMapOf()
    private val dungeonExitService: DungeonExitService

    init {
        dungeonExitService = DungeonExitService(plugin, configManager, backpackManager)
    }

    /**
     * 获取玩家当前所在的地牢实例
     * @param player 玩家
     * @return 地牢实例，不在地牢中返回null
     */
    fun getPlayerInstance(player: Player): sky4th.dungeon.model.DungeonInstance? {
        return playerInstances[player.uniqueId]
    }

    /**
     * 获取玩家当前所在的地牢ID
     * @param player 玩家
     * @return 地牢ID，不在地牢中返回null
     */
    fun getCurrentDungeonId(player: Player): String? {
        return playerInstances[player.uniqueId]?.config?.id
    }

    /**
     * 获取玩家当前所在的实例完整ID
     * @param player 玩家
     * @return 实例完整ID（格式：{dungeonId}_{instanceId}），不在地牢中返回null
     */
    fun getCurrentInstanceFullId(player: Player): String? {
        return playerInstances[player.uniqueId]?.getFullId()
    }

    /**
     * 将玩家传送到指定地牢实例
     * @param player 玩家
     * @param dungeonId 地牢ID
     * @param instanceId 实例ID
     * @return 是否成功传送
     */
    fun teleportToDungeon(player: Player, dungeonId: String, instanceId: String): Boolean {
        val fullInstanceId = "${dungeonId}_$instanceId"
        // 获取要进入的实例
        val instance = dungeonInstanceManager.getInstance(fullInstanceId) ?: run {
            player.sendLang(plugin, "command.enter.instance-not-found", "instanceId" to fullInstanceId)
            return false
        }

        // 检查实例是否已关闭
        if (instance.isClosed()) {
            player.sendLang(plugin, "dungeon.instance-closed")
            return false
        }

        // 保存玩家原始位置和游戏模式
        playerOriginalLocations[player.uniqueId] = player.location.clone()
        playerOriginalGameModes[player.uniqueId] = player.gameMode

        // 先清空玩家背包
        backpackManager.clearPlayerInventory(player)

        // 传送到随机出生点
        val success = spawnManager.teleportToRandomSpawn(player, instance.world, dungeonId)
        if (success) {
            // 切换为冒险模式
            player.gameMode = GameMode.ADVENTURE
            player.foodLevel = 20
            player.saturation = 0.0f
            player.exhaustion = 0.0f
            player.fireTicks = 0
            player.fallDistance = 0.0f
            player.level = 0
            player.exp = 0.0f
            player.clearActivePotionEffects()

            // 延迟1tick后设置玩家状态，确保获取到正确的最大生命值
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                val maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH)?.value ?: 20.0
                player.health = maxHealth
            }, 2L)

            // 添加玩家到实例
            instance.addPlayer(player)
            playerInstances[player.uniqueId] = instance
            backpackManager.onEnterDungeon(player)
            techLevelBonusHandler.onEnterDungeon(player)
            
            // 重置玩家状态（包括死亡状态）
            teamManager.resetPlayerState(player.uniqueId)

            // 自动创建单人队伍
            teamManager.createTeam(player, instance)

            // 记录玩家进入实例
            recordPlayerEnterInstance(player.uniqueId, fullInstanceId)

            // 保存玩家当前实例信息
            dataStorage.setPlayerCurrentInstance(player.uniqueId, instance)

            // 显示侧边栏
            DungeonContext.get()?.sidebarManager?.showSidebar(player)

            // 显示中文名称提示
            val displayName = instance.config.displayName
            player.sendLang(plugin, "dungeon.entered-with-name", "name" to displayName)
        }
        return success
    }



    /**
     * 将玩家传送出副本
     * @param player 玩家
     * @param success 是否成功撤离（true=成功，false=失败）
     * @return 是否成功传送
     */
    fun teleportFromDungeon(player: Player, success: Boolean = true): Boolean {
        val instance = playerInstances.remove(player.uniqueId) ?: return false

        // 隐藏侧边栏
        DungeonContext.get()?.sidebarManager?.hideSidebar(player)

        // 从实例中移除玩家
        instance.removePlayer(player)

        // 恢复原始游戏模式
        val originalGameMode = playerOriginalGameModes[player.uniqueId] ?: GameMode.SURVIVAL
        player.gameMode = originalGameMode
        playerOriginalGameModes.remove(player.uniqueId)

        // 获取原始位置
        val originalLocation = playerOriginalLocations[player.uniqueId]
        if (originalLocation != null) {
            player.teleport(originalLocation)
            playerOriginalLocations.remove(player.uniqueId)
        } else {
            // 如果没有原始位置，传送到主世界出生点（任一非副本世界）
            val mainWorld = plugin.server.worlds.firstOrNull { 
                !dungeonInstanceManager.getAllInstances().any { inst -> inst.world.name == it.name }
            }
            if (mainWorld != null) {
                player.teleport(mainWorld.spawnLocation)
            }
        }

        // 根据成功状态执行不同的背包处理逻辑
        if (success) {
            // 成功：正常转移所有背包物品
            dungeonExitService.handlePlayerExit(player)
        } else {
            // 失败：只保留背包中安全箱的东西和装备到仓库中，装备每部分扣除25%的耐久度
            dungeonExitService.handlePlayerExitWithPenalty(player)
        }

        techLevelBonusHandler.onLeaveDungeon(player)
        backpackManager.onLeaveDungeon(player)
        spawnManager.clearPlayerSpawn(player)

        // 清除玩家实例记录
        dataStorage.clearPlayerCurrentInstance(player.uniqueId)

        // 如果是死亡退出，标记玩家为死亡状态
        if (!success) {
            val playerState = teamManager.getPlayerState(player.uniqueId)
            if (playerState != null) {
                playerState.isDead = true
            }
        }

        // 显示中文名称提示
        val displayName = instance.config.displayName
        if (success) {
            player.sendLang(plugin, "dungeon.left-with-name", "name" to displayName)
        }

        return true
    }

    /**
     * 检查玩家是否在副本中
     */
    fun isPlayerInDungeon(player: Player): Boolean {
        return playerInstances.containsKey(player.uniqueId)
    }

    /**
     * 获取所有在副本中的玩家
     */
    fun getPlayersInDungeon(): Set<UUID> {
        return playerInstances.keys.toSet()
    }

    /**
     * 清除所有玩家数据
     */
    fun clearAllPlayers() {
        // 创建副本以避免并发修改异常
        val playersToTeleport = playerInstances.keys.toList()
        playersToTeleport.forEach { uuid ->
            val player = plugin.server.getPlayer(uuid)
            if (player != null) {
                try {
                    // 先清空背包，确保即使后续操作失败也不会带出物品
                    backpackManager.clearPlayerInventory(player)
                    // 再传送玩家
                    teleportFromDungeon(player, false)
                } catch (e: Exception) {
                    plugin.logger.severe("清理玩家 ${player.name} 时出错: ${e.message}")
                    e.printStackTrace()
                    // 确保背包被清空
                    backpackManager.clearPlayerInventory(player)
                }
            }
        }
        playerInstances.clear()
        playerOriginalLocations.clear()
        playerOriginalGameModes.clear()
    }

    /**
     * 清除所有死亡数据
     * 用于地牢重置时清理内存中的死亡相关数据
     */
    fun clearAllDeathData() {
        playerInstances.clear()
        playerOriginalLocations.clear()
        playerOriginalGameModes.clear()
        backpackManager.clearAllCash()
    }

    /**
     * 获取指定地牢实例中的所有玩家
     * @param instance 地牢实例
     * @return 玩家列表
     */
    fun getPlayersInInstance(instance: sky4th.dungeon.model.DungeonInstance): List<Player> {
        return instance.getPlayers().mapNotNull { plugin.server.getPlayer(it) }
    }

    /**
     * 检查玩家是否已经进入过指定实例
     * @param playerUuid 玩家UUID
     * @param fullInstanceId 实例完整ID
     * @return 是否已经进入过
     */
    fun hasPlayerEnteredInstance(playerUuid: UUID, fullInstanceId: String): Boolean {
        val entries = playerInstanceEntries[playerUuid] ?: return false
        return entries.containsKey(fullInstanceId)
    }

    /**
     * 记录玩家进入实例
     * @param playerUuid 玩家UUID
     * @param fullInstanceId 实例完整ID
     */
    private fun recordPlayerEnterInstance(playerUuid: UUID, fullInstanceId: String) {
        val entries = playerInstanceEntries.getOrPut(playerUuid) { mutableMapOf() }
        entries[fullInstanceId] = System.currentTimeMillis()
    }

    /**
     * 清除所有玩家在指定实例的进入记录（实例销毁时调用）
     * @param fullInstanceId 实例完整ID
     */
    fun clearPlayerEntryForInstance(fullInstanceId: String) {
        playerInstanceEntries.values.forEach { entries ->
            entries.remove(fullInstanceId)
        }
    }

}
