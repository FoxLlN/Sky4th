
package com.sky4th.equipment.ui.template

/**
 * 模板列表状态
 * 存储玩家在模板列表中的当前状态
 */
data class TemplateListState(
    var currentPage: Int = 0,               // 当前页码
    var selectedFilter: String? = null,      // 选中的筛选器
    var selectedSorter: String? = null,     // 选中的排序器
    var searchQuery: String = "",            // 搜索关键词
    val customFilters: MutableMap<String, Any> = mutableMapOf(),  // 自定义筛选参数
    var lastUpdateTime: Long = System.currentTimeMillis()  // 最后更新时间
)
