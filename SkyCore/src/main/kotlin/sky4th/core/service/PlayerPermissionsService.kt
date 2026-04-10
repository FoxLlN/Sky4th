package sky4th.core.service

import sky4th.core.database.DatabaseManager
import sky4th.core.database.PlayerPermissionsDAO
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 玩家权限服务
 * 负责管理玩家权限的加载、缓存和保存
 */
class PlayerPermissionsService(private val databaseManager: DatabaseManager) {

    private val dao = PlayerPermissionsDAO(databaseManager)

    // 缓存结构：Map<UUID, Set<String>>，存储玩家的权限集合
    private val permissionsCache = ConcurrentHashMap<UUID, Set<String>>()

    // 缓存标志：标记玩家的权限是否已加载
    private val cacheLoadedFlags = ConcurrentHashMap<UUID, Boolean>()

    /**
     * 初始化服务
     */
    fun initialize() {
        // 表的初始化现在由DatabaseManager统一管理
    }

    /**
     * 检查玩家是否有指定权限
     * @param uuid 玩家UUID
     * @param permissionName 权限名称
     * @return 是否有权限
     */
    fun hasPermission(uuid: UUID, permissionName: String): Boolean {
        // 确保权限已加载到缓存
        ensurePermissionsLoaded(uuid)

        // 从缓存中检查
        return permissionsCache[uuid]?.contains(permissionName) ?: false
    }

    /**
     * 获取玩家的所有权限
     * @param uuid 玩家UUID
     * @return 权限名称列表
     */
    fun getPlayerPermissions(uuid: UUID): Set<String> {
        // 确保权限已加载到缓存
        ensurePermissionsLoaded(uuid)

        return permissionsCache[uuid] ?: emptySet()
    }

    /**
     * 给玩家添加权限
     * @param uuid 玩家UUID
     * @param permissionName 权限名称
     * @return 是否添加成功（false表示已存在）
     */
    fun addPermission(uuid: UUID, permissionName: String): Boolean {
        // 先从数据库添加
        val success = dao.addPermission(uuid, permissionName)

        if (success) {
            // 更新缓存
            updateCacheAfterAdd(uuid, permissionName)
        }

        return success
    }

    /**
     * 移除玩家的权限
     * @param uuid 玩家UUID
     * @param permissionName 权限名称
     * @return 是否移除成功（false表示不存在）
     */
    fun removePermission(uuid: UUID, permissionName: String): Boolean {
        // 先从数据库移除
        val success = dao.removePermission(uuid, permissionName)

        if (success) {
            // 更新缓存
            updateCacheAfterRemove(uuid, permissionName)
        }

        return success
    }

    /**
     * 移除玩家的所有权限
     * @param uuid 玩家UUID
     * @return 移除的权限数量
     */
    fun removeAllPermissions(uuid: UUID): Int {
        // 先从数据库移除
        val count = dao.removeAllPermissions(uuid)

        if (count > 0) {
            // 清空缓存
            permissionsCache[uuid] = emptySet()
        }

        return count
    }

    /**
     * 获取拥有指定权限的所有玩家UUID
     * @param permissionName 权限名称
     * @return 玩家UUID列表
     */
    fun getPlayersWithPermission(permissionName: String): List<UUID> {
        // 此操作不使用缓存，直接查询数据库
        return dao.getPlayersWithPermission(permissionName)
    }

    /**
     * 从缓存中移除玩家的权限数据（玩家退出时调用）
     * @param uuid 玩家UUID
     */
    fun removeFromCache(uuid: UUID) {
        permissionsCache.remove(uuid)
        cacheLoadedFlags.remove(uuid)
    }

    /**
     * 清空所有缓存
     */
    fun clearAllCache() {
        permissionsCache.clear()
        cacheLoadedFlags.clear()
    }

    /**
     * 确保玩家的权限已加载到缓存
     * @param uuid 玩家UUID
     */
    private fun ensurePermissionsLoaded(uuid: UUID) {
        if (cacheLoadedFlags[uuid] != true) {
            // 从数据库加载权限
            val permissions = dao.getPlayerPermissions(uuid).toSet()
            permissionsCache[uuid] = permissions
            cacheLoadedFlags[uuid] = true
        }
    }

    /**
     * 添加权限后更新缓存
     * @param uuid 玩家UUID
     * @param permissionName 权限名称
     */
    private fun updateCacheAfterAdd(uuid: UUID, permissionName: String) {
        val current = permissionsCache[uuid] ?: emptySet()
        permissionsCache[uuid] = current + permissionName
    }

    /**
     * 移除权限后更新缓存
     * @param uuid 玩家UUID
     * @param permissionName 权限名称
     */
    private fun updateCacheAfterRemove(uuid: UUID, permissionName: String) {
        val current = permissionsCache[uuid] ?: emptySet()
        permissionsCache[uuid] = current - permissionName
    }
}
