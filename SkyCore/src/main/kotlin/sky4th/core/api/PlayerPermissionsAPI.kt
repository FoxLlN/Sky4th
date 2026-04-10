package sky4th.core.api

import sky4th.core.SkyCore
import org.bukkit.entity.Player
import java.util.*

/**
 * 玩家权限 API
 * 提供便捷的玩家权限管理接口
 */
object PlayerPermissionsAPI {

    /**
     * 获取权限服务
     */
    private fun getService() = SkyCore.getPlayerPermissionsService()

    /**
     * 检查玩家是否有指定权限（通过Player对象）
     * @param player 玩家对象
     * @param permissionName 权限名称
     * @return 是否有权限
     */
    fun hasPermission(player: Player, permissionName: String): Boolean {
        return getService()?.hasPermission(player.uniqueId, permissionName) ?: false
    }

    /**
     * 检查玩家是否有指定权限（通过UUID）
     * @param uuid 玩家UUID
     * @param permissionName 权限名称
     * @return 是否有权限
     */
    fun hasPermission(uuid: UUID, permissionName: String): Boolean {
        return getService()?.hasPermission(uuid, permissionName) ?: false
    }

    /**
     * 获取玩家的所有权限（通过Player对象）
     * @param player 玩家对象
     * @return 权限名称列表
     */
    fun getPlayerPermissions(player: Player): Set<String> {
        return getService()?.getPlayerPermissions(player.uniqueId) ?: emptySet()
    }

    /**
     * 获取玩家的所有权限（通过UUID）
     * @param uuid 玩家UUID
     * @return 权限名称列表
     */
    fun getPlayerPermissions(uuid: UUID): Set<String> {
        return getService()?.getPlayerPermissions(uuid) ?: emptySet()
    }

    /**
     * 给玩家添加权限（通过Player对象）
     * @param player 玩家对象
     * @param permissionName 权限名称
     * @return 是否添加成功（false表示已存在）
     */
    fun addPermission(player: Player, permissionName: String): Boolean {
        return getService()?.addPermission(player.uniqueId, permissionName)
            ?: throw IllegalStateException("PlayerPermissionsService 未初始化")
    }

    /**
     * 给玩家添加权限（通过UUID）
     * @param uuid 玩家UUID
     * @param permissionName 权限名称
     * @return 是否添加成功（false表示已存在）
     */
    fun addPermission(uuid: UUID, permissionName: String): Boolean {
        return getService()?.addPermission(uuid, permissionName)
            ?: throw IllegalStateException("PlayerPermissionsService 未初始化")
    }

    /**
     * 移除玩家的权限（通过Player对象）
     * @param player 玩家对象
     * @param permissionName 权限名称
     * @return 是否移除成功（false表示不存在）
     */
    fun removePermission(player: Player, permissionName: String): Boolean {
        return getService()?.removePermission(player.uniqueId, permissionName)
            ?: throw IllegalStateException("PlayerPermissionsService 未初始化")
    }

    /**
     * 移除玩家的权限（通过UUID）
     * @param uuid 玩家UUID
     * @param permissionName 权限名称
     * @return 是否移除成功（false表示不存在）
     */
    fun removePermission(uuid: UUID, permissionName: String): Boolean {
        return getService()?.removePermission(uuid, permissionName)
            ?: throw IllegalStateException("PlayerPermissionsService 未初始化")
    }

    /**
     * 移除玩家的所有权限（通过Player对象）
     * @param player 玩家对象
     * @return 移除的权限数量
     */
    fun removeAllPermissions(player: Player): Int {
        return getService()?.removeAllPermissions(player.uniqueId)
            ?: throw IllegalStateException("PlayerPermissionsService 未初始化")
    }

    /**
     * 移除玩家的所有权限（通过UUID）
     * @param uuid 玩家UUID
     * @return 移除的权限数量
     */
    fun removeAllPermissions(uuid: UUID): Int {
        return getService()?.removeAllPermissions(uuid)
            ?: throw IllegalStateException("PlayerPermissionsService 未初始化")
    }

    /**
     * 获取拥有指定权限的所有玩家UUID
     * @param permissionName 权限名称
     * @return 玩家UUID列表
     */
    fun getPlayersWithPermission(permissionName: String): List<UUID> {
        return getService()?.getPlayersWithPermission(permissionName) ?: emptyList()
    }

    /**
     * 从缓存中移除玩家的权限数据（通过Player对象）
     * @param player 玩家对象
     */
    fun removeFromCache(player: Player) {
        getService()?.removeFromCache(player.uniqueId)
    }

    /**
     * 从缓存中移除玩家的权限数据（通过UUID）
     * @param uuid 玩家UUID
     */
    fun removeFromCache(uuid: UUID) {
        getService()?.removeFromCache(uuid)
    }

    /**
     * 清空所有权限缓存
     */
    fun clearAllCache() {
        getService()?.clearAllCache()
    }

    /**
     * 检查 PlayerPermissionsService 是否可用
     */
    fun isAvailable(): Boolean {
        return getService() != null
    }
}
