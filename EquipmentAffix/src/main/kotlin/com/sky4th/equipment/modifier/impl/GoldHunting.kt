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
 * 猎金词条
 * 效果：击杀实体时，有5%概率获得额外金粒
 */
class GoldHunting : com.sky4th.equipment.modifier.ConfiguredModifier("gold_hunting") {

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(EntityDeathEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 处理实体死亡事件
        if (event !is EntityDeathEvent || playerRole != PlayerRole.ATTACKER) {
            return
        }

        // 5%概率触发
        if (Random.nextDouble() >= 0.05) return
        

        // 生成1-5个金粒
        val amount = Random.nextInt(1, 6)
        val goldNugget = ItemStack(Material.GOLD_NUGGET, amount)

        // 掉落到死亡实体的位置
        event.entity.location.world?.dropItemNaturally(event.entity.location, goldNugget)
    }
}
