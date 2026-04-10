package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * 节弹词条
 * 效果：弓/弩射击时有一定概率不掉落箭矢
 * 1级：20%概率不掉落箭矢
 * 2级：30%概率不掉落箭矢
 * 3级：40%概率不掉落箭矢
 */
class Recycle : com.sky4th.equipment.modifier.ConfiguredModifier("recycle") {

    companion object {
        // 每级配置：触发概率
        private val CONFIG = arrayOf(
            0.20,  // 20%概率
            0.30,  // 30%概率
            0.40   // 40%概率
        )
    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(EntityShootBowEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 只处理射箭事件
        if (event !is EntityShootBowEvent) {
            return
        }

        // 确保是玩家射箭
        if (event.entity !is Player) {
            return
        }

        // 获取要被消耗的箭矢
        val consumableArrow = event.consumable ?: return
        
        // 如果消耗的箭矢为空，说明有"无限"附魔，不处理
        if (consumableArrow.type.name.contains("ARROW", ignoreCase = true).not() &&
            consumableArrow.type.name.contains("SPECTRAL_ARROW", ignoreCase = true).not() &&
            consumableArrow.type.name.contains("TIPPED_ARROW", ignoreCase = true).not()) {
            return
        }

        // 获取当前等级的配置
        val chance = CONFIG.getOrNull(level - 1) ?: return

        // 进行随机判断
        if (Random.nextDouble() < chance) {
            // 节弹触发 在玩家脚下掉落一个对应的弹射物
            val dropItem = consumableArrow.clone()
            dropItem.amount = 1
            player.world.dropItemNaturally(player.location, dropItem)
        }
    }

    override fun onRemove(player: Player) {
    }
}
