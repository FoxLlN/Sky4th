package sky4th.dungeon.player

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.plugin.java.JavaPlugin

/**
 * 背包物品变化监听器
 * 监听玩家背包中物品的进入和离开，自动更新背包价值头颅
 */
class BackpackChangeListener(
    private val plugin: JavaPlugin,
    private val backpackManager: BackpackManager
) : Listener {

    /**
     * 监听玩家点击物品栏事件
     * 检测物品是否被移动、添加或移除
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val clickedInventory = event.clickedInventory

        // 处理玩家背包操作
        if (clickedInventory?.holder == player) {
            // 检查是否会影响背包内容
            val affectsInventory = when (event.action) {
                InventoryAction.MOVE_TO_OTHER_INVENTORY,
                InventoryAction.HOTBAR_MOVE_AND_READD,
                InventoryAction.HOTBAR_SWAP,
                InventoryAction.PICKUP_ALL,
                InventoryAction.PICKUP_HALF,
                InventoryAction.PICKUP_ONE,
                InventoryAction.PICKUP_SOME,
                InventoryAction.PLACE_ALL,
                InventoryAction.PLACE_ONE,
                InventoryAction.PLACE_SOME,
                InventoryAction.SWAP_WITH_CURSOR,
                InventoryAction.DROP_ALL_CURSOR,
                InventoryAction.DROP_ONE_CURSOR,
                InventoryAction.DROP_ALL_SLOT,
                InventoryAction.DROP_ONE_SLOT -> true
                else -> false
            }

            if (affectsInventory) {
                // 延迟1tick更新，确保物品操作已完成
                plugin.server.scheduler.runTaskLater(plugin, Runnable {
                    if (player.isOnline) {
                        backpackManager.updateBackpackValueHead(player)
                    }
                }, 1L)
            }
            return
        }

        // 处理容器操作（包括shift+数字键从容器移动物品到背包）
        if (clickedInventory != null && clickedInventory.holder != player) {
            // 检查是否会影响玩家背包
            val affectsPlayerInventory = when (event.action) {
                InventoryAction.MOVE_TO_OTHER_INVENTORY, // shift+点击
                InventoryAction.HOTBAR_MOVE_AND_READD,    // shift+数字键
                InventoryAction.HOTBAR_SWAP,             // shift+数字键交换
                InventoryAction.PICKUP_ALL,               // 双击
                InventoryAction.PICKUP_HALF,
                InventoryAction.PICKUP_ONE,
                InventoryAction.PICKUP_SOME,
                InventoryAction.PLACE_ALL,                // 放入容器
                InventoryAction.PLACE_ONE,
                InventoryAction.PLACE_SOME,
                InventoryAction.SWAP_WITH_CURSOR,
                InventoryAction.COLLECT_TO_CURSOR -> true // 双击收集
                else -> false
            }

            if (affectsPlayerInventory) {
                // 延迟1tick更新，确保物品操作已完成
                plugin.server.scheduler.runTaskLater(plugin, Runnable {
                    if (player.isOnline) {
                        backpackManager.updateBackpackValueHead(player)
                    }
                }, 1L)
            }
        }
    }

    /**
     * 监听玩家捡起物品事件
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerPickupItem(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return

        // 延迟1tick更新，确保物品已经添加到背包
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            if (player.isOnline) {
                backpackManager.updateBackpackValueHead(player)
            }
        }, 1L)
    }

    /**
     * 监听玩家丢弃物品事件
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        val player = event.player

        // 延迟1tick更新，确保物品已经从背包移除
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            if (player.isOnline) {
                backpackManager.updateBackpackValueHead(player)
            }
        }, 1L)
    }
}
