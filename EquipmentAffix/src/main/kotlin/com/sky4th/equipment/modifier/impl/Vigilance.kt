
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.util.DamageTypeUtil
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.Location
import org.bukkit.util.Vector

/**
 * 警觉词条
 * 效果：减少来自背后的伤害
 * - 背后受到的伤害减少 10%/15%/20%
 */
class Vigilance : com.sky4th.equipment.modifier.ConfiguredModifier("vigilance") {

    // 每级的背后伤害减免百分比
    private val CONFIG = doubleArrayOf(0.05, 0.10, 0.15)

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(EntityDamageByEntityEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: com.sky4th.equipment.modifier.config.PlayerRole
    ) {
        // 检查是否是玩家受到伤害
        if (event !is EntityDamageByEntityEvent || playerRole != com.sky4th.equipment.modifier.config.PlayerRole.DEFENDER) {
            return
        }

        // 检查伤害类型是否可以基础减伤
        if (!DamageTypeUtil.isBasicReduction(event)) {
            return
        }

        // 检查是否来自背后的伤害
        if (!isDamageFromBehind(event, player)) {
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

    /**
     * 检查伤害是否来自玩家背后
     * @param event 伤害事件
     * @param player 受到伤害的玩家
     * @return true如果伤害来自背后
     */
    private fun isDamageFromBehind(event: EntityDamageByEntityEvent, player: Player): Boolean {
        // 获取伤害来源实体
        val damager = event.damager

        // 获取玩家和伤害来源的位置
        val playerLocation = player.location
        val damagerLocation = damager.location

        // 计算从玩家指向伤害来源的向量
        val toDamager = damagerLocation.toVector().subtract(playerLocation.toVector()).normalize()

        // 获取玩家朝向向量
        val playerDirection = playerLocation.direction.normalize()

        // 计算点积，判断是否在背后（点积为负表示在背后）
        val dotProduct = toDamager.dot(playerDirection)

        // 如果点积小于0，说明伤害来源在玩家背后
        return dotProduct < 0
    }
}
