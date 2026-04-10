package sky4th.core.api

import sky4th.core.SkyCore
import sky4th.core.model.StorageEntry
import sky4th.core.service.StorageService
import java.util.*

/**
 * 玩家仓库对外 API（搜打撤物品存储）。
 * 委托给 SkyCore 的 StorageService，数据库不可用时返回空列表 / 写操作可抛异常。
 *
 * UI 布局：箱子 6 行×9 列（54 格），左一列、右一列为操作区，中间 7 列×6 行 = 42 格为存储。
 */
object StorageAPI {

    private fun getService(): StorageService? = SkyCore.getStorageService()

    // ---------- 仓库箱子 UI 布局（左1列+右1列=操作，中间 7 列=存储） ----------
    /** 箱子总格数 */
    const val CHEST_SIZE = 54
    /** 存储区格数（中间 7 列×6 行） */
    const val STORAGE_SLOT_COUNT = 42
    /** 每行 9 格 */
    private const val ROW_SIZE = 9
    /** 存储区每行占 7 列（列 1～7） */
    private const val STORAGE_COLS = 7

    /** 存储下标 → 箱子格子下标。storageIndex 0..41 对应中间 7 列×6 行。 */
    @JvmStatic
    fun storageIndexToUiSlot(storageIndex: Int): Int {
        require(storageIndex in 0 until STORAGE_SLOT_COUNT) { "storageIndex out of range: $storageIndex" }
        val row = storageIndex / STORAGE_COLS
        val col = storageIndex % STORAGE_COLS
        return row * ROW_SIZE + (col + 1)  // 列 0 留空，存储从列 1 开始
    }

    /** 箱子格子下标 → 存储下标；若该格不是存储区则返回 null。 */
    @JvmStatic
    fun uiSlotToStorageIndex(uiSlot: Int): Int? {
        if (uiSlot !in 0 until CHEST_SIZE) return null
        val row = uiSlot / ROW_SIZE
        val col = uiSlot % ROW_SIZE
        if (col == 0 || col == 8) return null  // 左一列、右一列为操作区
        return row * STORAGE_COLS + (col - 1)
    }

    /** 该箱子格子是否为存储区（可放仓库物品）。 */
    @JvmStatic
    fun isStorageSlot(uiSlot: Int): Boolean = uiSlotToStorageIndex(uiSlot) != null

    /** 操作区格子列表（左一列 + 右一列，共 12 格），用于放按钮等。 */
    @JvmStatic
    fun getOperationSlots(): List<Int> = listOf(
        0, 9, 18, 27, 36, 45,   // 左列
        8, 17, 26, 35, 44, 53   // 右列
    )

    fun isAvailable(): Boolean = getService() != null

    /** 获取玩家仓库（按存储槽位 0..41，空槽为 null） */
    fun getStorage(uuid: UUID, maxSlots: Int = STORAGE_SLOT_COUNT): List<StorageEntry?> {
        return getService()?.getStorage(uuid, maxSlots) ?: List(maxSlots) { null }
    }

    /** 设置某一存储槽位（slotIndex 0..41）；entry 为 null 时删除该槽 */
    fun setSlot(uuid: UUID, slotIndex: Int, entry: StorageEntry?) {
        getService()?.setSlot(uuid, slotIndex, entry)
            ?: throw IllegalStateException("仓库服务不可用，请确保 SkyCore 已初始化且数据库已连接")
    }

    /** 整仓覆盖写入（先清空再按存储槽位 0..41 写入） */
    fun setStorage(uuid: UUID, entries: List<StorageEntry?>) {
        getService()?.setStorage(uuid, entries)
            ?: throw IllegalStateException("仓库服务不可用，请确保 SkyCore 已初始化且数据库已连接")
    }

    /** 清空该玩家所有仓库槽位 */
    fun clearStorage(uuid: UUID) {
        getService()?.clearStorage(uuid)
    }
}
