
package sky4th.dungeon.player

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import sky4th.dungeon.model.DungeonInstance
import java.io.File
import java.util.*

/**
 * 地牢数据存储
 * 负责处理玩家在不同地牢实例中的数据持久化
 *
 * @property plugin 插件实例
 */
class DungeonDataStorage(private val plugin: JavaPlugin) {
    private val dataFolder: File = File(plugin.dataFolder, "dungeon_data")
    private val playerDataCache: MutableMap<UUID, PlayerDungeonData> = mutableMapOf()

    init {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }
    }

    /**
     * 获取玩家地牢数据
     * @param playerUuid 玩家UUID
     * @return 玩家地牢数据
     */
    fun getPlayerData(playerUuid: UUID): PlayerDungeonData {
        return playerDataCache.getOrPut(playerUuid) {
            loadPlayerData(playerUuid)
        }
    }

    /**
     * 保存玩家地牢数据
     * @param playerUuid 玩家UUID
     */
    fun savePlayerData(playerUuid: UUID) {
        val data = playerDataCache[playerUuid] ?: return
        val file = getPlayerDataFile(playerUuid)
        val config = YamlConfiguration()

        config.set("current_dungeon", data.currentDungeonId)
        config.set("current_instance", data.currentInstanceId)

        config.save(file)
    }

    /**
     * 从文件加载玩家数据
     * @param playerUuid 玩家UUID
     * @return 玩家地牢数据
     */
    private fun loadPlayerData(playerUuid: UUID): PlayerDungeonData {
        val file = getPlayerDataFile(playerUuid)
        if (!file.exists()) {
            return PlayerDungeonData()
        }

        val config = YamlConfiguration.loadConfiguration(file)
        return PlayerDungeonData(
            currentDungeonId = config.getString("current_dungeon"),
            currentInstanceId = config.getString("current_instance")
        )
    }

    /**
     * 设置玩家当前所在的地牢实例
     * @param playerUuid 玩家UUID
     * @param instance 地牢实例
     */
    fun setPlayerCurrentInstance(playerUuid: UUID, instance: DungeonInstance) {
        val data = getPlayerData(playerUuid)
        data.currentDungeonId = instance.config.id
        data.currentInstanceId = instance.instanceId
        savePlayerData(playerUuid)
    }

    /**
     * 清除玩家的当前地牢实例记录
     * @param playerUuid 玩家UUID
     */
    fun clearPlayerCurrentInstance(playerUuid: UUID) {
        val data = getPlayerData(playerUuid)
        data.currentDungeonId = null
        data.currentInstanceId = null
        savePlayerData(playerUuid)
    }

    /**
     * 获取玩家数据文件
     * @param playerUuid 玩家UUID
     * @return 数据文件
     */
    private fun getPlayerDataFile(playerUuid: UUID): File {
        return File(dataFolder, "$playerUuid.yml")
    }

    /**
     * 清除缓存中的玩家数据
     * @param playerUuid 玩家UUID
     */
    fun clearCache(playerUuid: UUID) {
        playerDataCache.remove(playerUuid)
    }

    /**
     * 清除所有缓存
     */
    fun clearAllCache() {
        playerDataCache.clear()
    }

    /**
     * 清理指定实例中的数据存储
     * @param instanceFullId 实例完整ID
     */
    fun clearForInstance(instanceFullId: String) {
        // 找出所有在该实例中的玩家
        val playersToRemove = playerDataCache.filter { (_, data) ->
            val fullId = "${data.currentDungeonId}_${data.currentInstanceId}"
            fullId == instanceFullId
        }.keys

        // 清理这些玩家的数据
        playersToRemove.forEach { playerUuid ->
            clearPlayerCurrentInstance(playerUuid)
            clearCache(playerUuid)
        }
    }
}

/**
 * 玩家地牢数据
 * 存储玩家在地牢系统中的相关信息
 *
 * @property currentDungeonId 当前所在的地牢ID
 * @property currentInstanceId 当前所在的实例ID
 */
data class PlayerDungeonData(
    var currentDungeonId: String? = null,
    var currentInstanceId: String? = null
)
