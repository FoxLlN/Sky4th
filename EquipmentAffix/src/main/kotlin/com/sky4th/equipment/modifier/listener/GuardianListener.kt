
package com.sky4th.equipment.modifier.listener

import com.sky4th.equipment.modifier.impl.Guardian
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent

/**
 * 守护监听器
 * 监听玩家受伤事件，让守护者承担伤害
 */
class GuardianListener : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityDamage(event: EntityDamageEvent) {
        // 只处理玩家受伤
        val victim = event.entity
        if (victim !is org.bukkit.entity.Player) {
            return
        }

        // 获取守护者列表
        val guardians = Guardian.getGuardians()
        if (guardians.isEmpty()) {
            return
        }

        val team2 = victim.scoreboard.getEntryTeam(victim.name)
        if (team2 == null) {
            return
        }

        // 找出受伤者周围所有可能的守护者（最大范围5格）
        val nearbyPlayers = victim.world.getNearbyEntities(victim.location, 5.0, 5.0, 5.0)
            .filterIsInstance<org.bukkit.entity.Player>()
            .filter { it != victim } // 排除自己

        // 遍历周围的守护者
        for (guardian in nearbyPlayers) {
            // 检查是否是守护者
            val (player, level) = guardians[guardian.uniqueId] ?: continue

            // 检查守护者是否有效
            if (!guardian.isValid || guardian.world != victim.world) {
                continue
            }

            // 获取两个玩家所在的队伍对象
            val team1 = guardian.scoreboard.getEntryTeam(guardian.name)

            // 判断是否同队：两者都不为 null 且是同一个队伍对象
            if (team1 == null || team1 != team2) {
                continue
            }

            // 获取当前等级的配置
            val (range, damageRatio) = Guardian.getConfig(level) ?: continue

            // 检查距离是否在守护范围内
            val distance = guardian.location.distance(victim.location)
            if (distance > range) {
                continue
            }

            // 计算守护者承担的伤害
            val originalDamage = event.damage
            val guardianDamage = originalDamage * damageRatio

            // 减少受伤者受到的伤害
            event.damage = originalDamage - guardianDamage

            // 对守护者造成伤害
            guardian.damage(guardianDamage)

            // 找到一个守护者后结束
            break
        }
    }
}
