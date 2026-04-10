
package sky4th.core.head

/**
 * 头颅服务
 * 管理头颅系统的初始化和清理
 */
object HeadService {
    private var initialized = false

    /**
     * 初始化头颅服务
     */
    fun initialize() {
        if (initialized) return

        initialized = true
    }

    /**
     * 检查服务是否已初始化
     * @return 如果已初始化返回true，否则返回false
     */
    fun isInitialized(): Boolean = initialized

    /**
     * 清理头颅服务
     */
    fun cleanup() {
        if (!initialized) return

        // 清理所有缓存
        HeadCache.clearCache()
        initialized = false
    }
}
