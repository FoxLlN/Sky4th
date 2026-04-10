
package com.sky4th.equipment.ui.template.sorter

import com.sky4th.equipment.loader.AffixTemplateLoader
import com.sky4th.equipment.modifier.AffixConfigManager
import com.sky4th.equipment.ui.template.TemplateListSorter
import com.sky4th.equipment.ui.template.TemplateListState

/**
 * 多模式排序器
 * 支持按名字、装备类别、材料等多种排序模式
 */
class MultiModeSorter : TemplateListSorter {

    companion object {
        // 排序模式枚举
        enum class SortMode(val displayName: String, val id: String) {
            NAME("ID", "name"),
            EQUIPMENT_TYPE("装备类别", "equipment_type"),
            MATERIAL("材料", "material")
        }

        // 根据ID获取排序模式
        fun getSortModeById(id: String?): SortMode {
            return SortMode.values().find { it.id == id } ?: SortMode.NAME
        }

        // 获取所有排序模式
        fun getAllModes(): List<SortMode> = SortMode.values().toList()

        // 获取下一个排序模式
        fun getNextMode(currentMode: String?): SortMode {
            val modes = getAllModes()
            val currentIndex = modes.indexOfFirst { it.id == currentMode }
            val nextIndex = if (currentIndex < 0 || currentIndex >= modes.size - 1) 0 else currentIndex + 1
            return modes[nextIndex]
        }
    }

    override fun sort(templates: List<String>, state: TemplateListState): List<String> {
        val sortModeId = state.customFilters["sort_mode"] as? String ?: SortMode.NAME.id
        val sortMode = getSortModeById(sortModeId)

        return when (sortMode) {
            SortMode.NAME -> sortByName(templates)
            SortMode.EQUIPMENT_TYPE -> sortByEquipmentType(templates)
            SortMode.MATERIAL -> sortByMaterial(templates)
        }
    }

    /**
     * 按ID排序
     */
    private fun sortByName(templates: List<String>): List<String> {
        return templates.sorted()
    }

    /**
     * 按装备类别排序
     */
    private fun sortByEquipmentType(templates: List<String>): List<String> {
        return templates.sortedWith { id1, id2 ->
            val config1 = AffixConfigManager.getAffixConfig(id1)
            val config2 = AffixConfigManager.getAffixConfig(id2)

            // 获取第一个装备类别作为排序依据
            val type1 = config1?.applicableTo?.firstOrNull()?.name ?: ""
            val type2 = config2?.applicableTo?.firstOrNull()?.name ?: ""

            // 先按装备类别排序，类别相同再按名字排序
            val typeCompare = type1.compareTo(type2, ignoreCase = true)
            if (typeCompare != 0) {
                typeCompare
            } else {
                val name1 = config1?.displayName?.removeColorCode() ?: id1
                val name2 = config2?.displayName?.removeColorCode() ?: id2
                name1.compareTo(name2, ignoreCase = true)
            }
        }
    }

    /**
     * 按材料排序
     */
    private fun sortByMaterial(templates: List<String>): List<String> {
        return templates.sortedWith { id1, id2 ->
            // 获取材料名称
            val material1 = AffixTemplateLoader.getUpgradeMaterialsCache(id1)?.first ?: ""
            val material2 = AffixTemplateLoader.getUpgradeMaterialsCache(id2)?.first ?: ""

            // 先按材料名称排序，材料相同再按ID排序
            val materialCompare = material1.compareTo(material2, ignoreCase = true)
            if (materialCompare != 0) {
                materialCompare
            } else {
                id1.compareTo(id2, ignoreCase = true)
            }
        }
    }

    override fun getSorterId(): String = "multi_mode"

    override fun getSorterName(): String {
        return "多模式排序"
    }

    /**
     * 移除颜色代码的扩展函数
     */
    private fun String.removeColorCode(): String {
        return this.replace(Regex("§[0-9a-fk-or]"), "")
    }
}
