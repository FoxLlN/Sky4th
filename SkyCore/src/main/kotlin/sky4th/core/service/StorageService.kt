package sky4th.core.service

import sky4th.core.database.DatabaseManager
import sky4th.core.database.PlayerStorageDAO
import sky4th.core.model.StorageEntry
import java.util.*

/**
 * 玩家仓库服务：读写数据库中的 player_storage 表。
 * 用于存储配装商店物品（id+数量+耐久）与搜打撤获得的物品（序列化）。
 * UI 为箱子 54 格，左/右各一列为操作区，中间 42 格为存储（与 StorageAPI 布局一致）。
 */
class StorageService(private val databaseManager: DatabaseManager) {

    private val dao = PlayerStorageDAO(databaseManager)

    /** 默认仓库槽位数（箱子中间 7 列×6 行 = 42） */
    var defaultMaxSlots: Int = 42
        private set

    /**
     * 获取玩家仓库（按槽位，空槽为 null）。
     */
    fun getStorage(uuid: UUID, maxSlots: Int = defaultMaxSlots): List<StorageEntry?> {
        return dao.getStorage(uuid, maxSlots)
    }

    /**
     * 设置某一槽位；entry 为 null 时删除该槽。
     */
    fun setSlot(uuid: UUID, slotIndex: Int, entry: StorageEntry?) {
        dao.setSlot(uuid, slotIndex, entry)
    }

    /**
     * 整仓覆盖写入（先清空该玩家再按槽位写入）。
     */
    fun setStorage(uuid: UUID, entries: List<StorageEntry?>) {
        dao.setStorage(uuid, entries)
    }

    /**
     * 清空该玩家所有仓库槽位。
     */
    fun clearStorage(uuid: UUID) {
        dao.clearPlayer(uuid)
    }
}
