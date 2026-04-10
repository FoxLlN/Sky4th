package com.sky4th.equipment.modifier.listener

import com.sky4th.equipment.modifier.manager.FreezeManager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import sky4th.core.api.MarkAPI

/**
 * 跳跃监听器
 * 处理与定身效果不可跳跃的玩家事件
 */
class JumpListener : Listener {

    /**
     * 处理玩家跳跃事件
     * 阻止冰冻玩家，束缚玩家跳跃
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerJump(event: com.destroystokyo.paper.event.player.PlayerJumpEvent) {
        val player = event.player
        // 检查玩家是否被冰冻
        if (FreezeManager.isFrozenEntity(player) || MarkAPI.hasMark(player, "bind")) {
            // 取消跳跃事件
            event.isCancelled = true
        }
    }
}
