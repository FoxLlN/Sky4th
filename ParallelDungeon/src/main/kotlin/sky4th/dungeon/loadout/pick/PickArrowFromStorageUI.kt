package sky4th.dungeon.loadout.pick

import sky4th.dungeon.loadout.screen.LoadoutScreenHolder
import sky4th.dungeon.loadout.screen.LoadoutUI
import sky4th.dungeon.loadout.storage.LootItemHelper
import sky4th.dungeon.loadout.equipment.ActualEquipmentResolver
import sky4th.dungeon.loadout.shop.LoadoutShopAPI
import sky4th.dungeon.head.EquipmentHead
import sky4th.core.model.StorageEntry
import sky4th.core.model.StorageEntryType
import sky4th.core.api.LanguageAPI
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import sky4th.core.api.StorageAPI
import sky4th.dungeon.config.ConfigManager
import net.kyori.adventure.text.minimessage.MiniMessage

/**
 * “从仓库选择箭矢”界面：根据当前选中的远程武器显示可选箭矢（爆破弩仅显示爆炸箭，其余显示普通箭矢），
 * 点击一项加入配装已选箭矢（可多选），右上角纸显示已选总数，返回按钮回到配装。
 */
object PickArrowFromStorageUI {

    private const val SIZE = 54
    /** 返回按钮 */
    const val SLOT_BACK = 53
    /** 最右列倒数第二个：购买箭矢按钮（打开当前武器对应箭矢购买界面） */
    const val SLOT_BUY_ARROWS = 44
    /** 右上角纸：展示已选箭矢总数 */
    private const val SLOT_ARROWS_TOTAL = 8

    @JvmStatic
    fun open(
        player: Player,
        loadoutHolder: LoadoutScreenHolder,
        plugin: JavaPlugin,
        configManager: ConfigManager
    ) {
        val rangedId = loadoutHolder.selectedRanged?.loadoutId
        val entries = LoadoutUI.getStorageEntriesArrows(player.uniqueId, rangedId)
        openWithEntries(player, loadoutHolder, entries, plugin, configManager, rangedId)
    }

    @JvmStatic
    fun openWithEntries(
        player: Player,
        loadoutHolder: LoadoutScreenHolder,
        entries: List<Pair<Int, StorageEntry>>,
        plugin: JavaPlugin,
        configManager: ConfigManager,
        rangedLoadoutId: String? = null
    ) {
        val holder = PickArrowFromStorageHolder(player.uniqueId, loadoutHolder, entries)
        val titleKey = if (LoadoutUI.isExplosiveArrowRanged(rangedLoadoutId)) "loadout.screen.pick-arrows-title-explosive" else "loadout.screen.pick-arrows-title"
        val title = LanguageAPI.getText(plugin, titleKey)
        val inv = Bukkit.createInventory(holder, SIZE, MiniMessage.miniMessage().deserialize(title))
        holder.backingInv = inv

        fillDecoration(plugin, inv, loadoutHolder, configManager, rangedLoadoutId)
        fillContent(inv, entries, loadoutHolder, plugin, configManager, rangedLoadoutId)

        player.openInventory(inv)
    }

    /** 刷新界面（已选数量、已选发光），选择一项箭矢后调用 */
    @JvmStatic
    fun refresh(holder: PickArrowFromStorageHolder, plugin: JavaPlugin, configManager: ConfigManager) {
        val inv = holder.backingInv
        val rangedId = holder.loadoutHolder.selectedRanged?.loadoutId
        fillDecoration(plugin, inv, holder.loadoutHolder, configManager ,rangedId)
        fillContent(inv, holder.slotToEntry, holder.loadoutHolder, plugin, configManager, rangedId)
    }

