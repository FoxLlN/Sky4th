
package com.sky4th.equipment.attributes

/**
 * 词条计算方式枚举
 * 定义多个相同词条如何计算最终效果
 */
enum class AffixCalculationMode {
    /**
     * 取最高等级
     * 多个相同词条时，只使用最高等级的效果
     */
    HIGHEST,

    /**
     * 叠加所有等级
     * 多个相同词条时，将所有等级相加
     */
    SUM,

    /**
     * 取平均值
     * 多个相同词条时，计算所有等级的平均值
     */
    AVERAGE
}
