package com.sky4th.equipment.command.impl

import com.sky4th.equipment.EquipmentAffix
import com.sky4th.equipment.monitor.PerformanceMonitor
import com.sky4th.equipment.util.LanguageUtil.sendLang
import org.bukkit.command.CommandSender

/**
 * 子命令：/equipment report
 */
fun runReport(plugin: EquipmentAffix, sender: CommandSender) {
    sender.sendLang(plugin, "command.report.header")

    val metrics = PerformanceMonitor.getAllMetrics()
    if (metrics.isEmpty()) {
        sender.sendLang(plugin, "command.report.no-data")
        return
    }

    metrics.forEach { (operation, data) ->
        val avgTime = data.getAverage() / 1_000_000.0 // 转换为毫秒
        val maxTime = data.getMax() / 1_000_000.0
        val minTime = data.getMin() / 1_000_000.0
        val count = data.getCount()

        sender.sendLang(plugin, "command.report.operation", "operation" to operation)
        sender.sendLang(plugin, "command.report.avg-time", "avg_time" to String.format("%.3f", avgTime))
        sender.sendLang(plugin, "command.report.max-time", "max_time" to String.format("%.3f", maxTime))
        sender.sendLang(plugin, "command.report.min-time", "min_time" to String.format("%.3f", minTime))
        sender.sendLang(plugin, "command.report.count", "count" to count)
        sender.sendMessage("")
    }

    sender.sendLang(plugin, "command.report.footer")
}
