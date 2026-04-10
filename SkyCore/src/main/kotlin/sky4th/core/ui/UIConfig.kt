package sky4th.core.ui

import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

/**
 * UI配置类
 * 存储单个UI的所有配置信息
 */
class UIConfig(
    val id: String,
    val title: String,
    val shape: List<String>,
    val templates: Map<String, UITemplate>,
    val pluginName: String = "SkyCore"
) {
    companion object {
        /**
         * 从文件加载UI配置
         */
        fun load(file: File, pluginName: String = "SkyCore"): UIConfig {
            val config = YamlConfiguration.loadConfiguration(file)

            val id = config.getString("id") ?: file.nameWithoutExtension
            val title = config.getString("title") ?: "未命名UI"
            val shape = config.getStringList("shape")

            // 加载模板
            val templates = mutableMapOf<String, UITemplate>()
            val templateSection = config.getConfigurationSection("template") ?: throw IllegalArgumentException("缺少template配置")

            templateSection.getKeys(false).forEach { key ->
                templates[key] = loadTemplate(templateSection, key, shape)
            }

            return UIConfig(id, title, shape, templates, pluginName)
        }

        /**
         * 加载单个模板
         */
        fun loadTemplate(section: org.bukkit.configuration.ConfigurationSection, key: String, shape: List<String>? = null): UITemplate {
            val templateSection = section.getConfigurationSection(key) ?: throw IllegalArgumentException("模板 $key 配置无效")

            val materialName = templateSection.getString("material")
            val material = if (materialName != null) {
                try {
                    Material.valueOf(materialName)
                } catch (e: Exception) {
                    Material.AIR
                }
            } else {
                Material.AIR
            }

            val name = templateSection.getString("name")
            val lore = templateSection.getStringList("lore")

            // 加载模板级别的特性
            val features = mutableMapOf<String, Any>()
            val featureSection = templateSection.getConfigurationSection("feature")
            if (featureSection != null) {
                featureSection.getKeys(false)?.forEach { featureKey ->
                    val featureValue = featureSection.get(featureKey)
                    features[featureKey] = featureValue!!
                }
            }

            // 计算槽位位置
            val slot = if (shape != null) {
                calculateSlotPosition(shape, key)
            } else {
                -1
            }

            return UITemplate(key, material, name ?: "", lore, features, slot)
        }

        /**
         * 计算指定key在shape中的槽位位置
         */
        private fun calculateSlotPosition(shape: List<String>, key: String): Int {
            var slot = 0
            for (line in shape) {
                for (char in line) {
                    if (char.toString() == key) {
                        return slot
                    }
                    slot++
                }
            }
            return -1
        }
    }
}

/**
 * UI模板类
 * 定义单个UI槽位的物品样式
 */
data class UITemplate(
    val key: String,
    val material: Material,
    val name: String,
    val lore: List<String>,
    val features: Map<String, Any> = emptyMap(),
    val slot: Int = -1  // 槽位在inventory中的位置
)
