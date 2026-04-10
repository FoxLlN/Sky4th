
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.util.DamageTypeUtil
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.inventory.ItemStack

/**
 * 海洋祝福词条
 * 效果：
 * - 在水中获得水下呼吸效果
 * - 获得一定所有伤害减免（5%/8%/10%）
 */
class OceanBlessing : com.sky4th.equipment.modifier.ConfiguredModifier("ocean_blessing") {

    // 每级的伤害减免百分比
    private val CONFIG = doubleArrayOf(0.05, 0.08, 0.10)

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(EntityDamageEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: com.sky4th.equipment.modifier.config.PlayerRole
    ) {
        // 检查是否是玩家受到伤害
        if (event !is EntityDamageEvent || playerRole != com.sky4th.equipment.modifier.config.PlayerRole.DEFENDER) {
            return
        }

        // 检查伤害类型是否可以基础减伤
        if (!DamageTypeUtil.isBasicReduction(event)) {
            return
        }

        // 从CONFIG数组获取减伤比例（level从1开始，数组索引从0开始）
        val reduction = CONFIG.getOrElse(level - 1) { CONFIG[0] }

        // 获取当前基础伤害
        val damage = event.damage

        // 计算减伤后的伤害
        val newDamage = damage * (1.0 - reduction)

        // 设置基础伤害
        event.damage = newDamage
    }

    // 当词条初始化时，添加水下呼吸效果
    override fun onInit(player: Player, item: ItemStack, level: Int) {
        // 添加无限水下呼吸效果（等级2，无粒子效果，无图标，环境效果）
        val waterBreathingEffect = PotionEffect(
            PotionEffectType.WATER_BREATHING,
            PotionEffect.INFINITE_DURATION,
            1,  // 等级 II
            false,  // 无粒子
            false,  // 无图标
            true  // 环境效果（让效果更隐蔽）
        )
        player.addPotionEffect(waterBreathingEffect)
    }

    // 当词条被移除时，清除水下呼吸效果
    override fun onRemove(player: Player) {
        player.removePotionEffect(PotionEffectType.WATER_BREATHING)
    }
}
