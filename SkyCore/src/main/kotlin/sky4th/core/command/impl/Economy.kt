package sky4th.core.command.impl

import sky4th.core.api.EconomyAPI
import sky4th.core.command.SkyCoreContext
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/** 子命令：/sky economy <status|balance> */
fun runEconomy(sender: CommandSender, args: Array<out String>) {
    val ctx = SkyCoreContext.getOrThrow()
    if (!sender.hasPermission("skycore.economy")) {
        sender.sendMessage("§c你没有权限执行此操作！")
        return
    }

    if (args.size < 2) {
        sender.sendMessage("§c用法: /sky economy <status|balance>")
        return
    }

    when (args[1].lowercase()) {
        "status" -> {
            val isAvailable = EconomyAPI.isAvailable()
            sender.sendMessage("§a经济系统状态: §e${if (isAvailable) "已注册" else "未注册"}")
            if (isAvailable) {
                val provider = EconomyAPI.getProvider()
                sender.sendMessage("§a提供者: §e${provider?.javaClass?.simpleName ?: "未知"}")
                sender.sendMessage("§a货币名称: §e${EconomyAPI.getCurrencyName()}")
            }
        }
        "balance" -> {
            if (sender !is Player) {
                sender.sendMessage("§c只有玩家可以使用此命令！")
                return
            }
            val balance = EconomyAPI.getBalance(sender)
            sender.sendMessage("§a余额: §e${EconomyAPI.format(balance)} ${EconomyAPI.getCurrencyName()}")
        }
        else -> {
            sender.sendMessage("§c用法: /sky economy <status|balance>")
        }
    }
}
