package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import com.sky4th.equipment.util.BlockTypeUtil
import com.sky4th.equipment.util.PlayereffectUtil
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * 砺刃词条
 * 效果：站在石头类方块上使用武器/工具时(挖掘/攻击生物)，有50%概率不消耗耐久
 */
class StoneSharp : com.sky4th.equipment.modifier.ConfiguredModifier("stone_sharp") {

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

        // 检查玩家脚下的方块是否为石质方块
        val blockBelow = player.location.subtract(0.0, 1.0, 0.0).block
        if (!BlockTypeUtil.isStone(blockBelow)) {
            return
        }

        // 检查是否是 PlayerItemDamageEvent
        if (event !is PlayerItemDamageEvent) {
            return
        }

        if (event.item != item) {
            return
        }
        
        // 50%概率触发
        if (Random.nextDouble() >= 0.5) return

        // 取消耐久度消耗
        event.isCancelled = true
    }
}
