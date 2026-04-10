
package sky4th.core.api

import org.bukkit.inventory.ItemStack
import sky4th.core.head.HeadCache
import sky4th.core.head.HeadUtil

/**
 * 头颅系统 API
 * 提供创建和缓存自定义头颅的功能
 */
object HeadAPI {

    /**
     * 创建自定义头颅（带缓存）
     * 如果该头颅已存在缓存，则直接返回缓存的头颅
     *
     * @param name 头颅显示名称
     * @param texture 头颅皮肤纹理（Base64编码）
     * @return 自定义头颅物品
     */
    @JvmStatic
    fun createCustomHead(name: String, texture: String): ItemStack {
        // 生成缓存键
        val cacheKey = "${name}_${texture.hashCode()}"

        // 检查缓存
        HeadCache.getCachedHead(cacheKey)?.let { return it }

        // 创建新头颅
        val head = HeadUtil.createCustomHead(name, texture)

        // 存入缓存
        HeadCache.cacheHead(cacheKey, head)

        return head
    }

    /**
     * 强制创建新的头颅（不使用缓存）
     *
     * @param name 头颅显示名称
     * @param texture 头颅皮肤纹理（Base64编码）
     * @return 新创建的自定义头颅物品
     */
    @JvmStatic
    fun createNewHead(name: String, texture: String): ItemStack {
        return HeadUtil.createCustomHead(name, texture)
    }

    /**
     * 清除所有头颅缓存
     */
    @JvmStatic
    fun clearCache() {
        HeadCache.clearCache()
    }

    /**
     * 获取当前缓存的头颅数量
     * @return 缓存中的头颅数量
     */
    @JvmStatic
    fun getCacheSize(): Int {
        return HeadCache.getCacheSize()
    }

    /**
     * 检查指定头颅是否已缓存
     * @param name 头颅名称
     * @param texture 头颅皮肤纹理（Base64编码）
     * @return 如果已缓存返回true，否则返回false
     */
    @JvmStatic
    fun isCached(name: String, texture: String): Boolean {
        val cacheKey = "${name}_${texture.hashCode()}"
        return HeadCache.isCached(cacheKey)
    }
}
