package sky4th.dungeon.loadout.storage

/**
 * 箭矢常量类
 * 统一管理所有箭矢的ID和类型
 */
object ArrowConstants {

    /** 普通箭矢ID */
    const val NORMAL_ARROW = "normal_arrow"

    /** 爆炸箭ID */
    const val TNT_ARROW = "baozha_jian"

    /** 特种弓的箭矢ID列表 */
    val SPECIAL_BOW_ARROW_IDS = setOf(
        "guangling_jian",
        "huanman_jian",
        "zhongdu_jian",
        "zhiliao_jian",
        "xunjie_jian"
    )

    /** 判断是否为普通箭矢 */
    fun isNormalArrow(entry: sky4th.core.model.StorageEntry): Boolean {
        return entry.type == sky4th.core.model.StorageEntryType.LOADOUT && entry.loadoutId == NORMAL_ARROW
    }

    /** 判断是否为爆炸箭 */
    fun isTntArrow(entry: sky4th.core.model.StorageEntry): Boolean {
        return entry.type == sky4th.core.model.StorageEntryType.LOADOUT && entry.loadoutId == TNT_ARROW
    }

    /** 判断是否为特种弓的箭矢 */
    fun isSpecialBowArrow(entry: sky4th.core.model.StorageEntry): Boolean {
        return entry.type == sky4th.core.model.StorageEntryType.LOADOUT && 
               entry.loadoutId != null && 
               entry.loadoutId in SPECIAL_BOW_ARROW_IDS
    }

    /** 判断是否为任意箭矢 */
    fun isAnyArrow(entry: sky4th.core.model.StorageEntry): Boolean {
        return isNormalArrow(entry) || isTntArrow(entry) || isSpecialBowArrow(entry)
    }
}
