
package com.sky4th.equipment.data

import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/**
 * NBT数据存储管理器
 * 使用NBT标签存储装备数据,不依赖Lore
 * 优化版本：添加批量读取方法，减少NBT访问次数
 */
object NBTEquipmentDataManager {

    // 定义NBT键
    val KEY_EQUIPMENT_ID = NamespacedKey("sky_equipment", "equipment_id")
    val KEY_INSTANCE_ID = NamespacedKey("sky_equipment", "instance_id")
    val KEY_PROFICIENCY_LEVEL = NamespacedKey("sky_equipment", "proficiency_level")
    val KEY_PROFICIENCY = NamespacedKey("sky_equipment", "proficiency")
    val KEY_AFFIX_PREFIX = NamespacedKey("sky_equipment", "affix_")

    // 属性键
    private val KEY_DODGE_CHANCE = NamespacedKey("sky_equipment", "dodge_chance")
    private val KEY_KNOCKBACK_RESISTANCE = NamespacedKey("sky_equipment", "knockback_resistance")
    private val KEY_HUNGER_PENALTY = NamespacedKey("sky_equipment", "hunger_penalty")
    private val KEY_MOVEMENT_PENALTY = NamespacedKey("sky_equipment", "movement_penalty")
    private val KEY_MATERIAL_EFFECT = NamespacedKey("sky_equipment", "material_effect")

    // 槽位上限键
    val KEY_MAX_AFFIX_SLOTS = NamespacedKey("sky_equipment", "max_affix_slots")
    val KEY_MAX_ENCHANTMENT_SLOTS = NamespacedKey("sky_equipment", "max_enchantment_slots")

    // 词条资源存储键前缀
    private val KEY_AFFIX_RESOURCE_PREFIX = "resource_"

    /**
     * 检查物品是否是装备
     */
    fun isEquipment(item: ItemStack): Boolean {
        val meta = item.itemMeta ?: return false
        val container = meta.persistentDataContainer
        return container.has(KEY_EQUIPMENT_ID, PersistentDataType.STRING)
    }

    /**
     * 获取装备ID（如 iron_sword）
     */
    fun getEquipmentId(item: ItemStack): String? {
        val meta = item.itemMeta ?: return null
        val container = meta.persistentDataContainer
        return container.get(KEY_EQUIPMENT_ID, PersistentDataType.STRING)
    }

    /**
     * 设置装备ID
     */
    fun setEquipmentId(item: ItemStack, typeId: String) {
        val meta = item.itemMeta ?: return
        val container = meta.persistentDataContainer
        container.set(KEY_EQUIPMENT_ID, PersistentDataType.STRING, typeId)
        item.itemMeta = meta
    }

    /**
     * 获取实例ID（用于缓存）
     * @return 实例ID，如果不存在则返回null
     */
    fun getInstanceId(item: ItemStack): String? {
        val meta = item.itemMeta ?: return null
        val container = meta.persistentDataContainer
        return container.get(KEY_INSTANCE_ID, PersistentDataType.STRING)
    }

    /**
     * 设置实例ID（用于缓存）
     * 自动生成UUID作为实例ID
     * @param item 装备物品
     */
    fun setInstanceId(item: ItemStack) {
        val meta = item.itemMeta ?: return
        val container = meta.persistentDataContainer
        val instanceId = generateInstanceId()
        container.set(KEY_INSTANCE_ID, PersistentDataType.STRING, instanceId)
        item.itemMeta = meta
    }

