package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import com.sky4th.equipment.util.PlayereffectUtil
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * 传导词条
 * 效果：攻击时，有10%概率额外攻击周围1格内的另一个目标，造成50%的本次伤害
 * 注意：传导效果不连续触发，即A打B触发传导给C，C不会继续传导
 */
class Conduction : com.sky4th.equipment.modifier.ConfiguredModifier("conduction") {

    // 存储正在传导中的实体，防止连续触发
    private val conductingEntities = mutableSetOf<LivingEntity>()

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(EntityDamageEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        val plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("EquipmentAffix") ?: return

        // 处理实体伤害事件
        if (event !is EntityDamageByEntityEvent || playerRole != PlayerRole.ATTACKER) {
            return
        }

        // 获取攻击者和受害者
        val attacker = event.damager
        val victim = event.entity

        // 检查受害者是否是正在传导中的实体（防止连续触发）
        if (victim is LivingEntity && victim in conductingEntities) {
            return
        }

        // 检查攻击者是否是玩家
        if (attacker !is Player) {
            return
        }

        // 10%概率触发
        if (Random.nextDouble() >= 0.1) return

        // 获取原始伤害
        val damage = event.damage

        // 计算传导伤害（50%）
        val conductionDamage = damage * 0.5

        // 获取受害者位置
        val victimLocation = victim.location

        // 获取周围1格内的其他生物实体
        val nearbyEntities = victimLocation.world?.getNearbyEntities(victimLocation, 1.0, 1.0, 1.0)
            ?.filterIsInstance<LivingEntity>()
            ?.filter { entity ->
                // 排除攻击者和受害者
                entity != attacker && entity != victim &&
                // 排除已经死亡或无敌的实体
                entity.health > 0 &&
                // 排除正在传导中的实体
                entity !in conductingEntities
            }
            ?.toList() ?: emptyList()

        // 如果没有可传导的目标，直接返回
        if (nearbyEntities.isEmpty()) {
            return
        }

        // 随机选择一个目标进行传导
        val target = nearbyEntities.random()

        // 将目标添加到传导集合中，防止连续触发
        conductingEntities.add(target)

        // 造成50%的伤害
        target.damage(conductionDamage, attacker)

        // 播放特效
        PlayereffectUtil.playCircleParticle(target, Particle.ELECTRIC_SPARK, 5)

        // 延迟1刻后从传导集合中移除目标
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            conductingEntities.remove(target)
        }, 1L ) 
    }
}
