package sky4th.dungeon.loadout.ranged

import sky4th.dungeon.command.DungeonContext
import org.bukkit.entity.Arrow
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionType

/**
 * 治疗箭、迅捷箭命中时取消伤害，保留正面药水效果（治疗/迅捷）。
 * 通过箭矢实体的药水类型判断，仅副本内生效。
 */
class SpecialArrowNoDamageListener(private val plugin: JavaPlugin) : Listener {

    /** 治疗箭、迅捷箭（1.21 为 HEALING、SWIFTNESS） */
    private val noDamagePotionTypes: Set<PotionType> = setOf(
        PotionType.HEALING,
        PotionType.SWIFTNESS
    )

    @EventHandler(priority = EventPriority.LOWEST)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        if (damager !is Arrow) return
        val shooter = damager.shooter as? Player ?: return
        val ctx = DungeonContext.get() ?: return
        if (!ctx.playerManager.isPlayerInDungeon(shooter)) return
        val potionType = getArrowPotionType(damager) ?: return
        if (potionType in noDamagePotionTypes) {
            event.damage = 0.0
        }
    }

    /** 获取箭矢药水类型（药水箭才有，普通箭返回 null）；使用反射兼容不同版本 */
    private fun getArrowPotionType(arrow: Arrow): PotionType? {
        val raw = try {
            // 使用反射获取 basePotionType，兼容不同版本
            arrow.javaClass.getMethod("getBasePotionType").invoke(arrow) as? PotionType
        } catch (_: Throwable) {
            null
        }
        return raw?.takeIf { it != PotionType.WATER }
    }
}
