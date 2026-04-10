package com.sky4th.equipment.modifier

/**
 * 词条配置数据类
 * 存储词条的所有配置信息
 */
data class AffixConfig(
    val id: String,
    val displayName: String,
    val description: String,
    val simpleDescription: String = "",
    val maxLevel: Int,
    val isInit: Boolean,
    val applicableTo: List<com.sky4th.equipment.attributes.EquipmentCategory>,
    val equipmentSlot: List<String> = emptyList(),
    val calculationMode: com.sky4th.equipment.attributes.AffixCalculationMode,
    val conflictingAffixes: List<String> = emptyList(),
    val conflictingEnchantments: List<org.bukkit.enchantments.Enchantment> = emptyList(),
    val priority: Int = 0,
    // 充能相关配置
    val isChargeable: Boolean = false,
    val chargeResource: String? = null,
    val chargeResourceName: String? = null,
    val chargeMaxStorage: Map<Int, Int> = emptyMap(),
    // 触发槽位配置：MAIN_HAND, OFF_HAND, HAND, 或具体槽位
    val triggerSlot: String = "MAIN_HAND"
)
