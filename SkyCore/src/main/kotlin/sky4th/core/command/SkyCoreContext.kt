package sky4th.core.command

import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin

/** SkyCore 命令上下文：插件实例，供 impl 子命令使用 */
data class CommandContext(
    val plugin: JavaPlugin
)

object SkyCoreContext {
    @Volatile
    private var ctx: CommandContext? = null

    fun init(plugin: JavaPlugin) {
        ctx = CommandContext(plugin)
    }

    fun get(): CommandContext? = ctx

    fun getOrThrow(): CommandContext = ctx ?: error("SkyCoreContext 未初始化，请确保 SkyCore 已加载")

    fun showHelp(sender: CommandSender, context: CommandContext) {
        val pluginName = context.plugin.name
        sender.sendMessage("§6===== §b${pluginName} §6帮助 =====")
        sender.sendMessage("§e/sky help §7- 显示帮助信息")
        sender.sendMessage("§e/sky info §7- 查看 API 信息")
        sender.sendMessage("§e/sky status §7- 查看系统状态")
        sender.sendMessage("§e/sky player <玩家> <info|economy|time> §7- 查看玩家数据")
        sender.sendMessage("§e/sky economy <status|balance> §7- 查看经济系统")
        sender.sendMessage("§e/sky permission <add|remove> <玩家> <权限名> §7- 管理玩家权限")
        sender.sendMessage("§e/sky reload §7- 重载配置")
    }
}
