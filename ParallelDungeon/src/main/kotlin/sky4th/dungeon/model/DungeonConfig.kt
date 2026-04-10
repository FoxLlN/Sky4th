package sky4th.dungeon.model

import sky4th.dungeon.config.ContainerConfig
import sky4th.dungeon.config.MonsterSpawnConfig
import sky4th.dungeon.config.Region
import sky4th.dungeon.config.SpawnPoint
import sky4th.dungeon.config.TechLevelBonus

/**
 * 地牢配置模型
 * 存储单个地牢类型的所有配置信息
 * 
 * @property id 地牢唯一标识符
 * @property displayName 地牢显示名称
 * @property templatePath 模板世界路径
 * @property cost 进入地牢所需费用
 * @property maxInstances 最大实例数量
 * @property maxPlayersPerInstance 每个实例最大玩家数
 * @property durationMinutes 地牢持续时间（分钟），0表示无限
 * @property spawnPoints 出生点列表
 * @property exitRegions 撤离区域列表
 * @property containers 容器配置列表
 * @property monsterSpawns 怪物生成点配置列表
 * @property techLevelBonuses 科技树等级加成
 */
data class DungeonConfig(
    val id: String,
    val displayName: String,
    val templatePath: String,
    val cost: Double = 0.0,
    val maxInstances: Int = 1,
    val maxPlayersPerInstance: Int = 10,
    val durationMinutes: Int = 0,
    val spawnPoints: List<SpawnPoint> = emptyList(),
    val exitRegions: List<Region> = emptyList(),
    val containers: List<ContainerConfig> = emptyList(),
    val monsterSpawns: List<MonsterSpawnConfig> = emptyList(),
    val techLevelBonuses: Map<Int, TechLevelBonus> = emptyMap()
) {
    init {
        require(id.isNotBlank()) { "Dungeon id cannot be blank" }
        require(maxInstances > 0) { "Max instances must be positive" }
        require(maxPlayersPerInstance > 0) { "Max players per instance must be positive" }
        require(cost >= 0) { "Cost must be non-negative" }
    }

    /**
     * 获取实例世界名称
     * @param instanceId 实例ID
     * @return 实例世界名称（格式：{dungeonId}_{instanceId}_world）
     */
    fun getInstanceWorldName(instanceId: String): String = "${id}_${instanceId}_world"
}
