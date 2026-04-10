package com.sky4th.equipment.attributes

/**
 * 熟练度等级枚举
 * 定义熟练度等级及其显示名称
 */
enum class ProficiencyLevel(val level: Int, val displayName: String) {
    NOVICE(0, "生疏"),
    BEGINNER(1, "初学"),
    COMPETENT(2, "掌握"),
    PROFICIENT(3, "精通"),
    EXPERT(4, "专家"),
    MASTER(5, "宗师");

    companion object {
        /**
         * 根据等级值获取对应的枚举
         */
        fun fromLevel(level: Int): ProficiencyLevel {
            return values().find { it.level == level } ?: NOVICE
        }

        /**
         * 获取指定等级的显示名称
         */
        fun getDisplayName(level: Int): String {
            return fromLevel(level).displayName
        }
    }
}
