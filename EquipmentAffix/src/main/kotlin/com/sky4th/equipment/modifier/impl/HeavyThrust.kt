
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

/**
 * 重戟词条
 * 效果：无法投掷，但近战伤害增加
 * 1级：近战伤害+30%
 * 2级：近战伤害+40%
 * 3级：近战伤害+50%
 * 仅适用于三叉戟
 */
class HeavyThrust : com.sky4th.equipment.modifier.ConfiguredModifier("heavy_thrust") {

    companion object {
        // 每级的近战伤害加成
        private val DAMAGE_BONUS = doubleArrayOf(0.30, 0.40, 0.50)
    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(
            EntityDamageEvent::class.java,
            PlayerInteractEvent::class.java
        )

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        when (event) {
            is PlayerInteractEvent -> {
                // 只禁用物品使用，保留方块交互
                if (event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK) {
                    if (player.inventory.itemInMainHand.type == Material.TRIDENT) {
                        // 只阻止三叉戟的投掷动作，不影响右键打开箱子、工作台等任何方块交互
                        event.setUseItemInHand(Event.Result.DENY)
                    }
                }
            }
            is EntityDamageEvent -> {
                // 只处理实体伤害事件，且玩家必须是攻击者
                if (playerRole != PlayerRole.ATTACKER) {
                    return
                }

                // 检查是否是实体攻击实体事件
                if (event !is org.bukkit.event.entity.EntityDamageByEntityEvent) {
                    return
                }

                // 检查攻击者是否是玩家
                if (event.damager !is Player) {
                    return
                }

                // 检查是否是近战攻击（非投掷）
                val attacker = event.damager as Player
                if (attacker.inventory.itemInMainHand != item) {
                    return
                }

                // 获取当前等级的伤害加成
                val damageBonus = if (level - 1 in 0..2) DAMAGE_BONUS[level - 1] else return

                // 计算新伤害
                val originalDamage = event.damage
                val newDamage = originalDamage * (1 + damageBonus)

                // 设置新伤害
                event.damage = newDamage
            }
        }
    }
}
