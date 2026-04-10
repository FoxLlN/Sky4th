package com.sky4th.equipment.modifier.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityTeleportEvent
import sky4th.core.api.MarkAPI

/**
 * 抑影监听器
 * 处理与抑制传送效果相关的事件
 */
class ShadowSuppressListener : Listener {

    /**
     * 处理生物传送事件
     * 如果生物被抑制传送，则取消传送
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityTeleport(event: EntityTeleportEvent) {
        val entity = event.entity
        // 检查生物是否有抑影标记
        if (MarkAPI.hasMark(entity, "shadow_suppress")) {
            // 取消传送事件
            event.isCancelled = true
        }
    }
}
