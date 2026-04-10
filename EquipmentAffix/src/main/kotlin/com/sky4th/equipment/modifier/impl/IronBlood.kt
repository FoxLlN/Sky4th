
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack

/**
 * 铁血词条
 * 效果：每次攻击时根据当前血量百分比增伤
 * 1级：每损失10%生命增伤2%，上限16%
 * 2级：每损失10%生命增伤3%，上限20%
 * 3级：每损失10%生命增伤4%，上限24%
 */
class IronBlood : com.sky4th.equipment.modifier.ConfiguredModifier("iron_blood") {

    companion object {
        // 每级配置：(每10%增伤, 最大增伤)
        private val CONFIG = arrayOf(
            0.02 to 0.16,       // 每10%生命增伤2%，上限16%
            0.03 to 0.20,       // 每10%生命增伤3%，上限20%
            0.04 to 0.24        // 每10%生命增伤4%，上限24%
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
        // 处理实体伤害事件
        if (event !is EntityDamageEvent || playerRole != PlayerRole.ATTACKER) {
            return
        }

        // 获取玩家的最大生命值和当前生命值
        @Suppress("DEPRECATION")
        val maxHealth = player.maxHealth
        val currentHealth = player.health

        // 计算生命值损失百分比
        val healthLostPercent = ((maxHealth - currentHealth) / maxHealth) * 100

        // 获取当前等级的配置
        val (bonusPer10, maxBonus) = CONFIG.getOrNull(level - 1) ?: return

        // 计算增伤（每损失10%生命值增加对应的增伤，不满10%的部分不计入）
        val tenPercentBlocks = (healthLostPercent / 10.0).toInt()
        val damageBonus = tenPercentBlocks * bonusPer10
        val cappedDamageBonus = minOf(damageBonus, maxBonus)

        // 如果增伤为0，直接返回
        if (cappedDamageBonus <= 0) return

        // 获取原始伤害
        val damage = event.damage

        // 计算增伤后的伤害
        val newDamage = damage * (1 + cappedDamageBonus)

        // 设置新伤害
        event.damage = newDamage
    }
}
