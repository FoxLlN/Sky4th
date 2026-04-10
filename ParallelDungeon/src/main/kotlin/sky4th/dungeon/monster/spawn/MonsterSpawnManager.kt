package sky4th.dungeon.monster.spawn

import sky4th.dungeon.config.ConfigManager
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.plugin.java.JavaPlugin
import sky4th.dungeon.monster.core.MonsterDataKeys
import sky4th.dungeon.monster.core.MonsterMetadata
import sky4th.dungeon.monster.core.MonsterRegistry

/**
 * 怪物生成管理器
 * 负责在世界生成时根据配置生成怪物
 */
class MonsterSpawnManager(
    private val plugin: JavaPlugin,
    private val configManager: ConfigManager,
    private val playerManager: sky4th.dungeon.player.PlayerManager
) {

    /**
     * 在指定世界中生成所有配置的怪物
     * @param world 目标世界
     * @param instanceFullId 实例完整ID
     */
    fun spawnAllMonsters(world: World, instanceFullId: String) {
        // 从实例完整ID中提取dungeonId
        val dungeonId = instanceFullId.substringBefore("_")
        // 获取所有地牢配置并找到对应的配置
        val dungeonConfigs = configManager.loadDungeonConfigs()
        val dungeonConfig = dungeonConfigs[dungeonId]
        val spawnConfigs = if (dungeonConfig != null) {
            dungeonConfig.monsterSpawns
        } else {
            emptyList()
        }

        if (spawnConfigs.isEmpty()) {
            plugin.logger.info("No monster spawn configurations found.")
            return
        }
        
        // 为实例ID添加前缀，用于区分不同实例的怪物
        val instancePrefix = "${instanceFullId}_"

        var successCount = 0
        var failCount = 0

        spawnConfigs.forEach { config ->
            try {
                repeat(config.count) {
                    val entity = MonsterRegistry.spawn(config.monsterId, config.toLocation(world))
                    if (entity != null) {
                        successCount++
                        // 记录实例ID到怪物实体
                        entity.persistentDataContainer.set(
                            MonsterDataKeys.INSTANCE_ID_KEY,
                            org.bukkit.persistence.PersistentDataType.STRING,
                            instanceFullId
                        )
                        // 记录生成点到怪物实体
                        entity.persistentDataContainer.set(
                            MonsterDataKeys.SPAWN_POINT_KEY,
                            org.bukkit.persistence.PersistentDataType.STRING,
                            "${config.x},${config.y},${config.z}"
                        )
                        // 记录初始仇恨状态（无仇恨）
                        entity.persistentDataContainer.set(
                            MonsterDataKeys.HAS_AGGRO_KEY,
                            org.bukkit.persistence.PersistentDataType.BYTE,
                            0.toByte()
                        )
                    } else {
                        failCount++
                        plugin.logger.warning("Failed to spawn monster: ${config.monsterId} at ${config.id}")
                    }
                }
            } catch (e: Exception) {
                failCount++
                plugin.logger.severe("Error spawning monster at ${config.id}: ${e.message}")
            }
        }

        plugin.logger.info("Monster spawn complete: $successCount spawned, $failCount failed")
    }

    /**
     * 清除指定世界中的所有怪物
     * @param world 目标世界
     * @param instanceFullId 实例完整ID，如果为null则清除所有实例的怪物
     */
    fun clearAllMonsters(world: World, instanceFullId: String? = null) {
        val entities = world.entities.filter {
            it is org.bukkit.entity.LivingEntity && 
            MonsterMetadata.getMonsterId(it) != null && 
            (instanceFullId == null || 
                it.persistentDataContainer.get(
                    MonsterDataKeys.INSTANCE_ID_KEY,
                    org.bukkit.persistence.PersistentDataType.STRING
                ) == instanceFullId
            )
        }
        entities.forEach { it.remove() }
    }
}
