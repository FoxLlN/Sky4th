package sky4th.dungeon.command.impl

import sky4th.dungeon.util.LanguageUtil.sendLang
import sky4th.dungeon.command.DungeonContext
import org.bukkit.command.CommandSender

/**
 * 子命令：/dungeon delete <实例ID>
 * 删除指定的地牢实例
 */
fun runDeleteInstance(sender: CommandSender, instanceId: String) {
    val ctx = DungeonContext.getOrThrow()

    if (!sender.hasPermission("dungeon.admin")) {
        sender.sendLang(ctx.plugin, "command.no-permission")
        return
    }

    // 检查实例是否存在
    val instance = ctx.dungeonInstanceManager.getInstance(instanceId)
    if (instance == null) {
        sender.sendLang(ctx.plugin, "command.delete.instance-not-found", "instanceId" to instanceId)
        return
    }

    // 获取实例中的玩家数量
    val playerCount = instance.getPlayerCount()
    if (playerCount > 0) {
        sender.sendLang(ctx.plugin, "command.delete.has-players",
            "instanceId" to instanceId,
            "players" to playerCount.toString()
        )
        return
    }

    // 删除实例
    val success = ctx.dungeonInstanceManager.destroyInstance(instanceId)

    if (success) {
        sender.sendLang(ctx.plugin, "command.delete.success",
            "instanceId" to instanceId,
            "dungeon" to instance.config.id
        )
    } else {
        sender.sendLang(ctx.plugin, "command.delete.failed", "instanceId" to instanceId)
    }
}
