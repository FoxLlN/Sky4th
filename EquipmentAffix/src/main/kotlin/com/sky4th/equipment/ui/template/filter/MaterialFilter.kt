package com.sky4th.equipment.ui.template.filter

import com.sky4th.equipment.loader.AffixTemplateLoader
import com.sky4th.equipment.ui.impl.MaterialList
import com.sky4th.equipment.ui.template.TemplateListFilter
import com.sky4th.equipment.ui.template.TemplateListState
import org.bukkit.entity.Player

/**
 * 材料筛选器
 * 根据词条的升级材料进行筛选
 */
class MaterialFilter : TemplateListFilter {

    override fun apply(templates: List<String>, state: TemplateListState, player: org.bukkit.entity.Player): List<String> {
        // 从 material_filter 状态中获取已选择的材料列表
        val materialFilterState = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(
            player,
            "material_filter"
        )
        val selectedMaterials = materialFilterState.customFilters["selected_materials"] as? Set<String>

        // 从 material_filter 状态中获取被排除的材料列表
        val excludedMaterials = materialFilterState.customFilters["excluded_materials"] as? Set<String>

        // 如果既没有选择也没有排除任何材料，返回所有模板
        if (selectedMaterials.isNullOrEmpty() && excludedMaterials.isNullOrEmpty()) {
            return templates
        }

        return templates.filter { templateId ->
            val materialCache = AffixTemplateLoader.getUpgradeMaterialsCache(templateId)
            if (materialCache == null) {
                return@filter false
            }

            val material = materialCache.first

            // 如果有选中的材料，检查是否在选中列表中
            val isSelected = selectedMaterials.isNullOrEmpty() || material in selectedMaterials

            // 如果有排除的材料，检查是否不在排除列表中
            val isNotExcluded = excludedMaterials.isNullOrEmpty() || material !in excludedMaterials

            // 必须同时满足选中条件（如果有）和排除条件（如果有）
            isSelected && isNotExcluded
        }
    }

    override fun getFilterId() = "material"
    override fun getFilterName() = "材料"
}
