package sky4th.dungeon.command.impl

import sky4th.dungeon.command.DungeonContext
import sky4th.dungeon.util.LanguageUtil.sendLang
import org.bukkit.command.CommandSender

/**
 * 子命令：/dungeon list
 * 列出所有地牢配置及其当前实例状态
 */
fun runList(sender: CommandSender) {
    val ctx = DungeonContext.getOrThrow()

    // 获取所有地牢配置
    val dungeonConfigs = ctx.dungeonInstanceManager.getAllDungeonConfigs()

    if (dungeonConfigs.isEmpty()) {
        sender.sendLang(ctx.plugin, "command.list.no-dungeons")
        return
    }

    // 显示地牢列表头部
    sender.sendLang(ctx.plugin, "command.list.header")

    // 遍历每个地牢配置
    for ((dungeonId, config) in dungeonConfigs) {
        // 获取该地牢的所有实例
        val instances = ctx.dungeonInstanceManager.getDungeonInstances(dungeonId)

        // 显示地牢基本信息
        sender.sendLang(ctx.plugin, "command.list.dungeon-info",
            "id" to dungeonId,
            "name" to config.displayName,
            "instances" to instances.size.toString(),
            "maxInstances" to config.maxInstances.toString(),
            "cost" to config.cost.toString()
        )

        // 显示每个实例的详细信息
        for (instance in instances) {
            sender.sendLang(ctx.plugin, "command.list.instance-info",
                "instanceId" to instance.getFullId(),
                "players" to instance.getPlayerCount().toString(),
                "maxPlayers" to config.maxPlayersPerInstance.toString(),
                "status" to if (instance.isClosed()) "已关闭" else "运行中"
            )
        }
    }

    // 显示列表尾部
    sender.sendLang(ctx.plugin, "command.list.footer")
}
