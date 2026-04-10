package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import com.sky4th.equipment.util.PlayereffectUtil
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * 坚固词条
 * 效果：挖掘时，有20%概率不消耗耐久
 */
class Solid : com.sky4th.equipment.modifier.ConfiguredModifier("solid") {

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

        // 确保事件涉及的物品是当前装备
        if (event.item != item) {
            return
        }

        // 20%概率触发
        if (Random.nextDouble() >= 0.3) return

        // 取消耐久度消耗
        event.isCancelled = true        
    }
}
