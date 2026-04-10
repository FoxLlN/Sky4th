package sky4th.bettermc.command

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import sky4th.bettermc.command.impl.runHelp
import sky4th.bettermc.command.impl.runReload
import sky4th.bettermc.command.impl.runList
import sky4th.bettermc.command.impl.runRule
import sky4th.bettermc.util.LanguageUtil.sendLang
import sky4th.bettermc.BetterMC

/**
 * BetterMC 主命令处理器
 */
object BetterMCCommandHandler : CommandExecutor, TabCompleter {

    private val subCommands = listOf("help", "reload", "list", "rule")

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val ctx = BetterMCContext.get() ?: run {
            sender.sendLang(BetterMC.instance, "plugin.context-not-initialized")
            return true
        }

        if (args.isEmpty()) {
            runHelp(sender)
            return true
        }

        val subCommand = args[0].lowercase()

        when (subCommand) {
            "help", "?" -> runHelp(sender)
            "reload" -> runReload(sender)
            "list" -> runList(sender)
            "rule" -> runRule(sender, args)
            else -> {
                sender.sendLang(BetterMC.instance, "command.unknown-command", "command" to subCommand)
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
            2 -> {
                when (args[0].lowercase()) {
                    "rule" -> {
                        FeatureManager.getFeatures().keys
                            .filter { it.startsWith(args[1], ignoreCase = true) }
                    }
                    else -> emptyList()
                }
            }
            3 -> {
                when (args[0].lowercase()) {
                    "rule" -> {
                        listOf("true", "false")
                            .filter { it.startsWith(args[2], ignoreCase = true) }
                    }
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }
    }
}
