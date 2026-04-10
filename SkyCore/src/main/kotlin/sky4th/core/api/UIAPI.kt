
package sky4th.core.api

import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import sky4th.core.ui.UIManager
import sky4th.core.ui.feature.UIFeatureHandler
import sky4th.core.ui.feature.UIFeatureManager

/**
 * UI系统 API
 * 提供UI系统的对外接口
 */
object UIAPI {

    /**
     * 从指定插件加载UI配置
     * @param plugin 插件实例
     */
    @JvmStatic
    fun loadPluginUIs(plugin: Plugin) {
        UIManager.loadPluginUIs(plugin)
    }

    /**
     * 注册插件UI处理器
     * 每个插件可以注册一个处理器来处理自己的UI逻辑
     * 
     * @param plugin 插件实例
     * @param handler UI处理器
     */
    @JvmStatic
    fun registerUIHandler(plugin: Plugin, handler: UIFeatureHandler) {
        UIFeatureManager.registerHandler(plugin.name, handler)
    }

    /**
     * 注销插件UI处理器
     * @param plugin 插件实例
     */
    @JvmStatic
    fun unregisterUIHandler(plugin: Plugin) {
        UIFeatureManager.unregisterHandler(plugin.name)
    }

    /**
     * 打开UI
     * @param player 玩家
     * @param uiId UI ID
     */
    @JvmStatic
    fun openUI(player: Player, uiId: String) {
        UIManager.openUI(player, uiId)
    }

    /**
     * 重载所有UI配置
     */
    @JvmStatic
    fun reload() {
        UIManager.reload()
    }
}
