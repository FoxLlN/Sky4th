package sky4th.dungeon.loadout

/**
 * 进入地牢前配装分类：装备、近战、远程、补给、维修。
 * 商店 UI 与未来地图/NPC 等入口共用此枚举。
 */
enum class LoadoutCategory(val displayKey: String, val configKey: String) {
    /** 装备（护甲等） */
    EQUIPMENT("loadout.category.equipment", "equipment"),
    /** 近战武器 */
    MELEE("loadout.category.melee", "melee"),
    /** 远程武器 */
    RANGED("loadout.category.ranged", "ranged"),
    /** 补给品 */
    SUPPLIES("loadout.category.supplies", "supplies"),
    /** 维修 */
    REPAIR("loadout.category.repair", "repair");

    companion object {
        fun fromConfigKey(key: String): LoadoutCategory? = entries.find { it.configKey.equals(key, ignoreCase = true) }
    }
}
