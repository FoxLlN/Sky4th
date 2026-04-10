package sky4th.dungeon.model

import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 地牢实例
 * 表示一个地牢配置的具体运行实例
 * 
 * @property instanceId 实例唯一标识符
 * @property config 地牢配置
 * @property world 实例对应的Bukkit世界对象
 * @property createdAt 创建时间戳
 */
class DungeonInstance(
    val instanceId: String,
    val config: DungeonConfig,
    val world: World
) {
    // 玩家集合（使用ConcurrentHashMap保证线程安全）
    private val players: ConcurrentHashMap.KeySetView<UUID, Boolean> = ConcurrentHashMap.newKeySet()

    // 创建时间
    val createdAt: Long = System.currentTimeMillis()

    // 玩家计数器（原子操作保证线程安全）
    private val playerCount = AtomicInteger(0)

    // ============ 容器数据管理 ============
    // 容器状态映射（容器ID -> 容器状态）
    private val containerStates: ConcurrentHashMap<String, Any> = ConcurrentHashMap()
    // 已搜索容器集合
    private val searchedContainers: ConcurrentHashMap.KeySetView<String, Boolean> = ConcurrentHashMap.newKeySet()

    // ============ 怪物头颅数据管理 ============
    // 怪物头颅状态映射（头颅ID -> 头颅状态）
    private val monsterHeadStates: ConcurrentHashMap<String, Any> = ConcurrentHashMap()

    // ============ 玩家死亡数据管理 ============
    // 死亡玩家物品映射（玩家UUID -> 物品列表）
    private val deathPlayerItems: ConcurrentHashMap<UUID, List<Pair<ItemStack, String>>> = ConcurrentHashMap()
    // 死亡玩家背包现金映射（玩家UUID -> 现金金额）
    private val deathPlayerCash: ConcurrentHashMap<UUID, Int> = ConcurrentHashMap()

    /**
     * 添加玩家到实例
     * @param player 要添加的玩家
     * @return 是否成功添加（false表示已达到最大玩家数或玩家已在实例中）
     */
    fun addPlayer(player: Player): Boolean {
        if (players.contains(player.uniqueId)) {
            return false
        }

        if (playerCount.get() >= config.maxPlayersPerInstance) {
            return false
        }

        players.add(player.uniqueId)
        playerCount.incrementAndGet()
        return true
    }

    /**
     * 从实例中移除玩家
     * @param player 要移除的玩家
     * @return 是否成功移除（false表示玩家不在实例中）
     */
    fun removePlayer(player: Player): Boolean {
        if (players.remove(player.uniqueId)) {
            playerCount.decrementAndGet()
            return true
        }
        return false
    }

    /**
     * 检查玩家是否在实例中
     * @param player 要检查的玩家
     * @return 玩家是否在实例中
     */
    fun containsPlayer(player: Player): Boolean = players.contains(player.uniqueId)

    /**
     * 获取实例中的玩家数量
     * @return 玩家数量
     */
    fun getPlayerCount(): Int = playerCount.get()

    /**
     * 获取实例中的所有玩家UUID
     * @return 玩家UUID集合
     */
    fun getPlayers(): Set<UUID> = players.toSet()

    /**
     * 检查实例是否为空
     * @return 实例是否没有玩家
     */
    fun isEmpty(): Boolean = playerCount.get() == 0

    /**
     * 检查实例是否已满
     * @return 实例是否已达到最大玩家数
     */
    fun isFull(): Boolean = playerCount.get() >= config.maxPlayersPerInstance

    /**
     * 获取实例的完整标识符
     * @return 完整标识符（格式：{dungeonId}_{instanceId}）
     */
    fun getFullId(): String = "${config.id}_$instanceId"

    /**
     * 获取实例的显示名称
     * @return 显示名称（格式：{地牢名称} #{实例号}）
     */
    fun getDisplayName(): String = "${config.displayName} #$instanceId"

    // 关闭时间，0表示未关闭
    @Volatile
    var closedAt: Long = 0
        private set

    /**
     * 检查实例是否已关闭
     * @return 是否已关闭
     */
    fun isClosed(): Boolean = closedAt > 0

    /**
     * 检查实例是否已超时
     * @return 是否超时
     */
    fun isExpired(): Boolean {
        if (config.durationMinutes <= 0) return false // 持续时间为0表示无限
        if (isClosed()) return true // 已关闭的实例视为超时

        val durationMs = config.durationMinutes * 60 * 1000L
        val elapsed = System.currentTimeMillis() - createdAt
        return elapsed >= durationMs
    }

    /**
     * 关闭实例
     */
    fun close() {
        if (!isClosed()) {
            closedAt = System.currentTimeMillis()
        }
    }

    /**
     * 重新打开实例
     */
    fun reopen() {
        closedAt = 0
    }

    /**
     * 获取剩余时间（分钟）
     * @return 剩余时间，无限返回-1
     */
    fun getRemainingMinutes(): Int {
        if (config.durationMinutes <= 0) return -1 // 无限
        if (isClosed()) return 0

        val durationMs = config.durationMinutes * 60 * 1000L
        val elapsed = System.currentTimeMillis() - createdAt
        val remainingMs = durationMs - elapsed
        return (remainingMs / 60000).toInt().coerceAtLeast(0)
    }

    /**
     * 获取剩余时间（秒）
     * @return 剩余时间，无限返回-1
     */
    fun getRemainingSeconds(): Int {
        if (config.durationMinutes <= 0) return -1 // 无限
        if (isClosed()) return 0

        val durationMs = config.durationMinutes * 60 * 1000L
        val elapsed = System.currentTimeMillis() - createdAt
        val remainingMs = durationMs - elapsed
        return (remainingMs / 1000).toInt().coerceAtLeast(0)
    }

    // ============ 容器数据管理方法 ============

    /**
     * 获取容器状态
     * @param containerId 容器ID
     * @return 容器状态，不存在返回null
     */
    fun getContainerState(containerId: String): Any? = containerStates[containerId]

    /**
     * 设置容器状态
     * @param containerId 容器ID
     * @param state 容器状态
     */
    fun setContainerState(containerId: String, state: Any) {
        containerStates[containerId] = state
    }

    /**
     * 移除容器状态
     * @param containerId 容器ID
     * @return 是否成功移除
     */
    fun removeContainerState(containerId: String): Boolean = containerStates.remove(containerId) != null

    /**
     * 检查容器是否已搜索
     * @param containerId 容器ID
     * @return 是否已搜索
     */
    fun isContainerSearched(containerId: String): Boolean = searchedContainers.contains(containerId)

    /**
     * 标记容器为已搜索
     * @param containerId 容器ID
     */
    fun markContainerSearched(containerId: String) {
        searchedContainers.add(containerId)
    }

    /**
     * 取消容器的已搜索标记
     * @param containerId 容器ID
     * @return 是否成功取消
     */
    fun unmarkContainerSearched(containerId: String): Boolean = searchedContainers.remove(containerId)

    /**
     * 清理所有容器数据
     */
    fun clearContainerData() {
        containerStates.clear()
        searchedContainers.clear()
    }

    // ============ 怪物头颅数据管理方法 ============

    /**
     * 获取怪物头颅状态
     * @param headId 头颅ID
     * @return 头颅状态，不存在返回null
     */
    fun getMonsterHeadState(headId: String): Any? = monsterHeadStates[headId]

    /**
     * 设置怪物头颅状态
     * @param headId 头颅ID
     * @param state 头颅状态
     */
    fun setMonsterHeadState(headId: String, state: Any) {
        monsterHeadStates[headId] = state
    }

    /**
     * 移除怪物头颅状态
     * @param headId 头颅ID
     * @return 是否成功移除
     */
    fun removeMonsterHeadState(headId: String): Boolean = monsterHeadStates.remove(headId) != null

    /**
     * 清理所有怪物头颅数据
     */
    fun clearMonsterHeadData() {
        monsterHeadStates.clear()
    }

    // ============ 玩家死亡数据管理方法 ============

    /**
     * 获取死亡玩家的物品
     * @param playerUuid 玩家UUID
     * @return 物品列表，不存在返回null
     */
    fun getDeathPlayerItems(playerUuid: UUID): List<Pair<ItemStack, String>>? = deathPlayerItems[playerUuid]

    /**
     * 设置死亡玩家的物品
     * @param playerUuid 玩家UUID
     * @param items 物品列表
     */
    fun setDeathPlayerItems(playerUuid: UUID, items: List<Pair<ItemStack, String>>) {
        deathPlayerItems[playerUuid] = items
    }

    /**
     * 移除死亡玩家的物品
     * @param playerUuid 玩家UUID
     * @return 是否成功移除
     */
    fun removeDeathPlayerItems(playerUuid: UUID): Boolean = deathPlayerItems.remove(playerUuid) != null

    /**
     * 获取死亡玩家的背包现金
     * @param playerUuid 玩家UUID
     * @return 现金金额，不存在返回0
     */
    fun getDeathPlayerCash(playerUuid: UUID): Int = deathPlayerCash[playerUuid] ?: 0

    /**
     * 设置死亡玩家的背包现金
     * @param playerUuid 玩家UUID
     * @param amount 现金金额
     */
    fun setDeathPlayerCash(playerUuid: UUID, amount: Int) {
        deathPlayerCash[playerUuid] = amount
    }

    /**
     * 移除死亡玩家的背包现金
     * @param playerUuid 玩家UUID
     * @return 是否成功移除
     */
    fun removeDeathPlayerCash(playerUuid: UUID): Boolean = deathPlayerCash.remove(playerUuid) != null

    /**
     * 清理所有玩家死亡数据
     */
    fun clearDeathData() {
        deathPlayerItems.clear()
        deathPlayerCash.clear()
    }

    /**
     * 清理所有实例数据
     * 包括容器数据、怪物头颅数据和玩家死亡数据
     */
    fun clearAllInstanceData() {
        clearContainerData()
        clearMonsterHeadData()
        clearDeathData()
    }
}
