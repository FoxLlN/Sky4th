package sky4th.core.lang

import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import org.bukkit.plugin.Plugin
import sky4th.core.SkyCore
import java.util.concurrent.ConcurrentHashMap

/**
 * Sky4th 语言系统扩展函数（简化版，仅支持中文）
 * 
 * 提供便捷的语言文本获取和发送功能
 */

/**
 * 获取语言管理器实例
 */
private fun getLanguageManager(): LanguageManager? {
    val plugin = SkyCore.getInstance() ?: return null
    return LanguageManagerCache.getOrCreate(plugin)
}

/**
 * 向命令发送者发送语言文本
 * 
 * @param node 语言节点路径（支持点分隔，如 "command.usage"）
 * @param args 参数对，格式为 key to value
 * 
 */
fun CommandSender.sendLang(prefix: String = "prefix", node: String, vararg args: Pair<String, Any>) {
    val manager = getLanguageManager() ?: return
    val prefixComponent = manager.toComponent(manager.getPrefix(prefix))
    val textComponent = manager.getComponent(node, *args)
    sendMessage(prefixComponent.append(textComponent))
}

/**
 * 向命令发送者发送指定插件的语言文本（使用该插件的 lang 文件与前缀）
 * 子插件（如平行地牢）应使用此重载，传入自己的 Plugin 实例。
 */
fun CommandSender.sendLang(plugin: Plugin, prefix: String = "prefix", node: String, vararg args: Pair<String, Any>) {
    val manager = LanguageManagerCache.getOrCreate(plugin)
    val prefixComponent = manager.toComponent(manager.getPrefix(prefix))
    val textComponent = manager.getComponent(node, *args)
    sendMessage(prefixComponent.append(textComponent))
}

/**
 * 获取语言文本（可空版本）
 * @param node 语言节点路径
 * @param args 参数对
 * @return 语言文本，如果节点不存在则返回 null
 */
fun CommandSender.asLangOrNull(node: String, vararg args: Pair<String, Any>): String? {
    val manager = getLanguageManager() ?: return null
    return manager.getTextOrNull(node, *args)
}

/**
 * 获取指定插件的语言文本（可空版本）
 */
fun CommandSender.asLangOrNull(plugin: Plugin, node: String, vararg args: Pair<String, Any>): String? {
    return LanguageManagerCache.getOrCreate(plugin).getTextOrNull(node, *args)
}

/**
 * 获取语言文本
 * @param node 语言节点路径
 * @param args 参数对
 * @return 语言文本，如果节点不存在则返回节点名称
 */
fun CommandSender.asLang(node: String, vararg args: Pair<String, Any>): String {
    return asLangOrNull(node, *args) ?: node
}

/**
 * 获取指定插件的语言文本
 */
fun CommandSender.asLang(plugin: Plugin, node: String, vararg args: Pair<String, Any>): String {
    return asLangOrNull(plugin, node, *args) ?: node
}

/**
 * 获取语言文本列表（多行文本）
 * @param node 语言节点路径
 * @param args 参数对
 * @return 语言文本列表
 */
fun CommandSender.asLangList(node: String, vararg args: Pair<String, Any>): List<String> {
    val manager = getLanguageManager() ?: return listOf(node)
    return manager.getTextList(node, *args)
}

/**
 * 获取指定插件的语言文本列表（多行文本，如 command.help）
 */
fun CommandSender.asLangList(plugin: Plugin, node: String, vararg args: Pair<String, Any>): List<String> {
    return LanguageManagerCache.getOrCreate(plugin).getTextList(node, *args)
}

/**
 * 获取已缓存的语言管理器
 */
internal fun getCachedLanguageManager(plugin: Plugin): LanguageManager =
    LanguageManagerCache.getOrCreate(plugin)

/**
 * 语言管理器缓存
 * 每个插件实例对应一个 LanguageManager
 */
object LanguageManagerCache {
    private val managers = ConcurrentHashMap<String, LanguageManager>()
    
    fun getOrCreate(plugin: Plugin): LanguageManager {
        return managers.getOrPut(plugin.name) {
            LanguageManager(plugin)
        }
    }
    
    fun remove(plugin: Plugin) {
        managers.remove(plugin.name)
    }
    
    fun clear() {
        managers.clear()
    }
}

/**
 * 直接获取语言文本（用于非 CommandSender 上下文，如 logger）
 * 
 * @param node 语言节点路径
 * @param args 参数对
 * @return 语言文本
 */
fun getLangText(node: String, vararg args: Pair<String, Any>): String {
    val manager = getLanguageManager() ?: return node
    return manager.getText(node, *args)
}

/**
 * 直接获取语言文本（指定插件实例）
 * 
 * @param plugin 插件实例
 * @param node 语言节点路径
 * @param args 参数对
 * @return 语言文本
 */
