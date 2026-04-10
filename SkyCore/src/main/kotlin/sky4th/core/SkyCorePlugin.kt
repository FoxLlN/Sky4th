package sky4th.core

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginEnableEvent
import org.bukkit.plugin.java.JavaPlugin
import sky4th.core.command.SkyCoreCommandHandler
import sky4th.core.command.SkyCoreContext
import sky4th.core.listener.MarkListener
import sky4th.core.listener.PlayerEnterListener
import sky4th.core.listener.PlayTimeListener
import sky4th.core.mark.MarkManager
import sky4th.core.scoreboard.ScoreboardListener
import sky4th.core.scoreboard.ScoreboardManager

class SkyCorePlugin : JavaPlugin() {

    override fun onEnable() {
        // 确保插件目录和 config.yml 存在
        saveDefaultConfig()

        // 初始化 SkyCore
        SkyCore.init(this)

        // 初始化命令上下文
        SkyCoreContext.init(this)

        // 初始化标记管理器
        MarkManager.init(this)

        // 初始化计分板系统
        ScoreboardManager.initialize()

        // 注册监听器
        server.pluginManager.registerEvents(PlayerEnterListener(), this)
        server.pluginManager.registerEvents(PlayTimeListener(), this)
        server.pluginManager.registerEvents(MarkListener(), this)
        server.pluginManager.registerEvents(ScoreboardListener(), this)

        // 注册命令
        getCommand("sky")?.setExecutor(SkyCoreCommandHandler)
        getCommand("sky")?.tabCompleter = SkyCoreCommandHandler

        // 启动统一的定时批量保存任务（每5分钟）
        server.scheduler.runTaskTimerAsynchronously(
            this,
            Runnable {
                try {
                    // 批量保存玩家属性
                    SkyCore.getPlayerAttributesService()?.flushPendingUpdates()
                    // 批量保存玩家数据
                    SkyCore.getPlayerService()?.saveAll()
                } catch (e: Exception) {
                    logger.warning("批量保存数据失败: ${e.message}")
                    e.printStackTrace()
                }
            },
            6000L,  // 5分钟后开始
            6000L   // 每5分钟执行一次
        )

        logger.info("SkyCore 已加载 - 版本 ${pluginMeta.version}")
    }

    override fun onDisable() {
        // 清理标记管理器
        MarkManager.cleanup()
        // 关闭计分板系统
        ScoreboardManager.shutdown()
        // 关闭 API 并清理资源
        SkyCore.shutdown()
        logger.info("SkyCore 已卸载")
    }
}