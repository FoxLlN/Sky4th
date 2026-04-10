package sky4th.dungeon.loadout.storage

import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import java.util.*

/**
 * 仓库界面 Holder，持有玩家 UUID 便于点击时判断归属。
 */
class StorageHolder(val ownerUuid: UUID) : InventoryHolder {
    var backingInv: Inventory? = null
    /** 是否处于出售模式 */
    var isSellingMode = false
    /** 选中的存储区槽位索引集合 */
    val selectedSlots = mutableSetOf<Int>()
    override fun getInventory(): Inventory = backingInv!!
}
