package sky4th.core.command.impl

import sky4th.core.SkyCore
import sky4th.core.command.SkyCoreContext
import org.bukkit.command.CommandSender

/** 子命令：/sky info */
fun runInfo(sender: CommandSender) {
    val ctx = SkyCoreContext.getOrThrow()
    if (!sender.hasPermission("skycore.info")) {
        sender.sendMessage("§c你没有权限执行此操作！")
        return
    }

    val instance = SkyCore.getInstance()
    sender.sendMessage("§a版本: §e${instance?.pluginMeta?.version ?: "未知"}")
    sender.sendMessage("§a作者: §e${instance?.pluginMeta?.authors?.joinToString() ?: "未知"}")
    sender.sendMessage("§a已初始化: §e${if (SkyCore.isInitialized()) "是" else "否"}")
}
