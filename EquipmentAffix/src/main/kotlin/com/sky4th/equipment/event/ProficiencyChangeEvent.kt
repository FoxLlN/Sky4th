package com.sky4th.equipment.event

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.inventory.ItemStack

/**
 * 熟练度变化事件
 * 当装备的熟练度发生变化时触发
 */
class ProficiencyChangeEvent(
    val player: Player,
    val item: ItemStack,
    val oldProficiency: Int,
    val newProficiency: Int,
    val oldLevel: Int,
    val newLevel: Int
) : Event() {

    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        private val handlerList = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return handlerList
        }
    }

    /**
     * 检查熟练度等级是否提升
     */
    fun isLevelUp(): Boolean = newLevel > oldLevel

    /**
     * 获取等级提升的幅度
     */
    fun getLevelIncrease(): Int = newLevel - oldLevel
}
