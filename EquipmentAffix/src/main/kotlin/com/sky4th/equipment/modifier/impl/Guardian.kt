
package com.sky4th.equipment.modifier.impl

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 守护词条
 * 效果：周围队友受到伤害时，为其承担一定伤害
 * 
 * 1级：周围3格内的队友受到伤害时，为其承担15%的伤害
 * 2级：周围4格内的队友受到伤害时，为其承担18%的伤害
 * 3级：周围5格内的队友受到伤害时，为其承担21%的伤害
 * 
 * 适用装备：重型护甲
 */
class Guardian : com.sky4th.equipment.modifier.ConfiguredModifier("guardian") {

    companion object {
        // 每级配置：(守护范围, 承担伤害比例)
        private val CONFIG = arrayOf(
            3.0 to 0.15,  // 3格，15%
            4.0 to 0.18,  // 4格，18%
            5.0 to 0.21   // 5格，21%
        )

        // 守护者列表：UUID -> (Player, level)
        private val guardians = ConcurrentHashMap<UUID, Pair<Player, Int>>()

        /**
         * 获取所有守护者
         * @return 守护者列表
         */
        fun getGuardians(): Map<UUID, Pair<Player, Int>> {
            return guardians.toMap()
        }

        /**
         * 获取指定等级的配置
         * @param level 词条等级
         * @return (守护范围, 承担伤害比例)，如果等级无效返回null
         */
        fun getConfig(level: Int): Pair<Double, Double>? {
            return CONFIG.getOrNull(level - 1)
        }
    }

    override fun getEventTypes(): List<Class<out Event>> = emptyList()

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: com.sky4th.equipment.modifier.config.PlayerRole
    ) {
        // 守护词条不在这里处理，由GuardianListener处理
    }

    override fun onInit(player: Player, item: ItemStack, level: Int) {
        // 将玩家加入守护列表
        guardians[player.uniqueId] = Pair(player, level)
    }

    override fun onRemove(player: Player) {
        // 将玩家从守护列表中移除
        guardians.remove(player.uniqueId)
    }
}
