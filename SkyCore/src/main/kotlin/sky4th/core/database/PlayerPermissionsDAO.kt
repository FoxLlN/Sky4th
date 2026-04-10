package sky4th.core.database

import java.sql.*
import java.util.*

/**
 * 玩家权限数据访问对象（DAO）
 * 负责玩家权限的数据库操作
 */
class PlayerPermissionsDAO(private val databaseManager: DatabaseManager) {
    /**
     * 检查玩家是否有指定权限
     * @param uuid 玩家UUID
     * @param permissionName 权限名称
     * @return 是否有权限
     */
    fun hasPermission(uuid: UUID, permissionName: String): Boolean {
        return databaseManager.getConnection().use { conn ->
            conn.prepareStatement("SELECT 1 FROM player_permissions WHERE uuid = ? AND name = ?").use { stmt ->
                stmt.setString(1, uuid.toString())
                stmt.setString(2, permissionName)
                stmt.executeQuery().next()
            }
        }
    }

    /**
     * 获取玩家的所有权限
     * @param uuid 玩家UUID
     * @return 权限名称列表
     */
    fun getPlayerPermissions(uuid: UUID): List<String> {
        return databaseManager.getConnection().use { conn ->
            val permissions = mutableListOf<String>()
            conn.prepareStatement("SELECT name FROM player_permissions WHERE uuid = ?").use { stmt ->
                stmt.setString(1, uuid.toString())
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        permissions.add(rs.getString("name"))
                    }
                }
            }
            permissions
        }
    }

    /**
     * 给玩家添加权限
     * @param uuid 玩家UUID
     * @param permissionName 权限名称
     * @return 是否添加成功（false表示已存在）
     */
    fun addPermission(uuid: UUID, permissionName: String): Boolean {
        return try {
            databaseManager.getConnection().use { conn ->
                conn.prepareStatement("INSERT INTO player_permissions (uuid, name) VALUES (?, ?)").use { stmt ->
                    stmt.setString(1, uuid.toString())
                    stmt.setString(2, permissionName)
                    stmt.executeUpdate() > 0
                }
            }
        } catch (e: SQLException) {
            if (e.sqlState == "23000") { // 唯一键冲突
                false
            } else {
                throw e
            }
        }
    }

    /**
     * 移除玩家的权限
     * @param uuid 玩家UUID
     * @param permissionName 权限名称
     * @return 是否移除成功（false表示不存在）
     */
    fun removePermission(uuid: UUID, permissionName: String): Boolean {
        return databaseManager.getConnection().use { conn ->
            conn.prepareStatement("DELETE FROM player_permissions WHERE uuid = ? AND name = ?").use { stmt ->
                stmt.setString(1, uuid.toString())
                stmt.setString(2, permissionName)
                stmt.executeUpdate() > 0
            }
        }
    }

    /**
     * 移除玩家的所有权限
     * @param uuid 玩家UUID
     * @return 移除的权限数量
     */
    fun removeAllPermissions(uuid: UUID): Int {
        return databaseManager.getConnection().use { conn ->
            conn.prepareStatement("DELETE FROM player_permissions WHERE uuid = ?").use { stmt ->
                stmt.setString(1, uuid.toString())
                stmt.executeUpdate()
            }
        }
    }

    /**
     * 获取拥有指定权限的所有玩家UUID
     * @param permissionName 权限名称
     * @return 玩家UUID列表
     */
    fun getPlayersWithPermission(permissionName: String): List<UUID> {
        return databaseManager.getConnection().use { conn ->
            val players = mutableListOf<UUID>()
            conn.prepareStatement("SELECT uuid FROM player_permissions WHERE name = ?").use { stmt ->
                stmt.setString(1, permissionName)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        players.add(UUID.fromString(rs.getString("uuid")))
                    }
                }
            }
            players
        }
    }
}
