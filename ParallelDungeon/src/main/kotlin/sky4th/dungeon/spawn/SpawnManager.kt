
package sky4th.dungeon.spawn

import sky4th.dungeon.config.ConfigManager
import sky4th.dungeon.config.SpawnPoint
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import java.util.*

class SpawnManager(private val configManager: ConfigManager) {

    private val playerSpawns: MutableMap<UUID, Location> = mutableMapOf()

    /**
     * 为玩家随机选择一个出生点
     * @param player 玩家
     * @param world 世界
     * @param dungeonId 地牢ID（可选，用于获取特定地牢的出生点）
     */
    fun assignRandomSpawn(player: Player, world: World, dungeonId: String? = null): Location? {
        org.bukkit.Bukkit.getLogger().info("=== SpawnManager.assignRandomSpawn 开始 ===")
        org.bukkit.Bukkit.getLogger().info("玩家: ${player.name}")
        org.bukkit.Bukkit.getLogger().info("世界: ${world.name}")
        org.bukkit.Bukkit.getLogger().info("地牢ID: $dungeonId")
        
        // 获取出生点列表，优先使用指定地牢的配置
        val dungeonConfigs = configManager.loadDungeonConfigs()
        org.bukkit.Bukkit.getLogger().info("已加载的地牢配置: ${dungeonConfigs.keys}")
        val spawnPoints = if (dungeonId != null) {
            val config = dungeonConfigs[dungeonId]
            config?.spawnPoints ?: emptyList()
        } else {
            // 如果没有指定dungeonId，使用第一个地牢的出生点
            dungeonConfigs.values.firstOrNull()?.spawnPoints ?: emptyList()
        }

        if (spawnPoints.isEmpty()) {
            org.bukkit.Bukkit.getLogger().warning("未找到出生点配置，使用世界默认出生点")
            return world.spawnLocation
        }

        org.bukkit.Bukkit.getLogger().info("可用出生点数量: ${spawnPoints.size}")
        val random = Random(System.currentTimeMillis())
        val selectedSpawn = spawnPoints[random.nextInt(spawnPoints.size)]
        org.bukkit.Bukkit.getLogger().info("选择的出生点: ${selectedSpawn.name} at (${selectedSpawn.x}, ${selectedSpawn.y}, ${selectedSpawn.z})")
        // 直接使用配置文件中的坐标（包括 y 坐标）
        val location = selectedSpawn.toLocation(world)

        playerSpawns[player.uniqueId] = location
        return location
    }

    /**
     * 获取玩家的出生点
     */
    fun getPlayerSpawn(player: Player): Location? {
        return playerSpawns[player.uniqueId]
    }

    /**
     * 传送玩家到随机出生点
     * @param player 玩家
     * @param world 世界
     * @param dungeonId 地牢ID（可选）
     */
    fun teleportToRandomSpawn(player: Player, world: World, dungeonId: String? = null): Boolean {
        val spawn = assignRandomSpawn(player, world, dungeonId) ?: return false
        // 确保区块加载
        val chunk = spawn.chunk
        if (!chunk.isLoaded) {
            chunk.load(true)
        }
        // 使用 teleport 返回值，失败就返回 false
        return player.teleport(spawn)
    }

    /**
     * 清除玩家的出生点记录
     */
    fun clearPlayerSpawn(player: Player) {
        playerSpawns.remove(player.uniqueId)
    }

    /**
     * 清除所有出生点记录
     */
    fun clearAllSpawns() {
        playerSpawns.clear()
    }
}
