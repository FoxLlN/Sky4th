package sky4th.core.api

import net.kyori.adventure.text.Component
import org.bukkit.plugin.Plugin
import sky4th.core.lang.LanguageManager
import sky4th.core.lang.LanguageManagerCache

/**
 * 语言系统 API
 * 供子插件访问 Core 的语言系统
 * 
 * 所有调用统一委托给 LanguageManagerCache，确保缓存一致性
 */
object LanguageAPI {
    /**
     * 获取指定插件的语言管理器
     * @param plugin 插件实例
     * @return 语言管理器实例
     */
    @JvmStatic
    fun getLanguageManager(plugin: Plugin): LanguageManager {
        return LanguageManagerCache.getOrCreate(plugin)
    }

    /**
     * 获取语言文本
     * @param plugin 插件实例
     * @param node 语言节点
     * @param args 参数
     * @return 文本内容
     */
    @JvmStatic
    fun getText(plugin: Plugin, node: String, vararg args: Pair<String, Any>): String {
        return getLanguageManager(plugin).getText(node, *args)
    }

    /**
     * 获取语言文本（可空版本）
     * @param plugin 插件实例
     * @param node 语言节点
     * @param args 参数
     * @return 文本内容，如果不存在返回 null
     */
    @JvmStatic
    fun getTextOrNull(plugin: Plugin, node: String, vararg args: Pair<String, Any>): String? {
        return getLanguageManager(plugin).getTextOrNull(node, *args)
    }

    /**
     * 获取语言文本列表
     * @param plugin 插件实例
     * @param node 语言节点
     * @param args 参数
     * @return 文本列表
     */
    @JvmStatic
    fun getTextList(plugin: Plugin, node: String, vararg args: Pair<String, Any>): List<String> {
        return getLanguageManager(plugin).getTextList(node, *args)
    }

    /**
     * 获取语言组件
     * @param plugin 插件实例
     * @param node 语言节点
     * @param args 参数
     * @return 组件
     */
    @JvmStatic
    fun getComponent(plugin: Plugin, node: String, vararg args: Pair<String, Any>): Component {
        return getLanguageManager(plugin).getComponent(node, *args)
    }

    /**
     * 获取语言组件列表
     * @param plugin 插件实例
     * @param node 语言节点
     * @param args 参数
     * @return 组件列表
     */
    @JvmStatic
    fun getComponentList(plugin: Plugin, node: String, vararg args: Pair<String, Any>): List<Component> {
        return getLanguageManager(plugin).getComponentList(node, *args)
    }

    /**
     * 获取前缀
     * @param plugin 插件实例
     * @return 前缀文本
     */
    @JvmStatic
    fun getPrefix(plugin: Plugin, prefix: String = "prefix"): String {
        return getLanguageManager(plugin).getPrefix(prefix)
    }

    /**
     * 重载语言文件
     * @param plugin 插件实例
     */
    @JvmStatic
    fun reload(plugin: Plugin) {
        getLanguageManager(plugin).reload()
    }

    /**
     * 将字符串转换为 Component
     * @param plugin 插件实例
     * @param text 文本
     * @return 组件
     */
    @JvmStatic
    fun toComponent(plugin: Plugin, text: String): Component {
        return getLanguageManager(plugin).toComponent(text)
    }

    /**
     * 将 Component 序列化为字符串
     * @param plugin 插件实例
     * @param component 组件
     * @return 序列化后的字符串
     */
    @JvmStatic
    fun serializeComponent(plugin: Plugin, component: Component): String {
        return getLanguageManager(plugin).serializeComponent(component)
    }

    /**
     * 获取当前语言代码
     * @param plugin 插件实例
     * @return 语言代码（如 "zh_CN"）
     */
    @JvmStatic
    fun getCurrentLocale(plugin: Plugin): String {
        return getLanguageManager(plugin).getCurrentLocale()
    }

    /**
     * 切换语言
     * @param plugin 插件实例
     * @param locale 语言代码（如 "zh_CN", "en_US"）
     */
    @JvmStatic
    fun setLocale(plugin: Plugin, locale: String) {
        getLanguageManager(plugin).setLocale(locale)
    }
}
