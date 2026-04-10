package com.sky4th.equipment.ui.template.filter

import com.sky4th.equipment.modifier.AffixConfigManager
import com.sky4th.equipment.ui.template.TemplateListFilter
import com.sky4th.equipment.ui.template.TemplateListState

/**
 * 名称筛选器
 * 根据词条的名称进行筛选
 */
class NameFilter : TemplateListFilter {

    override fun apply(templates: List<String>, state: TemplateListState, player: org.bukkit.entity.Player): List<String> {
        val searchQuery = state.searchQuery
        if (searchQuery.isEmpty()) {
            return templates
        }

        return templates.filter { templateId ->
            // 直接从配置管理器获取词条配置
            val affixConfig = AffixConfigManager.getAffixConfig(templateId)
            
            // 移除颜色代码（前9个字符，格式为<#RRGGBB>）
            val displayName = if (affixConfig != null && affixConfig.displayName.length >= 9 && affixConfig.displayName.startsWith("<#")) {
                affixConfig.displayName.substring(9)
            } else {
                affixConfig?.displayName ?: ""
            }

            // 检查词条名称是否包含搜索关键词（不区分大小写）
            displayName.contains(searchQuery, ignoreCase = true)
        }
    }

    override fun getFilterId() = "name"
    override fun getFilterName() = "名称"
}
