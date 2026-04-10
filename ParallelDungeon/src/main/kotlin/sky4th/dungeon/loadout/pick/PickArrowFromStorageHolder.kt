package sky4th.dungeon.loadout.pick

import sky4th.dungeon.loadout.screen.LoadoutScreenHolder
import sky4th.core.model.StorageEntry
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import java.util.*

/**
 * “从仓库选择箭矢”界面 Holder。
 * 显示仓库中所有箭矢 LOOT 条目，点击一项加入 selectedArrows，可多选，返回配装界面时保留已选。
 */
class PickArrowFromStorageHolder(
    val playerId: UUID,
    val loadoutHolder: LoadoutScreenHolder,
    /** 当前界面显示的槽位对应的 (仓库下标, 条目)，仅箭矢 LOOT */
    val slotToEntry: List<Pair<Int, StorageEntry>>
) : InventoryHolder {

    lateinit var backingInv: Inventory
    override fun getInventory(): Inventory = backingInv
}
