package sky4th.dungeon.player

import sky4th.dungeon.Dungeon
import sky4th.core.api.DungeonAPI
import sky4th.core.api.LanguageAPI
import sky4th.dungeon.config.ConfigManager
import sky4th.dungeon.config.TechLevelBonus
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

/**
 * 地牢科技树等级加成：进地牢时按等级增加血量上限，离开时移除。
 */
class TechLevelBonusHandler(
    private val plugin: JavaPlugin,
    private val configManager: ConfigManager
) {
    private val modifierUuid = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f23456789012")

    fun onEnterDungeon(player: Player) {
        if (!DungeonAPI.isAvailable()) return
        var level = DungeonAPI.getTechLevel(player)
        if (level < 0) level = 0
        // 获取玩家当前所在的地牢ID
        val ctx = sky4th.dungeon.command.DungeonContext.get() ?: return
        val dungeonId = ctx.playerManager.getCurrentDungeonId(player) ?: return
        // 获取对应地牢配置中的科技等级加成
        val dungeonConfigs = configManager.loadDungeonConfigs()
        val dungeonConfig = dungeonConfigs[dungeonId]
        val bonus: TechLevelBonus = if (dungeonConfig != null && level < dungeonConfig.techLevelBonuses.size) {
            dungeonConfig.techLevelBonuses[level] ?: TechLevelBonus()
        } else {
            TechLevelBonus() // 默认加成
        }
        if (bonus.health <= 0) return
        val attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH) ?: return
        @Suppress("DEPRECATION")
        val existing = attr.modifiers.firstOrNull { it.uniqueId == modifierUuid }
        existing?.let { attr.removeModifier(it) }
        @Suppress("DEPRECATION")
        attr.addModifier(
            AttributeModifier(
                modifierUuid,
                LanguageAPI.getText(plugin, "tech-level.modifier-name"),
                bonus.health.toDouble(),
                AttributeModifier.Operation.ADD_NUMBER
            )
        )
    }

    fun onLeaveDungeon(player: Player) {
        val attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH) ?: return
        @Suppress("DEPRECATION")
        val existing = attr.modifiers.firstOrNull { it.uniqueId == modifierUuid }
        existing?.let { attr.removeModifier(it) }
    }
}
