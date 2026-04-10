package sky4th.dungeon.loadout.equipment

import sky4th.dungeon.command.DungeonContext
import sky4th.dungeon.Dungeon
import sky4th.dungeon.util.LanguageUtil.sendLangSys
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.plugin.java.JavaPlugin

/**
 * 游侠套装效果（同类取最高倍率，不叠加）：
 * - 2件套：远程伤害 +10%
 * - 4件套：远程伤害 +25%
 * - 主动：蹲下 5 秒后下次远程伤害 +60%，每局 2 次
 */
class YouxiaSetListener(plugin: JavaPlugin) : BaseLoadoutSetListener(plugin, "ranger") {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        if (damager !is Projectile) return
        val shooter = damager.shooter
        if (shooter !is Player) return
        val player = shooter
        val ctx = DungeonContext.get() ?: return
        if (!ctx.playerManager.isPlayerInDungeon(player)) return
        val originalDamage = event.damage
        val count = countArmor(player)
        val passiveMult = when {
            count >= 4 -> 1.50
            count >= 2 -> 1.20
            else -> 1.0
        }
        val hadBoost = LoadoutSetSkillState.youxiaRangedBoost.remove(player.uniqueId)
        if (hadBoost) Dungeon.instance.refreshSidebar(player)
        val activeMult = 1.60 // 主动 +60% 伤害
        val damage = originalDamage * if (hadBoost) activeMult else passiveMult
        event.damage = damage
    }

    override fun onSneakActiveSkill(event: PlayerToggleSneakEvent) {
        if (!event.isSneaking) return
        val player = event.player
        val ctx = DungeonContext.get() ?: return
        if (!ctx.playerManager.isPlayerInDungeon(player)) return
        if (LoadoutSetSkillState.getYouxiaSkillRemaining(player.uniqueId) <= 0) {
            player.sendLangSys(plugin, "loadout.$setId.cooldown")
            return
        }
        if (LoadoutSetSkillState.youxiaRangedBoost.contains(player.uniqueId)) {
            return
        }
        if (!hasFourPiece(player)) return

        if (!LoadoutSetSkillState.canUseSetSkill(player.uniqueId, setId)) {
            player.sendLangSys(plugin, "loadout.$setId.wrong-set")
            return
        }

        scheduleSneakSkill(player, ctx,
            recheck = {
                LoadoutSetSkillState.canUseSetSkill(player.uniqueId, setId) &&
                LoadoutSetSkillState.getYouxiaSkillRemaining(player.uniqueId) > 0 &&
                    !LoadoutSetSkillState.youxiaRangedBoost.contains(player.uniqueId) && hasFourPiece(player)
            },
            onTrigger = onTrigger@{
                if (!LoadoutSetSkillState.useYouxiaSkill(player.uniqueId)) return@onTrigger
                LoadoutSetSkillState.recordSetSkillUse(player.uniqueId, setId)
                LoadoutSetSkillState.youxiaRangedBoost.add(player.uniqueId)
                player.sendLangSys(plugin, "loadout.$setId.active-used")
                Dungeon.instance.refreshSidebar(player)
            }
        )
    }

    override fun onLeaveDungeon(player: Player) {
        LoadoutSetSkillState.clearYouxiaSkillUsed(player.uniqueId)
        LoadoutSetSkillState.clearPlayerSetSkill(player.uniqueId)
    }

    override fun refreshPassive(player: Player) {
        return
    }
}
