
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import com.sky4th.equipment.util.DamageTypeUtil
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack

/**
 * 地狱壁垒词条
 * 效果：在下界所有伤害减免
 * 1级：减免5%
 * 2级：减免10%
 * 3级：减免15%
 */
class HellBulwark : com.sky4th.equipment.modifier.ConfiguredModifier("hell_bulwark") {

    companion object {
        // 每级的伤害减免百分比
        private val CONFIG = doubleArrayOf(0.05, 0.10, 0.15)
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

        // 检查玩家是否在下界
        if (player.world.environment != org.bukkit.World.Environment.NETHER) {
            return
        }

        // 从CONFIG数组获取减伤比例（level从1开始，数组索引从0开始）
        val reduction = CONFIG.getOrElse(level - 1) { CONFIG[0] }

        // 获取当前基础伤害
        val damage = event.damage

        // 计算减伤后的伤害
        val newDamage = damage * (1.0 - reduction)

        // 设置基础伤害
        event.damage = newDamage
    }
}
