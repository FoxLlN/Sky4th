package com.sky4th.equipment.command.impl

import com.sky4th.equipment.EquipmentAffix
import com.sky4th.equipment.monitor.PerformanceMonitor
import com.sky4th.equipment.util.LanguageUtil.sendLang
import org.bukkit.command.CommandSender

/**
 * 子命令：/equipment clear
 */
fun runClear(plugin: EquipmentAffix, sender: CommandSender) {
    PerformanceMonitor.clear()
    sender.sendLang(plugin, "command.clear.success")
}
