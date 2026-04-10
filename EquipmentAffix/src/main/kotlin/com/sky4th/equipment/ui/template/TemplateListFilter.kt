
package com.sky4th.equipment.ui.template

/**
 * 模板列表筛选器接口
 * 用于对模板列表进行筛选
 */
interface TemplateListFilter {
    /**
     * 应用筛选
     * @param templates 原始模板列表
     * @param state 玩家状态
     * @param player 玩家对象
     * @return 筛选后的模板列表
     */
    fun apply(templates: List<String>, state: TemplateListState, player: org.bukkit.entity.Player): List<String>

    /**
     * 获取筛选器ID
     */
    fun getFilterId(): String

    /**
     * 获取筛选器名称
     */
    fun getFilterName(): String
}
