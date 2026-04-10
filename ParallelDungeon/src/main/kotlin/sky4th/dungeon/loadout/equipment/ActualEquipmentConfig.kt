package sky4th.dungeon.loadout.equipment

/**
 * 单件实际装备配置：材质、显示名、lore、附魔、价格。
 * 商店里展示的是"商品简介"（LoadoutShopItemConfig），进入地牢时按此配置发放实际装备。
 *
 * @param material Bukkit 材质名（如 IRON_SWORD、TIPPED_ARROW）
 * @param displayName 可选显示名（支持 & 颜色符）
 * @param shopLore 商店描述（列表，支持 & 颜色符），用于商店界面展示（可选）
 * @param normalLore 常规描述（列表，支持 & 颜色符），用于地牢背包中的实际物品
 * @param enchants 附魔：键为 Bukkit 附魔名（如 PROTECTION），值为等级
 * @param buyPrice 购买价格（信用点），用于商店界面（可选）
 * @param sellPrice 出售价格（信用点），用于地牢背包中的价值显示
 * @param basePotionType 药水箭（TIPPED_ARROW）基础药水类型，如 SLOWNESS、POISON、INSTANT_HEAL、SPEED
 */
data class ActualEquipmentConfig(
    val material: String,
    val displayName: String? = null,
    val shopLore: List<String> = emptyList(),
    val normalLore: List<String> = emptyList(),
    val enchants: Map<String, Int> = emptyMap(),
    val buyPrice: Int? = null,
    val sellPrice: Int? = null,
    val basePotionType: String? = null
)
