
package com.sky4th.equipment.modifier

import com.sky4th.equipment.attributes.AffixCalculationMode
import com.sky4th.equipment.attributes.EquipmentCategory
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack

/**
 * 自动配置词条基类
 * 自动从AffixConfigManager加载配置，简化子类实现
 * 子类只需要指定词条ID和实现具体的事件处理逻辑
 */
abstract class ConfiguredModifier(
    private val affixId: String
) : Modifier {

    // 懒加载配置，避免在构造时立即加载
    private val config: AffixConfig by lazy {
        AffixConfigManager.getAffixConfig(affixId) 
            ?: throw IllegalArgumentException("未找到词条配置: $affixId")
    }

    override fun getAffixId(): String = affixId

    /**
     * 获取词条显示名称
     */
    fun getDisplayName(): String = config.displayName

    /**
     * 获取词条描述
     */
    fun getDescription(): String = config.description

    override fun getCalculationMode(): AffixCalculationMode = config.calculationMode

    override fun getApplicableTo(): List<EquipmentCategory> = config.applicableTo

    override fun getTriggerSlot(): String = config.triggerSlot

    override fun isInitModifier(): Boolean = config.isInit

    override fun getConflictingAffixes(): List<String> = config.conflictingAffixes

    override fun getConflictingEnchantments(): List<org.bukkit.enchantments.Enchantment> = config.conflictingEnchantments

    override fun getPriority(): Int = config.priority

    /**
     * 默认的空实现，子类可以按需重写
     */
    override fun onRemove(player: Player) {}
}
