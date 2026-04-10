package sky4th.dungeon.command.impl

import sky4th.dungeon.util.LanguageUtil.sendLang
import sky4th.dungeon.command.DungeonContext
import org.bukkit.command.CommandSender

/**
 * 子命令：/dungeon create <地牢ID>
 * 创建新的地牢实例
 */
fun runCreateInstance(sender: CommandSender, dungeonId: String) {
    val ctx = DungeonContext.getOrThrow()

    if (!sender.hasPermission("dungeon.admin")) {
        sender.sendLang(ctx.plugin, "command.no-permission")
        return
    }

    // 验证地牢配置是否存在
    if (!ctx.dungeonInstanceManager.hasDungeonConfig(dungeonId)) {
        sender.sendLang(ctx.plugin, "command.invalid-dungeon", "dungeon" to dungeonId)
        return
    }

    // 获取地牢配置
    val config = ctx.dungeonInstanceManager.getDungeonConfig(dungeonId)

    // 检查是否已达到最大实例数
    val currentInstances = ctx.dungeonInstanceManager.getDungeonInstances(dungeonId).size
    if (currentInstances >= config!!.maxInstances) {
        sender.sendLang(ctx.plugin, "command.create.max-instances-reached",
            "dungeon" to dungeonId,
            "current" to currentInstances.toString(),
            "max" to config.maxInstances.toString()
        )
        return
    }

    // 创建新实例（异步）
    ctx.dungeonInstanceManager.createDungeonInstance(dungeonId) { instance ->
        if (instance == null) {
            sender.sendLang(ctx.plugin, "command.create.failed", "dungeon" to dungeonId)
            return@createDungeonInstance
        }

        sender.sendLang(ctx.plugin, "command.create.success",
            "instanceId" to instance.getFullId(),
            "dungeon" to dungeonId,
            "dungeonName" to config.displayName
        )
    }
}
