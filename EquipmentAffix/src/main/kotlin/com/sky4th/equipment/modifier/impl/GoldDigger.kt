package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * 淘金词条
 * 效果：破坏方块时，有5%概率获得额外金粒
 */
class GoldDigger : com.sky4th.equipment.modifier.ConfiguredModifier("gold_digger") {

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(BlockBreakEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 处理方块破坏事件
        if (event !is BlockBreakEvent || playerRole != PlayerRole.SELF) {
            return
        }
   
        // 5%概率触发
        if (Random.nextDouble() >= 0.05) return

        // 生成1-5个金粒
        val amount = Random.nextInt(1, 6)
        val goldNugget = ItemStack(Material.GOLD_NUGGET, amount)

        // 掉落到方块位置
        event.block.location.world?.dropItemNaturally(event.block.location, goldNugget)
    }
}
