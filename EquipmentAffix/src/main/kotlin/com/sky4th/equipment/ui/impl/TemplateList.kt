package com.sky4th.equipment.ui.impl

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import sky4th.core.ui.UITemplate

/**
 * 模板列表处理器
 */
object TemplateList {

    /**
     * 处理模板列表的物品创建
     */
    fun handleItemCreation(
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): ItemStack? {
        // 获取当前UI配置
        val uiConfig = sky4th.core.ui.UIManager.getUI(getCurrentUIId(player))
        if (uiConfig == null) {
            return null
        }

        // 计算当前页的模板数量（统计shape中与template.key相同的字符数量）
        val pageSize = countTemplateSlots(uiConfig.shape, template.key)

        // 获取当前template在shape中的所有槽位位置
        val allSlotIndices = getAllSlotIndices(uiConfig.shape, template.key)

        // 根据template的槽位位置确定当前是第几个槽位
        val slotIndex = allSlotIndices.indexOf(template.slot)

        // 获取筛选后的词条模板ID列表（如果没有筛选，则返回所有词条）
        val filteredAffixIds = com.sky4th.equipment.ui.template.TemplateListManager.getFilteredTemplates(player, "affix_list")

        // 获取当前页码
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "affix_list")
        val currentPage = state.currentPage

        // 计算当前页的起始索引
        val startIndex = currentPage * pageSize

        // 计算当前槽位在整个列表中的实际索引
        val actualIndex = startIndex + slotIndex

        // 如果实际索引超出范围，返回null
        if (actualIndex >= filteredAffixIds.size) {
            return null
        }

        // 获取当前槽位对应的词条ID
        val currentAffixId = filteredAffixIds[actualIndex]

        // 使用AffixItemBuilder生成词条物品
        val resultItem = com.sky4th.equipment.ui.AffixItemBuilder.buildAffixItem(currentAffixId, template.features)

        // 返回词条物品，会在UI中显示
        return resultItem
    }

    /**
     * 获取玩家当前打开的UI ID
     * 通过UIManager获取UIListener实例来查询
     */
    private fun getCurrentUIId(player: Player): String {
        return sky4th.core.ui.UIManager.getListener()?.getPlayerUI(player) ?: ""
    }

    /**
     * 计算UI中指定字符的槽位数量
     * 通过统计shape中指定字符的数量来计算
     */
    private fun countTemplateSlots(shape: List<String>, slotChar: String): Int {
        return shape.sumOf { line ->
            line.count { it.toString() == slotChar }
        }
    }

    /**
     * 获取指定字符在shape中的所有槽位位置
     * 返回所有匹配字符的索引列表
     */
    private fun getAllSlotIndices(shape: List<String>, slotChar: String): List<Int> {
        val indices = mutableListOf<Int>()
        var index = 0
        for (line in shape) {
            for (char in line) {
                if (char.toString() == slotChar) {
                    indices.add(index)
                }
                index++
            }
        }
        return indices
    }

    /**
     * 处理模板列表的左键点击
     * 点击词条物品跳转到词条信息页面
     */
    fun handleLeftClick(
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): Boolean {
        // 获取当前UI配置
        val uiConfig = sky4th.core.ui.UIManager.getUI(getCurrentUIId(player)) ?: return false

        // 计算当前页的模板数量
        val pageSize = countTemplateSlots(uiConfig.shape, template.key)

        // 获取当前template在shape中的所有槽位位置
        val allSlotIndices = getAllSlotIndices(uiConfig.shape, template.key)

        // 根据template的槽位位置确定当前是第几个槽位
        val slotIndex = allSlotIndices.indexOf(template.slot)

        // 获取筛选后的词条模板ID列表
        val filteredAffixIds = com.sky4th.equipment.ui.template.TemplateListManager.getFilteredTemplates(player, "affix_list")

        // 获取当前页码
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "affix_list")
        val currentPage = state.currentPage

        // 计算当前页的起始索引
        val startIndex = currentPage * pageSize

        // 计算当前槽位在整个列表中的实际索引
        val actualIndex = startIndex + slotIndex

        // 如果实际索引超出范围，不处理
        if (actualIndex >= filteredAffixIds.size) {
            return false
        }

        // 获取当前槽位对应的词条ID
        val currentAffixId = filteredAffixIds[actualIndex]

        // 获取词条配置
        val affixConfig = com.sky4th.equipment.modifier.AffixConfigManager.getAffixConfig(currentAffixId)
        val maxLevel = affixConfig?.maxLevel ?: 1

        // 将词条ID和当前等级存储到affix_info的状态中
        val affixInfoState = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "affix_info")
        affixInfoState.customFilters["current_affix_id"] = currentAffixId
        affixInfoState.customFilters["current_level"] = maxLevel  // 初始化为最大等级

        // 打开词条信息页面
        sky4th.core.api.UIAPI.openUI(player, "affix_info")

        return true
    }
}
