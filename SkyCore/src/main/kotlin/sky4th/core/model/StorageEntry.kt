package sky4th.core.model

/**
 * 玩家仓库单格条目。
 * - loadout：配装商店购买的物品，用 id + 数量 + 耐久 表示。
 * - loot：搜打撤获得的任意物品，用序列化数据 itemData 表示。
 */
enum class StorageEntryType {
    /** 配装商店物品（由 loadout_id + count + durability 还原） */
    LOADOUT,
    /** 搜打撤等获得的任意物品（由 item_data 序列化还原） */
    LOOT
}

data class StorageEntry(
    val type: StorageEntryType,
    val count: Int,
    /** 仅 type=LOADOUT 时有效，对应 loadout-shop 的 id */
    val loadoutId: String? = null,
    /** 仅 type=LOADOUT 且为装备时有效，当前耐久 */
    val durability: Int? = null,
    /** 仅 type=LOOT 时有效，物品序列化字符串（如 Base64 NBT 或 JSON） */
    val itemData: String? = null
) {
    init {
        require(count > 0) { "count must be positive" }
        when (type) {
            StorageEntryType.LOADOUT -> require(!loadoutId.isNullOrBlank()) { "loadout entry requires loadoutId" }
            StorageEntryType.LOOT -> require(!itemData.isNullOrBlank()) { "loot entry requires itemData" }
        }
    }
}
