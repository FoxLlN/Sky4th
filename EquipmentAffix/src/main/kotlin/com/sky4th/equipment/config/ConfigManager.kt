
package com.sky4th.equipment.config

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import com.sky4th.equipment.EquipmentAffix
import java.io.File
import java.io.IOException

/**
 * 配置管理器
 * 
 * 负责加载和管理插件的所有配置项
 */
object ConfigManager {

    private lateinit var configFile: File
    private lateinit var config: FileConfiguration

    // 熟练度相关配置
    var levelRequirements: List<Int> = listOf(100, 250, 500, 1000, 2000)

    // 装备属性上限配置
    var maxDodgeChance: Double = 0.8 // 最大闪避率
    var maxKnockbackResistance: Double = 1.0 // 最大抗击退
    var maxMovementPenalty: Double = 1.0 // 最大移动惩罚
    var maxHungerPenalty: Double = 1.0 // 最大饥饿惩罚


    /**
     * 初始化配置文件
     */
    fun init(plugin: EquipmentAffix) {
        val dataFolder = plugin.dataFolder
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }

        configFile = File(dataFolder, "config.yml")

        // 如果配置文件不存在，从资源中复制
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false)
        }

        // 加载配置
        reloadConfig()
    }

    /**
     * 重新加载配置
     */
    fun reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile)
        loadValues()
    }

    /**
     * 保存配置
     */
    fun saveConfig() {
        try {
            config.save(configFile)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * 从配置文件加载所有值
     */
    private fun loadValues() {
        // 加载熟练度配置
        levelRequirements = config.getIntegerList("proficiency.level_requirements")
        if (levelRequirements.isEmpty()) {
            levelRequirements = listOf(100, 250, 500, 1000, 2000)
        }

        // 加载装备属性上限配置（如果配置文件中有这些项）
        maxDodgeChance = config.getDouble("equipment.max_dodge_chance", 0.8)
        maxKnockbackResistance = config.getDouble("equipment.max_knockback_resistance", 1.0)
        maxMovementPenalty = config.getDouble("equipment.max_movement_penalty", 1.0)
        maxHungerPenalty = config.getDouble("equipment.max_hunger_penalty", 1.0)

    }

    /**
     * 获取配置文件
     */
    fun getConfig(): FileConfiguration {
        return config
    }
}
