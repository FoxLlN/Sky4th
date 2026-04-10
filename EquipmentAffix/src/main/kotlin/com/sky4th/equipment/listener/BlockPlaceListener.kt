package com.sky4th.equipment.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.metadata.FixedMetadataValue

/**
 * 方块放置监听器
 * 用于标记玩家放置的方块，以便区分自然生成和玩家放置的方块
 */
class BlockPlaceListener : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        // 为玩家放置的方块添加元数据标记
        val plugin = event.player.server.pluginManager.getPlugin("EquipmentAffix")
        if (plugin != null) {
            event.block.setMetadata("player_placed", FixedMetadataValue(plugin, true))
        }
    }
}