fun getLangText(plugin: Plugin, node: String, vararg args: Pair<String, Any>): String {
    val manager = LanguageManagerCache.getOrCreate(plugin)
    return manager.getText(node, *args)
}

/**
 * 直接获取语言文本列表（用于非 CommandSender 上下文）
 *
 * @param node 语言节点路径
 * @param args 参数对
 * @return 语言文本列表
 */
fun getLangTextList(node: String, vararg args: Pair<String, Any>): List<String> {
    val manager = getLanguageManager() ?: return listOf()
    return manager.getTextList(node, *args)
}

/**
 * 直接获取指定插件的语言文本列表
 *
 * @param plugin 插件实例
 * @param node 语言节点路径
 * @param args 参数对
 * @return 语言文本列表
 */
fun getLangTextList(plugin: Plugin, node: String, vararg args: Pair<String, Any>): List<String> {
    return LanguageManagerCache.getOrCreate(plugin).getTextList(node, *args)
}

/**
 * 向命令发送者发送 Component 格式的语言文本（带前缀）
 *
 * @param node 语言节点路径
 * @param args 参数对
 */
fun CommandSender.sendLangComponent(prefix: String = "prefix", node: String, vararg args: Pair<String, Any>) {
    val manager = getLanguageManager() ?: return
    val prefix = manager.toComponent(manager.getPrefix(prefix))
    val text = manager.getComponent(node, *args)
    sendMessage(prefix.append(text))
}

/**
 * 向命令发送者发送指定插件的 Component 格式语言文本（带前缀）
 *
 * @param plugin 插件实例
 * @param node 语言节点路径
 * @param args 参数对
 */
fun CommandSender.sendLangComponent(plugin: Plugin, prefix: String = "prefix", node: String, vararg args: Pair<String, Any>) {
    val manager = LanguageManagerCache.getOrCreate(plugin)
    val prefix = manager.toComponent(manager.getPrefix(prefix))
    val text = manager.getComponent(node, *args)
    sendMessage(prefix.append(text))
}

/**
 * 获取 Component 格式的语言文本
 *
 * @param node 语言节点路径
 * @param args 参数对
 * @return Component 对象
 */
fun CommandSender.asLangComponent(node: String, vararg args: Pair<String, Any>): Component? {
    val manager = getLanguageManager() ?: return null
    return manager.getComponent(node, *args)
}

/**
 * 获取指定插件的 Component 格式语言文本
 *
 * @param plugin 插件实例
 * @param node 语言节点路径
 * @param args 参数对
 * @return Component 对象
 */
fun CommandSender.asLangComponent(plugin: Plugin, node: String, vararg args: Pair<String, Any>): Component? {
    return LanguageManagerCache.getOrCreate(plugin).getComponent(node, *args)
}

/**
 * 获取 Component 格式的语言文本列表
 *
 * @param node 语言节点路径
 * @param args 参数对
 * @return Component 列表
 */
fun CommandSender.asLangComponentList(node: String, vararg args: Pair<String, Any>): List<Component> {
    val manager = getLanguageManager() ?: return listOf()
    return manager.getComponentList(node, *args)
}

/**
 * 获取指定插件的 Component 格式语言文本列表
 *
 * @param plugin 插件实例
 * @param node 语言节点路径
 * @param args 参数对
 * @return Component 列表
 */
fun CommandSender.asLangComponentList(plugin: Plugin, node: String, vararg args: Pair<String, Any>): List<Component> {
    return LanguageManagerCache.getOrCreate(plugin).getComponentList(node, *args)
}

/**
 * 直接获取 Component 格式的语言文本（用于非 CommandSender 上下文）
 *
 * @param node 语言节点路径
 * @param args 参数对
 * @return Component 对象
 */
fun getLangComponent(node: String, vararg args: Pair<String, Any>): Component? {
    val manager = getLanguageManager() ?: return null
    return manager.getComponent(node, *args)
}

/**
 * 直接获取指定插件的 Component 格式语言文本
 *
 * @param plugin 插件实例
 * @param node 语言节点路径
 * @param args 参数对
 * @return Component 对象
 */
fun getLangComponent(plugin: Plugin, node: String, vararg args: Pair<String, Any>): Component? {
    return LanguageManagerCache.getOrCreate(plugin).getComponent(node, *args)
}

/**
 * 切换语言（全局）
 *
 * @param locale 语言代码（如 "zh_CN", "en_US"）
 */
fun setLocale(locale: String) {
    val plugin = SkyCore.getInstance() ?: return
    LanguageManagerCache.getOrCreate(plugin).setLocale(locale)
}

/**
 * 切换指定插件的语言
 *
 * @param plugin 插件实例
 * @param locale 语言代码（如 "zh_CN", "en_US"）
 */
fun setLocale(plugin: Plugin, locale: String) {
    LanguageManagerCache.getOrCreate(plugin).setLocale(locale)
}
