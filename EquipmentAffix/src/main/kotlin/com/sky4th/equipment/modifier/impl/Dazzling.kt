package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.util.PlayereffectUtil
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * 炫目词条
 * 效果：受到攻击时有5%概率使攻击者获得眩晕效果2秒（255级缓慢）
 */
class Dazzling : com.sky4th.equipment.modifier.ConfiguredModifier("dazzling") {

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

        // 检查是否是实体攻击事件
        if (event !is org.bukkit.event.entity.EntityDamageByEntityEvent) {
            return
        }

        // 获取攻击者
        val attacker = event.damager

        // 检查攻击者是否是生物实体
        if (attacker !is LivingEntity) {
            return
        }

        // 5%概率触发
        if (Random.nextDouble() >= 0.05) return

        // 给攻击者添加眩晕效果（255级缓慢，持续2秒）
        val slownessEffect = PotionEffect(
            PotionEffectType.SLOWNESS,
            40, // 2秒 = 40 ticks (20 ticks per second)
            254 // 255级缓慢（等级 = 强度 - 1，所以254对应255级）
        )
        attacker.addPotionEffect(slownessEffect)

        // 播放特效
        PlayereffectUtil.playSound(attacker, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.4, 0.6, 0.7, 0.9)
    }
}
