package sky4th.core.command.impl

import sky4th.core.SkyCore
import sky4th.core.command.SkyCoreContext
import org.bukkit.command.CommandSender

/** 子命令：/sky status */
fun runStatus(sender: CommandSender) {
    val ctx = SkyCoreContext.getOrThrow()
    if (!sender.hasPermission("skycore.status")) {
        sender.sendMessage("§c你没有权限执行此操作！")
        return
    }

    sender.sendMessage("§6===== §b系统状态 §6=====")
    sender.sendMessage("§aAPI 已初始化: §e${if (SkyCore.isInitialized()) "是" else "否"}")
    sender.sendMessage("§a数据库: §e${if (SkyCore.isDatabaseAvailable()) "可用" else "不可用"}")
    sender.sendMessage("§a玩家服务: §e${if (SkyCore.isPlayerServiceAvailable()) "可用" else "不可用"}")
    sender.sendMessage("§a经济系统: §e${if (SkyCore.isEconomyAvailable()) "已注册" else "未注册"}")
}
