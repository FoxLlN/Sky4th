package sky4th.dungeon.command.impl

import sky4th.dungeon.util.LanguageUtil.sendLang
import sky4th.dungeon.command.DungeonContext
import sky4th.dungeon.loadout.storage.StorageUI
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/** 子命令：/dungeon storage — 直接打开仓库（仅玩家） */
fun runStorage(sender: CommandSender) {
    val ctx = DungeonContext.getOrThrow()
    if (sender !is Player) {
        sender.sendLang(ctx.plugin, "command.player-only")
        return
    }
    StorageUI.open(sender)
}
