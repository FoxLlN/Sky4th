package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import com.sky4th.equipment.util.DamageTypeUtil
import com.sky4th.equipment.util.PlayereffectUtil
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * 反震词条
 * 效果：格挡近战攻击后，有概率击晕敌人
 * 1级：20%概率使目标眩晕1秒
 * 2级：25%概率使目标眩晕1.5秒
 * 3级：30%概率使目标眩晕2秒
 */
class Shock : com.sky4th.equipment.modifier.ConfiguredModifier("shock") {

    companion object {
        // 每级配置：(触发概率, 眩晕持续时间(秒))
        private val CONFIG = arrayOf(
            0.20 to 1.0,  // 1级：20%概率，眩晕1秒
            0.25 to 1.5,  // 2级：25%概率，眩晕1.5秒
            0.30 to 2.0   // 3级：30%概率，眩晕2秒
        )
    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(EntityDamageByEntityEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 只处理玩家受到近战攻击的事件
        if (event !is EntityDamageByEntityEvent || playerRole != PlayerRole.DEFENDER) {
            return
        }

        // 检查伤害类型是否为物理伤害（近战）
        if (!DamageTypeUtil.isPhysicalDamage(event)) {
            return
        }

        // 检查玩家是否成功格挡
        val blockDamage = event.getDamage(DamageModifier.BLOCKING)
        if (blockDamage == 0.0) {
            return
        }

        // 获取当前等级的配置
        val (triggerChance, stunDuration) = CONFIG.getOrNull(level - 1) ?: return

        // 检查是否触发反震
        if (Random.nextDouble() > triggerChance) {
            return
        }

        // 获取攻击者
        val attacker = event.damager
        if (attacker !is LivingEntity) {
            return
        }

        // 给攻击者添加眩晕效果（255级缓慢）
        val slownessEffect = PotionEffect(
            PotionEffectType.SLOWNESS,
            (stunDuration * 20).toInt(), // 转换为ticks
            254 // 255级缓慢（等级 = 强度 - 1，所以254对应255级）
        )
        attacker.addPotionEffect(slownessEffect)

        // 播放特效
        PlayereffectUtil.playSound(attacker, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.4, 0.6, 0.7, 0.9)
    }
}
