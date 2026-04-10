package com.sky4th.equipment.modifier

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.attribute.Attribute
import com.sky4th.equipment.attributes.EquipmentCategory

/**
 * 词条配置管理器
 * 负责管理所有词条的配置信息
 */
object AffixConfigManager {

    // 存储所有词条配置
    private val affixConfigs = mutableMapOf<String, AffixConfig>()

    /**
     * 注册词条配置
     */
    fun registerAffixConfig(config: AffixConfig) {
        affixConfigs[config.id] = config
    }

    /**
     * 获取词条配置
     */
    fun getAffixConfig(affixId: String): AffixConfig? {
        return affixConfigs[affixId]
    }

    /**
     * 获取所有词条配置
     */
    fun getAllAffixConfigs(): Map<String, AffixConfig> {
        return affixConfigs.toMap()
    }

    /**
     * 根据装备类别获取适用的词条
     * @param category 装备类别
     * @return 适用的词条配置列表
     */
    fun getApplicableAffixes(category: EquipmentCategory): List<AffixConfig> {
        return affixConfigs.values.filter { category in it.applicableTo }
    }

    /**
     * 根据装备类别和槽位获取适用的词条
     * @param category 装备类别
     * @param slot 装备槽位（如 HEAD, CHEST, LEGS, FEET, SWORD 等）
     * @return 适用的词条配置列表
     */
    fun getApplicableAffixes(category: EquipmentCategory, slot: String): List<AffixConfig> {
        val upperSlot = slot.uppercase()
        return affixConfigs.values.filter { config ->
            category in config.applicableTo &&
            (config.equipmentSlot.isEmpty() || upperSlot in config.equipmentSlot.map { it.uppercase() })
        }
    }

    /**
     * 根据装备槽位获取适用的词条
     * @param slot 装备槽位（如 HEAD, CHEST, LEGS, FEET, SWORD 等）
     * @return 适用的词条配置列表
     */
    fun getApplicableAffixesBySlot(slot: String): List<AffixConfig> {
        val upperSlot = slot.uppercase()
        return affixConfigs.values.filter { config ->
            config.equipmentSlot.isEmpty() || upperSlot in config.equipmentSlot.map { it.uppercase() }
        }
    }
}
