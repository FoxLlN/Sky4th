package sky4th.dungeon.command.impl

import sky4th.dungeon.command.DungeonContext
import org.bukkit.command.CommandSender

/** 子命令：/dungeon help */
fun runHelp(sender: CommandSender) {
    DungeonContext.get()?.let { DungeonContext.showHelp(sender, it) }
}
