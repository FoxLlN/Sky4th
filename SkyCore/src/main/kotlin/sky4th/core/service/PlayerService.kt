package sky4th.core.service

import sky4th.core.database.DatabaseManager
import sky4th.core.database.PlayerDataDAO
import sky4th.core.model.PlayerData
import org.bukkit.entity.Player
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 玩家数据服务
 * 负责管理玩家数据的加载、缓存和保存
 */
class PlayerService(private val databaseManager: DatabaseManager) {

    private val dao = PlayerDataDAO(databaseManager)
    private val playerCache = ConcurrentHashMap<UUID, PlayerData>()

    /**
     * 初始化服务
     */
    fun initialize() {
        // 表的初始化现在由DatabaseManager统一管理
    }

    /**
     * 获取玩家数据（优先从缓存加载）
     */
    fun getPlayerData(uuid: UUID): PlayerData? {
        // 先从缓存获取
        playerCache[uuid]?.let { return it }

        // 从数据库加载
        val data = dao.loadPlayerData(uuid)
        if (data != null) {
            playerCache[uuid] = data
        }

        return data
    }

    /**
     * 获取玩家数据（通过 Player 对象）
     */
    fun getPlayerData(player: Player): PlayerData {
        return getPlayerData(player.uniqueId) ?: createNewPlayer(player)
    }

    /**
     * 创建新玩家数据
     */
    fun createNewPlayer(player: Player): PlayerData {
        val data = PlayerData.createNew(player.uniqueId, player.name)
        savePlayerData(data)
        playerCache[player.uniqueId] = data
        return data
    }

    /**
     * 保存玩家数据（同步到数据库）
     */
    fun savePlayerData(playerData: PlayerData) {
        try {
            dao.savePlayerData(playerData)
            playerCache[playerData.identity.uuid] = playerData
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 保存玩家数据（通过 UUID）
     */
    fun savePlayerData(uuid: UUID) {
        playerCache[uuid]?.let { savePlayerData(it) }
    }

    /**
     * 保存玩家数据（通过 Player 对象）
     */
    fun savePlayerData(player: Player) {
        savePlayerData(player.uniqueId)
    }

    /**
     * 更新玩家登录时间
     */
    fun updateLoginTime(player: Player) {
        val data = getPlayerData(player)
        val now = Instant.now()

        // 如果是首次登录
        if (data.identity.firstLogin == data.identity.lastLogin &&
            data.identity.playTime == Duration.ZERO) {
            // 首次登录，不需要更新
        } else {
            // 更新最后登录时间
            val newIdentity = data.identity.copy(lastLogin = now)
            val newData = data.copy(identity = newIdentity)
            savePlayerData(newData)
        }
    }

    /**
     * 从缓存中移除玩家数据（玩家退出时调用）
     */
    fun removeFromCache(uuid: UUID) {
        // 保存数据后再移除缓存
        savePlayerData(uuid)
        playerCache.remove(uuid)
    }

    /**
     * 保存所有缓存中的玩家数据
     */
    fun saveAll() {
        playerCache.values.forEach { savePlayerData(it) }
    }

    /**
     * 检查玩家是否存在
     */
    fun playerExists(uuid: UUID): Boolean {
        return playerCache.containsKey(uuid) || dao.playerExists(uuid)
    }
}
