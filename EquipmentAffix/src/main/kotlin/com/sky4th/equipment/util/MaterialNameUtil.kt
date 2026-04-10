package com.sky4th.equipment.util

import org.bukkit.Material

/**
 * 材料名称工具类
 * 用于在中文名称和Material名称之间进行映射
 */
object MaterialNameUtil {

    /**
     * 中文名称到Material名称的映射
     */
    private val chineseToEnglish = mapOf(
        "铁锭" to "IRON_INGOT",
        "铁块" to "IRON_BLOCK",
        "金锭" to "GOLD_INGOT",
        "铜锭" to "COPPER_INGOT",
        "红石" to "REDSTONE",
        "绿宝石" to "EMERALD",
        "钻石" to "DIAMOND",
        "下界合金锭" to "NETHERITE_INGOT",
        "紫水晶碎片" to "AMETHYST_SHARD",
        "青金石" to "LAPIS_LAZULI",
        "石英" to "QUARTZ",
        "木炭" to "CHARCOAL",
        "黑曜石" to "OBSIDIAN",
        "缠根泥土" to "ROOTED_DIRT",
        "仙人掌" to "CACTUS",
        "线" to "STRING",
        "箭" to "ARROW",
        "燧石" to "FLINT",
        "糖" to "SUGAR",
        "幻翼膜" to "PHANTOM_MEMBRANE",
        "鹦鹉螺壳" to "NAUTILUS_SHELL",
        "风弹" to "WIND_CHARGE",
        "腐肉" to "ROTTEN_FLESH",
        "蜘蛛眼" to "SPIDER_EYE",
        "火药" to "GUNPOWDER",
        "骨头" to "BONE",
        "冰" to "ICE",
        "羽毛" to "FEATHER",
        "兔子脚" to "RABBIT_FOOT",
        "藤蔓" to "VINE",
        "雪球" to "SNOWBALL",
        "海晶碎片" to "PRISMARINE_SHARD",
        "粘液球" to "SLIME_BALL",
        "旋风棒" to "BREEZE_ROD",
        "荧石粉" to "GLOWSTONE_DUST",
        "烈焰粉" to "BLAZE_POWDER",
        "末影珍珠" to "ENDER_PEARL",
        "末影之眼" to "ENDER_EYE",
        "潜影壳" to "SHULKER_SHELL"
    )

    /**
     * 英文名称到中文名称的映射
     */
    private val englishToChinese = chineseToEnglish.entries.associate { (chinese, english) -> english to chinese }

    /**
     * 根据中文名称获取Material
     * @param chineseName 中文名称
     * @return Material对象，如果找不到则返回null
     */
    fun getMaterialFromChinese(chineseName: String): Material? {
        val englishName = chineseToEnglish[chineseName] ?: return null
        return try {
            Material.valueOf(englishName)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /**
     * 根据Material获取中文名称
     * @param material Material对象
     * @return 中文名称，如果找不到则返回Material的name
     */
    fun getChineseName(material: Material): String {
        return englishToChinese[material.name] ?: material.name
    }

    /**
     * 根据英文名称获取中文名称
     * @param englishName 英文名称
     * @return 中文名称，如果找不到则返回英文名称
     */
    fun getChineseName(englishName: String): String {
        return englishToChinese[englishName.uppercase()] ?: englishName
    }

    /**
     * 根据中文名称获取英文名称
     * @param chineseName 中文名称
     * @return 英文名称，如果找不到则返回null
     */
    fun getEnglishName(chineseName: String): String? {
        return chineseToEnglish[chineseName]
    }
}
