package sky4th.core.command.impl

import sky4th.core.command.SkyCoreContext
import org.bukkit.command.CommandSender

/** 子命令：/sky help */
fun runHelp(sender: CommandSender) {
    val ctx = SkyCoreContext.getOrThrow()
    SkyCoreContext.showHelp(sender, ctx)
}
