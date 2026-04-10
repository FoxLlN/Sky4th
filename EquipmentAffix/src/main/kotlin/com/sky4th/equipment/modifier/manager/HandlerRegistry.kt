package com.sky4th.equipment.modifier.manager

/**
 * 词条处理器注册表
 * 负责手动注册词条处理器
 */
object HandlerRegistry {

    // 已注册的处理器
    private val handlers = mutableMapOf<String, AffixHandler>()

    /**
     * 注册词条处理器
     * @param affixId 词条ID
     * @param handler 处理器实例
     */
    fun register(affixId: String, handler: AffixHandler) {
        handlers[affixId] = handler
    }

    /**
     * 获取指定词条的处理器
     * @param affixId 词条ID
     * @return 处理器实例，如果不存在则返回null
     */
    fun getHandler(affixId: String): AffixHandler? {
        return handlers[affixId]
    }

    /**
     * 清除所有已注册的处理器
     */
    fun clear() {
        handlers.clear()
    }
}
