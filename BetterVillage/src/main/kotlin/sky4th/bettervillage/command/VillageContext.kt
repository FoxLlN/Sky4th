package sky4th.bettervillage.command

import sky4th.bettervillage.BetterVillage
import sky4th.bettervillage.util.LanguageUtil.sendLang
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin

/** Village 命令上下文：插件实例，供 impl 子命令使用 */
data class CommandContext(
    val plugin: JavaPlugin
)

object VillageContext {
    @Volatile
    private var ctx: CommandContext? = null

    fun init(plugin: JavaPlugin) {
        ctx = CommandContext(plugin)
    }

    fun get(): CommandContext? = ctx

    fun getOrThrow(): CommandContext = ctx ?: error("VillageContext 未初始化，请确保 BetterVillage 已加载")

    fun showHelp(sender: CommandSender, context: CommandContext) {
        val pluginName = context.plugin.name
        sender.sendLang(BetterVillage.instance, "command.help.header", "plugin" to pluginName)
        sender.sendLang(BetterVillage.instance, "command.help.info")
        sender.sendLang(BetterVillage.instance, "command.help.nearby")
        sender.sendLang(BetterVillage.instance, "command.help.list")
        sender.sendLang(BetterVillage.instance, "command.help.level")
    }
}
