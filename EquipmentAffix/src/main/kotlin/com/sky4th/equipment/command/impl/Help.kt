package com.sky4th.equipment.command.impl

import com.sky4th.equipment.EquipmentAffix
import com.sky4th.equipment.util.LanguageUtil.sendLang
import org.bukkit.command.CommandSender

/**
 * 子命令：/equipment help
 */
fun runHelp(plugin: EquipmentAffix, sender: CommandSender) {
    sender.sendLang(plugin, "command.help.header")
    sender.sendLang(plugin, "command.help.give")
    sender.sendLang(plugin, "command.help.report")
    sender.sendLang(plugin, "command.help.clear")
}
