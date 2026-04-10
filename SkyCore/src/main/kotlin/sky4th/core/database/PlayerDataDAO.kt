package sky4th.core.database

import sky4th.core.model.*
import org.bukkit.Bukkit
import org.bukkit.Location
import java.sql.*
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * 玩家数据访问对象（DAO）
 * 负责所有玩家数据的数据库操作
 */
class PlayerDataDAO(private val databaseManager: DatabaseManager) {

    /**
     * 保存玩家数据
     */
    fun savePlayerData(playerData: PlayerData) {
        databaseManager.getConnection().use { conn ->
            conn.autoCommit = false
            try {
                // 保存身份信息
                saveIdentity(conn, playerData.identity)

                // 保存经济信息
                saveEconomy(conn, playerData.economy, playerData.identity.uuid)

                // 保存位置信息
                saveLocations(conn, playerData.locations, playerData.identity.uuid)

                conn.commit()
            } catch (e: SQLException) {
                conn.rollback()
                throw e
            }
        }
    }

    /**
     * 加载玩家数据
     */
    fun loadPlayerData(uuid: UUID): PlayerData? {
        return databaseManager.getConnection().use { conn ->
            try {
                // 加载身份信息
                val identity = loadIdentity(conn, uuid) ?: return null

                // 加载其他信息
                val economy = loadEconomy(conn, uuid) ?: PlayerEconomy()
                val locations = loadLocations(conn, uuid) ?: PlayerLocations()

                PlayerData(identity, economy, locations)
            } catch (e: SQLException) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * 检查玩家是否存在
     */
    fun playerExists(uuid: UUID): Boolean {
        return databaseManager.getConnection().use { conn ->
            conn.prepareStatement("SELECT 1 FROM player_identity WHERE uuid = ?").use { stmt ->
                stmt.setString(1, uuid.toString())
                stmt.executeQuery().next()
            }
        }
    }

    // ========== 私有辅助方法 ==========

    private fun saveIdentity(conn: Connection, identity: PlayerIdentity) {
        conn.prepareStatement("""
            INSERT INTO player_identity (uuid, username, first_login, last_login, play_time_minutes, today_play_time_minutes, current_life_play_time_minutes, last_life_start_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                username = VALUES(username),
                last_login = VALUES(last_login),
                play_time_minutes = VALUES(play_time_minutes),
                today_play_time_minutes = VALUES(today_play_time_minutes),
                current_life_play_time_minutes = VALUES(current_life_play_time_minutes),
                last_life_start_time = VALUES(last_life_start_time)
        """).use { stmt ->
            stmt.setString(1, identity.uuid.toString())
            stmt.setString(2, identity.username)
            stmt.setLong(3, identity.firstLogin.toEpochMilli())
            stmt.setLong(4, identity.lastLogin.toEpochMilli())
            stmt.setLong(5, identity.playTime.toMinutes())
            stmt.setLong(6, identity.todayPlayTime.toMinutes())
            stmt.setLong(7, identity.currentLifePlayTime.toMinutes())
            if (identity.lastLifeStartTime != null) {
                stmt.setLong(8, identity.lastLifeStartTime.toEpochMilli())
            } else {
                stmt.setNull(8, java.sql.Types.BIGINT)
            }
            stmt.executeUpdate()
        }
    }

    private fun loadIdentity(conn: Connection, uuid: UUID): PlayerIdentity? {
        conn.prepareStatement("""
            SELECT uuid, username, first_login, last_login, play_time_minutes,
                   today_play_time_minutes, current_life_play_time_minutes, last_life_start_time
            FROM player_identity WHERE uuid = ?
        """).use { stmt ->
            stmt.setString(1, uuid.toString())
            stmt.executeQuery().use { rs ->
                if (!rs.next()) return null

                val lastLifeStartTime = rs.getLong("last_life_start_time")
                return PlayerIdentity(
                    uuid = UUID.fromString(rs.getString("uuid")),
                    username = rs.getString("username"),
                    firstLogin = Instant.ofEpochMilli(rs.getLong("first_login")),
                    lastLogin = Instant.ofEpochMilli(rs.getLong("last_login")),
                    playTime = Duration.ofMinutes(rs.getLong("play_time_minutes")),
                    todayPlayTime = Duration.ofMinutes(rs.getLong("today_play_time_minutes")),
                    currentLifePlayTime = Duration.ofMinutes(rs.getLong("current_life_play_time_minutes")),
                    lastLifeStartTime = if (rs.wasNull()) null else Instant.ofEpochMilli(lastLifeStartTime)
                )
            }
        }
    }

    private fun saveEconomy(conn: Connection, economy: PlayerEconomy, uuid: UUID) {
        conn.prepareStatement("""
            INSERT INTO player_economy (uuid, credits, daily_earned, daily_spent, daily_limit, total_earned, total_spent)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                credits = VALUES(credits),
                daily_earned = VALUES(daily_earned),
                daily_spent = VALUES(daily_spent),
                daily_limit = VALUES(daily_limit),
                total_earned = VALUES(total_earned),
                total_spent = VALUES(total_spent)
        """).use { stmt ->
            stmt.setString(1, uuid.toString())
            stmt.setDouble(2, economy.credits)
            stmt.setDouble(3, economy.dailyEarned)
            stmt.setDouble(4, economy.dailySpent)
            stmt.setDouble(5, economy.dailyLimit)
            stmt.setDouble(6, economy.totalEarned)
            stmt.setDouble(7, economy.totalSpent)
            stmt.executeUpdate()
        }
    }

    private fun loadEconomy(conn: Connection, uuid: UUID): PlayerEconomy? {
        conn.prepareStatement("""
            SELECT uuid, credits, daily_earned, daily_spent, daily_limit, total_earned, total_spent
            FROM player_economy WHERE uuid = ?
        """).use { stmt ->
            stmt.setString(1, uuid.toString())
            stmt.executeQuery().use { rs ->
                if (!rs.next()) return null

                return PlayerEconomy(
                    credits = rs.getDouble("credits"),
                    dailyEarned = rs.getDouble("daily_earned"),
                    dailySpent = rs.getDouble("daily_spent"),
                    dailyLimit = rs.getDouble("daily_limit"),
                    totalEarned = rs.getDouble("total_earned"),
                    totalSpent = rs.getDouble("total_spent")
                )
            }
        }
    }

    private fun saveLocations(conn: Connection, locations: PlayerLocations, uuid: UUID) {
        conn.prepareStatement("""
            INSERT INTO player_locations (
                uuid,
                last_location_world, last_location_x, last_location_y, last_location_z, last_location_yaw, last_location_pitch,
                base_location_world, base_location_x, base_location_y, base_location_z, base_location_yaw, base_location_pitch
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                last_location_world = VALUES(last_location_world),
                last_location_x = VALUES(last_location_x),
                last_location_y = VALUES(last_location_y),
                last_location_z = VALUES(last_location_z),
                last_location_yaw = VALUES(last_location_yaw),
                last_location_pitch = VALUES(last_location_pitch),
                base_location_world = VALUES(base_location_world),
                base_location_x = VALUES(base_location_x),
                base_location_y = VALUES(base_location_y),
                base_location_z = VALUES(base_location_z),
                base_location_yaw = VALUES(base_location_yaw),
                base_location_pitch = VALUES(base_location_pitch)
        """).use { stmt ->
            stmt.setString(1, uuid.toString())

            // 最后位置
            if (locations.lastLocation != null) {
                val loc = locations.lastLocation!!
                stmt.setString(2, loc.world?.name)
                stmt.setDouble(3, loc.x)
                stmt.setDouble(4, loc.y)
                stmt.setDouble(5, loc.z)
                stmt.setFloat(6, loc.yaw)
                stmt.setFloat(7, loc.pitch)
            } else {
                stmt.setNull(2, Types.VARCHAR)
                stmt.setNull(3, Types.DOUBLE)
                stmt.setNull(4, Types.DOUBLE)
                stmt.setNull(5, Types.DOUBLE)
                stmt.setNull(6, Types.FLOAT)
                stmt.setNull(7, Types.FLOAT)
            }

            // 基地位置
            if (locations.baseLocation != null) {
                val loc = locations.baseLocation!!
                stmt.setString(8, loc.world?.name)
                stmt.setDouble(9, loc.x)
                stmt.setDouble(10, loc.y)
                stmt.setDouble(11, loc.z)
                stmt.setFloat(12, loc.yaw)
                stmt.setFloat(13, loc.pitch)
            } else {
                stmt.setNull(8, Types.VARCHAR)
                stmt.setNull(9, Types.DOUBLE)
                stmt.setNull(10, Types.DOUBLE)
                stmt.setNull(11, Types.DOUBLE)
                stmt.setNull(12, Types.FLOAT)
                stmt.setNull(13, Types.FLOAT)
            }

            stmt.executeUpdate()
        }
    }

    private fun loadLocations(conn: Connection, uuid: UUID): PlayerLocations? {
        conn.prepareStatement("""
            SELECT uuid,
                   last_location_world, last_location_x, last_location_y, last_location_z,
                   last_location_yaw, last_location_pitch,
                   base_location_world, base_location_x, base_location_y, base_location_z,
                   base_location_yaw, base_location_pitch
            FROM player_locations WHERE uuid = ?
        """).use { stmt ->
            stmt.setString(1, uuid.toString())
            stmt.executeQuery().use { rs ->
                if (!rs.next()) return null

                // 加载最后位置
                val lastLocation = if (rs.getString("last_location_world") != null) {
                    val world = Bukkit.getWorld(rs.getString("last_location_world"))
                    if (world != null) {
                        Location(
                            world,
                            rs.getDouble("last_location_x"),
                            rs.getDouble("last_location_y"),
                            rs.getDouble("last_location_z"),
                            rs.getFloat("last_location_yaw"),
                            rs.getFloat("last_location_pitch")
                        )
                    } else null
                } else null

                // 加载基地位置
                val baseLocation = if (rs.getString("base_location_world") != null) {
                    val world = Bukkit.getWorld(rs.getString("base_location_world"))
                    if (world != null) {
                        Location(
                            world,
                            rs.getDouble("base_location_x"),
                            rs.getDouble("base_location_y"),
                            rs.getDouble("base_location_z"),
                            rs.getFloat("base_location_yaw"),
                            rs.getFloat("base_location_pitch")
                        )
                    } else null
                } else null

                return PlayerLocations(
                    lastLocation = lastLocation,
                    baseLocation = baseLocation
                )
            }
        }
    }
}
