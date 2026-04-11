package sky4th.economy.util

import sky4th.core.lang.sendLang
import org.bukkit.command.CommandSender
import org.bukkit.plugin.Plugin

/**
 * CreditEconomy 语言工具类
 * 
 * 基于 SkyCore 的语言管理系统
 */
object LanguageUtil {
    /**
     * 发送带前缀的语言消息
     * @param plugin 插件实例
     * @param node 语言节点
     * @param args 参数对
     */
    fun CommandSender.sendLang(plugin: Plugin, node: String, vararg args: Pair<String, Any>) {
        sendLang(plugin, "prefix", node, *args)
    }
}
