package sky4th.dungeon.loadout.equipment

import sky4th.dungeon.player.BackpackManager
import sky4th.dungeon.player.PlayerManager
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.*

/**
 * 装备/背包刷新统一入口：所有可能改变护甲或背包价值的操作都经由此处，
 * 防抖后延迟执行「三套装被动 + 计分板」一次刷新，避免多处监听、延迟不一致导致单双数不更新等问题。
 *
 * 检测流程：
 * 1. 背包内操作：点击/拖拽 → 延迟 2 tick（等物品移动完成）
 * 2. 关闭背包 → 延迟 1 tick
 * 3. 丢弃物品 → 延迟 1 tick（可能脱下装备丢出）
 * 4. 手持右键（可能穿戴护甲，不打开背包）→ 延迟 8 tick（等原版完成穿戴）
 * 5. 拾取物品 → 延迟 1 tick（背包价值变化）
 */
class EquipmentRefreshCoordinator(
    private val plugin: JavaPlugin,
    private val playerManager: PlayerManager,
    private val tiebiSetListener: TiebiSetListener,
    private val chikeSetListener: ChikeSetListener,
    private val xuezheSetListener: XuezheSetListener,
    private val youxiaSetListener: YouxiaSetListener,
    private val samanSetListener: SamanSetListener,
    private val backpackManager: BackpackManager
) : Listener {

    /** 每个玩家当前待执行的刷新任务，用于防抖（新事件到来时取消旧任务再排新延迟） */
    private val pendingRefresh: MutableMap<UUID, BukkitTask> = mutableMapOf()

    private fun scheduleRefresh(player: Player, delayTicks: Long) {
        if (!playerManager.isPlayerInDungeon(player)) return
        val uuid = player.uniqueId
        pendingRefresh[uuid]?.cancel()
        val task = plugin.server.scheduler.runTaskLater(plugin, Runnable {
            pendingRefresh.remove(uuid)
            doFullRefresh(player)
        }, delayTicks)
        pendingRefresh[uuid] = task
    }

    /** 一次刷新：四套装被动 + 计分板 */
    private fun doFullRefresh(player: Player) {
        if (!player.isOnline) return
        tiebiSetListener.refreshPassive(player)
        chikeSetListener.refreshPassive(player)
        xuezheSetListener.refreshPassive(player)
        youxiaSetListener.refreshPassive(player)
        samanSetListener.refreshPassive(player)
        backpackManager.refreshSidebarSkill(player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInventoryClick(event: InventoryClickEvent) {
        val who = event.whoClicked
        if (who !is Player) return
        scheduleRefresh(who, 2L)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInventoryDrag(event: InventoryDragEvent) {
        val who = event.whoClicked
        if (who !is Player) return
        scheduleRefresh(who, 2L)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInventoryClose(event: InventoryCloseEvent) {
        val who = event.player
        if (who !is Player) return
        scheduleRefresh(who, 1L)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        scheduleRefresh(event.player, 1L)
    }

    /** 手持护甲且右键（空气或方块）时排刷新。*/
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        // 防止双击触发：只处理主手点击
        if (event.hand != org.bukkit.inventory.EquipmentSlot.HAND) {
            return
        }
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return
        val item = event.item ?: return
        if (item.type.isEmpty) return
        if (!isEquippableArmor(item.type)) return
        val player = event.player
        if (!playerManager.isPlayerInDungeon(player)) return
        plugin.server.scheduler.runTaskLater(plugin, Runnable { doFullRefresh(player) }, 2L)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityPickupItem(event: EntityPickupItemEvent) {
        if (event.entity !is Player) return
        scheduleRefresh(event.entity as Player, 1L)
    }
}
