package sky4th.core.command

import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import sky4th.core.command.impl.runHelp
import sky4th.core.command.impl.runInfo
import sky4th.core.command.impl.runReload
import sky4th.core.command.impl.runPlayer
import sky4th.core.command.impl.runEconomy
import sky4th.core.command.impl.runPermission
import sky4th.core.command.impl.runStatus
import sky4th.core.command.impl.runMark
import sky4th.core.command.impl.runEquipment

/**
 * SkyCore 主命令处理器
 */
object SkyCoreCommandHandler : CommandExecutor, TabCompleter {

    private val subCommands = listOf("help", "info", "reload", "player", "economy", "permission", "status", "mark", "equipment")

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val ctx = SkyCoreContext.get() ?: run {
            sender.sendMessage("§cSkyCoreContext 未初始化，请确保 SkyCore 已加载")
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
            "reload" -> runReload(sender)
            "player" -> runPlayer(sender, args)
            "economy" -> runEconomy(sender, args)
            "permission" -> runPermission(sender, args)
            "status" -> runStatus(sender)
            "mark" -> runMark(sender, args)
            "equipment" -> runEquipment(sender, args)
            else -> {
                sender.sendMessage("§c未知命令: $subCommand")
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
                    "player" -> {
                        sender.server.onlinePlayers.map { it.name }
                            .filter { it.startsWith(args[1], ignoreCase = true) }
                    }
                    "economy" -> {
                        listOf("status", "balance")
                            .filter { it.startsWith(args[1], ignoreCase = true) }
                    }
                    "permission" -> {
                        listOf("add", "remove")
                            .filter { it.startsWith(args[1], ignoreCase = true) }
                    }
                    "mark" -> {
                        listOf("@nearest")
                            .plus(sender.server.onlinePlayers.map { it.name })
                            .filter { it.startsWith(args[1], ignoreCase = true) }
                    }
                    "equipment" -> {
                        listOf("create")
                            .filter { it.startsWith(args[1], ignoreCase = true) }
                    }
                    else -> emptyList()
                }
            }
            3 -> {
                when (args[0].lowercase()) {
                    "player" -> {
                        listOf("info", "economy", "time")
                            .filter { it.startsWith(args[2], ignoreCase = true) }
                    }
                    "mark" -> {
                        Material.values().map { it.name }
                            .filter { it.startsWith(args[2], ignoreCase = true) }
                    }
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }
    }
}
