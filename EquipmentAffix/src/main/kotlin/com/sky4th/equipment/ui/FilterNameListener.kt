package com.sky4th.equipment.ui

import com.sky4th.equipment.ui.template.TemplateListManager
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerQuitEvent

/**
 * 筛选名称聊天监听器
 * 处理玩家在聊天框输入的筛选文本
 */
object FilterNameListener : Listener {
    // 存储正在等待输入的玩家
    private val waitingPlayers = mutableSetOf<String>()

    /**
     * 开始等待玩家输入
     */
    fun startWaiting(player: Player) {
        waitingPlayers.add(player.uniqueId.toString())
        player.closeInventory()
        player.sendMessage("§b[筛选] §7请在聊天框输入要筛选的词条名称关键词")
    }

    /**
     * 停止等待玩家输入
     */
    fun stopWaiting(player: Player) {
        waitingPlayers.remove(player.uniqueId.toString())
    }

    /**
     * 检查玩家是否正在等待输入
     */
    fun isWaiting(player: Player): Boolean {
        return waitingPlayers.contains(player.uniqueId.toString())
    }

    @EventHandler
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        val player = event.player
        val playerId = player.uniqueId.toString()

        // 检查玩家是否在等待输入
        if (!waitingPlayers.contains(playerId)) {
            return
        }

        // 取消聊天事件
        event.isCancelled = true

        // 获取输入的文本
        val input = event.message.trim().take(10)
        
        // 获取玩家状态
        val state = TemplateListManager.getPlayerState(player, "affix_list")

        // 更新搜索关键词
        state.searchQuery = input

        // 移除等待状态
        waitingPlayers.remove(playerId)

        // 在主线程中重新打开UI
        org.bukkit.Bukkit.getScheduler().runTask(
            com.sky4th.equipment.EquipmentAffix.instance,
            Runnable {
                sky4th.core.api.UIAPI.openUI(player, "affix_list")
            }
        )
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        // 玩家退出时移除等待状态
        waitingPlayers.remove(event.player.uniqueId.toString())
        // 重置筛选
        resetFilter(event.player)
    }

    /**
     * 重置筛选
     */
    private fun resetFilter(player: Player) {
        val state = TemplateListManager.getPlayerState(player, "affix_list")
        state.searchQuery = ""
    }
}
