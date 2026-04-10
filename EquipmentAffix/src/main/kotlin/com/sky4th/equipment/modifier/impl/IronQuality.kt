package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.inventory.ItemStack

/**
 * 铁质词条
 * 效果：每次耐久度消耗降低20%/35%/50%
 */
class IronQuality : com.sky4th.equipment.modifier.ConfiguredModifier("iron_quality") {

    companion object {
        // 每级耐久度消耗降低比例
        private val DURABILITY_REDUCTION = doubleArrayOf(0.20, 0.35, 0.50)
    }

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

        // 获取当前等级的耐久度降低比例
        val reduction = DURABILITY_REDUCTION[level - 1]

        // 计算新的耐久度消耗
        val originalDamage = event.damage
        val newDamage = (originalDamage * (1 - reduction)).toInt()

        // 确保至少消耗1点耐久度
        if (newDamage <= 0) {
            event.isCancelled = true
        } else {
            event.damage = newDamage
        }
    }
}
