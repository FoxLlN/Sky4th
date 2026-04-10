package sky4th.bettermc.config

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import sky4th.bettermc.BetterMC
import sky4th.core.api.LanguageAPI
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

    // 成长相关配置
    var breedCooldownTicks: Long = 5 * 1200L // 繁衍冷却时间（刻）
    var animalGrowthTicks: Int = 20 * 1200 // 动物长大时间（刻）
    var villagerGrowthTicks: Int = 20 * 1200 // 村民长大时间（刻）
    var breedFailureChance: Double = 0.5 // 无权限时繁衍失败概率

    // 时间相关配置（单位：分钟）
    var dayTime: Double = 40.0 // 白天时长
    var nightTime: Double = 20.0 // 夜间时长
    var fastForwardTime: Double = 10.0 // 快进时长

    // 动物惊慌相关配置
    var panicRadius: Double = 5.0 // 惊慌扩散半径（方块）
    var panicDurationTicks: Long = 5  *20L // 惊慌持续时间（刻）
    var panicSpeedMultiplier: Double = 2.0 // 惊慌速度倍数
    var panicDistance: Double = 8.0// 每次尝试逃跑的距离（格）

    // 铁傀儡相关配置
    var ironGolemAttackWindowSeconds: Double = 10.0 // 攻击记录有效时间窗口（秒）
    var ironGolemMaxReductionHits: Int = 5 // 达到最大减伤所需的连续攻击次数
    var ironGolemBaseReduction: Double = 0.2 // 基础减伤（20%）
    var ironGolemMaxReduction: Double = 0.7 // 最大减伤（70%）

    // 玩家部位受击相关配置
    var headDamageMultiplier: Double = 1.5 // 头部伤害倍率
    var bodyDamageMultiplier: Double = 1.0 // 躯干伤害倍率
    var legsDamageMultiplier: Double = 0.7 // 腿部伤害倍率

    /**
     * 初始化配置文件
     */
    fun init(plugin: BetterMC) {
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
        // 重载语言文件
        LanguageAPI.reload(BetterMC.instance)
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
        // 加载功能开关配置
        sky4th.bettermc.command.FeatureManager.loadFeatureToggles()

        // 成长相关配置（从配置文件读取分钟，转换为刻）
        val breedCooldownMinutes = config.getDouble("growth.breed-cooldown-minutes", 5.0)
        val animalGrowthMinutes = config.getDouble("growth.animal-growth-minutes", 20.0)
        val villagerGrowthMinutes = config.getDouble("growth.villager-growth-minutes", 20.0)

        // 1分钟 = 60秒 = 1200刻
        breedCooldownTicks = (breedCooldownMinutes * 1200).toLong()
        animalGrowthTicks = (animalGrowthMinutes * 1200).toInt()
        villagerGrowthTicks = (villagerGrowthMinutes * 1200).toInt()

        breedFailureChance = config.getDouble("growth.breed-failure-chance", 0.5)

        // 加载掉落物配置
        sky4th.bettermc.drops.DropManager.loadFromConfig()

        // 时间相关配置
        dayTime = config.getDouble("time.day-time-minutes", 40.0)
        nightTime = config.getDouble("time.night-time-minutes", 20.0)
        fastForwardTime = config.getDouble("time.fast-forward-time-minutes", 10.0)

        // 动物惊慌相关配置
        panicRadius = config.getDouble("animal-panic.panic-radius", 5.0)
        val panicDurationSeconds = config.getDouble("animal-panic.panic-duration-seconds", 6.0)
        // 将秒转换为刻（1秒 = 20刻）
        panicDurationTicks = (panicDurationSeconds * 20).toLong()
        panicSpeedMultiplier = config.getDouble("animal-panic.panic-speed-multiplier", 2.0)
        panicDistance = config.getDouble("animal-panic.panic-distance", 8.0)

        // 铁傀儡相关配置
        ironGolemAttackWindowSeconds = config.getDouble("iron-golem.attack-window-seconds", 10.0)
        ironGolemMaxReductionHits = config.getInt("iron-golem.max-reduction-hits", 5)
        ironGolemBaseReduction = config.getDouble("iron-golem.base-reduction", 0.2)
        ironGolemMaxReduction = config.getDouble("iron-golem.max-reduction", 0.7)

        // 玩家部位受击相关配置
        headDamageMultiplier = config.getDouble("player-hitbox.head-damage-multiplier", 1.5)
        bodyDamageMultiplier = config.getDouble("player-hitbox.body-damage-multiplier", 1.0)
        legsDamageMultiplier = config.getDouble("player-hitbox.legs-damage-multiplier", 0.7)
    }

    /**
     * 获取配置文件
     */
    fun getConfig(): FileConfiguration {
        return config
    }
}
