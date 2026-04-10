package sky4th.dungeon.loadout.pick

import sky4th.dungeon.loadout.screen.LoadoutScreenHolder
import sky4th.dungeon.loadout.screen.LoadoutUI
import sky4th.dungeon.loadout.shop.LoadoutShopAPI
import sky4th.dungeon.loadout.LoadoutCategory
import sky4th.dungeon.head.EquipmentHead
import sky4th.core.model.StorageEntry
import sky4th.core.model.StorageEntryType
import sky4th.core.api.LanguageAPI
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.plugin.java.JavaPlugin
import sky4th.core.api.StorageAPI
import sky4th.dungeon.config.ConfigManager
import net.kyori.adventure.text.minimessage.MiniMessage

/**
 * “从仓库选择”子界面：按分类显示仓库中的 LOADOUT 物品，点击一项即选中并返回配装界面。
 * 补给类可多选（有上限），装备/近战/远程单选。
 */
object PickFromStorageUI {

    private const val SIZE = 54
    /** 返回按钮 */
    const val SLOT_BACK = 53

    @JvmStatic
    fun open(
        player: Player,
        loadoutHolder: LoadoutScreenHolder,
        category: LoadoutCategory,
        plugin: JavaPlugin,
        configManager: ConfigManager
    ) {
        val list = LoadoutUI.getStorageEntriesByCategory(player.uniqueId, category, configManager)
        openWithEntries(player, loadoutHolder, category, list, plugin, configManager)
    }

    /** 使用指定条目列表打开（如按 loadoutId 筛选后的列表） */
    @JvmStatic
    fun openWithEntries(
        player: Player,
        loadoutHolder: LoadoutScreenHolder,
        category: LoadoutCategory,
        entries: List<Pair<Int, StorageEntry>>,
        plugin: JavaPlugin,
        configManager: ConfigManager
    ) {
        val holder = PickFromStorageHolder(player.uniqueId, loadoutHolder, category, entries)
        val title = LanguageAPI.getText(plugin, "loadout.screen.pick-storage-title")
        val inv = Bukkit.createInventory(holder, SIZE, MiniMessage.miniMessage().deserialize(title))
        holder.backingInv = inv

        fillDecoration(inv, configManager)
        fillContent(inv, entries, plugin, configManager)

        player.openInventory(inv)
    }

    private fun fillDecoration(inv: Inventory, configManager: ConfigManager) {
        val gray = ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
            editMeta { it.displayName(MiniMessage.miniMessage().deserialize(" ")) }
        }
        for (slot in StorageAPI.getOperationSlots()) {
            if (slot == SLOT_BACK) {
                inv.setItem(slot, EquipmentHead.createBackToEquipmentHead(configManager))
            } else {
                inv.setItem(slot, gray.clone())
            }
        }
    }

    private fun fillContent(inv: Inventory, slotToEntry: List<Pair<Int, StorageEntry>>, plugin: JavaPlugin, configManager: ConfigManager) {
        for (i in slotToEntry.indices) {
            if (i >= StorageAPI.STORAGE_SLOT_COUNT) break
            val (_, entry) = slotToEntry[i]
            val uiSlot = StorageAPI.storageIndexToUiSlot(i)
            val item = entryToItemStack(entry, plugin, configManager) ?: continue
            inv.setItem(uiSlot, item)
        }
    }

    private fun entryToItemStack(entry: sky4th.core.model.StorageEntry, plugin: JavaPlugin, configManager: ConfigManager): ItemStack? {
        if (entry.type != StorageEntryType.LOADOUT) return null
        val config = configManager.getLoadoutShopItemById(entry.loadoutId ?: return null) ?: return null

        // 检查是否是套装
        val actualConfigList = configManager.getActualEquipmentConfigList(entry.loadoutId ?: "")
        val isSet = actualConfigList != null && actualConfigList.size > 1

        val item = LoadoutShopAPI.createPurchasedItem(plugin, config)
        item.amount = entry.count.coerceIn(1, item.maxStackSize)
        val dur = entry.durability

        // 使用DurabilityManager统一处理耐久度
        if (dur != null) {
            sky4th.dungeon.loadout.storage.DurabilityManager.applyDurability(item, dur, isSet)
        }

        // 如果是套装且有各部位耐久度信息，添加耐久度描述并存储到PDC
        if (isSet) {
            val durabilityInfo = sky4th.dungeon.loadout.storage.DurabilityManager.getSetDurabilityInfo(entry)
            if (durabilityInfo != null) {
                // 将套装耐久度信息存储到PersistentDataContainer
                val durabilityManager = sky4th.dungeon.loadout.storage.DurabilityManager
                durabilityManager.setSetDurabilityToPDC(item, durabilityInfo, plugin)

                // 检查是否有任何部位的耐久度损失
                val hasDamage = durabilityInfo.slotDurabilities.any { it.value.first < it.value.second }
                if (hasDamage) {
                    item.editMeta { meta ->
                        val lore = meta.lore()?.toMutableList() ?: mutableListOf()
                        lore.add(LanguageAPI.getComponent(plugin, "storage.set-durability-title"))
                        durabilityInfo.slotDurabilities.forEach { entry ->
                            val slot = entry.key
                            val (current, max) = entry.value
                            lore.add(LanguageAPI.getComponent(plugin, "storage.set-durability-line", "slot" to slot.displayName, "current" to current, "max" to max))
                        }
                        meta.lore(lore)
                    }
                }
            }
        }

        return item
    }

    @JvmStatic
    fun isPickFromStorageGui(holder: Any?): Boolean = holder is PickFromStorageHolder

    /** 根据点击的 UI 槽位得到对应的 (仓库下标, 条目)，若不是内容格则返回 null */
    @JvmStatic
    fun getEntryAtSlot(holder: PickFromStorageHolder, clickedSlot: Int): Pair<Int, sky4th.core.model.StorageEntry>? {
        val idx = holder.slotToEntry.indices.firstOrNull { StorageAPI.storageIndexToUiSlot(it) == clickedSlot } ?: return null
        val pair = holder.slotToEntry.getOrNull(idx) ?: return null
        return pair
    }
}
