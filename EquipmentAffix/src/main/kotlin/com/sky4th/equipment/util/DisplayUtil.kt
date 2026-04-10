package com.sky4th.equipment.util

import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment
import org.bukkit.enchantments.Enchantment
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * 显示工具类
 * 用于处理装备显示相关的工具方法
 */
object DisplayUtil {

    /**
     * 获取熟练度等级对应的语言键
     */
    fun getProficiencyLevelKey(level: Int): String {
        return when (level) {
            0 -> "novice"
            1 -> "beginner"
            2 -> "competent"
            3 -> "proficient"
            4 -> "expert"
            5 -> "master"
            else -> "novice"
        }
    }

    /**
     * 将数字转换为罗马数字
     */
    fun toRoman(num: Int): String {
        if (num <= 0 || num > 10) return num.toString()
        return when (num) {
            1 -> ""
            2 -> "II"
            3 -> "III"
            4 -> "IV"
            5 -> "V"
            6 -> "VI"
            7 -> "VII"
            8 -> "VIII"
            9 -> "IX"
            10 -> "X"
            else -> num.toString()
        }
    }

    /**
     * 获取附魔的中文名称
     */

    fun getEnchantmentName(enchantment: Enchantment): String {
        val aiyatsbusEnch = enchantment as? AiyatsbusEnchantment
        val legacyName = aiyatsbusEnch?.rarity?.displayName(aiyatsbusEnch.basicData.name)
            ?: enchantment.key.key.replace('_', ' ')
        return legacyName
    }

    /**
    * 获取附魔的描述（通用描述，不代入具体等级）
     */
    fun getEnchantmentDescription(enchantment: Enchantment, level: Int): String {
        val aiyatsbusEnch = enchantment as? AiyatsbusEnchantment
        if (aiyatsbusEnch == null) {
            // 如果不是AiyatsbusEnchantment，返回默认描述
            return "§7"
        }
        // 获取详细描述
        val descriptionTemplate = aiyatsbusEnch.displayer.specificDescription
        val holders = aiyatsbusEnch.displayer.holders(level, null, null)
        val description = holders["description"] as String
        //println("占位符值: $holders")

        // 获得简单描述
        // "§7" + aiyatsbusEnch.displayer.generalDescription
        return ("§7" + description)
    }
}
