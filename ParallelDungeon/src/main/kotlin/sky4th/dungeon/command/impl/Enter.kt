package sky4th.dungeon.command.impl

import sky4th.dungeon.util.LanguageUtil.sendLang
import sky4th.dungeon.command.DungeonContext
import sky4th.dungeon.loadout.screen.LoadoutUI
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/** 子命令：/dungeon enter <实例ID>（仅玩家）。必须指定完整实例ID，先打开配装界面，确认后再扣费并进入地牢。 */
fun runEnter(sender: CommandSender, fullInstanceId: String) {
    val ctx = DungeonContext.getOrThrow()
    if (sender !is Player) {
        return
    }

    // 验证实例ID格式（必须包含地牢ID）
    val parts = fullInstanceId.split("_")
    if (parts.size < 2) {
        sender.sendLang(ctx.plugin, "command.enter.invalid-instance-format")
        return
    }

    val dungeonId = parts[0]
    val instanceId = parts.subList(1, parts.size).joinToString("_")

    // 验证地牢配置是否存在
    if (!ctx.dungeonInstanceManager.hasDungeonConfig(dungeonId)) {
        sender.sendLang(ctx.plugin, "command.invalid-dungeon", "dungeon" to dungeonId)
        return
    }

    // 验证实例是否存在
    val instance = ctx.dungeonInstanceManager.getInstance(fullInstanceId)
    if (instance == null) {
        sender.sendLang(ctx.plugin, "command.enter.instance-not-found", "instanceId" to fullInstanceId)
        return
    }

    // 检查实例是否已满
    val config = ctx.dungeonInstanceManager.getDungeonConfig(dungeonId)!!
    if (instance.getPlayerCount() >= config.maxPlayersPerInstance) {
        sender.sendLang(ctx.plugin, "command.enter.instance-full", "instanceId" to fullInstanceId)
        return
    }

    // 检查玩家是否已经在这个实例进入过
    if (ctx.playerManager.hasPlayerEnteredInstance(sender.uniqueId, fullInstanceId)) {
        sender.sendLang(ctx.plugin, "dungeon.already-entered")
        return
    }

    if (ctx.playerManager.isPlayerInDungeon(sender)) {
        sender.sendLang(ctx.plugin, "command.already-in-dungeon")
        return
    }

    // 打开配装界面，传入完整实例ID
    LoadoutUI.open(sender, dungeonId, instanceId)
}
