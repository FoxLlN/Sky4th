package sky4th.dungeon.region

import sky4th.dungeon.util.LanguageUtil.sendLangSys
import sky4th.core.api.LanguageAPI
import sky4th.dungeon.Dungeon
import sky4th.dungeon.config.ConfigManager
import sky4th.dungeon.config.Region
import sky4th.dungeon.player.PlayerManager
import org.bukkit.Bukkit
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.*

/**
 * 副本内撤离点检测：
 * 玩家在撤离区域内连续停留指定秒数后，自动执行退出副本逻辑。
 */
class ExitRegionDetector(
    private val plugin: JavaPlugin,
    private val configManager: ConfigManager,
    private val playerManager: PlayerManager
) : Listener {

    // 支持多个撤离区域 - 从地牢配置中获取
    private val exitRegions: List<Region> by lazy {
        val dungeonConfigs = configManager.loadDungeonConfigs()
        dungeonConfigs.values.flatMap { it.exitRegions }
    }
    private val waitSeconds: Int by lazy {
        val dungeonConfigs = configManager.loadDungeonConfigs()
        dungeonConfigs.values.firstOrNull()?.durationMinutes?.coerceAtLeast(1) ?: 5
    }
    private val waitTicks: Long get() = waitSeconds * 20L
    private val leaveGraceTicks: Long = 3 * 20L // 离开区域 3 秒内再回来不重置倒计时

    // 记录在撤离区域内"计时中的玩家"和对应的任务 + BossBar + 所在撤离区域
    private data class StayContext(
        val task: BukkitTask,
        val bossBar: BossBar,
        val region: Region
    )

    private val stayContexts: MutableMap<UUID, StayContext> = mutableMapOf()

    // 玩家离开撤离区域后的"延迟取消"任务，防止边缘反复横跳立刻打断倒计时
    private val leaveCancelTasks: MutableMap<UUID, BukkitTask> = mutableMapOf()

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        if (exitRegions.isEmpty()) return

        val from = event.from
        val to = event.to

        // 如果玩家没有移动方块位置，跳过，避免频繁计算
        if (from.blockX == to.blockX && from.blockY == to.blockY && from.blockZ == to.blockZ) {
            return
        }

        val player = event.player
        val uuid = player.uniqueId

        // 只有当前在副本中的玩家才需要检测撤离点
        if (!playerManager.isPlayerInDungeon(player)) {
            cancelStay(player, sendCancelledMessage = false)
            return
        }

        // 当前所在的撤离区域（可能为 null）
        val currentRegion = exitRegions.firstOrNull { it.contains(to) }
        val hasContext = stayContexts.containsKey(uuid)

        if (currentRegion != null) {
            // 回到撤离区域：取消等待取消任务
            leaveCancelTasks.remove(uuid)?.cancel()

            if (!hasContext) {
                // 刚进入撤离区域，启动计时任务
                startStayTask(player, currentRegion)
            }
        } else if (hasContext && !leaveCancelTasks.containsKey(uuid)) {
            // 刚刚离开撤离区域：启动一个短暂的延迟取消任务，给玩家一点容错空间
            val regionSnapshot = stayContexts[uuid]?.region ?: return
            val task = plugin.server.scheduler.runTaskLater(plugin, Runnable {
                leaveCancelTasks.remove(uuid)
                val online = plugin.server.getPlayer(uuid) ?: return@Runnable
                // 如果玩家此时已经回到撤离区域，就不取消计时
                if (regionSnapshot.contains(online.location)) return@Runnable
                cancelStay(online, sendCancelledMessage = true)
            }, leaveGraceTicks)
            leaveCancelTasks[uuid] = task
        }
    }

    private fun startStayTask(player: Player, region: Region) {
        val uuid = player.uniqueId

        // 初始化 BossBar
        val bossBar = Bukkit.createBossBar(
            LanguageAPI.getText(plugin, "exit.bossbar-title", "seconds" to waitSeconds),
            BarColor.GREEN,
            BarStyle.SOLID
        )
        bossBar.progress = 1.0
        bossBar.addPlayer(player)
        bossBar.isVisible = true

        var remaining = waitSeconds

        val task = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            val onlinePlayer = plugin.server.getPlayer(uuid)
            if (onlinePlayer == null) {
                // 玩家不在线，直接清理
                cancelStayInternal(uuid, sendCancelledMessage = false)
                return@Runnable
            }

            // 若玩家已不在副本或不在撤离区域内，倒计时取消（真正的取消逻辑由 onPlayerMove 中的延迟取消负责，这里只兜底）
            if (!playerManager.isPlayerInDungeon(onlinePlayer) || !region.contains(onlinePlayer.location)) {
                cancelStay(onlinePlayer, sendCancelledMessage = false)
                return@Runnable
            }

            remaining--
            if (remaining <= 0) {
                // 完成倒计时，执行退出副本
                cancelStayInternal(uuid, sendCancelledMessage = false)
                // 这里只调用 teleportFromDungeon，具体离开提示统一由 PlayerManager 处理
                playerManager.teleportFromDungeon(onlinePlayer, true)
                return@Runnable
            }

            // 更新 BossBar
            val progress = remaining.toDouble() / waitSeconds.toDouble()
            bossBar.progress = progress.coerceIn(0.0, 1.0)
            val onlinePlayerForLang = Bukkit.getPlayer(uuid)
            if (onlinePlayerForLang != null) {
                bossBar.setTitle(
                    LanguageAPI.getText(plugin, "exit.bossbar-title", "seconds" to remaining)
                )
            }
        }, 20L, 20L)

        stayContexts[uuid] = StayContext(task, bossBar, region)

        // 显示带名称的提示（如果没有名称则显示"撤离点"）
        val exitName = if (region.name.isNotEmpty()) region.name else "撤离点"
        player.sendLangSys(plugin, "exit.entered-with-name", "name" to exitName, "seconds" to waitSeconds)
    }

    private fun cancelStay(player: Player, sendCancelledMessage: Boolean) {
        val uuid = player.uniqueId
        cancelStayInternal(uuid, sendCancelledMessage)
        leaveCancelTasks.remove(uuid)?.cancel()
    }

    private fun cancelStayInternal(uuid: UUID, sendCancelledMessage: Boolean) {
        val context = stayContexts.remove(uuid) ?: return
        context.task.cancel()
        context.bossBar.removeAll()

        if (sendCancelledMessage) {
            val online = Bukkit.getPlayer(uuid) ?: return
            online.sendLangSys(plugin, "exit.cancelled")
        }
    }
}
