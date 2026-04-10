
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.manager.UnifiedModifierManager
import com.sky4th.equipment.modifier.manager.handlers.SolarHandler
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack

/**
 * 太阳能词条
 * 效果：站在太阳下缓慢恢复装备耐久，每30秒恢复1点耐久（需头顶无遮挡且为白天），最多恢复到60%
 *
 * 注意：此词条通过ModifierManager自动管理，当玩家装备变更时会自动更新
 * 耐久恢复通过定时任务实现，不依赖于玩家操作
 * 检测所有部位（装备/主副手）是否有词条
 */
class Solar : com.sky4th.equipment.modifier.ConfiguredModifier("solar") {
    // 监听玩家装备变更事件、加入游戏事件和副手交换事件
    override fun getEventTypes(): List<Class<out Event>> =
        listOf(
            org.bukkit.event.player.PlayerItemHeldEvent::class.java,
            org.bukkit.event.player.PlayerJoinEvent::class.java,
            org.bukkit.event.player.PlayerSwapHandItemsEvent::class.java,
            com.destroystokyo.paper.event.player.PlayerArmorChangeEvent::class.java
        )

    // 处理玩家装备变更事件，更新太阳能状态
    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: com.sky4th.equipment.modifier.config.PlayerRole
    ) {
    }

    override fun onInit(player: Player, item: ItemStack, level: Int) {
        // 获取或创建玩家的太阳能数据
        val data = UnifiedModifierManager.getAffixData(player, getAffixId()) as? SolarHandler.SolarData
        if (data == null) {
            // 创建新的太阳能数据
            val solarData = SolarHandler.SolarData(
                uuid = player.uniqueId,
                level = level
            )
            UnifiedModifierManager.addPlayerAffix(player, getAffixId(), solarData)
        }
        // 注意：物品列表由 SolarHandler.process() 自动更新，无需手动维护
    }

    // 当词条被移除时，从统一管理器中移除玩家的太阳能词条
    override fun onRemove(player: Player) {
        UnifiedModifierManager.removePlayerAffix(player, getAffixId())
    }
}
