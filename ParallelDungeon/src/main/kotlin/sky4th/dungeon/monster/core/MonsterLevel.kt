package sky4th.dungeon.monster.core

/**
 * 怪物等级（用于职业/装备等后续查找与加成）。
 *
 * 对应你的表格中的"怪物等级"列：
 * - STANDARD: 标准
 * - ENHANCED: 强化
 * - ELITE: 精英
 * - LEGENDARY: 史诗
 */
enum class MonsterLevel {
    STANDARD,
    ENHANCED,
    ELITE,
    LEGENDARY;

    companion object {
        fun fromDisplayName(name: String): MonsterLevel? =
            when (name.trim()) {
                "标准" -> STANDARD
                "强化" -> ENHANCED
                "精英" -> ELITE
                "史诗" -> LEGENDARY
                else -> null
            }
    }
}
