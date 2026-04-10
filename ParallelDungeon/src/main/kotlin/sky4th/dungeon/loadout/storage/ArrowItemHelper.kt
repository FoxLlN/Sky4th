package sky4th.dungeon.loadout.storage

import sky4th.core.model.StorageEntry

/**
 * 普通箭矢助手类
 * 用于判断存储条目是否为普通箭矢
 * 
 * 注意：普通箭矢现在统一使用LOADOUT类型，loadoutId为"normal_arrow"
 * 与其他箭矢（爆炸箭、特种箭）保持一致的存储和处理逻辑
 */
object ArrowItemHelper {

    /**
     * 判断存储条目是否为普通箭矢
     *
     * @param entry 存储条目
     * @return 如果是普通箭矢则返回true，否则返回false
     */
    fun isNormalArrow(entry: StorageEntry): Boolean {
        return entry.type == sky4th.core.model.StorageEntryType.LOADOUT && 
               entry.loadoutId == ArrowConstants.NORMAL_ARROW
    }
}
