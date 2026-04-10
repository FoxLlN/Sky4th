package sky4th.dungeon.loadout.screen

import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import sky4th.core.model.StorageEntry
import java.util.*

/**
 * 配装界面 Holder：进入地牢前选择装备/近战/远程/补给。
 * 保存目标地牢名、实例ID、当前选中的各项（各最多 1 件，补给有上限）。
 */
class LoadoutScreenHolder(
    val playerId: UUID,
    val dungeonName: String,
    val instanceId: String? = null
) : InventoryHolder {

    var selectedEquipment: StorageEntry? = null
    var selectedEquipmentStorageIndex: Int? = null
    var selectedMelee: StorageEntry? = null
    var selectedMeleeStorageIndex: Int? = null
    var selectedRanged: StorageEntry? = null
    var selectedRangedStorageIndex: Int? = null
    /** 已选箭矢（远程武器时从仓库选的 LOOT 箭矢，每项为 仓库槽位下标 to 条目） */
    val selectedArrows: MutableList<Pair<Int, StorageEntry>> = mutableListOf()
    /** 已选补给（数量由 config supplies-carry-limit 控制，0 或不设则不设上限） */
    val selectedSupplies: MutableList<StorageEntry> = mutableListOf()

    lateinit var backingInv: Inventory
    override fun getInventory(): Inventory = backingInv
}
