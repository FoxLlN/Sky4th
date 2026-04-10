package sky4th.core.lang

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import sky4th.core.util.ColorUtil
import java.io.File

/**
 * Sky4th 语言管理器
 * 
 * 提供语言文本管理功能，支持：
 * - 多语言支持（通过 locale 参数）
 * - 参数替换（key-value 格式）
 * - 前缀自动添加
 * - 语言文件热重载
 * - Adventure Component 支持
 * - 消息发送功能
 * - 旧版 & 颜色代码和新版 MiniMessage RGB 颜色同时支持
 */
class LanguageManager(private val plugin: Plugin) {
    
    private var langConfig: FileConfiguration? = null
    private var langFile: File? = null
    private var currentLocale: String = "zh_CN"

    private val miniMessage = MiniMessage.miniMessage()
    
    init {
        loadLanguage()
    }
    
    /**
     * 加载语言文件
     */
    fun loadLanguage(locale: String = "zh_CN") {
        currentLocale = locale
        val langDir = File(plugin.dataFolder, "lang")
        if (!langDir.exists()) {
            langDir.mkdirs()
        }
        
        // 加载指定语言文件
        val langFile = File(langDir, "$locale.yml")
        if (!langFile.exists()) {
            plugin.saveResource("lang/$locale.yml", false)
        }
        
        this.langFile = langFile
        this.langConfig = YamlConfiguration.loadConfiguration(langFile)
        
        plugin.logger.info("已加载语言文件: $locale")
    }
    
    /**
     * 获取当前语言代码
     */
    fun getCurrentLocale(): String = currentLocale

    /**
     * 切换语言
     * @param locale 语言代码（如 "zh_CN", "en_US"）
     */
    fun setLocale(locale: String) {
        loadLanguage(locale)
    }

    /**
     * 处理文本参数替换和颜色代码
     */
    private fun processText(text: String, args: Array<out Pair<String, Any>>): String {
        var result = text
        // 处理参数替换
        args.forEach { (paramKey, value) ->
            result = result.replace("{$paramKey}", value.toString())
        }
        return result
    }

    /**
     * 获取语言配置
     */
    private fun getLangConfig(): FileConfiguration {
        return langConfig ?: throw IllegalStateException("语言文件未加载")
    }
    
    /**
     * 获取语言文本（支持参数替换）
     * 
     * @param key 语言节点路径（支持点分隔，如 "command.usage"）
     * @param args 参数对，格式为 key to value
     * @return 格式化后的文本
     */
    fun getText(key: String, vararg args: Pair<String, Any>): String {
        val config = getLangConfig()
        val text = config.getString(key) ?: key
        val processed = processText(text, args)
        return ColorUtil.convertLegacyToMiniMessage(processed)
    }
    
    /**
     * 获取语言文本（可空版本）
     */
    fun getTextOrNull(key: String, vararg args: Pair<String, Any>): String? {
        val config = getLangConfig()
        val raw = config.getString(key) ?: return null
        val processed = processText(raw, args)
        return ColorUtil.convertLegacyToMiniMessage(processed)
    }

    
    /**
     * 获取语言文本列表（多行文本）
     */
    fun getTextList(key: String, vararg args: Pair<String, Any>): List<String> {
        val config = getLangConfig()
        val rawList = config.getStringList(key)

        if (rawList.isEmpty()) {
            // 尝试作为单个字符串获取
            val single = config.getString(key)
            if (single != null) {
                return listOf(ColorUtil.convertLegacyToMiniMessage(processText(single, args)))
            }
            return listOf(key)
        }

        return rawList.map { line -> ColorUtil.convertLegacyToMiniMessage(processText(line, args)) }
    }

    /**
     * 获取指定前缀
     */
    fun getPrefix(prefix: String = "prefix"): String {
        return getTextOrNull("$prefix") ?: ""
    }
    
    /**
     * 重载语言文件
     */
    fun reload() {
        langConfig = null
        langFile = null
        loadLanguage()
        plugin.logger.info("语言文件已重新加载")
    }

    /**
     * 将字符串转换为 Component
     */
    fun toComponent(text: String): Component {
        return miniMessage.deserialize(text)
    }

    /**
     * 获取语言文本并转换为 Component（支持参数替换）
     *
     * @param key 语言节点路径
     * @param args 参数对
     * @return Component 对象
     */
    fun getComponent(key: String, vararg args: Pair<String, Any>): Component {
        val text = getText(key, *args)
        return toComponent(text)
    }

    /**
     * 获取语言文本列表并转换为 Component 列表（支持参数替换）
     *
     * @param key 语言节点路径
     * @param args 参数对
     * @return Component 列表
     */
    fun getComponentList(key: String, vararg args: Pair<String, Any>): List<Component> {
        return getTextList(key, *args).map { toComponent(it) }
    }

    /**
     * 将 Component 序列化为字符串
     */
    fun serializeComponent(component: Component): String {
        return miniMessage.serialize(component)
    }
}
