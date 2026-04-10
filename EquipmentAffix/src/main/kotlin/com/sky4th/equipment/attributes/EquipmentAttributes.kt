package com.sky4th.equipment.attributes

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Material
import org.bukkit.inventory.meta.Damageable
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import com.sky4th.equipment.data.NBTEquipmentDataManager

/**
 * Component工具类
 */
private object ComponentUtils {
    private val serializer = LegacyComponentSerializer.legacySection()

    fun toString(component: Component): String {
        return serializer.serialize(component)
    }

    fun toComponent(text: String): Component {
        return serializer.deserialize(text)
    }
}

/**
 * 装备属性类
 * 包含装备的所有属性：材质、熟练度等级、熟练度（使用次数）、词条、附魔
 * 熟练度绑定在具体的武器/装备上
 * 数据存储在NBT标签中，Lore仅用于显示
 */
data class EquipmentAttributes(
    // 装备材质
    val material: Material,
    // 熟练度等级（该装备的使用等级）
    val proficiencyLevel: Int = 0,
    // 熟练度（使用次数）
    val proficiency: Int = 0,
    // 词条列表（词条ID及其等级）
    val affixes: Map<String, Int> = emptyMap(),
    // 附魔列表
    val enchantments: Map<Enchantment, Int> = emptyMap(),
    // 闪避率（0.0 - 1.0）
    val dodgeChance: Double = 0.0,
    // 抗击退（0.0 - 1.0）
    val knockbackResistance: Double = 0.0,
    // 饥饿惩罚（0.0 - 1.0）
    val hungerPenalty: Double = 0.0,
    // 移动惩罚（0.0 - 1.0）
    val movementPenalty: Double = 0.0,
    // 材质效果类型
    val materialEffect: String = "NONE"
) {
    companion object {
        /**
         * 从ItemStack创建装备属性
         * 从NBT标签中读取数据
         * 优化版本：使用缓存机制和批量读取方法
         */
        fun fromItemStack(item: ItemStack): EquipmentAttributes {
            // 检查是否是装备
            if (!NBTEquipmentDataManager.isEquipment(item)) {
                return EquipmentAttributes(item.type)
            }

            // 使用缓存获取属性
            return com.sky4th.equipment.cache.EquipmentAttributesCache.getAttributes(item)
        }
    }

    /**
     * 将装备属性应用到ItemStack
     * 数据存储到NBT标签中，Lore仅用于显示
     */
    fun applyToItemStack(item: ItemStack, displayName: String = "", maxDurability: Int = 0): ItemStack {
        // 物品类型应该在创建时设置，这里不再修改
        // 如果物品类型不匹配，记录警告但不修改
        if (item.type != material) {
            // 可以选择在这里创建一个新的 ItemStack，但为了保持引用不变，我们只记录警告
            // 实际使用中，应该确保传入的 ItemStack 类型与 material 匹配
        }

        val meta = item.itemMeta ?: return item

        // 设置装备名称（不使用斜体）
        if (displayName.isNotEmpty()) {
            meta.displayName(ComponentUtils.toComponent(displayName).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false))
        }

        // 将数据存储到NBT
        val container = meta.persistentDataContainer
        container.set(com.sky4th.equipment.data.NBTEquipmentDataManager.KEY_PROFICIENCY_LEVEL, org.bukkit.persistence.PersistentDataType.INTEGER, proficiencyLevel)
        container.set(com.sky4th.equipment.data.NBTEquipmentDataManager.KEY_PROFICIENCY, org.bukkit.persistence.PersistentDataType.INTEGER, proficiency)
        
        // 保存新属性
        container.set(org.bukkit.NamespacedKey("sky_equipment", "dodge_chance"), org.bukkit.persistence.PersistentDataType.DOUBLE, dodgeChance)
        container.set(org.bukkit.NamespacedKey("sky_equipment", "knockback_resistance"), org.bukkit.persistence.PersistentDataType.DOUBLE, knockbackResistance)
        container.set(org.bukkit.NamespacedKey("sky_equipment", "hunger_penalty"), org.bukkit.persistence.PersistentDataType.DOUBLE, hungerPenalty)
        container.set(org.bukkit.NamespacedKey("sky_equipment", "movement_penalty"), org.bukkit.persistence.PersistentDataType.DOUBLE, movementPenalty)
        container.set(org.bukkit.NamespacedKey("sky_equipment", "material_effect"), org.bukkit.persistence.PersistentDataType.STRING, materialEffect)

        // 清空并重新设置词条（直接在container上操作）
        // 移除所有以affix_开头的键
        container.keys.filter { it.key.startsWith("affix_") }.forEach { namespacedKey ->
            container.remove(namespacedKey)
        }

        // 设置普通词条（排除材质效果词条）
        affixes.forEach { (affixId, level) ->
            // 跳过材质效果词条（以material_开头的词条）
            if (!affixId.startsWith("material_")) {
                val affixKey = org.bukkit.NamespacedKey("sky_equipment", "affix_$affixId")
                container.set(affixKey, org.bukkit.persistence.PersistentDataType.INTEGER, level)
            }
        }

        // 应用附魔
        val enchantsToRemove = item.enchantments.keys.toList()
        enchantsToRemove.forEach { enchant ->
            item.removeEnchantment(enchant)
        }
        enchantments.forEach { (enchant, level) ->
            item.addUnsafeEnchantment(enchant, level)
        }

        // 设置耐久度（使用原版Durability属性）
        if (maxDurability > 0) {
            if (meta is org.bukkit.inventory.meta.Damageable) {
                meta.setMaxDamage(maxDurability) 
                // 设置当前耐久为0（满耐久）
                meta.damage = 0
            }
        }

        // 隐藏原版附魔显示
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)

        // 保存meta
        item.itemMeta = meta

        // 使缓存失效
        com.sky4th.equipment.cache.EquipmentAttributesCache.invalidate(item)
        com.sky4th.equipment.cache.LoreDisplayCache.invalidate(item)

        return item
    }

    /**
     * 获取指定词条的等级
     */
    fun getAffixLevel(affixId: String): Int {
        return affixes[affixId] ?: 0
    }

    /**
     * 检查是否有指定词条
     */
    fun hasAffix(affixId: String): Boolean {
        return affixes.containsKey(affixId)
    }

    /**
     * 添加词条
     */
    fun addAffix(affixId: String, level: Int = 1): EquipmentAttributes {
        // 检查词条是否存在
        val modifier = com.sky4th.equipment.modifier.ModifierManager.instance.getModifier(affixId)
        if (modifier == null) {
            return this
        }
        
        // 检查等级是否有效（默认最大等级为1，实际应从配置读取）
        val actualLevel = if (level < 1) 1 else level

        return copy(affixes = affixes.toMutableMap().apply { put(affixId, actualLevel) })
    }

    /**
     * 移除词条
     */
    fun removeAffix(affixId: String): EquipmentAttributes {
        return copy(affixes = affixes.toMutableMap().apply { remove(affixId) })
    }

    /**
     * 获取指定附魔的等级
     */
    fun getEnchantmentLevel(enchantment: Enchantment): Int {
        return enchantments[enchantment] ?: 0
    }

    /**
     * 检查是否有指定附魔
     */
    fun hasEnchantment(enchantment: Enchantment): Boolean {
        return enchantments.containsKey(enchantment)
    }

    /**
     * 添加附魔
     */
    fun addEnchantment(enchantment: Enchantment, level: Int): EquipmentAttributes {
        return copy(enchantments = enchantments.toMutableMap().apply { put(enchantment, level) })
    }

    /**
     * 移除附魔
     */
    fun removeEnchantment(enchantment: Enchantment): EquipmentAttributes {
        return copy(enchantments = enchantments.toMutableMap().apply { remove(enchantment) })
    }

    /**
     * 增加熟练度（使用次数）
     * 每级累计机制：升级后经验清零
     */
    fun increaseProficiency(): EquipmentAttributes {
        val levelRequirements = com.sky4th.equipment.config.ConfigManager.levelRequirements
        val requiredExp = if (proficiencyLevel < levelRequirements.size) {
            levelRequirements[proficiencyLevel]
        } else {
            0
        }

        val newProficiency = proficiency + 1
        var newProficiencyLevel = proficiencyLevel

        // 检查是否升级
        if (requiredExp > 0 && newProficiency >= requiredExp) {
            newProficiencyLevel++
            // 升级后经验清零
            val overflow = newProficiency - requiredExp
            return copy(
                proficiency = overflow,
                proficiencyLevel = newProficiencyLevel
            )
        }

        return copy(
            proficiency = newProficiency,
            proficiencyLevel = newProficiencyLevel
        )
    }

    /**
     * 获取下一级所需的使用次数
     * 每级累计机制：返回当前等级升级所需的经验减去当前经验
     */
    fun getRequiredUsageForNextLevel(): Int {
        val levelRequirements = com.sky4th.equipment.config.ConfigManager.levelRequirements
        val maxLevel = levelRequirements.size

        if (proficiencyLevel >= maxLevel) {
            return 0 // 已达到最高等级
        }

        val requiredExp = levelRequirements[proficiencyLevel]
        return (requiredExp - proficiency).coerceAtLeast(0)
    }

    /**
     * 获取当前等级的进度（0.0 - 1.0）
     * 每级累计机制：返回当前经验占当前等级升级所需经验的比例
     */
    fun getLevelProgress(): Double {
        val levelRequirements = com.sky4th.equipment.config.ConfigManager.levelRequirements
        val maxLevel = levelRequirements.size

        if (proficiencyLevel >= maxLevel) {
            return 1.0 // 已达到最高等级
        }

        val requiredExp = levelRequirements[proficiencyLevel]

        if (requiredExp == 0) {
            return 0.0
        }

        val progress = proficiency.toDouble() / requiredExp
        return progress.coerceIn(0.0, 1.0)
    }
}
