package sky4th.dungeon.util

import sky4th.core.lang.sendLang
import org.bukkit.command.CommandSender
import org.bukkit.plugin.Plugin

object LanguageUtil {
    /*
    * 地牢世界前缀播报
    */
    fun CommandSender.sendLang(plugin: Plugin, node: String, vararg args: Pair<String, Any>) {
        sendLang(plugin, "prefix", node, *args)
    }

    /*
    * 系统信息前缀播报
    */
    fun CommandSender.sendLangSys(plugin: Plugin, node: String, vararg args: Pair<String, Any>) {
        sendLang(plugin, "prefix-sys", node, *args)
    }

    /*
    * 队伍信息前缀播报
    */
    fun CommandSender.sendLangTeam(plugin: Plugin, node: String, vararg args: Pair<String, Any>) {
        sendLang(plugin, "prefix-team", node, *args)
    }

    /*
    * 公告信息前缀播报
    */
    fun CommandSender.sendLangMsg(plugin: Plugin, node: String, vararg args: Pair<String, Any>) {
        sendLang(plugin, "prefix-message", node, *args)
    }

    /*
    * 广播信息前缀播报
    */
    fun CommandSender.sendLangBroad(plugin: Plugin, node: String, vararg args: Pair<String, Any>) {
        sendLang(plugin, "prefix-broadcast", node, *args)
    }
}