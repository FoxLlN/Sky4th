package sky4th.dungeon.loadout.weapon

import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

/**
 * 配装近战/武器效果处理器：按 loadoutId 在「本 tick 内该武器造成的命中」收集完后统一处理。
 *
 * @param loadoutId 对应 config loadout-shop / actual-equipment 的 id（如 broadsword）
 */
interface LoadoutWeaponEffectHandler {
    val loadoutId: String

    /**
     * 本 tick 内该武器对若干目标的命中已收集完毕，在此做效果结算。
     * @param attacker 持该武器的玩家
     * @param hits 本 tick 内该武器命中的 [WeaponHit]（含主目标/副目标标记）
     */
    fun processHits(
        attacker: Player,
        hits: List<WeaponHit>,
        plugin: JavaPlugin
    )
}
