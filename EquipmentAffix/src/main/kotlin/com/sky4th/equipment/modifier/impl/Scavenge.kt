
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemStack

/**
 * 食腐词条
 * 效果：不生成掉落物和经验，每一个应该的掉落物恢复1点饱食度
 * 适用于：工具/武器
 * 冲突：磁力/贪婪/虚无
 */
class Scavenge : com.sky4th.equipment.modifier.ConfiguredModifier("scavenge") {

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(
            EntityDeathEvent::class.java,
            BlockDropItemEvent::class.java,
            BlockBreakEvent::class.java
        )

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 处理实体死亡事件
        if (event is EntityDeathEvent) {
            if (playerRole != PlayerRole.ATTACKER) {
                return
            }

            // 获取掉落物列表
            val drops = event.drops
            val xpAmount = event.droppedExp

            // 计算应该恢复的饱食度
            // 每个掉落物恢复1点饱食度
            val saturationRestore = drops.size

            // 清除掉落物
            drops.clear()

            // 清除经验值
            event.droppedExp = 0

            // 恢复玩家饱食度
            restoreFood(player, saturationRestore)
        }
        // 处理方块掉落物事件
        else if (event is BlockDropItemEvent) {
            if (playerRole != PlayerRole.SELF) {
                return
            }

            // 获取掉落物列表
            val drops = event.items

            // 计算应该恢复的饱食度
            // 每个掉落物恢复1点饱食度
            val saturationRestore = drops.size

            // 清除掉落物
            drops.clear()

            // 恢复玩家饱食度
            restoreFood(player, saturationRestore)
        }
        // 处理方块破坏事件（处理经验值）
        else if (event is BlockBreakEvent) {
            if (playerRole != PlayerRole.SELF) {
                return
            }

            // 获取经验值
            val xpAmount = event.expToDrop

            // 清除经验值
            event.expToDrop = 0
        }
    }

    /**
     * 恢复玩家饱食度
     * @param player 玩家
     * @param saturationRestore 要恢复的饱食度值
     */
    private fun restoreFood(player: Player, saturationRestore: Int) {
        val foodLevel = player.foodLevel
        val saturation = player.saturation
        val maxFoodLevel = 20

        // 恢复饱食度（不超过上限）
        val newFoodLevel = (foodLevel + saturationRestore).coerceAtMost(maxFoodLevel)
        player.foodLevel = newFoodLevel
    }

    override fun onRemove(player: Player) {
        // 移除时无需特殊处理
    }
}
