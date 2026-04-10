package sky4th.bettermc.command

import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import sky4th.bettermc.util.LanguageUtil.sendLang

/** BetterMC 命令上下文：插件实例，供 impl 子命令使用 */
data class CommandContext(
    val plugin: JavaPlugin
)

object BetterMCContext {
    @Volatile
    private var ctx: CommandContext? = null

    fun init(plugin: JavaPlugin) {
        ctx = CommandContext(plugin)
    }

    fun get(): CommandContext? = ctx

    fun getOrThrow(): CommandContext = ctx ?: error("BetterMCContext 未初始化，请确保 BetterMC 已加载")

    fun showHelp(sender: CommandSender, context: CommandContext) {
        val pluginName = context.plugin.name
        sender.sendLang(context.plugin, "command.help.header", "plugin" to pluginName)
        sender.sendLang(context.plugin, "command.help.help")
        sender.sendLang(context.plugin, "command.help.reload")
        sender.sendLang(context.plugin, "command.help.list")
        sender.sendLang(context.plugin, "command.help.rule")
    }
}
