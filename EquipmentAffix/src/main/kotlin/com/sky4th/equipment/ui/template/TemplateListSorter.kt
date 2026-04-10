
package com.sky4th.equipment.ui.template

/**
 * 模板列表排序器接口
 * 用于对模板列表进行排序
 */
interface TemplateListSorter {
    /**
     * 应用排序
     * @param templates 原始模板列表
     * @param state 玩家状态
     * @return 排序后的模板列表
     */
    fun sort(templates: List<String>, state: TemplateListState): List<String>

    /**
     * 获取排序器ID
     */
    fun getSorterId(): String

    /**
     * 获取排序器名称
     */
    fun getSorterName(): String
}
