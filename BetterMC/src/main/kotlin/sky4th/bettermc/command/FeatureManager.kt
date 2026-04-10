package sky4th.bettermc.command

import org.bukkit.configuration.file.FileConfiguration
import sky4th.bettermc.config.ConfigManager
import sky4th.core.api.LanguageAPI
import sky4th.bettermc.BetterMC

/**
 * 功能管理器
 * 
 * 负责管理插件功能的开启/关闭状态
 */
object FeatureManager {

    // 功能列表及其描述
    private val features = mapOf(
        "growth" to "features.growth",
        "animal-panic" to "features.animal-panic",
        "iron-golem" to "features.iron-golem",
        "villager-iron-golem" to "features.villager-iron-golem",
        "drops" to "features.drops",
        "player-kill" to "features.player-kill",
        "vehicle" to "features.vehicle",
        "portal" to "features.portal",
        "enderman" to "features.enderman",
        "fertilize" to "features.fertilize",
        "ender-pearl" to "features.ender-pearl",
        "crop-harvest" to "features.crop-harvest",
        "waterflow-immunity" to "features.waterflow-immunity",
        "player-hitbox" to "features.player-hitbox",
        "grass-attack" to "features.grass-attack",
    )

    // 功能开关映射
    private val featureToggles = mutableMapOf<String, Boolean>()

    /**
     * 获取所有功能（返回本地化后的描述）
     */
    fun getFeatures(): Map<String, String> {
        return features.mapValues { (_, key) ->
            key 
        }
    }


    /**
     * 检查功能是否启用
     */
    fun isFeatureEnabled(feature: String): Boolean {
        return featureToggles.getOrDefault(feature, true)
    }

    /**
     * 设置功能开关
     */
    fun setFeatureEnabled(feature: String, enabled: Boolean) {
        featureToggles[feature] = enabled
    }

    /**
     * 从配置加载功能开关
     */
    fun loadFeatureToggles() {
        val config = ConfigManager.getConfig()
        features.keys.forEach { feature ->
            val enabled = config.getBoolean("features.$feature.enabled", true)
            featureToggles[feature] = enabled
        }
    }

    /**
     * 保存功能开关到配置
     */
    fun saveFeatureToggles() {
        val config = ConfigManager.getConfig()
        features.keys.forEach { feature ->
            config.set("features.$feature.enabled", featureToggles.getOrDefault(feature, true))
        }
        ConfigManager.saveConfig()
    }
}
