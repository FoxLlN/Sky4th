package sky4th.dungeon.command.impl

import sky4th.dungeon.command.DungeonContext
import sky4th.dungeon.util.LanguageUtil.sendLang
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/** 子命令：/dungeon exit（仅玩家） */
fun runExit(sender: CommandSender) {
    val ctx = DungeonContext.getOrThrow()
    if (sender !is Player) {
        sender.sendLang(ctx.plugin, "command.player-only")
        return
    }
    if (!ctx.playerManager.isPlayerInDungeon(sender)) {
        sender.sendLang(ctx.plugin, "command.not-in-dungeon")
        return
    }
    ctx.playerManager.teleportFromDungeon(sender, true)
}
