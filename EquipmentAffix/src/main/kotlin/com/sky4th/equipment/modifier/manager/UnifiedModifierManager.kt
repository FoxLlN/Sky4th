
package com.sky4th.equipment.modifier.manager

import com.sky4th.equipment.modifier.manager.handlers.*
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 统一词条管理器
 * 负责管理所有词条的周期性任务执行
 * 使用内置计数器控制每个词条的执行频率
 */
object UnifiedModifierManager {

    // 统一的定时任务
    private var unifiedTask: BukkitTask? = null

    // 玩家数据：UUID -> PlayerData
    private val playerData = ConcurrentHashMap<UUID, PlayerData>()

    /**
     * 玩家数据类
     * 包含Player对象和该玩家的所有词条数据
     */
    data class PlayerData(
        val uuid: UUID,
        var player: Player?,  // 缓存的Player对象
        val activeAffixes: MutableMap<String, AffixData> = mutableMapOf()
    ) {
        /**
         * 检查Player对象是否有效
         * @return 如果Player对象有效返回true，否则返回false
         */
        fun isPlayerValid(): Boolean {
            val currentPlayer = player
            return currentPlayer != null && currentPlayer.isOnline
        }

        /**
         * 更新Player对象
         * 如果缓存的Player对象无效，则重新查找
         */
        fun updatePlayer(): Boolean {
            val currentPlayer = player
            if (currentPlayer != null && currentPlayer.isOnline) {
                return true
            }
            player = Bukkit.getPlayer(uuid)
            val newPlayer = player
            return newPlayer != null && newPlayer.isOnline
        }
    }

    /**
     * 初始化统一管理器
     * @param plugin 插件实例
     */
    fun initialize(plugin: JavaPlugin) {
        // 如果任务已存在，先取消
        unifiedTask?.cancel()

        // 注册所有词条处理器
        registerAllHandlers()

        // 注册定时任务，每tick执行一次
        unifiedTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            processAllPlayers()
        }, 0L, 1L)
    }

    /**
     * 注册所有词条处理器
     */
    private fun registerAllHandlers() {
        HandlerRegistry.register("gale_wind", GaleWindHandler())
        HandlerRegistry.register("flowing_light", FlowingLightHandler())
        HandlerRegistry.register("rooted", RootedHandler())
        HandlerRegistry.register("magnetic", MagneticHandler())
        HandlerRegistry.register("echo_location", EchoLocationHandler())
        HandlerRegistry.register("golden_body", GoldenBodyHandler())
        HandlerRegistry.register("prism", PrismHandler())
        HandlerRegistry.register("resonance", ResonanceHandler())
        HandlerRegistry.register("solar", SolarHandler())
    }

    /**
     * 关闭统一管理器
     */
    fun shutdown() {
        unifiedTask?.cancel()
        unifiedTask = null
        playerData.clear()
        HandlerRegistry.clear()
    }

    /**
     * 添加玩家词条
     * @param player 玩家
     * @param affixId 词条ID
     * @param data 词条数据
     */
    fun addPlayerAffix(player: Player, affixId: String, data: AffixData) {
        val playerDataEntry = playerData.computeIfAbsent(player.uniqueId) {
            PlayerData(player.uniqueId, player)
        }
        playerDataEntry.player = player
        playerDataEntry.activeAffixes[affixId] = data
    }

    /**
     * 移除玩家词条
     * @param player 玩家
     * @param affixId 词条ID
     */
    fun removePlayerAffix(player: Player, affixId: String) {
        val data = playerData[player.uniqueId]
        data?.activeAffixes?.remove(affixId)
        // 如果玩家没有词条了，移除玩家
        if (data?.activeAffixes?.isEmpty() == true) {
            playerData.remove(player.uniqueId)
        }
    }

    /**
     * 处理所有玩家的词条逻辑
     */
    private fun processAllPlayers() {
        playerData.values.forEach outer@{ data ->
            // 检查Player对象是否有效
            if (!data.updatePlayer()) {
                playerData.remove(data.uuid)
                return@outer
            }

            // 使用局部变量避免智能转换问题
            val player = data.player
            if (player == null || !player.isOnline) {
                playerData.remove(data.uuid)
                return@outer
            }

            // 遍历该玩家的所有词条
            data.activeAffixes.forEach inner@{ (affixId, affixData) ->
                // 检查是否需要执行
                if (!affixData.shouldProcess()) {
                    return@inner
                }

                val handler = HandlerRegistry.getHandler(affixId)
                if (handler != null) {
                    try {
                        handler.process(player, affixData)
                    } catch (e: Exception) {
                        Bukkit.getLogger().warning("Error processing affix $affixId for player ${player.name}: ${e.message}")
                    }
                }
            }
        }
    }

    /**
     * 玩家登录时调用
     * @param player 玩家
     */
    fun onPlayerJoin(player: Player) {
        val data = playerData.computeIfAbsent(player.uniqueId) {
            PlayerData(player.uniqueId, player)
        }
        data.player = player
    }

    /**
     * 玩家离线时调用
     * @param player 玩家
     */
    fun onPlayerQuit(player: Player) {
        playerData.remove(player.uniqueId)
    }

    /**
     * 玩家切换世界时调用
     * @param player 玩家
     */
    fun onPlayerChangedWorld(player: Player) {
        val data = playerData[player.uniqueId]
        if (data != null) {
            data.player = player
        }
    }

    /**
     * 获取玩家数据
     * @param player 玩家
     * @return 玩家数据，如果不存在返回null
     */
    fun getPlayerData(player: Player): PlayerData? {
        return playerData[player.uniqueId]
    }

    /**
     * 获取玩家词条数据
     * @param player 玩家
     * @param affixId 词条ID
     * @return 词条数据，如果不存在返回null
     */
    fun getAffixData(player: Player, affixId: String): AffixData? {
        return playerData[player.uniqueId]?.activeAffixes?.get(affixId)
    }
}
