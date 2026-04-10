package sky4th.core.service

import sky4th.core.database.DatabaseManager
import sky4th.core.database.PlayerDataDAO
import sky4th.core.model.PlayerData
import org.bukkit.entity.Player
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * 玩家数据服务
 * 负责管理玩家数据的加载、缓存和保存
 */
class PlayerService(private val databaseManager: DatabaseManager) {

    private val dao = PlayerDataDAO(databaseManager)
    private val playerCache = ConcurrentHashMap<UUID, PlayerData>()
    private val pendingUpdates = ConcurrentHashMap<UUID, PlayerData>()
    private val saveLock = Any()
    private var isFlushing = false

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
     * 保存玩家数据（同步到数据库）- 用于关键操作
     */
    fun savePlayerDataSync(playerData: PlayerData) {
        try {
            dao.savePlayerData(playerData)
            playerCache[playerData.identity.uuid] = playerData
            pendingUpdates.remove(playerData.identity.uuid)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 保存玩家数据（同步到数据库）
     */
    fun savePlayerData(playerData: PlayerData) {
        try {
            dao.savePlayerData(playerData)
            playerCache[playerData.identity.uuid] = playerData
            pendingUpdates.remove(playerData.identity.uuid)
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
     * 将玩家数据加入待保存队列（异步批量保存）
     */
    fun queueSave(playerData: PlayerData) {
        pendingUpdates[playerData.identity.uuid] = playerData
    }

    /**
     * 批量保存待更新的数据（异步）
     * 使用线程池异步执行，避免阻塞主线程
     */
    fun flushPendingUpdates() {
        synchronized(saveLock) {
            if (isFlushing || pendingUpdates.isEmpty()) return
            isFlushing = true
        }

        val toSave = pendingUpdates.values.toList()
        pendingUpdates.clear()

        // 使用线程池异步执行
        CompletableFuture.runAsync {
            try {
                // 分批保存，每批100个
                toSave.chunked(100).forEach { chunk ->
                    try {
                        chunk.forEach { data ->
                            dao.savePlayerData(data)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // 保存失败，重新加入队列
                        chunk.forEach { data ->
                            pendingUpdates[data.identity.uuid] = data
                        }
                    }
                }
            } finally {
                synchronized(saveLock) {
                    isFlushing = false
                }
            }
        }
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
     * 更新玩家游戏时长（每秒调用一次）
     */
    fun updatePlayTime(player: Player) {
        val data = getPlayerData(player)
        val now = Instant.now()

        // 检查是否需要重置今日时长（跨天）
        val lastLoginDate = data.identity.lastLogin.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        val currentDate = now.atZone(java.time.ZoneId.systemDefault()).toLocalDate()

        val newIdentity = if (lastLoginDate != currentDate) {
            // 跨天了，重置今日时长
            data.identity.copy(
                todayPlayTime = Duration.ZERO,
                currentLifePlayTime = Duration.ZERO,
                lastLifeStartTime = now
            )
        } else {
            // 同一天，累加时长（每秒加1秒）
            data.identity.copy(
                playTime = data.identity.playTime.plusSeconds(1),
                todayPlayTime = data.identity.todayPlayTime.plusSeconds(1),
                currentLifePlayTime = data.identity.currentLifePlayTime.plusSeconds(1)
            )
        }

        val newData = data.copy(identity = newIdentity)
        playerCache[player.uniqueId] = newData
        
        // 跨天时立即同步保存，否则加入待保存队列
        if (lastLoginDate != currentDate) {
            savePlayerDataSync(newData)
        } else {
            queueSave(newData)
        }
    }

    /**
     * 重置玩家单命时长（玩家死亡时调用）
     */
    fun resetLifePlayTime(player: Player) {
        val data = getPlayerData(player)
        val now = Instant.now()

        val newIdentity = data.identity.copy(
            currentLifePlayTime = Duration.ZERO,
            lastLifeStartTime = now
        )

        val newData = data.copy(identity = newIdentity)
        playerCache[player.uniqueId] = newData
        // 关键操作，立即同步保存
        savePlayerDataSync(newData)
    }

    /**
     * 从缓存中移除玩家数据（玩家退出时调用）
     */
    fun removeFromCache(uuid: UUID) {
        // 先保存待更新的数据
        pendingUpdates[uuid]?.let { savePlayerDataSync(it) }
        // 再保存并移除缓存
        savePlayerData(uuid)
        playerCache.remove(uuid)
    }

    /**
     * 保存所有缓存中的玩家数据
     */
    fun saveAll() {
        // 先保存所有待更新的数据
        flushPendingUpdates()
        // 再保存所有缓存中的数据
        playerCache.values.forEach { savePlayerData(it) }
    }

    /**
     * 检查玩家是否存在
     */
    fun playerExists(uuid: UUID): Boolean {
        return playerCache.containsKey(uuid) || dao.playerExists(uuid)
    }
}
