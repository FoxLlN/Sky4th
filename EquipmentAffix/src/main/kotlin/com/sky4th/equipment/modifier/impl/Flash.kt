
package com.sky4th.equipment.modifier.impl

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.inventory.ItemStack

/**
 * 闪光词条
 * 效果：手持工具获得夜视效果（无限夜视）
 * 当词条被移除时，清除夜视效果
 */
class Flash : com.sky4th.equipment.modifier.ConfiguredModifier("flash") {

    // 不监听任何事件，只依赖onInit和onRemove
    override fun getEventTypes(): List<Class<out Event>> = emptyList()

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: com.sky4th.equipment.modifier.config.PlayerRole
    ) {
        // 不需要处理事件
    }

    // 当词条初始化时，添加夜视效果
    override fun onInit(player: Player, item: ItemStack, level: Int) {
        // 添加无限夜视效果（等级1，无粒子效果，无图标，环境效果）
        val nightVisionEffect = PotionEffect(
            PotionEffectType.NIGHT_VISION,
            PotionEffect.INFINITE_DURATION,
            0,  // 等级 I
            false,  // 无粒子
            false,  // 无图标
            true  // 环境效果（让效果更隐蔽）
        )
        player.addPotionEffect(nightVisionEffect)
    }

    // 当词条被移除时，清除夜视效果
    override fun onRemove(player: Player) {
        player.removePotionEffect(PotionEffectType.NIGHT_VISION)
    }
}
