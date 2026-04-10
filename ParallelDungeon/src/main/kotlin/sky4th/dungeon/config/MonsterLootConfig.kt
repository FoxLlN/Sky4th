package sky4th.dungeon.config

import sky4th.dungeon.monster.core.MonsterLevel

/**
 * 怪物掉落物配置
 *
 * @param itemId 掉落物ID（对应dungeon.loot-items中的id）
 * @param chance 掉落概率（0.0-1.0，1.0表示100%掉落）
 */
data class MonsterLootItem(
    val itemId: String,
    val chance: Double
) {
    init {
        require(chance in 0.0..1.0) { "chance must be in [0, 1]" }
        require(itemId.isNotBlank()) { "itemId must not be blank" }
    }
}

/**
 * 怪物掉落物表配置
 *
 * @param level 怪物等级
 * @param drops 掉落物列表
 */
data class MonsterLootTable(
    val level: MonsterLevel,
    val drops: List<MonsterLootItem>
)
