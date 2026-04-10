package sky4th.dungeon.command.impl

import sky4th.dungeon.util.LanguageUtil.sendLang
import sky4th.dungeon.command.DungeonContext
import sky4th.dungeon.loadout.shop.LoadoutShopAPI
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/** 子命令：/dungeon shop — 打开配装商店（仅玩家） */
fun runShop(sender: CommandSender) {
    val ctx = DungeonContext.getOrThrow()
    if (sender !is Player) {
        sender.sendLang(ctx.plugin, "command.player-only")
        return
    }
    LoadoutShopAPI.openShop(sender)
}
