package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import com.sky4th.equipment.util.BiomesTypeUtil
import com.sky4th.equipment.util.DamageTypeUtil
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack

/**
 * 自然词条
 * 效果：处于自然生物群系时，获得小额物理减伤（5%）
 */
class Nature : com.sky4th.equipment.modifier.ConfiguredModifier("nature") {
    override fun getEventTypes(): List<Class<out Event>> =
        listOf(EntityDamageEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 检查是否是玩家受到伤害
        if (event !is EntityDamageEvent || playerRole != PlayerRole.DEFENDER) {
            return
        }

        // 检查是否处于自然生物群系
        if (!BiomesTypeUtil.isNaturalBiome(player.location)) {
            return
        }

        // 检查伤害类型是否可以基础减伤
        if (!DamageTypeUtil.isBasicReduction(event)) {
            return
        }

        // 应用5%的物理减伤
        val reducedDamage = event.damage * (1 - 0.05)
        event.damage = reducedDamage
    }
}
