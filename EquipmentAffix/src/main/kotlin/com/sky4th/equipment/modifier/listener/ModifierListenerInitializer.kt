package com.sky4th.equipment.modifier.listener

import org.bukkit.plugin.java.JavaPlugin

/**
 * 词条监听器初始化器
 * 自动注册modifier.listener包下的所有监听器
 */
object ModifierListenerInitializer {

    // 已注册的监听器列表
    private val registeredListeners = mutableListOf<org.bukkit.event.Listener>()

    /**
     * 注册所有词条监听器
     * @param plugin 插件实例
     */
    fun registerAll(plugin: JavaPlugin) {
        // 创建并注册所有监听器
        val listeners = listOf<org.bukkit.event.Listener>(
            JumpListener(),
            ShadowSuppressListener(),
            VulnerabilityMarkListener(),
            WeakenMarkListener(),
            GuardianListener()
        )

        listeners.forEach { listener ->
            try {
                plugin.server.pluginManager.registerEvents(listener, plugin)
                registeredListeners.add(listener)
            } catch (e: Exception) {
                plugin.logger.warning("注册词条监听器 ${listener.javaClass.simpleName} 失败: ${e.message}")
            }
        }
    }
}
