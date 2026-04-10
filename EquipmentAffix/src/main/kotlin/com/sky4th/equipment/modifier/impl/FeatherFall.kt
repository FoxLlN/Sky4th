package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack

/**
 * 羽落词条
 * 效果：降落时降低动能伤害
 *
 * 1级：降低40%动能伤害
 * 2级：降低50%动能伤害
 * 3级：降低60%动能伤害
 */
class FeatherFall : com.sky4th.equipment.modifier.ConfiguredModifier("feather_fall") {

    companion object {
        // 每级伤害减免比例（40%, 50%, 60%）
        private val DAMAGE_REDUCTION = arrayOf(0.40, 0.50, 0.60)
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
        // 只处理玩家受到伤害的事件
        if (event !is EntityDamageEvent || playerRole != PlayerRole.DEFENDER) {
            return
        }

        // 只处理坠落伤害
        if (event.cause !=  EntityDamageEvent.DamageCause.FLY_INTO_WALL) {
            return
        }

        // 获取当前等级的伤害减免比例
        val damageReduction = DAMAGE_REDUCTION.getOrNull(level - 1) ?: return

        // 计算减免后的伤害
        val originalDamage = event.damage
        val reducedDamage = originalDamage * (1.0 - damageReduction)

        // 应用减免后的伤害
        event.damage = reducedDamage
    }

    override fun onRemove(player: Player) {
        // 词条移除时不需要特殊处理
    }

    override fun onInit(player: Player, item: ItemStack, level: Int) {
        // 初始化时不需要特殊处理
    }
}
