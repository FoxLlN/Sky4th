package sky4th.dungeon.loadout.shop

import sky4th.dungeon.loadout.LoadoutCategory
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

/**
 * 配装商店 GUI 的 InventoryHolder。
 * 保存当前分类、页码、选中商品下标（仅选中项附魔发光）；供 [LoadoutShopAPI] 与 [LoadoutShopListener] 使用。
 */
class LoadoutShopHolder(
    val category: LoadoutCategory,
    var page: Int = 0,
    var selectedContentIndex: Int = -1
) : InventoryHolder {
    lateinit var backingInv: Inventory
    override fun getInventory(): Inventory = backingInv
}
