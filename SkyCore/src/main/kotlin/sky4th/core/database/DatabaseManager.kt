package sky4th.core.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import java.sql.Connection
import java.sql.SQLException

/**
 * 数据库管理器 - 从 config.yml 的 database 节点读取配置，使用 HikariCP 连接 MySQL。
 */
class DatabaseManager(private val plugin: Plugin) {

    companion object {
        private const val DRIVER = "com.mysql.cj.jdbc.Driver"
        // characterEncoding 必须用 Java 支持的名称（UTF-8），不能用 MySQL 的 utf8mb4
        private const val JDBC_OPTIONS = "?useSSL=false" +
                "&serverTimezone=Asia/Shanghai" +
                "&characterEncoding=UTF-8" +
                "&allowPublicKeyRetrieval=true" +
                "&autoReconnect=true"

        private const val DEFAULT_HOST = "localhost"
        private const val DEFAULT_PORT = 3306
        private const val DEFAULT_USERNAME = "minecraft"
        private const val DEFAULT_PASSWORD = ""
        private const val DEFAULT_DATABASE = "minecraft"
        private const val DEFAULT_POOL_MIN = 2
        private const val DEFAULT_POOL_MAX = 10
        private const val DEFAULT_CONNECTION_TIMEOUT = 10000L
        private const val DEFAULT_IDLE_TIMEOUT = 300000L
        private const val DEFAULT_MAX_LIFETIME = 1800000L
    }

    @Volatile
    private var dataSource: HikariDataSource? = null

    private fun getConfig(): FileConfiguration {
        return (plugin as? JavaPlugin)?.config
            ?: throw IllegalStateException("DatabaseManager 需要 JavaPlugin 才能读取 config.yml")
    }

    /**
     * 从 config.yml 读取数据库配置并初始化连接池。
     */
    @Throws(SQLException::class)
    fun initialize() {
        if (dataSource != null) {
            plugin.logger.warning("DatabaseManager 已初始化，跳过重复初始化")
            return
        }

        val cfg = getConfig()
        val host = cfg.getString("database.host") ?: DEFAULT_HOST
        val port = cfg.getInt("database.port", DEFAULT_PORT)
        val username = cfg.getString("database.username") ?: DEFAULT_USERNAME
        val password = cfg.getString("database.password") ?: DEFAULT_PASSWORD
        val database = cfg.getString("database.database") ?: DEFAULT_DATABASE

        val jdbcUrl = "jdbc:mysql://$host:$port/$database$JDBC_OPTIONS"

        val poolSection = cfg.getConfigurationSection("pool")
        val poolMin = poolSection?.getInt("minimum-idle", DEFAULT_POOL_MIN) ?: DEFAULT_POOL_MIN
        // 根据服务器最大玩家数量动态调整连接池大小
        val maxPlayers = plugin.server.maxPlayers
        val defaultPoolMax = when {
            maxPlayers <= 50 -> 20
            maxPlayers <= 200 -> 50
            else -> 100
        }
        val poolMax = poolSection?.getInt("maximum-pool-size", defaultPoolMax) ?: defaultPoolMax
        val connTimeout = poolSection?.getLong("connection-timeout", DEFAULT_CONNECTION_TIMEOUT) ?: DEFAULT_CONNECTION_TIMEOUT
        val idleTimeout = poolSection?.getLong("idle-timeout", DEFAULT_IDLE_TIMEOUT) ?: DEFAULT_IDLE_TIMEOUT
        val maxLifetime = poolSection?.getLong("max-lifetime", DEFAULT_MAX_LIFETIME) ?: DEFAULT_MAX_LIFETIME

        try {
            Class.forName(DRIVER)
        } catch (e: ClassNotFoundException) {
            throw SQLException("未找到 MySQL 驱动，请确保 mysql-connector-java 在 classpath 中", e)
        }

        val hikariConfig = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = username
            this.password = password
            this.driverClassName = DRIVER
            minimumIdle = poolMin
            maximumPoolSize = poolMax
            connectionTimeout = connTimeout
            this.idleTimeout = idleTimeout
            this.maxLifetime = maxLifetime
            poolName = "SkyCore-MySQL"
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        }

        dataSource = HikariDataSource(hikariConfig)

        dataSource?.connection?.use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT 1").use { rs ->
                    if (!rs.next()) throw SQLException("数据库连接验证失败")
                }
            }
        }

        plugin.logger.info("MySQL 连接池已就绪: $host:$port/$database (HikariCP)")
    }

    /**
     * 从连接池获取一条连接。调用方使用完毕后应关闭（如使用 .use {}）。
     */
    @Throws(SQLException::class)
    fun getConnection(): Connection {
        val ds = dataSource
            ?: throw SQLException("DatabaseManager 未初始化或已关闭，请先调用 initialize()")
        return ds.connection
    }

    /**
     * 初始化所有数据库表
     */
    @Throws(SQLException::class)
    fun initializeTables() {
        getConnection().use { conn ->
            DatabaseSchema.initializeAllTables(conn)
        }
        plugin.logger.info("数据库表结构初始化完成")
    }

    /**
     * 当前是否已连接并可获取连接（连接池处于运行状态）。
     */
    fun isConnected(): Boolean = dataSource?.isRunning == true

    /**
     * 关闭连接池并释放资源。
     */
    fun close() {
        try {
            dataSource?.close()
        } catch (e: Exception) {
            plugin.logger.warning("关闭数据库连接池时发生异常: ${e.message}")
        } finally {
            dataSource = null
        }
        plugin.logger.info("数据库连接池已关闭")
    }
}
