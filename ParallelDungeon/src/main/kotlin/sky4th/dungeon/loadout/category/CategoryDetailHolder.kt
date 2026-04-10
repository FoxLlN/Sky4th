package sky4th.dungeon.loadout.category

import sky4th.dungeon.loadout.screen.LoadoutScreenHolder
import sky4th.dungeon.loadout.LoadoutCategory
import sky4th.dungeon.loadout.LoadoutShopItemConfig
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import java.util.*

/**
 * 分类详情界面 Holder：展示某分类下所有商品及仓库数量，首次点击选中、再次点击选择/购买。
 */
class CategoryDetailHolder(
    val playerId: UUID,
    val loadoutHolder: LoadoutScreenHolder,
    /** 当前展示的分类，点击左侧目录可切换 */
    var category: LoadoutCategory,
    /** 当前选中的商品下标（-1 未选；为当前页内下标 0..14） */
    var selectedItemIndex: Int = -1,
    /** 当前页码（每页 15 格） */
    var page: Int = 0
) : InventoryHolder {

    /** 本分类下的商店商品列表（与 UI 内容槽位一一对应） */
    var categoryItems: List<LoadoutShopItemConfig> = emptyList()
        internal set

    lateinit var backingInv: Inventory
    override fun getInventory(): Inventory = backingInv
}
