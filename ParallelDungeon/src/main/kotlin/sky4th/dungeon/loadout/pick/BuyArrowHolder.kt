package sky4th.dungeon.loadout.pick

import sky4th.dungeon.loadout.screen.LoadoutScreenHolder
import sky4th.dungeon.loadout.LoadoutShopItemConfig
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import java.util.*

/**
 * “购买箭矢”子界面 Holder：根据当前远程武器显示可购买的箭矢及价格，首次点击选中、再次点击购买。
 */
class BuyArrowHolder(
    val playerId: UUID,
    val loadoutHolder: LoadoutScreenHolder,
    /** 当前远程武器 loadoutId，用于筛选箭矢 */
    val rangedLoadoutId: String,
    /** 当前武器可购买的箭矢配置列表（来自 supplies 筛选） */
    val arrowConfigs: List<LoadoutShopItemConfig>,
    /** 当前选中的内容格下标（0..14），-1 未选 */
    var selectedIndex: Int = -1
) : InventoryHolder {

    lateinit var backingInv: Inventory
    override fun getInventory(): Inventory = backingInv
}
