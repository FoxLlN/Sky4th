package sky4th.dungeon.loadout.pick

import sky4th.dungeon.loadout.screen.LoadoutScreenHolder
import sky4th.dungeon.loadout.LoadoutCategory
import sky4th.core.model.StorageEntry
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import java.util.*

/**
 * “从仓库选择”子界面 Holder。
 * 用于配装时按分类筛选仓库物品，点击一项后写回 LoadoutScreenHolder 并返回配装界面。
 */
class PickFromStorageHolder(
    val playerId: UUID,
    val loadoutHolder: LoadoutScreenHolder,
    val category: LoadoutCategory,
    /** 当前界面显示的槽位对应的 (仓库下标, 条目)，仅 LOADOUT 且属于本分类 */
    val slotToEntry: List<Pair<Int, StorageEntry>>
) : InventoryHolder {

    lateinit var backingInv: Inventory
    override fun getInventory(): Inventory = backingInv
}
