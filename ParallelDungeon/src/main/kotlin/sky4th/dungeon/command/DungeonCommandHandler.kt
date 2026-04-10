package sky4th.dungeon.command

import sky4th.dungeon.command.impl.runCost
import sky4th.dungeon.command.impl.runEnter
import sky4th.dungeon.command.impl.runExit
import sky4th.dungeon.command.impl.runHelp
import sky4th.dungeon.command.impl.runLevel
import sky4th.dungeon.command.impl.runRandom
import sky4th.dungeon.command.impl.runReload
import sky4th.dungeon.command.impl.runReset
import sky4th.dungeon.command.impl.runSetLevel
import sky4th.dungeon.command.impl.runShop
import sky4th.dungeon.command.impl.runStorage
import sky4th.dungeon.command.impl.runList
import sky4th.dungeon.command.impl.runCreateInstance
import sky4th.dungeon.command.impl.runDeleteInstance
import sky4th.dungeon.command.impl.TeamCommand
import sky4th.dungeon.util.LanguageUtil.sendLang
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

/**
 * 地牢命令的 Bukkit 执行器与 Tab 补全。
 */
object DungeonCommandHandler : CommandExecutor, TabCompleter {

    private val subCommands = listOf("help", "enter", "exit", "cost", "shop", "storage", "random", "reload", "reset", "level", "setlevel", "list", "create", "delete", "team")

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val ctx = DungeonContext.get() ?: run {
            sender.sendMessage("§cDungeonContext 未初始化，请确保 ParallelDungeon 已加载")
            return true
        }
        if (!sender.hasPermission("dungeon.use")) {
            sender.sendLang(ctx.plugin, "command.no-permission")
            return true
        }
        val sub = args.getOrNull(0)?.lowercase()
        when (sub) {
            null, "help" -> runHelp(sender)
            "enter" -> {
                val fullInstanceId = args.getOrNull(1)?.trim()
                if (fullInstanceId.isNullOrEmpty()) {
                    sender.sendLang(ctx.plugin, "command.usage")
                    return true
                }
                runEnter(sender, fullInstanceId)
            }
            "exit" -> runExit(sender)
            "shop" -> runShop(sender)
            "storage" -> runStorage(sender)
            "random" -> runRandom(sender, args.getOrNull(1)?.trim())
            "cost" -> {
                val dungeonName = args.getOrNull(1)?.trim()
                if (dungeonName.isNullOrEmpty()) {
                    sender.sendLang(ctx.plugin, "command.usage")
                    return true
                }
                val newCostArg = args.getOrNull(2)?.trim()
                val newCost = if (newCostArg.isNullOrEmpty()) null else newCostArg.toDoubleOrNull()
                if (newCostArg != null && newCost == null) {
                    sender.sendLang(ctx.plugin, "command.cost-invalid-number")
                    return true
                }
                runCost(sender, dungeonName, newCost)
            }
            "reload" -> {
                val arg = args.getOrNull(1)?.trim()
                if (arg.isNullOrEmpty()) {
                    sender.sendLang(ctx.plugin, "command.usage")
                    return true
                }
                runReload(sender, arg)
            }
            "reset" -> {
                val arg = args.getOrNull(1)?.trim()
                if (arg.isNullOrEmpty()) {
                    sender.sendLang(ctx.plugin, "command.usage")
                    return true
                }
                runReset(sender, arg)
            }
            "level" -> runLevel(sender, args.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() })
            "setlevel" -> {
                val target = args.getOrNull(1)?.trim()
                val levelArg = args.getOrNull(2)?.trim()
                if (target.isNullOrEmpty() || levelArg.isNullOrEmpty()) {
                    sender.sendLang(ctx.plugin, "command.usage")
                    return true
                }
                runSetLevel(sender, target, levelArg)
            }
            "list" -> runList(sender)
            "create" -> {
                val dungeonId = args.getOrNull(1)?.trim()
                if (dungeonId.isNullOrEmpty()) {
                    sender.sendLang(ctx.plugin, "command.usage")
                    return true
                }
                runCreateInstance(sender, dungeonId)
            }
            "delete" -> {
                val instanceId = args.getOrNull(1)?.trim()
                if (instanceId.isNullOrEmpty()) {
                    sender.sendLang(ctx.plugin, "command.usage")
                    return true
                }
                runDeleteInstance(sender, instanceId)
            }
            "team" -> {
                val sub = args.getOrNull(1)?.trim()?.lowercase() ?: run {
                    sender.sendLang(ctx.plugin, "command.usage")
                    return true
                }
                when (sub) {
                    "invite" -> {
                        val targetName = args.getOrNull(2)?.trim()
                        if (targetName.isNullOrEmpty()) {
                            sender.sendLang(ctx.plugin, "command.usage")
                            return true
                        }
                        TeamCommand.runInvite(sender as? org.bukkit.entity.Player ?: run {
                            sender.sendLang(ctx.plugin, "command.player-only")
                            return true
                        }, targetName)
                    }
                    "info" -> TeamCommand.runInfo(sender as? org.bukkit.entity.Player ?: run {
                        sender.sendLang(ctx.plugin, "command.player-only")
                        return true
                    })
                    else -> sender.sendLang(ctx.plugin, "command.usage")
                }
            }
            else -> sender.sendLang(ctx.plugin, "command.usage")
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> {
        val ctx = DungeonContext.get() ?: return emptyList()
        when (args.size) {
            1 -> {
                val prefix = args[0].lowercase()
                return subCommands.filter { it.startsWith(prefix) }
            }
            2 -> {
                val prefix = args[1].lowercase()
                when (args[0].lowercase()) {
                    "enter", "delete" -> {
                        val instanceIds = ctx.dungeonInstanceManager.getAllInstances().map { it.getFullId() }
                        return instanceIds.filter { it.lowercase().startsWith(prefix) }
                    }
                    "cost", "create" -> {
                        val dungeonIds = ctx.configManager.loadDungeonConfigs().keys.toList()
                        return dungeonIds.filter { it.lowercase().startsWith(prefix) }
                    }
                    "random" -> {
                        // 使用第一个可用地牢ID获取lootItems的品级列表
                        val dungeonId = ctx.configManager.loadDungeonConfigs().keys.firstOrNull() ?: return emptyList()
                        val tiers = ctx.configManager.getLootItems(dungeonId).map { it.tier }.distinct()
                        return tiers.filter { it.lowercase().startsWith(prefix) }
                    }
                    "reload", "reset" -> {
                        val dungeonIds = ctx.configManager.loadDungeonConfigs().keys.toList()
                        val choices = dungeonIds + "ALL"
                        return choices.filter { it.lowercase().startsWith(prefix) }
                    }
                    "level", "setlevel" -> {
                        val names = ctx.plugin.server.onlinePlayers.map { it.name }.filter { it.lowercase().startsWith(prefix) }
                        return names
                    }
                    "team" -> {
                        val teamSubCommands = listOf("invite", "accept", "decline", "info")
                        return teamSubCommands.filter { it.startsWith(prefix) }
                    }
                    "downed" -> {
                        val downedSubCommands = listOf("help", "giveup")
                        return downedSubCommands.filter { it.startsWith(prefix) }
                    }
                }
            }
            3 -> {
                if (args[0].lowercase() == "setlevel") {
                    val prefix = args[2]
                    return (0..5).map { it.toString() }.filter { it.startsWith(prefix) }
                }
                if (args[0].lowercase() == "team" && args[1].lowercase() == "invite") {
                    val prefix = args[2]
                    return ctx.plugin.server.onlinePlayers.map { it.name }.filter { it.lowercase().startsWith(prefix) }
                }
            }
        }
        return emptyList()
    }
}
