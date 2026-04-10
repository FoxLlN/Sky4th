package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.manager.UnifiedModifierManager
import com.sky4th.equipment.modifier.manager.handlers.MagneticHandler
import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack

/**
 * 磁力词条
 * 效果：缓慢吸引周围掉落物到玩家
 * 
 * 注意：此词条通过ModifierManager自动管理，当玩家装备变更时会自动更新
 * 磁力效果通过定时任务实现，不依赖于玩家移动
 * 只检测主手和副手的物品，不检测装备情况
 */
class Magnetic : com.sky4th.equipment.modifier.ConfiguredModifier("magnetic") {
    // 监听玩家装备变更事件和加入游戏事件
    override fun getEventTypes(): List<Class<out Event>> = listOf()

    // 处理玩家装备变更事件，更新磁力状态
    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 在handle方法中不处理事件，磁力效果通过UnifiedModifierManager定时任务实现
    }

    override fun onInit(player: Player, item: ItemStack, level: Int) {
        // 创建MagneticData并注册到UnifiedModifierManager
        val magneticData = MagneticHandler.MagneticData(
            uuid = player.uniqueId,
            level = level,
            item = item
        )
        UnifiedModifierManager.addPlayerAffix(player, getAffixId(), magneticData)
    }
        
    override fun onRemove(player: Player) {
        UnifiedModifierManager.removePlayerAffix(player, getAffixId())
    }

}
