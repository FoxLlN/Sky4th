package com.sky4th.equipment.loader

import com.sky4th.equipment.EquipmentAffix
import com.sky4th.equipment.modifier.ModifierManager
import com.sky4th.equipment.modifier.AffixConfigManager
import com.sky4th.equipment.modifier.AffixConfig
import com.sky4th.equipment.modifier.AffixInitializer
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.InputStreamReader
import java.util.jar.JarFile

/**
 * 词条加载器
 * 从JAR文件内的资源文件夹中加载词条配置
 */
object AffixLoader {

    /**
     * 从资源文件夹加载所有词条配置
     */
    fun loadAll(plugin: EquipmentAffix, modifierManager: ModifierManager) {
        val jarFile = File(plugin.javaClass.protectionDomain.codeSource.location.toURI())

        JarFile(jarFile).use { jar ->
            val entries = jar.entries()
            var loadedCount = 0
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.name.startsWith("affixes/") && entry.name.endsWith(".yml")) {
                    try {
                        val input = jar.getInputStream(entry)
                        val config = YamlConfiguration.loadConfiguration(InputStreamReader(input))
                        val affixConfig = loadFromYaml(config)
                        if (affixConfig != null) {
                            // 将配置注册到AffixConfigManager
                            AffixConfigManager.registerAffixConfig(affixConfig)
                            // 使用AffixInitializer注册词条实例
                            AffixInitializer.registerFromConfig(affixConfig)
                            loadedCount++
                        }
                    } catch (e: Exception) {
                        plugin.logger.warning("加载词条配置失败: ${entry.name} - ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
            plugin.logger.info("已从JAR加载 $loadedCount 个词条配置")
        }
    }

    /**
     * 从YAML配置加载词条
     */
    fun loadFromYaml(config: YamlConfiguration): AffixConfig? {
        val id = config.getString("id")?.replace("'", "")?: return null
        val displayName = config.getString("display_name")?: id
        val description = config.getString("description") ?: "暂无描述"
        val simpleDescription = config.getString("simple_description") ?: ""
        val maxLevel = config.getInt("max_level", 1)
        val isInit = config.getBoolean("is_init", false)

        // 加载适用的装备类别
        val applicableToList = config.getStringList("applicable_to")
        val applicableTo = mutableListOf<com.sky4th.equipment.attributes.EquipmentCategory>()
        applicableToList.forEach { category ->
            try {
                applicableTo.add(com.sky4th.equipment.attributes.EquipmentCategory.valueOf(category.uppercase()))
            } catch (e: IllegalArgumentException) {
                // 忽略无效的装备类别
            }
        }

        // 加载装备槽位
        val equipmentSlot = config.getStringList("equipment_slot")

        // 加载计算方式
        val calculationModeString = config.getString("calculation_mode", "HIGHEST") ?: "HIGHEST"
        val calculationMode = try {
            com.sky4th.equipment.attributes.AffixCalculationMode.valueOf(calculationModeString.uppercase())
        } catch (e: IllegalArgumentException) {
            com.sky4th.equipment.attributes.AffixCalculationMode.HIGHEST
        }

        // 加载冲突词条列表
        val conflictingAffixList = config.getStringList("conflicting_affixes")
        val conflictingAffixes = mutableListOf<String>()
        conflictingAffixList.forEach { affixId ->
            if (affixId.isNotBlank()) {
                conflictingAffixes.add(affixId)
            }
        }

        // 加载冲突附魔列表
        val conflictingEnchantmentList = config.getStringList("conflicting_enchantments")
        val conflictingEnchantments = mutableListOf<org.bukkit.enchantments.Enchantment>()
        conflictingEnchantmentList.forEach { enchantmentKey ->
            if (enchantmentKey.isNotBlank()) {
                try {
                    val enchantment = org.bukkit.Registry.ENCHANTMENT.get(org.bukkit.NamespacedKey.minecraft(enchantmentKey.lowercase()))
                    if (enchantment != null) {
                        conflictingEnchantments.add(enchantment)
                    }
                } catch (e: Exception) {
                    // 忽略无效的附魔
                }
            }
        }

        // 加载优先级
        val priority = config.getInt("priority", 0)

        // 加载触发槽位配置
        val triggerSlot = config.getString("trigger_slot", "MAIN_HAND") ?: "MAIN_HAND"

        // 加载充能相关配置
        val isChargeable = config.getBoolean("is_chargeable", false)
        val chargeResource = if (isChargeable) config.getString("charge_resource") else null
        val chargeResourceName = if (isChargeable) config.getString("charge_resource_name") else null
        
        // 加载各等级的资源上限
        val chargeMaxStorage = mutableMapOf<Int, Int>()
        if (isChargeable) {
            val storageSection = config.getConfigurationSection("charge_max_storage")
            if (storageSection != null) {
                storageSection.getKeys(false).forEach { key ->
                    val level = key.toIntOrNull()
                    if (level != null) {
                        chargeMaxStorage[level] = storageSection.getInt(key)
                    }
                }
            }
        }

        return AffixConfig(
            id = id,
            displayName = displayName,
            description = description,
            simpleDescription = simpleDescription,
            maxLevel = maxLevel,
            isInit = isInit,
            applicableTo = applicableTo,
            equipmentSlot = equipmentSlot,
            calculationMode = calculationMode,
            conflictingAffixes = conflictingAffixes,
            conflictingEnchantments = conflictingEnchantments,
            priority = priority,
            isChargeable = isChargeable,
            chargeResource = chargeResource,
            chargeResourceName = chargeResourceName,
            chargeMaxStorage = chargeMaxStorage,
            triggerSlot = triggerSlot
        )
    }
}
