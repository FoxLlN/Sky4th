package sky4th.dungeon.player

import sky4th.dungeon.util.LanguageUtil.sendLangSys
import sky4th.dungeon.Dungeon
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

/**
 * 副本内背包交互限制：
 * - 锁定 9*3 背包外圈 12 个格子，玩家不能在这些格子放入/取出物品；
 * - 防止玩家把占位物品丢出，从而绕过格子限制。
 */
class BackpackInteractionListener(
    private val playerManager: PlayerManager,
    private val backpackManager: BackpackManager,
    private val plugin: JavaPlugin
) : Listener {

    private val cashIdKey: NamespacedKey by lazy { NamespacedKey(plugin, "dungeon_cash_id") }
    private val cashValueKey: NamespacedKey by lazy { NamespacedKey(plugin, "dungeon_cash_value") }

    private fun isInDungeon(player: Player): Boolean = playerManager.isPlayerInDungeon(player)

    /**
     * 点击玩家背包时，禁止操作被锁定的格子。
     * 物品进入背包时，检查是否为现金物品并转换为信用点
     */
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        if (!isInDungeon(player)) return

        val action = event.action
        val cursor = event.cursor
        val currentItem = event.currentItem

        // 检查光标中的物品是否为现金物品
        if (action == InventoryAction.PLACE_ALL || action == InventoryAction.PLACE_ONE || 
            action == InventoryAction.PLACE_SOME || action == InventoryAction.SWAP_WITH_CURSOR) {
            val meta = cursor.itemMeta
            if (meta != null) {
                val pdc = meta.persistentDataContainer
                val cashId = pdc.get(cashIdKey, PersistentDataType.STRING)
                if (cashId != null) {
                    // 获取现金价值
                    val cashValue = pdc.get(cashValueKey, PersistentDataType.INTEGER) ?: return

                    // 取消事件，防止物品放入背包
                    event.isCancelled = true

                    // 增加玩家背包现金
                    backpackManager.addPlayerCash(player, cashValue)

                    // 清除光标中的物品
                    player.setItemOnCursor(null)

                    // 发送提示消息
                    player.sendLangSys(plugin, "backpack.cash-received", "value" to cashValue)
                    return
                }
            }
        }

        // 检查点击的物品是否为现金物品（shift+点击的情况）
        if (currentItem != null && (action == InventoryAction.MOVE_TO_OTHER_INVENTORY || 
            action == InventoryAction.PICKUP_ALL || action == InventoryAction.PICKUP_HALF ||
            action == InventoryAction.PICKUP_ONE || action == InventoryAction.PICKUP_SOME)) {
            val meta = currentItem.itemMeta
            if (meta != null) {
                val pdc = meta.persistentDataContainer
                val cashId = pdc.get(cashIdKey, PersistentDataType.STRING)
                if (cashId != null) {
                    // 获取现金价值
                    val cashValue = pdc.get(cashValueKey, PersistentDataType.INTEGER) ?: return

                    // 取消事件，防止物品被操作
                    event.isCancelled = true

                    // 增加玩家背包现金
                    backpackManager.addPlayerCash(player, cashValue)

                    // 移除物品
                    event.currentItem = null

                    // 发送提示消息
                    player.sendLangSys(plugin, "backpack.cash-received", "value" to cashValue)
                    return
                }
            }
        }

        val clickedInv = event.clickedInventory ?: return
        if (clickedInv != player.inventory) return

        val slot = event.slot
        if (!backpackManager.isBlockedStorageSlot(slot)) return

        event.isCancelled = true
    }

    /**
     * 拖动物品到玩家背包时，若包含被锁定的格子则整体取消。
     */
    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        val player = event.whoClicked as? Player ?: return
        if (!isInDungeon(player)) return

        val view = event.view
        for (rawSlot in event.rawSlots) {
            val inventory = view.getInventory(rawSlot) ?: continue
            if (inventory != player.inventory) continue
            val slot = view.convertSlot(rawSlot)
            if (backpackManager.isBlockedStorageSlot(slot)) {
                event.isCancelled = true
                return
            }
        }
    }

    /**
     * 防止玩家将占位物品丢出背包，从而恢复被锁定的格子。
     */
    @EventHandler
    fun onDropPlaceholder(event: PlayerDropItemEvent) {
        val player = event.player
        if (!isInDungeon(player)) return

        val stack = event.itemDrop.itemStack
        if (!backpackManager.isBlockedPlaceholder(stack)) return

        event.isCancelled = true
    }
}

