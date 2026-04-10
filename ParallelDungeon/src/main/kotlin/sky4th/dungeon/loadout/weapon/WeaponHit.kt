package sky4th.dungeon.loadout.weapon

import org.bukkit.entity.LivingEntity

/**
 * 单次武器命中的信息，用于区分主目标与副目标（横扫）。
 */
data class WeaponHit(
    val victim: LivingEntity,
    var damage: Double,
    val isMainTarget: Boolean
)
