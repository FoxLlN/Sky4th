package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import com.sky4th.equipment.modifier.manager.UnifiedModifierManager
import com.sky4th.equipment.modifier.manager.handlers.EchoLocationHandler
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack

/**
 * 回声词条
 * 效果：潜行时，可以显示周围生物的位置
 * 潜行时对周围3格内的所有生物造成发光效果
 */
class EchoLocation : com.sky4th.equipment.modifier.ConfiguredModifier("echo_location") {

    // 监听玩家潜行事件
    override fun getEventTypes(): List<Class<out Event>> =
        listOf(org.bukkit.event.player.PlayerToggleSneakEvent::class.java)

    // 处理玩家潜行事件
    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        if (event !is org.bukkit.event.player.PlayerToggleSneakEvent) return
        if (playerRole != PlayerRole.SELF) return

        // 玩家开始潜行
        if (event.isSneaking) {
            // 检查玩家是否已有回声定位词条数据
            val echoData = UnifiedModifierManager.getAffixData(player, getAffixId()) as? EchoLocationHandler.EchoLocationData

            // 如果没有回声定位词条数据，创建并注册
            if (echoData == null) {
                val newData = EchoLocationHandler.EchoLocationData(
                    uuid = player.uniqueId,
                    item = item  // 直接传入物品
                )
                UnifiedModifierManager.addPlayerAffix(player, getAffixId(), newData)
            }
        } else {
            // 玩家停止潜行，从统一管理器中移除回声定位词条数据
            UnifiedModifierManager.removePlayerAffix(player, getAffixId())
        }
    }
}
