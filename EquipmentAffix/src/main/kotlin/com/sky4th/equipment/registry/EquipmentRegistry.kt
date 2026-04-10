
package com.sky4th.equipment.registry

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import com.sky4th.equipment.attributes.EquipmentAttributes
import com.sky4th.equipment.attributes.EquipmentCategory

/**
 * 装备类型数据类
 */
data class EquipmentType(
    val id: String,                             // 装备ID
    val material: Material,                     // 装备模型
    val categories: List<EquipmentCategory>,    // 装备类型列表（支持多个类别）
    val displayName: String,                    // 装备名称
    val defaultAttributes: EquipmentAttributes, // 默认词条
    val armor: Double = 0.0,                    // 护甲值
    val toughness: Double = 0.0,                // 韧性
    val attackDamage: Double = 0.0,             // 攻击伤害
    val attackSpeed: Double = 0.0,              // 攻击速度
    val maxEnchantmentSlots: Int = 0,           // 最大附魔槽数量
    val maxAffixSlots: Int = 0,                 // 最大词条槽数量
    val maxDurability: Int = 0                  // 最大耐久度
)

/**
 * 装备注册表
 * 用于管理所有装备类型及其默认属性
 */
object EquipmentRegistry {

    // 装备类型注册表
    private val equipmentTypes = mutableMapOf<String, EquipmentType>()
    
    /**
     * 注册装备类型
     */
    fun registerEquipmentType(equipmentType: EquipmentType) {
        equipmentTypes[equipmentType.id] = equipmentType
    }

    /**
     * 获取装备类型
     */
    fun getEquipmentType(id: String): EquipmentType? {
        return equipmentTypes[id]
    }

    /**
     * 通过材质获取装备类型
     */
    fun getEquipmentTypeByMaterial(material: Material): EquipmentType? {
        return equipmentTypes.values.find { it.material == material }
    }

    /**
     * 获取所有装备类型
     */
    fun getAllEquipmentTypes(): List<EquipmentType> {
        return equipmentTypes.values.toList()
    }

    /**
     * 获取所有装备ID
     */
    fun getAllEquipmentIds(): List<String> {
        return equipmentTypes.keys.toList()
    }

    /**
     * 获取指定类别的装备类型
     */
    fun getEquipmentTypesByCategory(category: EquipmentCategory): List<EquipmentType> {
        return equipmentTypes.values.filter { category in it.categories }
    }

    /**
     * 检查装备类型是否存在
     */
    fun hasEquipmentType(id: String): Boolean {
        return equipmentTypes.containsKey(id)
    }

    /**
     * 取消注册装备类型
     */
    fun unregisterEquipmentType(id: String): Boolean {
        return equipmentTypes.remove(id) != null
    }
}
