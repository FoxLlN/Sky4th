package com.sky4th.equipment.loader

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.NamespacedKey
import com.sky4th.equipment.attributes.EquipmentAttributes
import com.sky4th.equipment.attributes.EquipmentCategory
import com.sky4th.equipment.registry.EquipmentRegistry
import com.sky4th.equipment.registry.EquipmentType
import java.io.File
import java.io.InputStreamReader
import java.util.jar.JarFile

/**
 * 装备加载器
 * 从资源文件夹中的YAML文件加载装备配置
 */
object EquipmentLoader {

    /**
     * 从资源文件夹加载所有装备
     */
    fun loadAll(plugin: Any) {
        // 获取资源文件夹中的装备目录
        val equipmentDir = File(plugin.javaClass.protectionDomain.codeSource.location.toURI())

        loadFromJar(equipmentDir)
        
    }

    /**
     * 从JAR文件中加载装备配置
     */
    private fun loadFromJar(jarFile: File) {
        JarFile(jarFile).use { jar ->
            val entries = jar.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.name.startsWith("equipments/") && entry.name.endsWith(".yml")) {
                    try {
                        val input = jar.getInputStream(entry)
                        val config = YamlConfiguration.loadConfiguration(InputStreamReader(input))
                        loadEquipmentFromConfig(config)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    /**
     * 从配置对象加载装备
     */
    private fun loadEquipmentFromConfig(config: YamlConfiguration) {
        val id = config.getString("id") ?: return
        val materialName = config.getString("material") ?: return
        val material = Material.getMaterial(materialName) ?: return
        // 读取装备类别列表
        val categories = config.getStringList("categories").mapNotNull { categoryName ->
            try {
                EquipmentCategory.valueOf(categoryName.uppercase())
            } catch (e: IllegalArgumentException) {
                null
            }
        }
        

        val displayName = config.getString("display_name") ?: "未命名装备"
        val armor = config.getDouble("armor", 0.0)
        val toughness = config.getDouble("toughness", 0.0)
        val attackDamage = config.getDouble("attack_damage", 0.0)
        val attackSpeed = config.getDouble("attack_speed", 0.0)
        val maxEnchantmentSlots = config.getInt("max_enchantment_slots", 0)
        val maxAffixSlots = config.getInt("max_affix_slots", 0)
        val maxDurability = config.getInt("max_durability", 0)

        // 读取新属性
        val dodgeChance = config.getDouble("dodge_chance", 0.0)
        val knockbackResistance = config.getDouble("knockback_resistance", 0.0)
        val hungerPenalty = config.getDouble("hunger_penalty", 0.0)
        val movementPenalty = config.getDouble("movement_penalty", 0.0)
        val materialEffect = config.getString("material_effect") ?: "NONE"

        // 加载词条
        val affixes = mutableMapOf<String, Int>()
        val affixSection = config.getConfigurationSection("affixes")
        affixSection?.getKeys(false)?.forEach { affixId ->
            val level = affixSection.getInt(affixId, 1)
            affixes[affixId] = level
        }

        // 加载附魔
        val enchantments = mutableMapOf<Enchantment, Int>()
        val enchantSection = config.getConfigurationSection("enchantments")
        enchantSection?.getKeys(false)?.forEach { enchantName ->
            try {
                val namespacedKey = org.bukkit.NamespacedKey.minecraft(enchantName.lowercase())
                @Suppress("DEPRECATION")
                val enchant = org.bukkit.Registry.ENCHANTMENT.get(namespacedKey)
                if (enchant != null) {
                    val level = enchantSection.getInt(enchantName, 1)
                    enchantments[enchant] = level
                }
            } catch (e: Exception) {
                // 忽略无效的附魔
            }
        }

        // 创建装备属性
        val attributes = EquipmentAttributes(
            material = material,
            proficiencyLevel = 0,
            proficiency = 0,
            affixes = affixes,
            enchantments = enchantments,
            dodgeChance = dodgeChance,
            knockbackResistance = knockbackResistance,
            hungerPenalty = hungerPenalty,
            movementPenalty = movementPenalty,
            materialEffect = materialEffect
        )

        // 创建装备类型
        val equipmentType = EquipmentType(
            id = id,
            material = material,
            categories = categories,
            displayName = displayName,
            defaultAttributes = attributes,
            armor = armor,
            toughness = toughness,
            attackDamage = attackDamage,
            attackSpeed = attackSpeed,
            maxEnchantmentSlots = maxEnchantmentSlots,
            maxAffixSlots = maxAffixSlots,
            maxDurability = maxDurability
        )

        // 注册装备类型
        EquipmentRegistry.registerEquipmentType(equipmentType)

        // 加载配方（在装备注册之后）
        val plugin = com.sky4th.equipment.EquipmentAffix.instance
        RecipeLoader.loadRecipeFromConfig(config, plugin)
    }
}
