package sky4th.core.listener

import sky4th.core.api.PlayerAPI
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
 */
class PlayTimeListener : Listener {
    private val playTimeTasks = mutableMapOf<UUID, Int>()
    private var flushTaskId: Int = -1

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (!PlayerAPI.isAvailable()) return

        val player = event.player
        val uuid = player.uniqueId
        val plugin = player.server.pluginManager.getPlugin("SkyCore")?: return
        // 启动定时任务，每分钟更新一次玩家游戏时长
        val taskId = player.server.scheduler.runTaskTimer(
            plugin,
            Runnable {
                if (player.isOnline) {
                    try {
                        PlayerAPI.updatePlayTime(player)
                    } catch (_: Exception) { /* 忽略异常 */ }
                }
            },
            1200L,  // 1分钟后开始（60秒 * 20 ticks/秒）
            1200L   // 每1分钟执行一次
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

        // 重置单命时长
        try {
            PlayerAPI.resetLifePlayTime(player)
        } catch (_: Exception) { /* 忽略异常 */ }
    }
}
