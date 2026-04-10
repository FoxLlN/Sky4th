package sky4th.bettervillage.command.impl

import sky4th.bettervillage.BetterVillage
import sky4th.bettervillage.util.LanguageUtil.sendLang
import org.bukkit.entity.Player
import sky4th.bettervillage.manager.VillageManager

/** 子命令：/village level <等级> */
fun runLevel(sender: Player, args: Array<out String>) {
    if (args.size < 2) {
        sender.sendLang(BetterVillage.instance, "command.level.usage")
        return
    }

    val level = args[1].toIntOrNull()
    if (level == null || level < 1) {
        sender.sendLang(BetterVillage.instance, "command.level.invalid")
        return
    }

    val village = VillageManager.getVillageByLocation(sender.location)
    if (village == null) {
        sender.sendLang(BetterVillage.instance, "command.level.no-village")
        return
    }

    val success = VillageManager.updateVillageLevel(village.id, level)
    if (success) {
        sender.sendLang(BetterVillage.instance, "command.level.success", "level" to level)
    } else {
        sender.sendLang(BetterVillage.instance, "command.level.failed")
    }
}
