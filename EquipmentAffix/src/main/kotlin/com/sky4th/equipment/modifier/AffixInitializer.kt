package com.sky4th.equipment.modifier

import com.sky4th.equipment.modifier.AffixConfigManager
import com.sky4th.equipment.modifier.AffixConfig
import com.sky4th.equipment.modifier.Modifier

/**
 * 词条初始化器
 * 根据配置自动创建并注册词条实例
 * 根据配置ID自动查找对应的类并创建实例
 */
object AffixInitializer {

    /**
     * 根据配置创建并注册词条实例
     */
    fun registerFromConfig(config: AffixConfig) {
        val modifier = createModifier(config)
        if (modifier != null) {
            // 将创建的词条实例注册到ModifierManager
            ModifierManager.instance.registerModifier(modifier)
        }
    }

    /**
     * 根据配置创建词条实例
     * 根据config的id自动查找对应的实现类
     * 例如：id为"iron_armor"会查找"IronArmor"类
     * 使用ConfiguredModifier的子类，配置自动从YAML加载
     */
    private fun createModifier(config: AffixConfig): Modifier? {
        try {
            // 将配置ID转换为类名
            // 例如：IRON_ARMOR -> IronArmor
            val className = config.id.lowercase().split("_")
                .joinToString("") { it.replaceFirstChar { char -> char.uppercase() } }

            // 获取类
            val clazz = Class.forName("com.sky4th.equipment.modifier.impl.$className")

            // 使用无参构造函数创建实例
            val constructor = clazz.getDeclaredConstructor()
            constructor.isAccessible = true
            return constructor.newInstance() as? Modifier
        } catch (e: Exception) {
            // 如果找不到对应的类或创建失败，记录错误
            com.sky4th.equipment.EquipmentAffix.instance.logger.warning("无法创建词条实例: ${config.id} - ${e.message}")
            e.printStackTrace()
            return null
        }
    }
}
