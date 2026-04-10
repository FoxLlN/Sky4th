package sky4th.dungeon.loadout.equipment

import sky4th.dungeon.command.DungeonContext
import sky4th.dungeon.Dungeon
import sky4th.dungeon.util.LanguageUtil.sendLangSys
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

/**
 * 萨满套装效果：
 * - 2件套：10 秒未受伤获得生命恢复 I
 * - 4件套：5 秒未受伤获得生命恢复 I
 * - 主动：蹲下 5 秒清除负面效果并恢复 6 血，每局 2 次
 */
class SamanSetListener(plugin: JavaPlugin) : BaseLoadoutSetListener(plugin, "shaman") {

    private val lastDamageTime: MutableMap<UUID, Long> = mutableMapOf()
    private val regenDurationTicks = 5 * 20
    private val regenEffect = PotionEffect(PotionEffectType.REGENERATION, regenDurationTicks, 0, false, false)
    private val negativeEffectTypes = setOf(
        PotionEffectType.POISON,
        PotionEffectType.WEAKNESS,
        PotionEffectType.SLOWNESS,
        PotionEffectType.WITHER,
        PotionEffectType.UNLUCK,
        PotionEffectType.NAUSEA,
        PotionEffectType.BLINDNESS
    )

    init {
        plugin.server.scheduler.runTaskTimer(plugin, Runnable { tickPassiveRegen() }, 20L, 20L)
    }

    private fun tickPassiveRegen() {
        val ctx = DungeonContext.get() ?: return
        val now = System.currentTimeMillis()
        for (uuid in ctx.playerManager.getPlayersInDungeon()) {
            val player = plugin.server.getPlayer(uuid) ?: continue
            if (!player.isOnline) continue
            val count = countArmor(player)
            if (count < 2) continue
            val last = lastDamageTime[uuid] ?: 0L
            val noDamageMs = now - last
            val thresholdMs = if (count >= 4) 5_000L else 10_000L
            if (noDamageMs >= thresholdMs) {
                player.addPotionEffect(regenEffect)
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityDamage(event: EntityDamageEvent) {
        val entity = event.entity
        if (entity !is Player) return
        val ctx = DungeonContext.get() ?: return
        if (!ctx.playerManager.isPlayerInDungeon(entity)) return
        if (countArmor(entity) < 2) return
        lastDamageTime[entity.uniqueId] = System.currentTimeMillis()
    }

    override fun onSneakActiveSkill(event: PlayerToggleSneakEvent) {
        if (!event.isSneaking) return
        val player = event.player
        val ctx = DungeonContext.get() ?: return
        if (!ctx.playerManager.isPlayerInDungeon(player)) return
        if (LoadoutSetSkillState.getSamanSkillRemaining(player.uniqueId) <= 0) {
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
                LoadoutSetSkillState.getSamanSkillRemaining(player.uniqueId) > 0 && hasFourPiece(player)
            },
            onTrigger = onTrigger@{
                if (!LoadoutSetSkillState.useSamanSkill(player.uniqueId)) return@onTrigger
                LoadoutSetSkillState.recordSetSkillUse(player.uniqueId, setId)
                for (effectType in negativeEffectTypes) {
                    player.removePotionEffect(effectType)
                }
                player.health = (player.health + 6.0).coerceIn(
                    0.0,
                    player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH)?.value ?: 20.0
                )
                player.sendLangSys(plugin, "loadout.$setId.active-used")
                Dungeon.instance.refreshSidebar(player)
            }
        )
    }

    override fun onLeaveDungeon(player: Player) {
        LoadoutSetSkillState.clearSamanSkillUsed(player.uniqueId)
        LoadoutSetSkillState.clearPlayerSetSkill(player.uniqueId)
        lastDamageTime.remove(player.uniqueId)
    }

    override fun refreshPassive(player: Player) {
        val count = countArmor(player)
        if (count < 2) return
        val now = System.currentTimeMillis()
        val last = lastDamageTime[player.uniqueId] ?: 0L
        val noDamageMs = now - last
        val thresholdMs = if (count >= 4) 5_000L else 10_000L
        if (noDamageMs >= thresholdMs) {
            player.addPotionEffect(regenEffect)
        }
    }
}
