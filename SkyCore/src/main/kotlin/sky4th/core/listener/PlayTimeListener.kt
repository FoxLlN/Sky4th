package sky4th.core.listener

import sky4th.core.api.PlayerAPI
import sky4th.core.event.PlayTimeHourEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*

/**
 * 玩家游玩时间监听器
 * - 登录：启动定时任务追踪游玩时间
 * - 退出：停止定时任务并保存数据
 * - 死亡：重置单命时长
 * - 每满1小时：触发PlayTimeHourEvent事件
 */
class PlayTimeListener : Listener {
    private val playTimeTasks = mutableMapOf<UUID, Int>()
    private var flushTaskId: Int = -1

    // 记录每个玩家上次的小时数，用于检测是否跨过整点
    private val lastHours = mutableMapOf<UUID, Triple<Int, Int, Int>>() // (dailyHours, lifeHours, totalHours)

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (!PlayerAPI.isAvailable()) return

        val player = event.player
        val uuid = player.uniqueId
        val plugin = player.server.pluginManager.getPlugin("SkyCore")?: return

        // 初始化记录当前小时数
        val playerData = PlayerAPI.getPlayerData(player)
        val identity = playerData.identity
        lastHours[uuid] = Triple(
            (identity.todayPlayTime.seconds / 3600).toInt(),
            (identity.currentLifePlayTime.seconds / 3600).toInt(),
            (identity.playTime.seconds / 3600).toInt()
        )

        // 启动定时任务，每秒更新一次玩家游戏时长
        val taskId = player.server.scheduler.runTaskTimer(
            plugin,
            Runnable {
                if (player.isOnline) {
                    try {
                        PlayerAPI.updatePlayTime(player)
                        checkHourMilestone(player)
                    } catch (_: Exception) { /* 忽略异常 */ }
                }
            },
            20L,  // 1秒后开始（1秒 * 20 ticks/秒）
            20L   // 每1秒执行一次
        ).taskId
        playTimeTasks[uuid] = taskId
        
        // 如果是第一个玩家，启动批量保存任务（每5分钟执行一次）
        if (flushTaskId == -1) {
            flushTaskId = player.server.scheduler.runTaskTimerAsynchronously(
                plugin,
                Runnable {
                    try {
                        PlayerAPI.flushPendingUpdates()
                    } catch (_: Exception) { /* 忽略异常 */ }
                },
                6000L,  // 5分钟后开始（5分钟 * 60秒 * 20 ticks/秒）
                6000L   // 每5分钟执行一次
            ).taskId
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (!PlayerAPI.isAvailable()) return

        val uuid = event.player.uniqueId

        // 停止定时任务
        playTimeTasks[uuid]?.let { taskId ->
            event.player.server.scheduler.cancelTask(taskId)
            playTimeTasks.remove(uuid)
        }
        
        // 清理小时数记录
        lastHours.remove(uuid)

        // 如果没有玩家了，停止批量保存任务
        if (playTimeTasks.isEmpty() && flushTaskId != -1) {
            event.player.server.scheduler.cancelTask(flushTaskId)
            flushTaskId = -1
        }
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        if (!PlayerAPI.isAvailable()) return

        val player = event.entity
        val uuid = player.uniqueId

        // 重置单命时长
        try {
            PlayerAPI.resetLifePlayTime(player)
            // 重置存活时长的小时数记录
            lastHours[uuid]?.let { (daily, _, total) ->
                lastHours[uuid] = Triple(daily, 0, total)
            }
        } catch (_: Exception) { /* 忽略异常 */ }
    }

    /**
     * 检查玩家游玩时间是否跨过整点小时，如果是则触发事件
     */
    private fun checkHourMilestone(player: Player) {
        val uuid = player.uniqueId
        val playerData = PlayerAPI.getPlayerData(player) ?: return
        val identity = playerData.identity

        // 计算当前小时数
        val currentDailyHours = (identity.todayPlayTime.seconds / 3600).toInt()
        val currentLifeHours = (identity.currentLifePlayTime.seconds / 3600).toInt()
        val currentTotalHours = (identity.playTime.seconds / 3600).toInt()

        // 获取上次记录的小时数
        val (lastDaily, lastLife, lastTotal) = lastHours.getOrDefault(uuid, Triple(0, 0, 0))

        // 检查并触发每日时长事件
        if (currentDailyHours > lastDaily) {
            Bukkit.getPluginManager().callEvent(
                PlayTimeHourEvent(
                    player = player,
                    fromHour = lastDaily,
                    toHour = currentDailyHours,
                    timeType = PlayTimeHourEvent.TimeType.DAILY
                )
            )
        }

        // 检查并触发存活时长事件
        if (currentLifeHours > lastLife) {
            Bukkit.getPluginManager().callEvent(
                PlayTimeHourEvent(
                    player = player,
                    fromHour = lastLife,
                    toHour = currentLifeHours,
                    timeType = PlayTimeHourEvent.TimeType.CURRENT_LIFE
                )
            )
        }

        // 检查并触发总时长事件
        if (currentTotalHours > lastTotal) {
            Bukkit.getPluginManager().callEvent(
                PlayTimeHourEvent(
                    player = player,
                    fromHour = lastTotal,
                    toHour = currentTotalHours,
                    timeType = PlayTimeHourEvent.TimeType.TOTAL
                )
            )
        }

        // 更新记录
        lastHours[uuid] = Triple(currentDailyHours, currentLifeHours, currentTotalHours)
    }
}
