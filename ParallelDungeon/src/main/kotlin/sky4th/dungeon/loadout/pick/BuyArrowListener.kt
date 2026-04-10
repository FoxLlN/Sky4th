package sky4th.dungeon.loadout.pick

import sky4th.dungeon.loadout.screen.LoadoutUI
import sky4th.dungeon.loadout.shop.LoadoutShopAPI
import sky4th.dungeon.util.LanguageUtil.sendLang
import sky4th.core.api.EconomyAPI
import sky4th.core.api.StorageAPI
import sky4th.core.model.StorageEntry
import sky4th.core.model.StorageEntryType
import sky4th.dungeon.command.DungeonContext
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.meta.Damageable
import org.bukkit.scheduler.BukkitRunnable
import sky4th.dungeon.config.ConfigManager

/**
 * “购买箭矢”界面点击：返回则关闭并回到选择箭矢界面；内容区首次点击选中、再次点击购买。
 */
class BuyArrowListener : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder ?: return
        if (!BuyArrowUI.isBuyArrowGui(holder)) return

        event.isCancelled = true
        val who = event.whoClicked
        if (who !is Player) return

        val buyHolder = holder as BuyArrowHolder
        val loadoutHolder = buyHolder.loadoutHolder
        val ctx = DungeonContext.get() ?: return
        val plugin = ctx.plugin
        val configManager = ctx.configManager
        val slot = event.rawSlot

        if (slot == BuyArrowUI.SLOT_BACK) {
            who.closeInventory()
            reopenPickArrow(who, loadoutHolder, plugin, configManager)
            return
        }

        val contentIndex = BuyArrowUI.getContentSlotIndex(slot) ?: return
        val configs = buyHolder.arrowConfigs
        if (contentIndex >= configs.size) return
        val config = configs[contentIndex]

        if (buyHolder.selectedIndex == contentIndex) {
            // 第二次点击：确认购买
            fun failAndClear(messageKey: String, vararg pairs: Pair<String, Any>) {
                who.sendLang(plugin, messageKey, *pairs)
                buyHolder.selectedIndex = -1
                BuyArrowUI.refreshContent(event.inventory, configManager, plugin)
            }
            if (!EconomyAPI.isAvailable()) {
                failAndClear("shop.economy-unavailable")
                return
            }
            if (!StorageAPI.isAvailable()) {
                failAndClear("shop.storage-unavailable")
                return
            }
            val price = config.buyPrice.toDouble()
            if (!EconomyAPI.hasEnough(who, price)) {
                failAndClear("shop.not-enough", "cost" to EconomyAPI.format(price))
                return
            }
            val entries = StorageAPI.getStorage(who.uniqueId, StorageAPI.STORAGE_SLOT_COUNT)
            val emptySlot = entries.indexOfFirst { it == null }
            if (emptySlot < 0) {
                failAndClear("storage.full")
                return
            }
            if (!EconomyAPI.withdraw(who, price)) {
                failAndClear("shop.purchase-failed")
                return
            }
            val itemToGive = LoadoutShopAPI.createPurchasedItem(plugin, config)
            val entryCount = when (config.id) {
                "normal_arrow" -> 16
                "baozha_jian", "guangling_jian", "huanman_jian", "zhongdu_jian", "zhiliao_jian", "xunjie_jian" -> 1
                else -> itemToGive.amount
            }
            val maxD = itemToGive.type.maxDurability.toInt()
            val durability = if (maxD > 0) {
                val meta = itemToGive.itemMeta as? Damageable
                if (meta != null) (maxD - meta.damage).coerceIn(0, maxD) else null
            } else null
            val entry = StorageEntry(
                type = StorageEntryType.LOADOUT,
                count = entryCount,
                loadoutId = config.id,
                durability = durability?.takeIf { it > 0 }
            )
            StorageAPI.setSlot(who.uniqueId, emptySlot, entry)
            who.sendLang(plugin, "shop.purchased-to-storage", "item" to config.name.replace("&", "§"), "cost" to EconomyAPI.format(price))
            buyHolder.selectedIndex = -1
        } else {
            buyHolder.selectedIndex = contentIndex
        }
        BuyArrowUI.refreshContent(event.inventory, configManager, plugin)
    }

    private fun reopenPickArrow(
        player: Player,
        loadoutHolder: sky4th.dungeon.loadout.screen.LoadoutScreenHolder,
        plugin: org.bukkit.plugin.java.JavaPlugin,
        configManager: ConfigManager
    ) {
        object : BukkitRunnable() {
            override fun run() {
                PickArrowFromStorageUI.open(player, loadoutHolder, plugin, configManager)
            }
        }.runTaskLater(plugin, 1L)
    }
}
