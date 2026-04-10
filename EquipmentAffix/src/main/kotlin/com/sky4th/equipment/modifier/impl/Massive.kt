package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.util.DamageTypeUtil
import com.sky4th.equipment.util.BlockTypeUtil
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack

/**
 * 磐石词条
 * 效果：脚下方块为石质方块时，获得5%伤害减免
 */
class Massive : com.sky4th.equipment.modifier.ConfiguredModifier("massive") {

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

        // 检查伤害类型是否可以基础减伤
        if (!DamageTypeUtil.isBasicReduction(event)) {
            return
        }

        // 获取当前脚下方块（玩家位置y-1）
        val block = player.location.subtract(0.0, 1.0, 0.0).block

        // 检查脚下方块是否为石质方块
        if (!BlockTypeUtil.isStone(block)) {
            return
        }

        // 获取当前基础伤害
        val damage = event.damage

        // 计算减伤后的伤害（减少5%）
        val newDamage = damage * 0.95

        // 设置基础伤害
        event.damage = newDamage
    }
}
