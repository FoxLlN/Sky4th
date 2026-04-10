package sky4th.bettervillage.command.impl

import sky4th.bettervillage.BetterVillage
import sky4th.bettervillage.util.LanguageUtil.sendLang
import org.bukkit.entity.Player
import sky4th.bettervillage.manager.VillageManager

/** 子命令：/village nearby */
fun runNearby(sender: Player) {
    val village = VillageManager.getVillageByChunk(sender.location.chunk)
    if (village == null) {
        sender.sendLang(BetterVillage.instance, "command.nearby.no-village")
        return
    }

    val distance = village.getCenterLocation()?.distance(sender.location) ?: 0.0
    sender.sendLang(BetterVillage.instance, "command.nearby.header")
    sender.sendLang(BetterVillage.instance, "command.nearby.id", "id" to village.id)
    sender.sendLang(BetterVillage.instance, "command.nearby.level", "level" to village.level)
    sender.sendLang(BetterVillage.instance, "command.nearby.distance", "distance" to String.format("%.1f", distance))
}
