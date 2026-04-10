package sky4th.core.scoreboard

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import sky4th.core.api.PlayerAPI
import sky4th.core.lang.getLangText
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 计分板管理器
 * 负责管理所有玩家的计分板显示
 */
object ScoreboardManager {

    private val playerScoreboards = ConcurrentHashMap<UUID, ScoreboardData>()
    private val playerConfigs = ConcurrentHashMap<UUID, ScoreboardConfig>()
    private var updateTaskId: Int = -1

    /**
     * 初始化计分板系统
     */
    fun initialize() {
        // 启动定时更新任务，每秒更新一次
        updateTaskId = Bukkit.getScheduler().runTaskTimer(
            Bukkit.getPluginManager().getPlugin("SkyCore") ?: return,
            Runnable { updateAllScoreboards() },
            20L,  // 1秒后开始
            20L   // 每1秒执行一次
        ).taskId
    }

    /**
     * 关闭计分板系统
     */
    fun shutdown() {
        if (updateTaskId != -1) {
            Bukkit.getScheduler().cancelTask(updateTaskId)
            updateTaskId = -1
        }
        playerScoreboards.clear()
        playerConfigs.clear()
    }

    /**
     * 为玩家创建计分板
     */
    fun createScoreboard(player: Player) {
        val scoreboard = Bukkit.getScoreboardManager()?.newScoreboard ?: return
        val objective = scoreboard.registerNewObjective("sky4th_scoreboard", "dummy", 
            getLangText("scoreboard.title"))
        objective.displaySlot = DisplaySlot.SIDEBAR

        // 创建计分板数据
        val data = ScoreboardData(
            scoreboard = scoreboard,
            objective = objective
        )

        // 获取或创建玩家配置
        val config = playerConfigs.getOrPut(player.uniqueId) {
            ScoreboardConfig.createDefault(player.uniqueId)
        }

        playerScoreboards[player.uniqueId] = data
        player.scoreboard = scoreboard

        // 立即更新一次计分板
        updateScoreboard(player, data, config)
    }

    /**
     * 移除玩家的计分板
     */
    fun removeScoreboard(player: Player) {
        playerScoreboards.remove(player.uniqueId)
        playerConfigs.remove(player.uniqueId)
        // 恢复默认计分板
        player.scoreboard = Bukkit.getScoreboardManager()?.mainScoreboard ?: return
    }

    /**
     * 获取玩家的计分板配置
     */
    fun getPlayerConfig(uuid: UUID): ScoreboardConfig? {
        return playerConfigs[uuid]
    }

    /**
     * 设置玩家的计分板配置
     */
    fun setPlayerConfig(uuid: UUID, config: ScoreboardConfig) {
        playerConfigs[uuid] = config
        // 更新计分板显示
        Bukkit.getPlayer(uuid)?.let { player ->
            playerScoreboards[uuid]?.let { data ->
                updateScoreboard(player, data, config)
            }
        }
    }

    /**
     * 更新所有玩家的计分板
     */
    private fun updateAllScoreboards() {
        playerScoreboards.entries.removeIf { entry ->
            val player = Bukkit.getPlayer(entry.key)
            if (player != null && player.isOnline) {
                val config = playerConfigs[entry.key] ?: ScoreboardConfig.createDefault(entry.key)
                updateScoreboard(player, entry.value, config)
                false
            } else {
                true
            }
        }
    }

    /**
     * 更新单个玩家的计分板
     */
    private fun updateScoreboard(player: Player, data: ScoreboardData, config: ScoreboardConfig) {
        if (!PlayerAPI.isAvailable()) return

        try {
            val playerData = PlayerAPI.getPlayerData(player)
            val identity = playerData.identity

            // 清除所有旧的分数
            data.scoreboard.entries.forEach { entry ->
                data.scoreboard.resetScores(entry)
            }

            var score = 20 // 从高分开始，确保显示顺序正确

            // 标题（始终显示，但可以通过配置控制）
            data.objective.displayName = getLangText("scoreboard.title")
            

            // 今日游玩时长
            if ("today_playtime" in config.customFilters) {
                data.objective.getScore(
                    getLangText("scoreboard.today_playtime", 
                    "time" to formatTime(identity.todayPlayTime))
                ).score = score--
            }

            // 存活时长
            if ("current_life_playtime" in config.customFilters) {
                data.objective.getScore(
                    getLangText("scoreboard.current_life_playtime",
                    "time" to formatTime(identity.currentLifePlayTime))
                ).score = score--
            }

            // 信用点
            if ("credits" in config.customFilters) {
                val economy = playerData.economy
                data.objective.getScore(
                    getLangText("scoreboard.credits",
                    "credits" to String.format("%.2f", economy.credits))
                ).score = score--
            }

            // 总游玩时长
            if ("total_playtime" in config.customFilters) {
                data.objective.getScore(
                    getLangText("scoreboard.total_playtime",
                    "time" to formatTime(identity.playTime))
                ).score = score--
            }

            // QQ群信息
            data.objective.getScore(getLangText("scoreboard.qq_group")).score = score--
            

            // 底部信息
            data.objective.getScore(getLangText("scoreboard.footer")).score = score--
            

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 格式化时间显示
     */
    private fun formatTime(duration: java.time.Duration): String {
        val totalSeconds = duration.seconds
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return when {
            hours > 0 -> "${hours}时${minutes}分"
            minutes > 0 -> "${minutes}分${seconds}秒"
            else -> "00分${seconds}秒"
        }
    }

    /**
     * 计分板数据类
     */
    data class ScoreboardData(
        val scoreboard: Scoreboard,
        val objective: Objective
    )
}
