package sky4th.bettermc.command.impl

import sky4th.bettermc.command.BetterMCContext
import org.bukkit.command.CommandSender

/** 子命令：/bettermc help */
fun runHelp(sender: CommandSender) {
    val ctx = BetterMCContext.getOrThrow()
    BetterMCContext.showHelp(sender, ctx)
}