    private fun generateInstanceId(): String {
        val timestamp = System.currentTimeMillis() / 1000  // 秒级
        val uuid = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8)
        return "$timestamp-$uuid"
    }

    /**
     * 清除实例ID
     * 用于铁砧等场景，清除instance_id后，物品会被视为新实例
     * 同时清理相关的缓存，避免内存泄漏
     * @param item 装备物品
     */
    fun clearInstanceId(item: ItemStack) {
        val meta = item.itemMeta ?: return
        val container = meta.persistentDataContainer

        // 先获取instance_id，用于清理缓存
        val oldInstanceId = container.get(KEY_INSTANCE_ID, PersistentDataType.STRING)

        // 清除instance_id
        container.remove(KEY_INSTANCE_ID)
        item.itemMeta = meta

        // 手动清除旧缓存
        if (oldInstanceId != null) {
            com.sky4th.equipment.cache.EquipmentAttributesCache.invalidateId(oldInstanceId)
            com.sky4th.equipment.cache.LoreDisplayCache.invalidateId(oldInstanceId)
        }
    }

    /**
     * 获取熟练度等级
     */
    fun getProficiencyLevel(item: ItemStack): Int {
        val meta = item.itemMeta ?: return 0
        val container = meta.persistentDataContainer
        val level = container.getOrDefault(KEY_PROFICIENCY_LEVEL, PersistentDataType.INTEGER, 0)
        return level
    }

    /**
     * 设置熟练度等级
     */
    fun setProficiencyLevel(item: ItemStack, level: Int) {
        val meta = item.itemMeta ?: return
        val container = meta.persistentDataContainer
        container.set(KEY_PROFICIENCY_LEVEL, PersistentDataType.INTEGER, level)
        item.itemMeta = meta

        // 自动失效缓存
        com.sky4th.equipment.cache.EquipmentAttributesCache.invalidate(item)
        com.sky4th.equipment.cache.LoreDisplayCache.invalidate(item)
    }

    /**
     * 获取熟练度
     */
    fun getProficiency(item: ItemStack): Int {
        val meta = item.itemMeta ?: return 0
        val container = meta.persistentDataContainer
        val proficiency = container.getOrDefault(KEY_PROFICIENCY, PersistentDataType.INTEGER, 0)
        return proficiency
    }

    /**
     * 设置熟练度
     */
    fun setProficiency(item: ItemStack, proficiency: Int) {
        val meta = item.itemMeta ?: return
        val container = meta.persistentDataContainer
        container.set(KEY_PROFICIENCY, PersistentDataType.INTEGER, proficiency)
        item.itemMeta = meta

        // 自动失效缓存
        com.sky4th.equipment.cache.EquipmentAttributesCache.invalidate(item)
        com.sky4th.equipment.cache.LoreDisplayCache.invalidate(item)
    }

    /**
     * 增加熟练度
     */
    fun increaseProficiency(item: ItemStack): Int {
        val currentProficiency = getProficiency(item)
        val newProficiency = currentProficiency + 1
        setProficiency(item, newProficiency)
        return newProficiency
    }

    /**
     * 获取词条
     */
    fun getAffixes(item: ItemStack): Map<String, Int> {
        val meta = item.itemMeta ?: return emptyMap()
        val container = meta.persistentDataContainer
        val affixes = mutableMapOf<String, Int>()

        container.keys.forEach { key ->
            if (key.key.startsWith("affix_")) {
                val affixId = key.key.substring(6).lowercase()
                val level = container.get(key, PersistentDataType.INTEGER) ?: 0
                affixes[affixId] = level
            }
        }

        return affixes
    }

    /**
     * 设置词条
     */
    fun setAffix(item: ItemStack, affixId: String, level: Int) {
        val meta = item.itemMeta ?: return
        val container = meta.persistentDataContainer
        val key = NamespacedKey("sky_equipment", "affix_${affixId.lowercase()}")
        container.set(key, PersistentDataType.INTEGER, level)
        item.itemMeta = meta

        // 自动失效缓存
        com.sky4th.equipment.cache.EquipmentAttributesCache.invalidate(item)
        com.sky4th.equipment.cache.LoreDisplayCache.invalidate(item)
    }

    /**
     * 移除词条
     */
    fun removeAffix(item: ItemStack, affixId: String) {
        val meta = item.itemMeta ?: return
        val container = meta.persistentDataContainer
        val key = NamespacedKey("sky_equipment", "affix_${affixId.lowercase()}")
        container.remove(key)
        item.itemMeta = meta

        // 自动失效缓存
        com.sky4th.equipment.cache.EquipmentAttributesCache.invalidate(item)
        com.sky4th.equipment.cache.LoreDisplayCache.invalidate(item)
    }

    /**
     * 清空所有词条
     */
    fun clearAffixes(item: ItemStack) {
        val meta = item.itemMeta ?: return
        val container = meta.persistentDataContainer

        // 移除所有以affix_开头的键
        container.keys.filter { it.key.startsWith("affix_") }.forEach { namespacedKey ->
            container.remove(namespacedKey)
        }

        item.itemMeta = meta

        // 自动失效缓存
        com.sky4th.equipment.cache.EquipmentAttributesCache.invalidate(item)
        com.sky4th.equipment.cache.LoreDisplayCache.invalidate(item)
    }

    /**
     * 获取Double类型属性
     */
    fun getDouble(item: ItemStack, key: String): Double {
        val meta = item.itemMeta ?: return 0.0
        val container = meta.persistentDataContainer
        val namespacedKey = NamespacedKey("sky_equipment", key)
        return container.getOrDefault(namespacedKey, PersistentDataType.DOUBLE, 0.0)
    }

    /**
     * 设置Double类型属性
     */
    fun setDouble(item: ItemStack, key: String, value: Double) {
        val meta = item.itemMeta ?: return
        val container = meta.persistentDataContainer
        val namespacedKey = NamespacedKey("sky_equipment", key)
        container.set(namespacedKey, PersistentDataType.DOUBLE, value)
        item.itemMeta = meta
    }

    /**
     * 获取闪避率
     */
    fun getDodgeChance(item: ItemStack): Double {
        return getDouble(item, "dodge_chance")
    }

    /**
     * 设置闪避率
     */
    fun setDodgeChance(item: ItemStack, value: Double) {
        setDouble(item, "dodge_chance", value.coerceIn(0.0, 1.0))
    }

    /**
     * 获取抗击退
     */
    fun getKnockbackResistance(item: ItemStack): Double {
        return getDouble(item, "knockback_resistance")
    }

    /**
     * 设置抗击退
     */
    fun setKnockbackResistance(item: ItemStack, value: Double) {
        setDouble(item, "knockback_resistance", value.coerceIn(0.0, 1.0))
    }

    /**
     * 获取饥饿惩罚
     */
    fun getHungerPenalty(item: ItemStack): Double {
        return getDouble(item, "hunger_penalty")
    }

    /**
     * 设置饥饿惩罚
     */
    fun setHungerPenalty(item: ItemStack, value: Double) {
        setDouble(item, "hunger_penalty", value.coerceIn(0.0, 1.0))
    }

    /**
     * 获取移动惩罚
     */
    fun getMovementPenalty(item: ItemStack): Double {
        return getDouble(item, "movement_penalty")
    }

    /**
     * 设置移动惩罚
     */
    fun setMovementPenalty(item: ItemStack, value: Double) {
        setDouble(item, "movement_penalty", value.coerceIn(0.0, 1.0))
    }

    /**
     * 获取String类型属性
     */
    fun getString(item: ItemStack, key: String): String? {
        val meta = item.itemMeta ?: return null
        val container = meta.persistentDataContainer
        val namespacedKey = NamespacedKey("sky_equipment", key)
        return container.get(namespacedKey, PersistentDataType.STRING)
    }

    /**
     * 设置String类型属性
     */
    fun setString(item: ItemStack, key: String, value: String) {
        val meta = item.itemMeta ?: return
        val container = meta.persistentDataContainer
        val namespacedKey = NamespacedKey("sky_equipment", key)
        container.set(namespacedKey, PersistentDataType.STRING, value)
        item.itemMeta = meta
    }

    /**
     * 获取材质效果
     */
    fun getMaterialEffect(item: ItemStack): String {
        return getString(item, "material_effect") ?: "NONE"
    }

    /**
     * 设置材质效果
     */
    fun setMaterialEffect(item: ItemStack, effect: String) {
        setString(item, "material_effect", effect)
    }

    /**
     * 获取最大锻造词条槽位
     */
    fun getMaxAffixSlots(item: ItemStack): Int {
        val meta = item.itemMeta ?: return 0
        val container = meta.persistentDataContainer
        return container.getOrDefault(KEY_MAX_AFFIX_SLOTS, PersistentDataType.INTEGER, 0)
    }

    /**
     * 设置最大锻造词条槽位
     */
    fun setMaxAffixSlots(item: ItemStack, slots: Int) {
        val meta = item.itemMeta ?: return
        val container = meta.persistentDataContainer
        container.set(KEY_MAX_AFFIX_SLOTS, PersistentDataType.INTEGER, slots)
        item.itemMeta = meta
    }

    /**
     * 获取最大附魔槽位
     */
    fun getMaxEnchantmentSlots(item: ItemStack): Int {
        val meta = item.itemMeta ?: return 0
        val container = meta.persistentDataContainer
        return container.getOrDefault(KEY_MAX_ENCHANTMENT_SLOTS, PersistentDataType.INTEGER, 0)
    }

    /**
     * 设置最大附魔槽位
     */
    fun setMaxEnchantmentSlots(item: ItemStack, slots: Int) {
        val meta = item.itemMeta ?: return
        val container = meta.persistentDataContainer
        container.set(KEY_MAX_ENCHANTMENT_SLOTS, PersistentDataType.INTEGER, slots)
        item.itemMeta = meta
    }

    /**
     * 获取词条存储的资源数量
     * @param item 装备物品
     * @param affixId 词条ID
     * @return 存储的资源数量，如果不存在则返回0
     */
    fun getAffixResource(item: ItemStack, affixId: String): Int {
        val meta = item.itemMeta ?: return 0
        val container = meta.persistentDataContainer
        val key = NamespacedKey("sky_equipment", "${KEY_AFFIX_RESOURCE_PREFIX}${affixId.lowercase()}")
        return container.getOrDefault(key, PersistentDataType.INTEGER, 0)
    }

    /**
     * 设置词条存储的资源数量
     * @param item 装备物品
     * @param affixId 词条ID
     * @param amount 资源数量
     */
    fun setAffixResource(item: ItemStack, affixId: String, amount: Int) {
        val meta = item.itemMeta ?: return
        val container = meta.persistentDataContainer
        val key = NamespacedKey("sky_equipment", "${KEY_AFFIX_RESOURCE_PREFIX}${affixId.lowercase()}")
        if (amount <= 0) {
            container.remove(key)
        } else {
            container.set(key, PersistentDataType.INTEGER, amount)
        }
        item.itemMeta = meta

        // 失效两个缓存
        com.sky4th.equipment.cache.EquipmentAttributesCache.invalidate(item)
        com.sky4th.equipment.cache.LoreDisplayCache.invalidate(item)
        
        val isDetailed = container.getOrDefault(NamespacedKey("sky_equipment", "detailed_lore"), PersistentDataType.BYTE, 0) == 1.toByte()
        com.sky4th.equipment.manager.LoreDisplayManager.modifyItemLore(item, isDetailed)
    }

    /**
     * 增加词条存储的资源数量
     * @param item 装备物品
     * @param affixId 词条ID
     * @param amount 要增加的数量
     * @return 增加后的总数量
     */
    fun addAffixResource(item: ItemStack, affixId: String, amount: Int): Int {
        val currentAmount = getAffixResource(item, affixId)
        val newAmount = currentAmount + amount
        setAffixResource(item, affixId, newAmount)
        return newAmount
    }

    /**
     * 减少词条存储的资源数量
     * @param item 装备物品
     * @param affixId 词条ID
     * @param amount 要减少的数量
     * @return 实际减少的数量（如果资源不足则返回实际可减少的数量）
     */
    fun consumeAffixResource(item: ItemStack, affixId: String, amount: Int): Int {
        val currentAmount = getAffixResource(item, affixId)
        val actualConsume = minOf(amount, currentAmount)
        if (actualConsume > 0) {
            setAffixResource(item, affixId, currentAmount - actualConsume)
        }
        return actualConsume
    }

    /**
     * 批量读取装备属性
     * 优化版本：一次性读取所有属性，减少NBT访问次数
     */
    fun readAllAttributes(item: ItemStack): EquipmentData {
        val meta = item.itemMeta ?: return EquipmentData()
        val container = meta.persistentDataContainer

        return EquipmentData(
            equipmentId = getEquipmentId(item),
            proficiencyLevel = container.getOrDefault(KEY_PROFICIENCY_LEVEL, PersistentDataType.INTEGER, 0),
            proficiency = container.getOrDefault(KEY_PROFICIENCY, PersistentDataType.INTEGER, 0),
            dodgeChance = container.getOrDefault(KEY_DODGE_CHANCE, PersistentDataType.DOUBLE, 0.0),
            knockbackResistance = container.getOrDefault(KEY_KNOCKBACK_RESISTANCE, PersistentDataType.DOUBLE, 0.0),
            hungerPenalty = container.getOrDefault(KEY_HUNGER_PENALTY, PersistentDataType.DOUBLE, 0.0),
            movementPenalty = container.getOrDefault(KEY_MOVEMENT_PENALTY, PersistentDataType.DOUBLE, 0.0),
            materialEffect = container.get(KEY_MATERIAL_EFFECT, PersistentDataType.STRING) ?: "NONE",
            affixes = readAffixes(container)
        )
    }

    /**
     * 从容器中读取词条
     */
    private fun readAffixes(container: org.bukkit.persistence.PersistentDataContainer): Map<String, Int> {
        val affixes = mutableMapOf<String, Int>()
        container.keys.forEach { key ->
            if (key.key.startsWith("affix_")) {
                val affixId = key.key.substring(6).lowercase()
                val level = container.get(key, PersistentDataType.INTEGER) ?: 0
                affixes[affixId] = level
            }
        }
        return affixes
    }

    /**
     * 装备数据
     */
    data class EquipmentData(
        val equipmentId: String? = null,
        val proficiencyLevel: Int = 0,
        val proficiency: Int = 0,
        val dodgeChance: Double = 0.0,
        val knockbackResistance: Double = 0.0,
        val hungerPenalty: Double = 0.0,
        val movementPenalty: Double = 0.0,
        val materialEffect: String = "NONE",
        val affixes: Map<String, Int> = emptyMap()
    )
}
