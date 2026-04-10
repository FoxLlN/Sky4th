package com.sky4th.equipment.ui.impl

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import sky4th.core.ui.UITemplate

/**
 * 翻页按钮处理器
 */
object PageTurn {

    /**
     * 处理翻页按钮的点击
     * @return true表示已处理，false表示未处理
     */
    fun handleClick(
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): Boolean {
        val direction = template.features["direction"] as? String ?: "next"

        // 获取当前UI配置
        val uiId = sky4th.core.ui.UIManager.getListener()?.getPlayerUI(player) ?: ""
        val uiConfig = sky4th.core.ui.UIManager.getUI(uiId)
        if (uiConfig == null) {
            return true
        }

        // 获取槽位字符（从feature中获取，默认为"T"）
        val slotChar = (template.features["slot_char"] as? String) ?: "T"

        // 计算当前页的模板数量
        val pageSize = countTemplateSlots(uiConfig.shape, slotChar)

        // 根据UI ID确定使用哪个列表的状态
        val listId = when (uiId) {
            "affix_filter_materials" -> "material_filter"
            else -> "affix_list"
        }

        // 获取筛选后的列表
        val filteredList = when (uiId) {
            "affix_filter_materials" -> {
                // 获取所有材料（不再过滤排除的材料）
                com.sky4th.equipment.ui.impl.MaterialList.getAllMaterials()
            }
            else -> {
                // 获取筛选后的词条模板ID列表
                com.sky4th.equipment.ui.template.TemplateListManager.getFilteredTemplates(player, "affix_list")
            }
        }

        // 获取当前页码
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, listId)
        val currentPage = state.currentPage

        val totalPages = (filteredList.size + pageSize - 1) / pageSize
        when (direction) {
            "next" -> {
                if (currentPage < totalPages - 1) {
                    state.currentPage = currentPage + 1
                    // 更新UI内容，不重新打开inventory
                    sky4th.core.ui.UIManager.updateCurrentUI(player)
                }
            }
            "previous" -> {
                if (currentPage > 0) {
                    state.currentPage = currentPage - 1
                    // 更新UI内容，不重新打开inventory
                    sky4th.core.ui.UIManager.updateCurrentUI(player)
                }
            }
        }
        return true
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
}
