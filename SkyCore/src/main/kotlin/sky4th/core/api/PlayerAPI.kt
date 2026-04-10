package sky4th.core.api

import sky4th.core.SkyCore
import sky4th.core.model.PlayerData
import sky4th.core.service.PlayerService
import org.bukkit.entity.Player
import java.util.*

/**
 * 玩家数据 API
 * 提供便捷的玩家数据访问接口
 */
object PlayerAPI {

    /**
     * 获取玩家服务
     */
    private fun getService(): PlayerService? {
        return SkyCore.getPlayerService()
    }

    /**
     * 获取玩家数据（通过 Player 对象）
     * @param player 玩家对象
     * @return 玩家数据，如果不存在则创建新数据
     */
    fun getPlayerData(player: Player): PlayerData {
        return getService()?.getPlayerData(player)
            ?: throw IllegalStateException("PlayerService 未初始化")
    }

    /**
     * 获取玩家数据（通过 UUID）
     * @param uuid 玩家 UUID
     * @return 玩家数据，如果不存在则返回 null
     */
    fun getPlayerData(uuid: UUID): PlayerData? {
        return getService()?.getPlayerData(uuid)
    }

    /**
     * 保存玩家数据
     * @param playerData 玩家数据
     */
    fun savePlayerData(playerData: PlayerData) {
        getService()?.savePlayerData(playerData)
            ?: throw IllegalStateException("PlayerService 未初始化")
    }

    /**
     * 保存玩家数据（通过 Player 对象）
     * @param player 玩家对象
     */
    fun savePlayerData(player: Player) {
        getService()?.savePlayerData(player)
            ?: throw IllegalStateException("PlayerService 未初始化")
    }

    /**
     * 保存玩家数据（通过 UUID）
     * @param uuid 玩家 UUID
     */
    fun savePlayerData(uuid: UUID) {
        getService()?.savePlayerData(uuid)
            ?: throw IllegalStateException("PlayerService 未初始化")
    }

    /**
     * 创建新玩家数据
     * @param player 玩家对象
     * @return 新创建的玩家数据
     */
    fun createNewPlayer(player: Player): PlayerData {
        return getService()?.createNewPlayer(player)
            ?: throw IllegalStateException("PlayerService 未初始化")
    }

    /**
     * 更新玩家登录时间
     * @param player 玩家对象
     */
    fun updateLoginTime(player: Player) {
        getService()?.updateLoginTime(player)
            ?: throw IllegalStateException("PlayerService 未初始化")
    }

    /**
     * 从缓存中移除玩家数据（玩家退出时调用）
     * @param uuid 玩家 UUID
     */
    fun removeFromCache(uuid: UUID) {
        getService()?.removeFromCache(uuid)
            ?: throw IllegalStateException("PlayerService 未初始化")
    }

    /**
     * 检查玩家是否存在
     * @param uuid 玩家 UUID
     * @return 是否存在
     */
    fun playerExists(uuid: UUID): Boolean {
        return getService()?.playerExists(uuid) ?: false
    }

    /**
     * 检查 PlayerService 是否可用
     */
    fun isAvailable(): Boolean {
        return getService() != null
    }
}
