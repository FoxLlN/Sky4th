package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import com.sky4th.equipment.util.PlayereffectUtil
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * 生息词条
 * 效果：击杀生物时，有10%概率恢复1点生命值
 */
class Vitality : com.sky4th.equipment.modifier.ConfiguredModifier("vitality") {

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(EntityDeathEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 确保事件是EntityDeathEvent
        if (event !is EntityDeathEvent || playerRole != PlayerRole.ATTACKER) {
            return
        }

        // 确保击杀者是玩家
        if (event.entity.killer != player) return

        // 10%概率触发
        if (Random.nextDouble() >= 0.1) return

        // 恢复1点生命值（使用heal方法会触发EntityRegainHealthEvent）
        player.heal(1.0)

        // 播放触发特效
        PlayereffectUtil.playCircleParticle(player, Particle.HEART, 4)
        PlayereffectUtil.playSound(player, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5, 0.5, 1.5, 1.5)
    }
}
