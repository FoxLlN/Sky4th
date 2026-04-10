package sky4th.dungeon.command.impl

import sky4th.core.api.DungeonAPI
import sky4th.core.SkyCore
import sky4th.dungeon.util.LanguageUtil.sendLang
import sky4th.dungeon.command.DungeonContext
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/** 子命令：/dungeon setlevel <玩家> <等级> — 设置指定玩家的地牢科技树等级（需 dungeon.admin） */
fun runSetLevel(sender: CommandSender, targetName: String, levelArg: String) {
    val ctx = DungeonContext.get() ?: return
    if (!sender.hasPermission("dungeon.admin")) {
        sender.sendLang(ctx.plugin, "command.no-permission")
        return
    }
    if (!DungeonAPI.isAvailable()) {
        sender.sendLang(ctx.plugin, "command.level.tech-unavailable")
        return
    }
    val level = levelArg.toIntOrNull()
    if (level == null || level !in 0..5) {
        sender.sendLang(ctx.plugin, "command.level.invalid-level")
        return
    }
    val target = Bukkit.getPlayer(targetName) ?: Bukkit.getOfflinePlayer(targetName)
    val uuid = target.uniqueId
    val service = SkyCore.getDungeonTechService() ?: run {
        sender.sendLang(ctx.plugin, "command.level.tech-unavailable")
        return
    }
    service.setTechLevel(uuid, level)
    val name = (target as? Player)?.name ?: target.name
    sender.sendLang(ctx.plugin, "command.level.set-success", "player" to (name ?: uuid.toString()), "level" to level)
}
