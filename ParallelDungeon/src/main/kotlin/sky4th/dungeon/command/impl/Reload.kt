package sky4th.dungeon.command.impl

import sky4th.dungeon.Dungeon
import sky4th.dungeon.util.LanguageUtil.sendLang
import sky4th.dungeon.command.DungeonContext
import org.bukkit.command.CommandSender

/** 子命令：/dungeon reload <地牢名|ALL> */
fun runReload(sender: CommandSender, dungeonNameOrAll: String) {
    val ctx = DungeonContext.getOrThrow()
    if (!sender.hasPermission("dungeon.admin")) {
        sender.sendLang(ctx.plugin, "command.no-permission")
        return
    }

    val isAll = dungeonNameOrAll.equals("ALL", ignoreCase = true)

    if (!isAll && !ctx.dungeonInstanceManager.hasDungeonConfig(dungeonNameOrAll)) {
        sender.sendLang(ctx.plugin, "command.invalid-dungeon", "dungeon" to dungeonNameOrAll)
        return
    }

    if (ctx.plugin is Dungeon) {
        ctx.plugin.reloadPluginConfig()

        // 重新初始化地牢实例管理器
        ctx.dungeonInstanceManager.initialize()

        val msgKey = if (isAll) "command.reload-success-all" else "command.reload-success"
        sender.sendLang(ctx.plugin, msgKey, "dungeon" to dungeonNameOrAll)
    } else {
        sender.sendLang(ctx.plugin, "command.reload-failed")
    }
}
