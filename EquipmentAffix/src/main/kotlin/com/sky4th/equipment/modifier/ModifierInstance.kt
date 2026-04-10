
package com.sky4th.equipment.modifier

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * 词条实例类
 * 存储词条的运行时实例信息
 */
data class ModifierInstance(
    val modifier: Modifier,
    val player: Player,
    val item: ItemStack,
    val level: Int,
    val slot: String = "UNKNOWN"
) {
    /**
     * 获取词条ID
     */
    fun getAffixId(): String = modifier.getAffixId()

    /**
     * 获取优先级
     */
    fun getPriority(): Int = modifier.getPriority()
}
