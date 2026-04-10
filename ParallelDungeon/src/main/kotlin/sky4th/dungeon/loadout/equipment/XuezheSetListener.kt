package sky4th.dungeon.loadout.equipment

import sky4th.dungeon.command.DungeonContext
import sky4th.dungeon.Dungeon
import sky4th.dungeon.util.LanguageUtil.sendLangSys
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.plugin.java.JavaPlugin

/**
 * 学者套装效果：
 * - 2件套：幸运 I（安慰剂）、搜索时间 -2 秒（在 ContainerSearchManager 中应用）
 * - 4件套：蹲下 5 秒后刷新最近已搜索容器，可重新搜索，每局 2 次
 */
class XuezheSetListener(plugin: JavaPlugin) : BaseLoadoutSetListener(plugin, "scholar") {

    private val refreshRange = 5.0
    private val xuezheLuckEffect = PotionEffect(PotionEffectType.LUCK, Int.MAX_VALUE, 0, false, false)

    override fun onSneakActiveSkill(event: PlayerToggleSneakEvent) {
        if (!event.isSneaking) return
        val player = event.player
        val ctx = DungeonContext.get() ?: return
        if (!ctx.playerManager.isPlayerInDungeon(player)) return
        if (LoadoutSetSkillState.getXuezheSkillRemaining(player.uniqueId) <= 0) {
            player.sendLangSys(plugin, "loadout.$setId.cooldown")
            return
        }
        if (!hasFourPiece(player)) {
            return
        }
        if (!LoadoutSetSkillState.canUseSetSkill(player.uniqueId, setId)) {
            player.sendLangSys(plugin, "loadout.$setId.wrong-set")
            return
        }

        scheduleSneakSkill(player, ctx,
            recheck = {
                LoadoutSetSkillState.canUseSetSkill(player.uniqueId, setId) &&
                LoadoutSetSkillState.getXuezheSkillRemaining(player.uniqueId) > 0 && hasFourPiece(player)
            },
            onTrigger = onTrigger@{
                val searchManager = ctx.containerSearchManager
                val nearestSearched = searchManager.findNearestSearchedContainer(player, refreshRange)
                if (nearestSearched == null) {
                    player.sendLangSys(plugin, "loadout.$setId.no-container")
                    return@onTrigger
                }
                if (!LoadoutSetSkillState.useXuezheSkill(player.uniqueId)) return@onTrigger
                LoadoutSetSkillState.recordSetSkillUse(player.uniqueId, setId)
                searchManager.resetContainer(player, nearestSearched)
                player.sendLangSys(plugin, "loadout.$setId.active-used")
                Dungeon.instance.refreshSidebar(player)
            }
        )
    }

    override fun onLeaveDungeon(player: Player) {
        LoadoutSetSkillState.clearXuezheSkillUsed(player.uniqueId)
        LoadoutSetSkillState.clearPlayerSetSkill(player.uniqueId)
    }

    override fun refreshPassive(player: Player) {
        if (hasTwoPiece(player)) {
            player.addPotionEffect(xuezheLuckEffect)
        } else {
            player.removePotionEffect(PotionEffectType.LUCK)
        }
    }
}
