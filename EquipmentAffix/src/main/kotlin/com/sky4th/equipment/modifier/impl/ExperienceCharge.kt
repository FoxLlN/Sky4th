package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack

/**
 * 经验冲击词条
 * 效果：攻击时自动消耗经验值提升伤害
 * 1级：消耗10点经验增加5%伤害
 * 2级：消耗10点经验增加10%伤害
 * 3级：消耗10点经验增加15%伤害
 * 要求：玩家经验等级大于10级才触发
 */
class ExperienceCharge : com.sky4th.equipment.modifier.ConfiguredModifier("experience_charge") {

    companion object {
        // 每级消耗的经验点数
        private const val CONSUME_AMOUNT = 10
        // 每级增加的伤害百分比
        private val DAMAGE_BONUS = doubleArrayOf(0.05, 0.10, 0.15)
        // 触发所需的最小经验等级
        private const val MIN_EXPERIENCE_LEVEL = 10
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
        // 只处理实体伤害事件，且玩家必须是攻击者
        if (event !is EntityDamageByEntityEvent || playerRole != PlayerRole.ATTACKER) {
            return
        }

        // 获取受击者
        val victim = event.entity
        if (victim !is LivingEntity) {
            return
        }

        // 检查玩家经验等级是否满足要求
        if (player.level < MIN_EXPERIENCE_LEVEL) {
            return
        }

        // 获取当前等级的伤害加成
        val levelIndex = level - 1
        if (levelIndex !in 0..2) return

        val damageBonus = DAMAGE_BONUS[levelIndex]

        // 检查玩家是否有足够的经验点数
        if (player.totalExperience < CONSUME_AMOUNT) {
            return
        }

        // 消耗经验点数
        player.giveExp(-CONSUME_AMOUNT)

        // 计算额外伤害
        val extraDamage = event.damage * damageBonus

        // 增加伤害
        event.damage += extraDamage
    }

    override fun onRemove(player: Player) {}
}
