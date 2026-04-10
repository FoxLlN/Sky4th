package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * 聚宝词条
 * 效果：钓鱼时有10%/20%/30%获得双倍掉落
 */
class TreasureGathering : com.sky4th.equipment.modifier.ConfiguredModifier("treasure_gathering") {

    companion object {
        // 每级双倍掉落概率
        private val DOUBLE_DROP_CHANCE = doubleArrayOf(0.10, 0.20, 0.30)
    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(
            PlayerFishEvent::class.java
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

        // 检查是否是 PlayerFishEvent
        if (event !is PlayerFishEvent) {
            return
        }

        // 只在成功钓到鱼时触发
        if (event.state != PlayerFishEvent.State.CAUGHT_FISH) {
            return
        }

        // 获取当前等级的双倍掉落概率
        val chance = DOUBLE_DROP_CHANCE[level - 1]

        // 检查是否触发双倍掉落
        if (Random.nextDouble() >= chance) {
            return
        }

        // 获取钓到的实体
        val caughtEntity = event.caught ?: return

        // 检查是否是物品实体
        if (caughtEntity !is Item) {
            return
        }

        // 获取物品实体持有的物品堆
        val caughtItem = caughtEntity.itemStack

        // 直接将物品数量翻倍
        caughtItem.amount *= 2
    }
}
