package sky4th.dungeon.command.impl

import sky4th.core.api.EconomyAPI
import sky4th.dungeon.util.LanguageUtil.sendLang
import sky4th.dungeon.command.DungeonContext
import org.bukkit.command.CommandSender

/** 子命令：/dungeon cost <地牢名> [新费用] — 查看或设置入场费用（设置需 dungeon.admin） */
fun runCost(sender: CommandSender, dungeonName: String, newCost: Double?) {
    val ctx = DungeonContext.getOrThrow()

    // 验证地牢配置是否存在
    if (!ctx.dungeonInstanceManager.hasDungeonConfig(dungeonName)) {
        sender.sendLang(ctx.plugin, "command.invalid-dungeon", "dungeon" to dungeonName)
        return
    }

    val config = ctx.dungeonInstanceManager.getDungeonConfig(dungeonName)!!

    if (newCost != null) {
        if (!sender.hasPermission("dungeon.admin")) {
            sender.sendLang(ctx.plugin, "command.no-permission")
            return
        }
        val value = newCost.coerceAtLeast(0.0)
        ctx.plugin.config.set("dungeons.$dungeonName.cost", value)
        ctx.plugin.saveConfig()
        ctx.configManager.reload()
        sender.sendLang(ctx.plugin, "command.cost-set", "dungeon" to dungeonName, "cost" to EconomyAPI.format(value))
    } else {
        val current = config.cost
        sender.sendLang(ctx.plugin, "command.cost-show", "dungeon" to dungeonName, "cost" to EconomyAPI.format(current))
    }
}
