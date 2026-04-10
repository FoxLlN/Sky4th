package sky4th.dungeon.loadout.ranged

import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

/**
 * 远程武器行为注册表：按 loadoutId 注册 [RangedWeaponBehavior]，发放远程武器时统一执行。
 * 新增远程武器时在此注册对应行为即可。
 */
object RangedWeaponBehaviorRegistry {

    private val behaviors = mutableMapOf<String, MutableList<RangedWeaponBehavior>>()

    /**
     * 为指定 loadoutId 注册一个行为（可多次注册，按注册顺序执行）。
     */
    @JvmStatic
    fun register(loadoutId: String, behavior: RangedWeaponBehavior) {
        behaviors.getOrPut(loadoutId) { mutableListOf() }.add(behavior)
    }

    /**
     * 对远程武器物品列表执行该 loadoutId 已注册的所有行为。
     * 若未注册任何行为则不做修改。
     */
    @JvmStatic
    fun process(loadoutId: String, items: MutableList<ItemStack>, plugin: JavaPlugin) {
        behaviors[loadoutId]?.forEach { it.process(loadoutId, items, plugin) }
    }
}
