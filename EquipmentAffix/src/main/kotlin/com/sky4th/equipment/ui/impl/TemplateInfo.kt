package com.sky4th.equipment.ui.impl

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import sky4th.core.ui.UITemplate

/**
 * 词条信息处理器
 * 处理词条信息页面的显示
 */
object TemplateInfo {

    /**
     * 处理词条信息页面的物品创建
     */
    fun handleItemCreation(
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): ItemStack? {
        // 获取当前UI ID
        val currentUIId = sky4th.core.ui.UIManager.getListener()?.getPlayerUI(player) ?: return null

        // 只处理词条信息页面
        if (currentUIId != "affix_info") {
            return null
        }

        // 获取玩家状态中的当前词条ID
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "affix_info")

        val currentAffixId = state.customFilters["current_affix_id"] as? String ?: run {
            return null
        }

        // 获取当前查看的等级（默认为1）
        val currentLevel = state.customFilters["current_level"] as? Int ?: 1

        // 使用AffixItemBuilder生成词条物品，传入当前等级
        val resultItem = com.sky4th.equipment.ui.AffixItemBuilder.buildAffixItem(currentAffixId, template.features, currentLevel)

        return resultItem
    }
}
