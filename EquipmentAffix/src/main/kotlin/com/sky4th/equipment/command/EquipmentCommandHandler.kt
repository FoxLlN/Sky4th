package com.sky4th.equipment.command

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import com.sky4th.equipment.EquipmentAffix
import com.sky4th.equipment.command.impl.runGive
import com.sky4th.equipment.command.impl.runHelp
import com.sky4th.equipment.command.impl.runReport
import com.sky4th.equipment.command.impl.runClear
import com.sky4th.equipment.command.impl.runMenu
import com.sky4th.equipment.registry.EquipmentRegistry
import com.sky4th.equipment.util.LanguageUtil.sendLang

/**
 * 装备系统命令处理器
 */
class EquipmentCommandHandler(private val plugin: EquipmentAffix) : CommandExecutor, TabCompleter {

    private val subCommands = listOf("give", "help", "report", "clear", "menu")

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) {
            runHelp(plugin, sender)
            return true
        }
        
        if (sender !is Player) {
            sender.sendLang(plugin, "command.player-only")
            return true
        }

        val subCommand = args[0].lowercase()

        when (subCommand) {
            "help" -> { 
                if (!sender.hasPermission("equipment.admin")) {
                    sender.sendLang(plugin, "command.no-permission")
                    return true
                }
                runHelp(plugin, sender)
            }
            "give" -> {
                if (!sender.hasPermission("equipment.admin")) {
                    sender.sendLang(plugin, "command.no-permission")
                    return true
                }
                runGive(plugin, sender, args)
            }
            "report" -> {
                if (!sender.hasPermission("equipment.admin")) {
                    sender.sendLang(plugin, "command.no-permission")
                    return true
                }
                runReport(plugin, sender)
            }
            "clear" -> {
                if (!sender.hasPermission("equipment.admin")) {
                    sender.sendLang(plugin, "command.no-permission")
                    return true
                }
                runClear(plugin, sender)
            }
            "menu" -> {
                runMenu(plugin, sender, args)
            }
            else -> {
                sender.sendLang(plugin, "command.unknown", "command" to subCommand)
                runHelp(plugin, sender)
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
                    "give" -> {
                        // 返回 "affix" 和所有装备ID
                        (listOf("affix") + EquipmentRegistry.getAllEquipmentIds())
                            .filter { it.startsWith(args[1], ignoreCase = true) }
                    }
                    else -> emptyList()
                }
            }
            3 -> {
                // 当用户输入 /equipment give affix <词条名> 时，提供词条ID的自动补全
                when (args[0].lowercase()) {
                    "give" -> {
                        if (args[1].lowercase() == "affix") {
                            com.sky4th.equipment.loader.AffixTemplateLoader.getAllAffixIds()
                                .filter { it.startsWith(args[2], ignoreCase = true) }
                        } else {
                            emptyList()
                        }
                    }
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }
    }
}
