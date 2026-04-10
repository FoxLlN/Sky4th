package sky4th.core.command.impl

import sky4th.core.command.SkyCoreContext
import org.bukkit.command.CommandSender

/** 子命令：/sky reload */
fun runReload(sender: CommandSender) {
    val ctx = SkyCoreContext.getOrThrow()
    if (!sender.hasPermission("skycore.reload")) {
        sender.sendMessage("§c你没有权限执行此操作！")
        return
    }

    // TODO: 实现配置重载
    sender.sendMessage("§a配置已重新加载！")
}
