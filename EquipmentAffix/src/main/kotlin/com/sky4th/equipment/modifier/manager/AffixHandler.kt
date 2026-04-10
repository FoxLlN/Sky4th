package com.sky4th.equipment.modifier.manager

import org.bukkit.entity.Player

/**
 * 词条处理器接口
 * 所有词条处理器都需要实现此接口
 */
interface AffixHandler {

    /**
     * 处理词条逻辑
     * @param player 玩家对象
     * @param data 词条数据
     */
    fun process(player: Player, data: AffixData)
}

/**
 * 词条数据基类
 * 包含内置计数器，控制执行频率
 */
abstract class AffixData {
    var tickCounter: Int = 0  // 内置计数器
    abstract val interval: Int  // 执行间隔（tick）

    /**
     * 检查是否需要执行实际逻辑
     * @return 如果需要执行返回true，否则返回false
     */
    fun shouldProcess(): Boolean {
        tickCounter++
        if (tickCounter >= interval) {
            tickCounter = 0
            return true
        }
        return false
    }
}
