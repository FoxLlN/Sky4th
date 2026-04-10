package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.manager.UnifiedModifierManager
import com.sky4th.equipment.modifier.manager.handlers.GoldenBodyHandler
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack

/**
 * 金身词条
 * 效果：周期性获得伤害吸收护盾，每60秒获得2点，上限根据等级为4/6/8
 * 伤害吸收会在受伤时自动消耗，需要等待自然恢复
 * 1级：上限4点
 * 2级：上限6点
 * 3级：上限8点
 */
class GoldenBody : com.sky4th.equipment.modifier.ConfiguredModifier("golden_body") {

    override fun getEventTypes(): List<Class<out Event>> = 
        listOf(EntityDamageEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: com.sky4th.equipment.modifier.config.PlayerRole
    ) {
        if (playerRole != com.sky4th.equipment.modifier.config.PlayerRole.SELF) return

        if (event is EntityDamageEvent) {
            // 更新玩家受伤时间
            val goldenBodyData = UnifiedModifierManager.getAffixData(player, getAffixId()) as? GoldenBodyHandler.GoldenBodyData
            if (goldenBodyData != null) {
                goldenBodyData.startTime = System.currentTimeMillis()
            }
        }
    }

    override fun onInit(player: Player, item: ItemStack, level: Int) {
        val goldenBodyData = GoldenBodyHandler.GoldenBodyData(
            uuid = player.uniqueId,
            level = level,
            item = item  // 直接传入物品
        )
        UnifiedModifierManager.addPlayerAffix(player, getAffixId(), goldenBodyData)
    }


    /**
     * 当词条被移除时，从金身列表中移除玩家
     */
    override fun onRemove(player: Player) {
        UnifiedModifierManager.removePlayerAffix(player, getAffixId())
    }
}
