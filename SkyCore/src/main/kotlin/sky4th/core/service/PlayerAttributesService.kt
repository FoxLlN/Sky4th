package sky4th.core.service

import sky4th.core.database.DatabaseManager
import sky4th.core.database.PlayerAttributesDAO
import sky4th.core.model.PlayerAttributes
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * 玩家属性服务
 * 负责管理玩家属性的加载、缓存和保存
 */
class PlayerAttributesService(private val databaseManager: DatabaseManager) {

    private val dao = PlayerAttributesDAO(databaseManager)
    private val attributesCache = ConcurrentHashMap<UUID, PlayerAttributes>()
    private val pendingUpdates = ConcurrentHashMap<UUID, PlayerAttributes>()
    private val saveLock = Any()
    private var isFlushing = false

    /**
     * 初始化服务
     */
    fun initialize() {
        // 表的初始化现在由DatabaseManager统一管理
    }

    /**
     * 获取玩家属性（优先从缓存加载）
     * @param uuid 玩家UUID
     * @return 玩家属性
     */
    fun getAttributes(uuid: UUID): PlayerAttributes {
        // 先从缓存获取
        attributesCache[uuid]?.let { return it }

        // 从数据库加载
        val attributes = dao.getAttributes(uuid)
        attributesCache[uuid] = attributes
        return attributes
    }

    /**
     * 保存玩家属性（同步到数据库）- 用于关键操作
     */
    fun saveAttributesSync(attributes: PlayerAttributes) {
        try {
            dao.saveAttributes(attributes)
            attributesCache[attributes.uuid] = attributes
            pendingUpdates.remove(attributes.uuid)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 保存玩家属性（同步到数据库）
     */
    fun saveAttributes(attributes: PlayerAttributes) {
        try {
            dao.saveAttributes(attributes)
            attributesCache[attributes.uuid] = attributes
            pendingUpdates.remove(attributes.uuid)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 保存玩家属性（通过UUID）
     * @param uuid 玩家UUID
     */
    fun saveAttributes(uuid: UUID) {
        attributesCache[uuid]?.let { saveAttributes(it) }
    }

    /**
     * 将玩家属性加入待保存队列（异步批量保存）
     */
    fun queueSave(attributes: PlayerAttributes) {
        pendingUpdates[attributes.uuid] = attributes
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
                        chunk.forEach { attributes ->
                            dao.saveAttributes(attributes)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // 保存失败，重新加入队列
                        chunk.forEach { attributes ->
                            pendingUpdates[attributes.uuid] = attributes
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
     * 更新闪避率
     * @param uuid 玩家UUID
     * @param value 新值 (0.0-1.0)
     */
    fun updateDodge(uuid: UUID, value: Double) {
        val attributes = getAttributes(uuid).copy(dodge = value.coerceIn(0.0, 1.0))
        attributesCache[uuid] = attributes
        queueSave(attributes)
    }

    /**
     * 更新饥饿消耗倍率
     * @param uuid 玩家UUID
     * @param value 新值
     */
    fun updateHungerConsumptionMultiplier(uuid: UUID, value: Double) {
        val attributes = getAttributes(uuid).copy(hungerConsumptionMultiplier = value)
        attributesCache[uuid] = attributes
        queueSave(attributes)
    }

    /**
     * 更新经验获取加成
     * @param uuid 玩家UUID
     * @param value 新值
     */
    fun updateExpGainMultiplier(uuid: UUID, value: Double) {
        val attributes = getAttributes(uuid).copy(expGainMultiplier = value)
        attributesCache[uuid] = attributes
        queueSave(attributes)
    }

    /**
     * 更新交易折扣
     * @param uuid 玩家UUID
     * @param value 新值 (0.0-1.0)
     */
    fun updateTradeDiscount(uuid: UUID, value: Double) {
        val attributes = getAttributes(uuid).copy(tradeDiscount = value.coerceIn(0.0, 1.0))
        attributesCache[uuid] = attributes
        queueSave(attributes)
    }

    /**
     * 更新锻造成功率
     * @param uuid 玩家UUID
     * @param value 新值 (0.0-1.0)
     */
    fun updateForgingSuccessRate(uuid: UUID, value: Double) {
        val attributes = getAttributes(uuid).copy(forgingSuccessRate = value.coerceIn(0.0, 1.0))
        attributesCache[uuid] = attributes
        queueSave(attributes)
    }

    /**
     * 更新天赋列表
     * @param uuid 玩家UUID
     * @param talents 天赋列表
     */
    fun updateTalents(uuid: UUID, talents: List<String>) {
        val attributes = getAttributes(uuid).copy(talents = talents)
        attributesCache[uuid] = attributes
        // 关键操作，立即同步保存
        saveAttributesSync(attributes)
    }

    /**
     * 添加天赋
     * @param uuid 玩家UUID
     * @param talent 天赋ID
     */
    fun addTalent(uuid: UUID, talent: String) {
        val attributes = getAttributes(uuid)
        val newTalents = attributes.talents.toMutableList()
        if (!newTalents.contains(talent)) {
            newTalents.add(talent)
            val newAttributes = attributes.copy(talents = newTalents)
            attributesCache[uuid] = newAttributes
            // 关键操作，立即同步保存
            saveAttributesSync(newAttributes)
        }
    }

    /**
     * 移除天赋
     * @param uuid 玩家UUID
     * @param talent 天赋ID
     */
    fun removeTalent(uuid: UUID, talent: String) {
        val attributes = getAttributes(uuid)
        val newTalents = attributes.talents.toMutableList()
        if (newTalents.remove(talent)) {
            val newAttributes = attributes.copy(talents = newTalents)
            attributesCache[uuid] = newAttributes
            // 关键操作，立即同步保存
            saveAttributesSync(newAttributes)
        }
    }

    /**
     * 从缓存中移除玩家属性
     * @param uuid 玩家UUID
     */
    fun removeFromCache(uuid: UUID) {
        // 先保存待更新的数据
        pendingUpdates[uuid]?.let { saveAttributesSync(it) }
        // 再保存并移除缓存
        saveAttributes(uuid)
        attributesCache.remove(uuid)
    }

    /**
     * 保存所有缓存中的玩家属性
     */
    fun saveAll() {
        // 先保存所有待更新的数据
        flushPendingUpdates()
        // 再保存所有缓存中的数据
        attributesCache.values.forEach { saveAttributes(it) }
    }

    /**
     * 检查玩家属性是否存在
     * @param uuid 玩家UUID
     * @return 是否存在
     */
    fun attributesExists(uuid: UUID): Boolean {
        return attributesCache.containsKey(uuid) || dao.attributesExists(uuid)
    }
}
