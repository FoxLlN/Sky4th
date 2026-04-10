package sky4th.dungeon.loadout.pick

import sky4th.dungeon.loadout.screen.LoadoutScreenHolder
import sky4th.dungeon.loadout.screen.LoadoutUI
import sky4th.dungeon.command.DungeonContext
import sky4th.dungeon.util.LanguageUtil.sendLang
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.scheduler.BukkitRunnable
import sky4th.core.model.StorageEntry
import sky4th.core.model.StorageEntryType
import sky4th.dungeon.loadout.storage.LootItemHelper

/**
 * “从仓库选择箭矢”界面点击：选中一项加入 selectedArrows（可多选）；返回则关闭并回到配装界面。
 */
class PickArrowFromStorageListener : Listener {

    private enum class AmmoType(val max: Int, val displayName: String) {
        NORMAL(64, "普通箭矢"),
        TNT(16, "TNT箭矢"),
        SPECIAL(24, "特种箭矢")
    }

    private fun ammoTypeOf(entry: StorageEntry): AmmoType? = when {
        entry.type == StorageEntryType.LOADOUT && entry.loadoutId == "normal_arrow" -> AmmoType.NORMAL
        entry.type == StorageEntryType.LOADOUT && entry.loadoutId == "baozha_jian" -> AmmoType.TNT
        entry.type == StorageEntryType.LOADOUT && entry.loadoutId != null && entry.loadoutId in LoadoutUI.SPECIAL_BOW_ARROW_IDS -> AmmoType.SPECIAL
        else -> null
    }

    private fun selectedTotal(loadoutHolder: LoadoutScreenHolder, type: AmmoType): Int {
        return loadoutHolder.selectedArrows.sumOf { (_, e) -> if (ammoTypeOf(e) == type) e.count else 0 }
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder ?: return
        if (!PickArrowFromStorageUI.isPickArrowFromStorageGui(holder)) return

        event.isCancelled = true
        val who = event.whoClicked
        if (who !is Player) return

        val pickHolder = holder as PickArrowFromStorageHolder
        val loadoutHolder = pickHolder.loadoutHolder
        val ctx = DungeonContext.get() ?: return
        val plugin = ctx.plugin
        val configManager = ctx.configManager
        val slot = event.rawSlot

        if (slot == PickArrowFromStorageUI.SLOT_BACK) {
            who.closeInventory()
            reopenLoadout(who, loadoutHolder, plugin, configManager)
            return
        }

        if (slot == PickArrowFromStorageUI.SLOT_BUY_ARROWS) {
            val rangedId = loadoutHolder.selectedRanged?.loadoutId ?: return
            who.closeInventory()
            BuyArrowUI.open(who, loadoutHolder, rangedId, plugin, configManager)
            return
        }

        val pair = PickArrowFromStorageUI.getEntryAtSlot(pickHolder, slot) ?: return
        val (storageSlot, entry) = pair
        val existingIndex = loadoutHolder.selectedArrows.indexOfFirst { it.first == storageSlot }

        // 右键：取消已选（若已在 selectedArrows 中）
        if (event.isRightClick) {
            if (existingIndex >= 0) {
                loadoutHolder.selectedArrows.removeAt(existingIndex)
                // 可选：提示取消选中，这里保持安静避免刷屏
                PickArrowFromStorageUI.refresh(pickHolder, plugin, configManager)
            }
            return
        }

        // 左键：选中（若尚未选中）
        if (!event.isLeftClick) return
        if (existingIndex >= 0) return

        val type = ammoTypeOf(entry) ?: return
        val current = selectedTotal(loadoutHolder, type)
        val remain = (type.max - current).coerceAtLeast(0)
        if (remain <= 0) {
            who.sendLang(
                plugin,
                "loadout.screen.arrows-limit-reached",
                "type" to type.displayName,
                "max" to type.max,
                "current" to current
            )
            return
        }
        val take = minOf(entry.count, remain)
        val selectedEntry = entry.copy(count = take)
        loadoutHolder.selectedArrows.add(storageSlot to selectedEntry)
        if (take < entry.count) {
            who.sendLang(
                plugin,
                "loadout.screen.arrows-limit-partial",
                "type" to type.displayName,
                "remain" to remain,
                "taken" to take
            )
        }
        who.sendLang(plugin,"loadout.screen.selected-confirm", "name" to "${type.displayName} x${take}")
        PickArrowFromStorageUI.refresh(pickHolder, plugin, configManager)
    }

    private fun reopenLoadout(
        player: Player,
        loadoutHolder: LoadoutScreenHolder,
        plugin: org.bukkit.plugin.java.JavaPlugin,
        configManager: sky4th.dungeon.config.ConfigManager
    ) {
        object : BukkitRunnable() {
            override fun run() {
                LoadoutUI.openWithHolder(player, loadoutHolder, plugin, configManager)
            }
        }.runTaskLater(plugin, 1L)
    }
}
