package sky4th.dungeon.loadout

/**
 * 配装商店中单件商品的配置。
 *
 * @param id 唯一 ID（用于持久化/后续配装槽位）
 * @param name 显示名称（支持 & 颜色符）
 * @param material Bukkit 材质名（如 DIAMOND_SWORD、POTION）
 * @param buyPrice 购买价格（信用点），用于商店界面和配装界面
 * @param sellPrice 出售价格（信用点），用于仓库和地牢背包中的价值显示
 * @param category 所属分类
 * @param shopLore 商店描述（列表，支持 & 颜色符），用于商店界面展示
 * @param normalLore 常规描述（列表，支持 & 颜色符），用于配装、仓库、地牢背包等常规界面
 * @param tier 物品品级（用于死亡时确定搜索时间），默认为史诗品级
 */
data class LoadoutShopItemConfig(
    val id: String,
    val name: String,
    val material: String,
    val buyPrice: Int,
    val sellPrice: Int,
    val category: LoadoutCategory,
    val shopLore: List<String> = emptyList(),
    val normalLore: List<String> = emptyList(),
    val tier: String = "epic"
)
