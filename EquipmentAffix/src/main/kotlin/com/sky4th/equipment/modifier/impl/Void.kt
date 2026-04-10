
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.inventory.ItemStack

/**
 * 虚无词条
 * 效果：挖掘/击杀不生成掉落物，只保留经验
 * 适用于：工具/武器
 * 冲突：磁力/贪婪/食腐
 */
class Void : com.sky4th.equipment.modifier.ConfiguredModifier("void") {

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(
            EntityDeathEvent::class.java,
            BlockDropItemEvent::class.java
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

            // 清除掉落物，但保留经验值
            event.drops.clear()
        }
        // 处理方块掉落物事件
        else if (event is BlockDropItemEvent) {
            if (playerRole != PlayerRole.SELF) {
                return
            }

            // 清除掉落物，但保留经验值
            event.items.clear()
        }
    }

    override fun onRemove(player: Player) {
        // 移除时无需特殊处理
    }
}
