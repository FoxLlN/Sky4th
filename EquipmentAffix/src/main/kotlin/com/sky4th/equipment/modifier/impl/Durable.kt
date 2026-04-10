package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * 耐用词条
 * 效果：消耗耐久度的时候有10%概率不消耗
 */
class Durable : com.sky4th.equipment.modifier.ConfiguredModifier("durable") {

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(
            PlayerItemDamageEvent::class.java
        )

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 检查是否是玩家自己的事件
        if (playerRole != PlayerRole.SELF) {
            return
        }

        // 检查是否是 PlayerItemDamageEvent
        if (event !is PlayerItemDamageEvent) {
            return
        }

        // 10%概率触发
        if (Random.nextDouble() >= 0.1) return

        // 取消耐久度消耗
        event.isCancelled = true
    }
}
