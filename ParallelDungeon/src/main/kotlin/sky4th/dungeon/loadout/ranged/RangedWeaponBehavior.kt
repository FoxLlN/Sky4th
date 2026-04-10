package sky4th.dungeon.loadout.ranged

import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

/**
 * 远程武器发放后的行为扩展（模块化）。
 * 在 [ActualEquipmentResolver] 生成基础物品后，按 loadoutId 执行对应行为（如无限弓补 1 支箭）。
 * 后续新增远程武器只需实现此接口并注册即可。
 */
fun interface RangedWeaponBehavior {

    /**
     * 对已生成的物品列表做后处理（可增删改）。
     * @param loadoutId 配装 id（与 loadout-shop / actual-equipment 一致）
     * @param items 当前要发放的物品列表，可原地修改
     * @param plugin 插件实例
    */ 
    fun process(loadoutId: String, items: MutableList<ItemStack>, plugin: JavaPlugin)
}
