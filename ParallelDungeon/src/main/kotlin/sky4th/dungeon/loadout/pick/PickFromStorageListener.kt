package sky4th.dungeon.loadout.pick

import sky4th.dungeon.loadout.screen.LoadoutScreenHolder
import sky4th.dungeon.loadout.screen.LoadoutUI
import sky4th.dungeon.loadout.LoadoutCategory
import sky4th.dungeon.command.DungeonContext
import sky4th.dungeon.util.LanguageUtil.sendLang
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.scheduler.BukkitRunnable

/**
 * “从仓库选择”界面点击：选中一项后写回配装界面并返回；或点击返回仅关闭。
 */
class PickFromStorageListener : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder ?: return
        if (!PickFromStorageUI.isPickFromStorageGui(holder)) return

        event.isCancelled = true
        val who = event.whoClicked
        if (who !is Player) return

        val pickHolder = holder as PickFromStorageHolder
        val loadoutHolder = pickHolder.loadoutHolder
        val ctx = DungeonContext.get() ?: return
        val plugin = ctx.plugin
        val configManager = ctx.configManager
        val slot = event.rawSlot

        // 返回
        if (slot == PickFromStorageUI.SLOT_BACK) {
            who.closeInventory()
            reopenLoadout(who, loadoutHolder, plugin, configManager)
            return
        }

        val pair = PickFromStorageUI.getEntryAtSlot(pickHolder, slot) ?: return
        val (storageIndex, entry) = pair
        val itemName = configManager.getLoadoutShopItemById(entry.loadoutId ?: "")?.name?.replace("&", "§") ?: "?"
        when (pickHolder.category) {
            LoadoutCategory.EQUIPMENT -> {
                loadoutHolder.selectedEquipment = entry
                loadoutHolder.selectedEquipmentStorageIndex = storageIndex
                who.closeInventory()
                who.sendLang(plugin, "loadout.screen.selected-confirm", "name" to itemName)
                reopenLoadout(who, loadoutHolder, plugin, configManager)
            }
            LoadoutCategory.MELEE -> {
                loadoutHolder.selectedMelee = entry
                loadoutHolder.selectedMeleeStorageIndex = storageIndex
                who.closeInventory()
                who.sendLang(plugin, "loadout.screen.selected-confirm", "name" to itemName)
                reopenLoadout(who, loadoutHolder, plugin, configManager)
            }
            LoadoutCategory.RANGED -> {
                // 切换远程武器时清空已选弹药（重置弹药装配）
                if (loadoutHolder.selectedRanged != null && loadoutHolder.selectedRanged != entry) {
                    loadoutHolder.selectedArrows.clear()
                }
                loadoutHolder.selectedRanged = entry
                loadoutHolder.selectedRangedStorageIndex = storageIndex
                who.closeInventory()
                who.sendLang(plugin, "loadout.screen.selected-confirm", "name" to itemName)
                reopenLoadout(who, loadoutHolder, plugin, configManager)
            }
            LoadoutCategory.SUPPLIES -> {
                val limit = configManager.suppliesCarryLimit
                if (loadoutHolder.selectedSupplies.size < limit) {
                    loadoutHolder.selectedSupplies.add(entry)
                }
                who.closeInventory()
                who.sendLang(plugin, "loadout.screen.selected-confirm", "name" to itemName)
                reopenLoadout(who, loadoutHolder, plugin, configManager)
            }
            LoadoutCategory.REPAIR -> { }
        }
    }

    private fun reopenLoadout(
        player: Player,
        loadoutHolder: LoadoutScreenHolder,
        plugin: org.bukkit.plugin.java.JavaPlugin,
        configManager: sky4th.dungeon.config.ConfigManager,
    ) {
        object : BukkitRunnable() {
            override fun run() {
                LoadoutUI.openWithHolder(player, loadoutHolder, plugin, configManager)
            }
        }.runTaskLater(plugin, 1L)
    }
}
