package sky4th.dungeon.loadout.weapon

import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

/**
 * 阔剑效果（锋利3 横扫之刃2 铁剑）：
 * - 攻击 3–4 个目标：主目标伤害 +40%，副目标 +20%
 * - 攻击 5+ 个目标：主目标伤害 +60%，副目标 +40%
 */
class KuoJianEffectHandler : LoadoutWeaponEffectHandler {
    override val loadoutId: String = "broadsword"

    override fun processHits(
        attacker: Player,
        hits: List<WeaponHit>,
        plugin: JavaPlugin
    ) {
        if (hits.size < 3) return
        val (mainBonus, secondaryBonus) = when {
            hits.size >= 5 -> 0.60 to 0.40
            else -> 0.40 to 0.20
        }
        for (hit in hits) {
            if (!hit.victim.isValid || hit.victim.isDead) continue
            val extraMultiplier = if (hit.isMainTarget) mainBonus else secondaryBonus
            val extra = hit.damage * extraMultiplier
            if (extra <= 0) continue
            val maxHp = hit.victim.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: 20.0
            val healthBefore = hit.victim.health
            val remaining = (healthBefore - extra).coerceIn(0.0, maxHp)
            hit.victim.health = remaining
        }
    }
}
