package sky4th.dungeon.command.impl

import sky4th.dungeon.command.DungeonContext
import sky4th.dungeon.util.LanguageUtil.sendLang
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/** 子命令：/dungeon random [稀有度]（仅玩家，随机抽一个物品放入背包） */
fun runRandom(sender: CommandSender, tier: String?) {
    val ctx = DungeonContext.getOrThrow()
    if (sender !is Player) {
        sender.sendLang(ctx.plugin, "command.player-only")
        return
    }

    // 获取玩家当前所在的地牢
    val currentDungeonId = ctx.playerManager.getCurrentDungeonId(sender)
    if (currentDungeonId == null) {
        sender.sendLang(ctx.plugin, "command.random-not-in-dungeon")
        return
    }

    val success = ctx.containerSearchManager.giveRandomLootToPlayer(sender, currentDungeonId, tier?.trim()?.takeIf { it.isNotEmpty() })
    if (success) {
        sender.sendLang(ctx.plugin, "command.random-success", "tier" to (tier ?: ""))
    } else {
        sender.sendLang(ctx.plugin, "command.random-fail", "tier" to (tier ?: ""))
    }
}
