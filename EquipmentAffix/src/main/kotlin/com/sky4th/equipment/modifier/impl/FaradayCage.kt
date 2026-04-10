package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * 法拉第笼词条
 * 效果：免疫闪电伤害；被闪电击中时获得速度效果
 * 1级：获得速度Ⅰ 5秒
 * 2级：获得速度Ⅰ 10秒
 * 3级：获得速度Ⅰ 15秒
 */
class FaradayCage : com.sky4th.equipment.modifier.ConfiguredModifier("faraday_cage") {

    companion object {
        // 每级配置：(速度效果等级, 持续时间tick)
        private val CONFIG = arrayOf(
            1 to 100,   // 速度Ⅰ 5秒 (100 ticks)
            1 to 200,   // 速度Ⅰ 10秒 (200 ticks)
            1 to 300    // 速度Ⅰ 15秒 (300 ticks)
        )
    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(EntityDamageEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 只处理实体伤害事件，且玩家必须是受害者
        if (event !is EntityDamageEvent || playerRole != PlayerRole.DEFENDER) {
            return
        }

        // 检查伤害类型是否为闪电
        if (event.cause != EntityDamageEvent.DamageCause.LIGHTNING) {
            return
        }

        // 取消闪电伤害
        event.isCancelled = true
        event.damage = 0.0

        // 获取当前等级的配置
        val (amplifier, duration) = CONFIG.getOrNull(level - 1) ?: return

        // 给玩家添加速度效果
        val speedEffect = PotionEffect(PotionEffectType.SPEED, duration, amplifier - 1)
        player.addPotionEffect(speedEffect)
    }
}
