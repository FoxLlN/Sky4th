package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.util.DamageTypeUtil
import com.sky4th.equipment.util.PlayereffectUtil
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * 间隙词条
 * 效果：有5%概率闪避本次攻击
 */
class Gap : com.sky4th.equipment.modifier.ConfiguredModifier("gap") {

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

        // 检查伤害类型是否可以闪避
        if (!DamageTypeUtil.isEvadable(event)) {
            return
        }

        // 5%概率触发
        if (Random.nextDouble() >= 0.05) return

        // 设置伤害为0
        event.damage = 0.0

        // 播放触发特效
        PlayereffectUtil.playCircleParticle(player, Particle.CLOUD, 10)
        PlayereffectUtil.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP)
    }
}
