
package sky4th.core.head

import org.bukkit.inventory.ItemStack
import java.util.concurrent.ConcurrentHashMap

/**
 * 头颅缓存管理器
 * 用于缓存已创建的头颅，避免重复创建
 */
object HeadCache {
    // 使用ConcurrentHashMap保证线程安全
    private val headCache = ConcurrentHashMap<String, ItemStack>()

    /**
     * 获取缓存中的头颅
     * @param key 头颅的唯一标识（name + texture的组合）
     * @return 缓存的头颅，如果不存在则返回null
     */
    fun getCachedHead(key: String): ItemStack? = headCache[key]

    /**
     * 将头颅存入缓存
     * @param key 头颅的唯一标识
     * @param head 要缓存的头颅物品
     */
    fun cacheHead(key: String, head: ItemStack) {
        headCache[key] = head
    }

    /**
     * 检查头颅是否已缓存
     * @param key 头颅的唯一标识
     * @return 如果已缓存返回true，否则返回false
     */
    fun isCached(key: String): Boolean = headCache.containsKey(key)

    /**
     * 清除所有缓存
     */
    fun clearCache() {
        headCache.clear()
    }

    /**
     * 移除指定的缓存
     * @param key 头颅的唯一标识
     */
    fun removeCache(key: String) {
        headCache.remove(key)
    }

    /**
     * 获取缓存大小
     * @return 当前缓存的头颅数量
     */
    fun getCacheSize(): Int = headCache.size
}
