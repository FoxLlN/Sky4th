package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.Material
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.inventory.ItemStack

/**
 * 料理词条
 * 效果：钓鱼时自动煮熟鱼类
 */
class Cooking : com.sky4th.equipment.modifier.ConfiguredModifier("cooking") {

    // 可煮熟的鱼类映射（生鱼 -> 熟鱼）
    private val FISH_COOKING_MAP = mapOf(
        Material.COD to Material.COOKED_COD,
        Material.SALMON to Material.COOKED_SALMON
    )

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

        // 获取钓到的实体
        val caughtEntity = event.caught ?: return

        // 检查是否是物品实体
        if (caughtEntity !is Item) {
            return
        }

        // 获取物品实体持有的物品堆
        val caughtItem = caughtEntity.itemStack

        // 获取物品类型
        val material = caughtItem.type

        // 检查是否是可煮熟的鱼类
        val cookedMaterial = FISH_COOKING_MAP[material] ?: return

        // 创建煮熟的鱼物品堆
        val cookedFish = ItemStack(cookedMaterial)
        cookedFish.amount = caughtItem.amount
        cookedFish.itemMeta = caughtItem.itemMeta?.clone()

        // 替换物品实体持有的物品堆
        caughtEntity.itemStack = cookedFish
    }
}