    private fun fillDecoration(plugin: JavaPlugin, inv: Inventory, loadoutHolder: LoadoutScreenHolder, configManager: ConfigManager, rangedLoadoutId: String? = null) {
        val gray = ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
            editMeta { it.displayName(MiniMessage.miniMessage().deserialize(" ")) }
        }
        // 统计三类弹药已选总数：普通 / TNT / 特种
        var normalTotal = 0
        var tntTotal = 0
        var specialTotal = 0
        loadoutHolder.selectedArrows.forEach { (_, e) ->
            when {
                e.type == sky4th.core.model.StorageEntryType.LOADOUT && e.loadoutId == "normal_arrow" ->
                    normalTotal += e.count
                e.type == sky4th.core.model.StorageEntryType.LOADOUT && e.loadoutId == "baozha_jian" ->
                    tntTotal += e.count
                e.type == sky4th.core.model.StorageEntryType.LOADOUT && e.loadoutId != null && e.loadoutId in LoadoutUI.SPECIAL_BOW_ARROW_IDS ->
                    specialTotal += e.count
            }
        }
        for (slot in StorageAPI.getOperationSlots()) {
            when {
                slot == SLOT_BACK -> inv.setItem(slot, EquipmentHead.createBackToEquipmentHead(configManager))
                slot == SLOT_BUY_ARROWS && rangedLoadoutId != null -> inv.setItem(slot, ItemStack(Material.EMERALD).apply {
                    editMeta { meta ->
                        meta.displayName(LanguageAPI.getComponent(plugin, "loadout.screen.buy-arrows"))
                        meta.lore(listOf(LanguageAPI.getComponent(plugin, "loadout.screen.buy-arrows-lore")))
                    }
                })
                slot == SLOT_ARROWS_TOTAL -> inv.setItem(slot, ItemStack(Material.PAPER).apply {
                    editMeta { meta ->
                        meta.displayName(LanguageAPI.getComponent(plugin, "loadout.screen.arrows-total-title"))
                        val loreLines: List<String> = when {
                            LoadoutUI.isExplosiveArrowRanged(rangedLoadoutId) ->
                                listOf(LanguageAPI.getText(plugin, "loadout.screen.arrows-explosive").replace("{count}", tntTotal.toString()))
                            rangedLoadoutId == "special_bow" ->
                                listOf(
                                    LanguageAPI.getText(plugin, "loadout.screen.arrows-normal").replace("{count}", normalTotal.toString()),
                                    LanguageAPI.getText(plugin, "loadout.screen.arrows-special").replace("{count}", specialTotal.toString())
                                )
                            else ->
                                listOf(LanguageAPI.getText(plugin, "loadout.screen.arrows-normal").replace("{count}", normalTotal.toString()))
                        }
                        meta.lore(loreLines.map { MiniMessage.miniMessage().deserialize(it) })
                    }
                })
                else -> inv.setItem(slot, gray.clone())
            }
        }
    }

    private fun fillContent(
        inv: Inventory,
        slotToEntry: List<Pair<Int, StorageEntry>>,
        loadoutHolder: LoadoutScreenHolder,
        plugin: JavaPlugin,
        configManager: ConfigManager,
        rangedLoadoutId: String? = null
    ) {
        val selectedCountsByStorageIdx = loadoutHolder.selectedArrows.associate { (idx, e) -> idx to e.count }
        val selectedStorageIndices = selectedCountsByStorageIdx.keys
        for ((storageIdx, entry) in slotToEntry) {
            if (storageIdx !in 0 until StorageAPI.STORAGE_SLOT_COUNT) continue
            val uiSlot = StorageAPI.storageIndexToUiSlot(storageIdx)
            val item = when {
                LoadoutUI.isExplosiveArrowRanged(rangedLoadoutId) ->
                    if (entry.type == StorageEntryType.LOADOUT && entry.loadoutId == "baozha_jian")
                        ActualEquipmentResolver.toActualItems(entry, plugin, configManager).firstOrNull()
                    else null
                entry.type == StorageEntryType.LOADOUT && entry.loadoutId != null && entry.loadoutId in LoadoutUI.SPECIAL_BOW_ARROW_IDS ->
                    ActualEquipmentResolver.toActualItems(entry, plugin, configManager).firstOrNull()
                entry.type == StorageEntryType.LOADOUT && entry.loadoutId == "normal_arrow" ->
                    ActualEquipmentResolver.toActualItems(entry, plugin, configManager).firstOrNull()
                else -> null
            } ?: continue
            if (selectedStorageIndices.contains(storageIdx)) {
                // 若该槽位只选取了部分数量，展示为“将携带的数量”
                val selectedCount = selectedCountsByStorageIdx[storageIdx]
                if (selectedCount != null) {
                    item.amount = selectedCount.coerceIn(1, item.maxStackSize)
                }
                item.editMeta { LoadoutShopAPI.addEnchantGlowPublic(it) }
            }
            inv.setItem(uiSlot, item)
        }
    }

    @JvmStatic
    fun isPickArrowFromStorageGui(holder: Any?): Boolean = holder is PickArrowFromStorageHolder

    @JvmStatic
    fun getEntryAtSlot(holder: PickArrowFromStorageHolder, clickedSlot: Int): Pair<Int, StorageEntry>? {
        return holder.slotToEntry.firstOrNull { (storageIdx, _) ->
            StorageAPI.storageIndexToUiSlot(storageIdx) == clickedSlot
        }
    }
}
