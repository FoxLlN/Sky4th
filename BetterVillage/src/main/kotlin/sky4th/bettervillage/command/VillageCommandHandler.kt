package sky4th.bettervillage.command

import sky4th.bettervillage.BetterVillage
import sky4th.bettervillage.util.LanguageUtil.sendLang
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import sky4th.bettervillage.command.impl.runHelp
import sky4th.bettervillage.command.impl.runInfo
import sky4th.bettervillage.command.impl.runNearby
import sky4th.bettervillage.command.impl.runList
import sky4th.bettervillage.command.impl.runLevel
import sky4th.bettervillage.command.impl.Villager

/**
 * Village 命令处理器
 */
object VillageCommandHandler : CommandExecutor, TabCompleter {

    private val subCommands = listOf("help", "info", "nearby", "list", "level", "villager")

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendLang(BetterVillage.instance, "command.player-only")
            return true
        }

        if (args.isEmpty()) {
            runHelp(sender)
            return true
        }

        val subCommand = args[0].lowercase()

        when (subCommand) {
            "help", "?" -> runHelp(sender)
            "info" -> runInfo(sender)
            "nearby" -> runNearby(sender)
            "list" -> runList(sender)
            "level" -> runLevel(sender, args)
            "villager" -> Villager().execute(sender, args.drop(1).toTypedArray())
            else -> {
                sender.sendLang(BetterVillage.instance, "command.unknown", "command" to subCommand)
                runHelp(sender)
            }
        }

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        return when (args.size) {
            1 -> {
                subCommands.filter { it.startsWith(args[0], ignoreCase = true) }
            }
            else -> emptyList()
        }
    }
}
