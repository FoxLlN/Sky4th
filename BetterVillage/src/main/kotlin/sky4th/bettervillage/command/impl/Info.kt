package sky4th.bettervillage.command.impl

import sky4th.bettervillage.BetterVillage
import sky4th.bettervillage.util.LanguageUtil.sendLang
import org.bukkit.entity.Player
import sky4th.bettervillage.manager.VillageManager
import sky4th.core.api.LanguageAPI

/** 子命令：/village info */
fun runInfo(sender: Player) {
    val village = VillageManager.getVillageByLocation(sender.location)
    if (village == null) {
        sender.sendLang(BetterVillage.instance, "command.info.no-village")
        return
    }

    // 使用语言文件中的消息
    sender.sendLang(BetterVillage.instance, "command.info.header")
    sender.sendLang(BetterVillage.instance, "command.info.id", "id" to village.id)
    sender.sendLang(BetterVillage.instance, "command.info.world", "world" to village.worldName)
    sender.sendLang(BetterVillage.instance, "command.info.level", "level" to village.level)
    sender.sendLang(BetterVillage.instance, "command.info.center-chunk", "chunkX" to village.chunkX, "chunkZ" to village.chunkZ)
    sender.sendLang(BetterVillage.instance, "command.info.bounds",
        "minX" to village.minX, "minY" to village.minY, "minZ" to village.minZ,
        "maxX" to village.maxX, "maxY" to village.maxY, "maxZ" to village.maxZ
    )

    val lastRaidTime = if (village.lastRaidTime > 0) java.util.Date(village.lastRaidTime * 1000) else "从未"
    val lastLootTime = if (village.lastLootTime > 0) java.util.Date(village.lastLootTime * 1000) else "从未"

    sender.sendLang(BetterVillage.instance, "command.info.last-raid", "time" to lastRaidTime)
    sender.sendLang(BetterVillage.instance, "command.info.last-loot", "time" to lastLootTime)
    sender.sendLang(BetterVillage.instance, "command.info.allied-teams", "count" to village.alliedTeams.size)
    sender.sendLang(BetterVillage.instance, "command.info.hostile-teams", "count" to village.hostileTeams.size)

    // 显示村民统计信息
    sender.sendLang(BetterVillage.instance, "command.info.villager-stats-header")
    sender.sendLang(BetterVillage.instance, "command.info.villager-total", "count" to village.getTotalVillagerCount())
    sender.sendLang(BetterVillage.instance, "command.info.villager-baby", "count" to village.babyVillagerCount)
    sender.sendLang(BetterVillage.instance, "command.info.villager-adult", "count" to village.getAdultVillagerCount())
    sender.sendLang(BetterVillage.instance, "command.info.villager-unemployed", "count" to village.unemployedVillagerCount)
    sender.sendLang(BetterVillage.instance, "command.info.villager-employed", "count" to village.getEmployedVillagerCount())
    sender.sendLang(BetterVillage.instance, "command.info.villager-levels",
        "level1" to village.level1VillagerCount,
        "level2" to village.level2VillagerCount,
        "level3" to village.level3VillagerCount,
        "level4" to village.level4VillagerCount,
        "level5" to village.level5VillagerCount
    )
}
