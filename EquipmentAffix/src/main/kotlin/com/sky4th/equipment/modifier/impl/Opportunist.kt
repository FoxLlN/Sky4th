
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.event.PlayerDodgeEvent
import com.sky4th.equipment.modifier.config.PlayerRole
import com.sky4th.equipment.util.PlayereffectUtil
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 趁势词条
 * 效果：成功闪避后，下一次攻击伤害提升
 * 等级1：+10%伤害
 * 等级2：+15%伤害
 * 等级3：+20%伤害
 */
class Opportunist : com.sky4th.equipment.modifier.ConfiguredModifier("opportunist") {

    companion object {
        // 存储玩家趁势增益状态（玩家UUID -> 伤害加成比例）
        private val bonusMap = ConcurrentHashMap<UUID, Double>()
        
        // 增益持续时间（5秒）
        private const val BONUS_DURATION = 5
        
        // 各等级的伤害加成比例
        private val CONFIG = doubleArrayOf(0.10, 0.15, 0.20)
    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(PlayerDodgeEvent::class.java, EntityDamageByEntityEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        when (event) {
            is PlayerDodgeEvent -> handleDodge(event, player, level)
            is EntityDamageByEntityEvent -> handleAttack(event, player, level, playerRole)
        }
    }

    /**
     * 处理闪避事件
     * 设置趁势增益状态
     */
    private fun handleDodge(event: PlayerDodgeEvent, player: Player, level: Int) {
        // 获取伤害加成比例
        val bonus = CONFIG.getOrElse(level - 1) { CONFIG[0] }

        // 存储增益状态到内存Map
        val playerUuid = player.uniqueId
        bonusMap[playerUuid] = bonus

        // 设置定时任务，
        org.bukkit.Bukkit.getScheduler().runTaskLater(
            com.sky4th.equipment.EquipmentAffix.instance,
            Runnable {
                bonusMap.remove(playerUuid)
            },
            BONUS_DURATION * 20L  // 转换为tick（20 tick = 1秒）
        )
    }

    /**
     * 处理攻击事件
     * 如果有趁势增益，提升伤害并清除增益
     */
    private fun handleAttack(event: EntityDamageByEntityEvent, player: Player, level: Int, playerRole: PlayerRole) {
        // 只处理玩家作为攻击者的情况
        if (playerRole != PlayerRole.ATTACKER) return

        // 检查是否有趁势增益
        val playerUuid = player.uniqueId
        val bonus = bonusMap[playerUuid] ?: return
        bonusMap.remove(playerUuid)
        // 计算加成伤害
        val bonusDamage = event.damage * bonus
        event.damage += bonusDamage

        // 播放特效
        PlayereffectUtil.playCircleParticle(player, Particle.SWEEP_ATTACK, 3)
        PlayereffectUtil.playSound(player, org.bukkit.Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5, 1.0, 1.0, 1.2)
    }

    override fun onRemove(player: Player) {
    }
}
