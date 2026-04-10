package sky4th.core.api

import org.bukkit.plugin.java.JavaPlugin
import sky4th.core.SkyCore
import sky4th.core.api.LanguageAPI

/**
 * Sky4th 插件基类
 * 所有 Sky4th 插件都应继承此类
 * 
 * 提供：
 * - 自动初始化 SkyCore
 * - 便捷的 API 访问方法
 * - 统一的生命周期管理
 */
abstract class SkyPlugin : JavaPlugin() {
    
    override fun onEnable() {
        // 确保 SkyCore 已初始化
        SkyCore.init(this)
        
        // 调用子类的启用方法
        onSkyEnable()
    }
    
    override fun onDisable() {
        // 调用子类的禁用方法
        onSkyDisable()
    }
    
    /**
     * 插件启用时的回调（替代 onEnable）
     */
    abstract fun onSkyEnable()
    
    /**
     * 插件禁用时的回调（替代 onDisable）
     */
    abstract fun onSkyDisable()
    
    // ========== 便捷方法 ==========
    
    /**
     * 检查 SkyCore 是否已初始化
     */
    protected fun isCoreAPIInitialized(): Boolean {
        return SkyCore.isInitialized()
    }
    
    /**
     * 检查数据库是否可用
     */
    protected fun isDatabaseAvailable(): Boolean {
        return SkyCore.isDatabaseAvailable()
    }
    
    /**
     * 检查玩家服务是否可用
     */
    protected fun isPlayerServiceAvailable(): Boolean {
        return SkyCore.isPlayerServiceAvailable()
    }
    
    /**
     * 检查经济系统是否可用
     */
    protected fun isEconomyAvailable(): Boolean {
        return SkyCore.isEconomyAvailable()
    }

    // ========== 语言系统便捷方法 ==========

    /**
     * 获取语言管理器
     */
    protected fun getLanguageManager() = LanguageAPI.getLanguageManager(this)

    /**
     * 获取当前语言代码
     */
    protected fun getCurrentLocale(): String = LanguageAPI.getCurrentLocale(this)

    /**
     * 切换语言
     * @param locale 语言代码（如 "zh_CN", "en_US"）
     */
    protected fun setLocale(locale: String) = LanguageAPI.setLocale(this, locale)

    /**
     * 获取语言文本
     * @param node 语言节点路径
     * @param args 参数对
     * @return 语言文本
     */
    protected fun getLangText(node: String, vararg args: Pair<String, Any>): String {
        return LanguageAPI.getText(this, node, *args)
    }

    /**
     * 获取语言文本列表
     * @param node 语言节点路径
     * @param args 参数对
     * @return 语言文本列表
     */
    protected fun getLangTextList(node: String, vararg args: Pair<String, Any>): List<String> {
        return LanguageAPI.getTextList(this, node, *args)
    }

    /**
     * 重载语言文件
     */
    protected fun reloadLang() = LanguageAPI.reload(this)
}
