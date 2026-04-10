package sky4th.dungeon.player

import sky4th.core.api.DungeonAPI
import sky4th.dungeon.config.ConfigManager
import sky4th.dungeon.command.DungeonContext
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent

/**
 * 地牢内「对玩家伤害减免」：按受害者科技树等级减少来自其他玩家的伤害。
 */
class TechLevelDamageListener(
    private val configManager: ConfigManager,
    private val playerManager: PlayerManager
) : Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val victim = event.entity as? Player ?: return
        val ctx = DungeonContext.get() ?: return
        if (!ctx.playerManager.isPlayerInDungeon(victim)) return
        val attacker: Player? = when (val damager = event.damager) {
            is Player -> damager
            is Projectile -> damager.shooter as? Player
            else -> null
        }
        if (attacker == null) return
        if (!ctx.playerManager.isPlayerInDungeon(attacker)) return
        if (!DungeonAPI.isAvailable()) return
        var level = DungeonAPI.getTechLevel(victim)
        if (level < 0) level = 0
        // 获取玩家当前所在的地牢ID
        val dungeonId = ctx.playerManager.getCurrentDungeonId(victim) ?: return
        // 获取对应地牢配置中的科技等级加成
        val dungeonConfigs = configManager.loadDungeonConfigs()
        val dungeonConfig = dungeonConfigs[dungeonId]
        val bonus = if (dungeonConfig != null && level < dungeonConfig.techLevelBonuses.size) {
            dungeonConfig.techLevelBonuses[level] ?: sky4th.dungeon.config.TechLevelBonus()
        } else {
            sky4th.dungeon.config.TechLevelBonus() // 默认加成
        }
        if (bonus.playerDamageReduction <= 0) return
        val factor = 1.0 - bonus.playerDamageReduction
        event.damage = event.damage * factor
    }
}
