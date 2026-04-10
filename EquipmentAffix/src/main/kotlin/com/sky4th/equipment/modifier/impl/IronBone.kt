package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import com.sky4th.equipment.util.DamageTypeUtil
import com.sky4th.equipment.util.PlayereffectUtil
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * 铁骨护甲词条
 * 效果：5%概率减少20%的基础伤害，只对基础伤害类型生效
 */
class IronBone : com.sky4th.equipment.modifier.ConfiguredModifier("iron_bone") {

    override fun getEventTypes(): List<Class<out org.bukkit.event.Event>> = 
        listOf(EntityDamageEvent::class.java)

    override fun handle(
        event: org.bukkit.event.Event, 
        player: Player, 
        item: ItemStack, 
        level: Int, 
        playerRole: com.sky4th.equipment.modifier.config.PlayerRole
    ) {
        // 检查是否是玩家受到伤害
        if (event !is EntityDamageEvent || playerRole != PlayerRole.DEFENDER) {
            return
        }

        // 检查伤害类型是否可以基础减伤
        if (!DamageTypeUtil.isBasicReduction(event)) {
            return
        }

        // 5%概率触发
        if (Random.nextDouble() >= 0.05) return

        // 获取当前基础伤害
        val damage = event.damage

        // 计算减伤后的伤害（减少20%）
        val newDamage = damage * 0.8

        // 设置基础伤害
        event.damage = newDamage

        // 播放触发特效
        PlayereffectUtil.playCircleParticle(player, Particle.DAMAGE_INDICATOR, 5)
        PlayereffectUtil.playSound(player, Sound.ITEM_ARMOR_EQUIP_IRON)
    }
}
