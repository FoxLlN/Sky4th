
package com.sky4th.equipment.ui.template

/**
 * 模板列表配置
 * 定义一个模板列表的显示规则和行为
 */
data class TemplateListConfig(
    val id: String,                          // 列表ID
    val name: String,                        // 列表名称
    val description: String,                  // 描述
    val filters: List<TemplateListFilter> = emptyList(),  // 筛选器列表
    val sorter: TemplateListSorter? = null,  // 排序器
    val cacheEnabled: Boolean = true,        // 是否启用缓存
    val cacheExpireTime: Long = 5 * 60 * 1000L // 缓存过期时间（毫秒）
)
