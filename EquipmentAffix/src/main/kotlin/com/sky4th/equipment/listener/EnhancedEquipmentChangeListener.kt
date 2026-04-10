
package com.sky4th.equipment.listener

import com.sky4th.equipment.EquipmentAffix
import com.sky4th.equipment.data.NBTEquipmentDataManager
import com.sky4th.equipment.manager.EquipmentAttributesManager
import com.sky4th.equipment.modifier.ModifierManager
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent

/**
 * 增强版装备变更监听器
 * 全面监听所有可能导致装备变更的事件，确保活跃词条及时更新
 */
class EnhancedEquipmentChangeListener(
    private val plugin: EquipmentAffix,
    private val attributesManager: EquipmentAttributesManager,
    private val modifierManager: ModifierManager
) : Listener {

    // 用于记录玩家上次装备状态，避免不必要的更新
    private val lastEquipmentState = mutableMapOf<java.util.UUID, EquipmentState>()

    /**
     * 装备状态数据类
     */
    private data class EquipmentState(
        val mainHand: String?,
        val offHand: String?,
        val helmet: String?,
        val chestplate: String?,
        val leggings: String?,
        val boots: String?
    )

    /**
     * 监听玩家加入事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        // 延迟更新，确保其他插件完成初始化
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            updateAll(player)
        }, 2L)
    }

    /**
     * 监听玩家退出事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        // 清理玩家数据
        clearPlayerData(player)
    }

    /**
     * 监听玩家重生事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerRespawn(event: org.bukkit.event.player.PlayerRespawnEvent) {
        val player = event.player
        // 延迟更新，确保重生完成
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            updateAll(player)
        }, 2L)
    }

    /**
     * 监听玩家盔甲变更事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onArmorChange(event: PlayerArmorChangeEvent) {
        val player = event.player
        val newItem = event.newItem
        val oldItem = event.oldItem

        // 检查是否需要更新
        val isUpdate = shouldUpdate(newItem, oldItem)
        if (!isUpdate) return

        // 延迟更新，确保物品切换完成
        plugin.server.scheduler.runTask(plugin, Runnable {
            updateAll(player)
        })
    }

    /**
     * 监听玩家手持物品变更事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onItemHeld(event: PlayerItemHeldEvent) {
        val player = event.player
        val newItem = player.inventory.getItem(event.newSlot)
        val oldItem = player.inventory.getItem(event.previousSlot)

        // 检查是否需要更新
        val isUpdate = shouldUpdate(newItem, oldItem)
        if (!isUpdate) return

        // 延迟更新，确保物品切换完成
        plugin.server.scheduler.runTask(plugin, Runnable {
            updateAll(player)
        })
    }

    /**
     * 监听玩家切换主副手物品事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onSwapHandItems(event: PlayerSwapHandItemsEvent) {
        val player = event.player
        val mainHandItem = event.mainHandItem
        val offHandItem = event.offHandItem

        // 检查是否需要更新
        val isUpdate = shouldUpdate(mainHandItem, offHandItem)
        if (!isUpdate) return

        // 延迟更新，确保物品切换完成
        plugin.server.scheduler.runTask(plugin, Runnable {
            updateAll(player)
        })
    }

    /**
     * 监听玩家捡起物品事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerPickupItem(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return
        val item = event.item.itemStack

        // 只处理装备物品
        if (!NBTEquipmentDataManager.isEquipment(item)) {
            return
        }

        // 延迟更新，确保物品捡起完成
        plugin.server.scheduler.runTask(plugin, Runnable {
            updateAll(player)
        })
    }

    /**
     * 监听玩家丢弃物品事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        val player = event.player
        val item = event.itemDrop.itemStack

        // 只处理装备物品
        if (!NBTEquipmentDataManager.isEquipment(item)) {
            return
        }

        // 延迟更新，确保物品丢弃完成
        plugin.server.scheduler.runTask(plugin, Runnable {
            updateAll(player)
        })
    }

    /**
     * 监听物品栏打开事件
     * 记录玩家打开界面前的装备状态
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val player = event.player as? Player ?: return

        // 记录当前装备状态
        lastEquipmentState[player.uniqueId] = getCurrentEquipmentState(player)
    }

    /**
     * 监听物品栏关闭事件
     * 检查装备状态是否变化，如果变化则更新
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return

        // 检查装备状态是否变化
        if (!hasEquipmentStateChanged(player)) {
            return
        }

        // 延迟更新，确保物品栏关闭完成
        plugin.server.scheduler.runTask(plugin, Runnable {
            updateAll(player)
        })
    }

    /**
     * 检查是否需要更新
     * @param newItem 新物品
     * @param oldItem 旧物品
     * @return 如果需要更新返回true，否则返回false
     */
    private fun shouldUpdate(newItem: org.bukkit.inventory.ItemStack?, oldItem: org.bukkit.inventory.ItemStack?): Boolean {
        // 如果新旧物品都是装备且装备ID相同，检查NBT数据是否变化
        if (oldItem != null && newItem != null &&
            NBTEquipmentDataManager.isEquipment(oldItem) &&
            NBTEquipmentDataManager.isEquipment(newItem)) {
            val oldId = NBTEquipmentDataManager.getEquipmentId(oldItem)
            val newId = NBTEquipmentDataManager.getEquipmentId(newItem)
            if (oldId == newId) {
                // 检查材质效果是否变化
                val oldMaterialEffect = NBTEquipmentDataManager.getMaterialEffect(oldItem)
                val newMaterialEffect = NBTEquipmentDataManager.getMaterialEffect(newItem)
                if (oldMaterialEffect != newMaterialEffect) {
                    return true
                }

                // 检查词条是否变化
                val oldAffixes = NBTEquipmentDataManager.getAffixes(oldItem)
                val newAffixes = NBTEquipmentDataManager.getAffixes(newItem)
                if (oldAffixes != newAffixes) {
                    return true
                }

                // 其他NBT数据变化不需要更新词条
                return false
            }
        }

        // 装备ID不同或物品类型不同，需要更新
        return true
    }

    /**
     * 检查装备状态是否变化
     */
    private fun hasEquipmentStateChanged(player: Player): Boolean {
        val currentState = getCurrentEquipmentState(player)
        val lastState = lastEquipmentState[player.uniqueId]

        if (lastState == null) {
            lastEquipmentState[player.uniqueId] = currentState
            return true
        }

        val hasChanged = currentState != lastState
        if (hasChanged) {
            lastEquipmentState[player.uniqueId] = currentState
        }

        return hasChanged
    }

    /**
     * 获取当前装备状态
     */
    private fun getCurrentEquipmentState(player: Player): EquipmentState {
        return EquipmentState(
            mainHand = getEquipmentId(player.inventory.itemInMainHand),
            offHand = getEquipmentId(player.inventory.itemInOffHand),
            helmet = getEquipmentId(player.inventory.helmet),
            chestplate = getEquipmentId(player.inventory.chestplate),
            leggings = getEquipmentId(player.inventory.leggings),
            boots = getEquipmentId(player.inventory.boots)
        )
    }

    /**
     * 获取物品的装备ID
     */
    private fun getEquipmentId(item: org.bukkit.inventory.ItemStack?): String? {
        if (item == null || item.type.isAir) {
            return null
        }
        return if (NBTEquipmentDataManager.isEquipment(item)) {
            NBTEquipmentDataManager.getEquipmentId(item)
        } else {
            null
        }
    }

    /**
     * 更新玩家的所有装备相关数据
     */
    private fun updateAll(player: Player) {
        // 使用性能监控
        com.sky4th.equipment.monitor.PerformanceMonitorHelper.monitor("equipment_update") {
            // 更新装备属性
            attributesManager.updatePlayerEquipmentAttributes(player)

            // 更新词条
            modifierManager.updatePlayerModifiers(player)

            // 更新装备状态
            lastEquipmentState[player.uniqueId] = getCurrentEquipmentState(player)
        }
    }

    /**
     * 清理玩家数据
     */
    private fun clearPlayerData(player: Player) {
        // 移除装备状态记录
        lastEquipmentState.remove(player.uniqueId)

        // 清除玩家的词条
        modifierManager.clearPlayerModifiers(player)

        // 清除饥饿累积小数数据
        val hungerFractionKey = org.bukkit.NamespacedKey(plugin, "hunger_fraction")
        player.persistentDataContainer.remove(hungerFractionKey)

        // 从SkyCore缓存中移除玩家属性
        sky4th.core.api.PlayerAttributesAPI.removeFromCache(player.uniqueId)
    }
}
