package sky4th.dungeon.region

import sky4th.dungeon.config.ConfigManager
import sky4th.dungeon.config.Region
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID

class RegionDetector(
    private val plugin: JavaPlugin,
    private val configManager: ConfigManager,
    private val onPlayerEnter: (Player) -> Unit
) : Listener {
    
    // 入口区域 - 从地牢配置中获取
    private val entryRegion: Region by lazy {
        val dungeonConfigs = configManager.loadDungeonConfigs()
        // 获取第一个地牢配置的第一个spawnPoint作为入口区域
        val firstDungeon = dungeonConfigs.values.firstOrNull()
        val firstSpawn = firstDungeon?.spawnPoints?.firstOrNull()
        if (firstSpawn != null) {
            Region(
                minX = (firstSpawn.x - 5).toInt(),
                minY = (firstSpawn.y - 3).toInt(),
                minZ = (firstSpawn.z - 5).toInt(),
                maxX = (firstSpawn.x + 5).toInt(),
                maxY = (firstSpawn.y + 3).toInt(),
                maxZ = (firstSpawn.z + 5).toInt(),
                world = firstDungeon.displayName,
                name = "entry"
            )
        } else {
            // 默认区域
            Region(0, 0, 0, 10, 10, 10, "world", "entry")
        }
    }
    private val playersInRegion: MutableSet<UUID> = mutableSetOf()
    
    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        // 优化：如果玩家没有移动方块位置，跳过检测
        val from = event.from
        val to = event.to
        if (from.blockX == to.blockX && from.blockY == to.blockY && from.blockZ == to.blockZ) {
            return
        }
        
        val player = event.player
        val location = event.to
        
        // 检查玩家是否在区域内
        val isInRegion = entryRegion.contains(location)
        val wasInRegion = playersInRegion.contains(player.uniqueId)
        
        if (isInRegion && !wasInRegion) {
            // 玩家刚进入区域
            playersInRegion.add(player.uniqueId)
            onPlayerEnter(player)
        } else if (!isInRegion && wasInRegion) {
            // 玩家离开区域
            playersInRegion.remove(player.uniqueId)
        }
    }
    
    fun isPlayerInRegion(player: Player): Boolean {
        return playersInRegion.contains(player.uniqueId)
    }
    
    fun clearRegionPlayers() {
        playersInRegion.clear()
    }
}
