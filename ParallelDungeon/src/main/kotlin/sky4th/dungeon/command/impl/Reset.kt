package sky4th.dungeon.command.impl

import org.bukkit.Bukkit
import sky4th.dungeon.util.LanguageUtil.sendLang
import sky4th.dungeon.command.DungeonContext
import org.bukkit.command.CommandSender

/** 子命令：/dungeon reset <地牢名|ALL> */
fun runReset(sender: CommandSender, dungeonNameOrAll: String) {
    val ctx = DungeonContext.getOrThrow()
    if (!sender.hasPermission("dungeon.admin")) {
        sender.sendLang(ctx.plugin, "command.no-permission")
        return
    }

    val isAll = dungeonNameOrAll.equals("ALL", ignoreCase = true)

    if (isAll) {
        // 重置所有地牢
        val allDungeons = ctx.dungeonInstanceManager.getAllDungeonConfigs()
        var successCount = 0
        var failCount = 0

        for (dungeonId in allDungeons.keys) {
            if (resetSingleDungeon(sender, dungeonId)) {
                successCount++
            } else {
                failCount++
            }
        }

        sender.sendLang(ctx.plugin, "command.reset-all-result",
            "success" to successCount.toString(),
            "failed" to failCount.toString()
        )
    } else {
        // 重置单个地牢
        if (!ctx.dungeonInstanceManager.hasDungeonConfig(dungeonNameOrAll)) {
            sender.sendLang(ctx.plugin, "command.invalid-dungeon", "dungeon" to dungeonNameOrAll)
            return
        }

        if (!resetSingleDungeon(sender, dungeonNameOrAll)) {
            sender.sendLang(ctx.plugin, "command.reset-failed", "dungeon" to dungeonNameOrAll)
        }
    }
}

/**
 * 重置单个地牢的所有实例
 */
private fun resetSingleDungeon(sender: CommandSender, dungeonId: String): Boolean {
    val ctx = DungeonContext.getOrThrow()
    sender.sendLang(ctx.plugin, "command.reset-start", "dungeon" to dungeonId)

    // 获取该地牢的所有实例
    val instances = ctx.dungeonInstanceManager.getDungeonInstances(dungeonId)

    if (instances.isEmpty()) {
        sender.sendLang(ctx.plugin, "command.reset.no-instances", "dungeon" to dungeonId)
        return true
    }

    var successCount = 0
    var failCount = 0

    // 重置每个实例
    for (instance in instances) {
        if (ctx.dungeonInstanceManager.resetInstance(instance.getFullId())) {
            successCount++
        } else {
            failCount++
        }
    }

    sender.sendLang(ctx.plugin, "command.reset-result",
        "dungeon" to dungeonId,
        "success" to successCount.toString(),
        "failed" to failCount.toString()
    )

    return failCount == 0
}
