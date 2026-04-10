package com.sky4th.equipment.util

import sky4th.core.api.LanguageAPI
import sky4th.core.lang.sendLang
import org.bukkit.command.CommandSender
import org.bukkit.plugin.Plugin
import net.kyori.adventure.text.Component

/**
 * EquipmentAffix 语言工具类
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


    /**
     * 去除Component的斜体样式
     * @param component 原始Component
     * @return 去除斜体后的Component
     */
    fun removeItalic(component: Component): Component {
        return component.decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)
    }

    /**
     * 获取语言组件并自动去除斜体
     * @param plugin 插件实例
     * @param node 语言节点
     * @param args 参数对
     * @return 去除斜体后的Component
     */
    fun getComponentNoItalic(plugin: Plugin, node: String, vararg args: Pair<String, Any>): Component {
        return removeItalic(LanguageAPI.getComponent(plugin, node, *args))
    }
}
