package sky4th.dungeon.loadout.equipment

import sky4th.dungeon.command.DungeonContext
import sky4th.dungeon.Dungeon
import sky4th.dungeon.util.LanguageUtil.sendLangSys
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

/**
 * 铁壁套装效果：
 * - 2件套：最大生命值 +3 心
 * - 4件套：最大生命值 +4 心；受到远程攻击后获得 5 秒速度 I
 * - 主动：蹲下 3 秒获得 40 秒抗性 II，每局 2 次
 */
class TiebiSetListener(plugin: JavaPlugin) : BaseLoadoutSetListener(plugin, "ironwall") {

    private val tiebiModifierUuid = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890")

    /** 4件套：受到远程攻击后获得 5 秒速度 I */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val victim = event.entity
        if (victim !is Player) return
        if (event.damager !is Projectile) return
        val ctx = DungeonContext.get() ?: return
        if (!ctx.playerManager.isPlayerInDungeon(victim)) return
        if (!hasFourPiece(victim)) return
        victim.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 5 * 20, 0))
    }

    override fun onSneakActiveSkill(event: PlayerToggleSneakEvent) {
        if (!event.isSneaking) return
        val player = event.player
        val ctx = DungeonContext.get() ?: return
        if (!ctx.playerManager.isPlayerInDungeon(player)) return
        if (!hasFourPiece(player)) {
            return
        }
        if (!LoadoutSetSkillState.canUseSetSkill(player.uniqueId, setId)) {
            player.sendLangSys(plugin, "loadout.$setId.wrong-set")
            return
        }
        if (LoadoutSetSkillState.getTiebiSkillRemaining(player.uniqueId) <= 0) {
            player.sendLangSys(plugin, "loadout.$setId.cooldown")
            return
        }

        scheduleSneakSkill(player, ctx,
            recheck = {
                LoadoutSetSkillState.canUseSetSkill(player.uniqueId, setId) &&
                LoadoutSetSkillState.getTiebiSkillRemaining(player.uniqueId) > 0 && hasFourPiece(player)
            },
            onTrigger = {
                if (LoadoutSetSkillState.useTiebiSkill(player.uniqueId)) {
                    LoadoutSetSkillState.recordSetSkillUse(player.uniqueId, setId)
                    LoadoutSetSkillState.setTiebiSkillEndTime(player.uniqueId, System.currentTimeMillis() + 40 * 1000)
                    player.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, 40 * 20, 1))
                    player.sendLangSys(plugin, "loadout.$setId.active-used")
                    Dungeon.instance.refreshSidebar(player)
                }
            }
        )
    }

    override fun onLeaveDungeon(player: Player) {
        LoadoutSetSkillState.clearTiebiSkillUsed(player.uniqueId)
        LoadoutSetSkillState.clearPlayerSetSkill(player.uniqueId)
    }

    override fun refreshPassive(player: Player) {
        updateTiebiPassive(player)
        // 监控铁壁抗性效果，显示剩余时间
        val remainingSeconds = LoadoutSetSkillState.getTiebiSkillRemainingSeconds(player.uniqueId)
    }

    private fun updateTiebiPassive(player: Player) {
        val attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH) ?: return
        val count = countArmor(player)
        @Suppress("DEPRECATION")
        val existing = attr.modifiers.firstOrNull { it.uniqueId == tiebiModifierUuid }
        existing?.let { attr.removeModifier(it) }
        when {
            count >= 4 -> {
                @Suppress("DEPRECATION")
                attr.addModifier(AttributeModifier(tiebiModifierUuid, "tiebi_set_bonus", 8.0, AttributeModifier.Operation.ADD_NUMBER))
            }
            count >= 2 -> {
                @Suppress("DEPRECATION")
                attr.addModifier(AttributeModifier(tiebiModifierUuid, "tiebi_set_bonus", 6.0, AttributeModifier.Operation.ADD_NUMBER))
            }
        }
    }
}
