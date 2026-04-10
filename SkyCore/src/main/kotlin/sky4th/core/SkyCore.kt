package sky4th.core

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginEnableEvent
import org.bukkit.plugin.java.JavaPlugin
import sky4th.core.command.SkyCoreCommandHandler
import sky4th.core.command.SkyCoreContext
import sky4th.core.listener.MarkListener
import sky4th.core.listener.PlayerEnterListener
import sky4th.core.mark.MarkManager

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

        // 注册监听器
        server.pluginManager.registerEvents(PlayerEnterListener(), this)
        server.pluginManager.registerEvents(MarkListener(), this)

        // 注册命令
        getCommand("sky")?.setExecutor(SkyCoreCommandHandler)
        getCommand("sky")?.tabCompleter = SkyCoreCommandHandler


        logger.info("SkyCore 已加载 - 版本 ${pluginMeta.version}")
    }

    override fun onDisable() {
        // 清理标记管理器
        MarkManager.cleanup()
        // 关闭 API 并清理资源
        SkyCore.shutdown()
        logger.info("SkyCore 已卸载")
    }
}