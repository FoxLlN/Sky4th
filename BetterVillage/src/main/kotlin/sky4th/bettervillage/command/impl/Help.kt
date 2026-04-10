package sky4th.bettervillage.command.impl

import org.bukkit.command.CommandSender
import sky4th.bettervillage.command.VillageContext

/** 子命令：/village help */
fun runHelp(sender: CommandSender) {
    val ctx = VillageContext.getOrThrow()
    VillageContext.showHelp(sender, ctx)
}
