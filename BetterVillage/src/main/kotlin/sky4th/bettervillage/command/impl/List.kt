package sky4th.bettervillage.command.impl

import sky4th.bettervillage.BetterVillage
import sky4th.bettervillage.util.LanguageUtil.sendLang
import org.bukkit.entity.Player
import sky4th.bettervillage.manager.VillageManager

/** 子命令：/village list */
fun runList(sender: Player) {
    val villages = VillageManager.getVillagesByWorld(sender.world.name)
    if (villages.isEmpty()) {
        sender.sendLang(BetterVillage.instance, "command.list.empty")
        return
    }

    sender.sendLang(BetterVillage.instance, "command.list.header", "world" to sender.world.name)
    villages.forEach { village ->
        sender.sendLang(BetterVillage.instance, "command.list.item",
            "id" to village.id,
            "level" to village.level,
            "chunkX" to village.chunkX,
            "chunkZ" to village.chunkZ
        )
    }
}
