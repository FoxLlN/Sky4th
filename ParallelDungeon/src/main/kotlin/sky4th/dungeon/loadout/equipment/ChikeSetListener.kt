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
 * 斥候套装效果：
 * - 2件套：速度 I
 * - 4件套：蹲下 5 秒后下一次搜索高品质物品概率提升，每局 2 次；激活中不可重复使用
 */
class ChikeSetListener(plugin: JavaPlugin) : BaseLoadoutSetListener(plugin, "scout") {

    private val chikeSpeedEffect = PotionEffect(PotionEffectType.SPEED, Int.MAX_VALUE, 0, false, false)

    override fun onSneakActiveSkill(event: PlayerToggleSneakEvent) {
        if (!event.isSneaking) return
        val player = event.player
        val ctx = DungeonContext.get() ?: return
        if (!ctx.playerManager.isPlayerInDungeon(player)) return
        if (LoadoutSetSkillState.getChikeSkillRemaining(player.uniqueId) <= 0) {
            player.sendLangSys(plugin, "loadout.$setId.cooldown")
            return
        }
        if (LoadoutSetSkillState.chikeQualityBoost.contains(player.uniqueId)) {
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
                LoadoutSetSkillState.getChikeSkillRemaining(player.uniqueId) > 0 &&
                    !LoadoutSetSkillState.chikeQualityBoost.contains(player.uniqueId) && hasFourPiece(player)
            },
            onTrigger = onTrigger@{
                if (!LoadoutSetSkillState.useChikeSkill(player.uniqueId)) return@onTrigger
                LoadoutSetSkillState.recordSetSkillUse(player.uniqueId, setId)
                LoadoutSetSkillState.chikeQualityBoost.add(player.uniqueId)
                player.sendLangSys(plugin, "loadout.$setId.active-used")
                Dungeon.instance.refreshSidebar(player)
            }
        )
    }

    override fun onLeaveDungeon(player: Player) {
        LoadoutSetSkillState.clearChikeSkillUsed(player.uniqueId)
        LoadoutSetSkillState.clearPlayerSetSkill(player.uniqueId)
    }

    override fun refreshPassive(player: Player) {
        if (hasTwoPiece(player)) {
            player.addPotionEffect(chikeSpeedEffect)
        } else {
            player.removePotionEffect(PotionEffectType.SPEED)
        }
    }
}
