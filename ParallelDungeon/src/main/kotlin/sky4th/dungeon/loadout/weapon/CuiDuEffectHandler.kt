package sky4th.dungeon.loadout.weapon

import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * 效果（锋利3 铁剑）：
 * 攻击敌方施加凋零 II，持续 6 秒。
 */
class CuiDuEffectHandler : LoadoutWeaponEffectHandler {
    override val loadoutId: String = "venom_blade"

    /** 凋零 II 持续 6 秒（120 tick） */
    private val poisonEffect = PotionEffect(PotionEffectType.WITHER, 6 * 20, 1, false, true)

    override fun processHits(
        attacker: Player,
        hits: List<WeaponHit>,
        plugin: JavaPlugin
    ) {
        for (hit in hits) {
            if (!hit.victim.isValid || hit.victim.isDead) continue
            hit.victim.addPotionEffect(poisonEffect)
        }
    }
}
