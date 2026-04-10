
package com.sky4th.equipment.ui.template.filter

import com.sky4th.equipment.modifier.AffixConfigManager
import com.sky4th.equipment.ui.template.TemplateListFilter
import com.sky4th.equipment.ui.template.TemplateListState

/**
 * 类型筛选器
 * 根据词条的类型进行筛选
 * 支持基于category和slot的组合筛选
 */
class TypeFilter : TemplateListFilter {

    override fun apply(templates: List<String>, state: TemplateListState, player: org.bukkit.entity.Player): List<String> {
        // 从 type_filter 状态中获取已选择的类型列表
        val typeFilterState = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(
            player,
            "type_filter"
        )
        val selectedTypes = typeFilterState.customFilters["selected_types"] as? Set<String>

        // 从 type_filter 状态中获取被排除的类型列表
        val excludedTypes = typeFilterState.customFilters["excluded_types"] as? Set<String>

        // 如果没有选择任何类型且没有排除任何类型，返回所有模板
        if ((selectedTypes == null || selectedTypes.isEmpty()) && (excludedTypes == null || excludedTypes.isEmpty())) {
            return templates
        }

        return templates.filter { templateId ->
            // 直接从配置管理器获取词条配置
            val affixConfig = AffixConfigManager.getAffixConfig(templateId) ?: return@filter false

            // 获取词条适用的所有类型组合
            val applicableTypes = mutableSetOf<String>()

            // 如果equipmentSlot为空，则该词条适用于category下的所有部位
            if (affixConfig.equipmentSlot.isEmpty()) {
                // 遍历所有可能的部位
                val allSlots = listOf("HEAD", "CHEST", "LEGS", "FEET", "SWORD", "AXE", "SHOVEL", "PICKAXE", "HOE", "FISHING_ROD", "MACE", "BOW", "CROSSBOW", "SHIELD", "ELYTRA")
                for (category in affixConfig.applicableTo) {
                    for (slot in allSlots) {
                        applicableTypes.add("${category.name}_$slot")
                    }
                }
            } else {
                // 如果有指定equipmentSlot，则只适用于指定的部位
                for (category in affixConfig.applicableTo) {
                    for (slot in affixConfig.equipmentSlot) {
                        applicableTypes.add("${category.name}_$slot")
                    }
                }
            }

            // 检查是否被排除
            val isExcluded = excludedTypes != null && applicableTypes.any { it in excludedTypes }
            if (isExcluded) return@filter false

            // 检查是否被选中
            val isSelected = selectedTypes != null && applicableTypes.any { it in selectedTypes }
            if (selectedTypes != null && selectedTypes.isNotEmpty() && !isSelected) {
                return@filter false
            }

            return@filter true
        }
    }

    override fun getFilterId() = "type"
    override fun getFilterName() = "类型"
}
