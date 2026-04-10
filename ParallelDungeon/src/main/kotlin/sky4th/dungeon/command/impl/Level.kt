package sky4th.dungeon.command.impl

import sky4th.core.api.DungeonAPI
import sky4th.dungeon.util.LanguageUtil.sendLang
import sky4th.dungeon.command.DungeonContext
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/** 子命令：/dungeon level [玩家] — 查看自己或指定玩家的地牢科技树等级 */
fun runLevel(sender: CommandSender, targetName: String?) {
    val ctx = DungeonContext.get() ?: return
    if (!DungeonAPI.isAvailable()) {
        sender.sendLang(ctx.plugin, "command.level.tech-unavailable")
        return
    }
    val (targetUuid, targetLabel) = if (targetName.isNullOrBlank()) {
        if (sender !is Player) {
            sender.sendLang(ctx.plugin, "command.player-only")
            return
        }
        sender.uniqueId to null
    } else {
        val target = Bukkit.getPlayer(targetName) ?: Bukkit.getOfflinePlayer(targetName)
        target.uniqueId to (target.name ?: targetName)
    }
    val level = DungeonAPI.getTechLevel(targetUuid)
    val displayLevel = if (level < 0) 0 else level
    val isSelf = sender is Player && sender.uniqueId == targetUuid
    if (isSelf || targetLabel == null) {
        sender.sendLang(ctx.plugin, "command.level.show-self", "level" to displayLevel)
    } else {
        sender.sendLang(ctx.plugin, "command.level.show-other", "player" to targetLabel, "level" to displayLevel)
    }
}
