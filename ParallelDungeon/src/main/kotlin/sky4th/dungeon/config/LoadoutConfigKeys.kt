package sky4th.dungeon.config

/**
 * 配装相关 config.yml 路径与字段名常量。
 * 新增分类或字段时在此补充，保证 config 与代码一致。
 *
 * config 结构：
 * - loadout-shop.<category.configKey>：列表，每项 id / name / material / buy-price / sell-price / shop-lore / normal-lore
 * - actual-equipment.<loadoutId>：单件用 material/name/shop-lore/normal-lore/enchants/buy-price/sell-price；套装用 items 列表，每项同上
 */
object LoadoutConfigKeys {
    /** 配装商店根节点（其下为 equipment / melee / ranged / supplies / repair） */
    const val LOADOUT_SHOP = "common.loadout-shop"

    /** 实际装备根节点（键为 loadoutId，与 loadout-shop 的 id 对应） */
    const val ACTUAL_EQUIPMENT = "common.actual-equipment"

    // 商店单项字段（loadout-shop.*.id / name / material / buy-price / sell-price / shop-lore / normal-lore / tier）
    const val ID = "id"
    const val NAME = "name"
    const val MATERIAL = "material"
    /** 购买价格（用于商店界面和配装界面） */
    const val BUY_PRICE = "buy-price"
    /** 出售价格（用于仓库和地牢背包中的价值显示） */
    const val SELL_PRICE = "sell-price"
    /** 商店描述（用于商店界面展示） */
    const val SHOP_LORE = "shop-lore"
    /** 常规描述（用于配装、仓库、地牢背包等常规界面） */
    const val NORMAL_LORE = "normal-lore"
    /** 物品品级（用于死亡时确定搜索时间） */
    const val TIER = "tier"

    // 实际装备字段（actual-equipment.<id> 下）
    const val ITEMS = "items"
    const val ENCHANTS = "enchants"
    /** 药水箭（TIPPED_ARROW）基础药水类型，如 SLOWNESS、POISON、INSTANT_HEAL、SPEED */
    const val BASE_POTION_TYPE = "base_potion_type"
}
