
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.concurrent.ConcurrentHashMap

/**
 * 钻石护体词条
 * 效果：当生命低于30%时会获得一个护盾
 * 1级：获得一个吸收8点伤害的护盾(黄心)，持续8秒，冷却30秒
 * 2级：获得一个吸收12点伤害的护盾(黄心)，持续8秒，冷却30秒
 * 3级：获得一个吸收16点伤害的护盾(黄心)，持续8秒，冷却30秒
 * 仅适用于重型护甲
 */
class DiamondProtection : com.sky4th.equipment.modifier.ConfiguredModifier("diamond_protection") {

    companion object {
        // 每级吸收伤害量
        private val ABSORPTION_AMOUNT = intArrayOf(8, 12, 16)

        // 护盾持续时间（秒）
        private const val SHIELD_DURATION = 8

        // 冷却时间（秒）
        private const val COOLDOWN_TIME = 30

        // 生命值阈值（30%）
        private const val HEALTH_THRESHOLD = 0.30

        // 存储每个玩家上次触发护盾的时间戳
        // Key: 玩家UUID
        // Value: 上次触发时间戳（秒）
        private val cooldownMap = ConcurrentHashMap<java.util.UUID, Long>()
    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(EntityDamageEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 只处理玩家受到伤害的事件
        if (event !is EntityDamageEvent || playerRole != PlayerRole.DEFENDER) {
            return
        }

        // 获取当前血量和最大血量
        val currentHealth = player.health
        val maxHealth = player.maxHealth

        // 计算血量百分比
        val healthPercent = currentHealth / maxHealth

        // 检查血量是否低于30%
        if (healthPercent > HEALTH_THRESHOLD) {
            return
        }

        // 检查冷却时间
        val currentTime = System.currentTimeMillis() / 1000
        val lastTriggerTime = cooldownMap[player.uniqueId] ?: 0

        // 如果在冷却时间内，不触发效果
        if (currentTime - lastTriggerTime < COOLDOWN_TIME) {
            return
        }

        // 获取当前等级的吸收伤害量
        val absorptionAmount = ABSORPTION_AMOUNT.getOrNull(level - 1) ?: return

        // 更新冷却时间
        cooldownMap[player.uniqueId] = currentTime

        // 直接设置玩家的吸收值
        val newAbsorption = player.absorptionAmount + absorptionAmount
        player.absorptionAmount = newAbsorption

        // 设置定时任务，在护盾持续时间结束后检查吸收buff
        org.bukkit.Bukkit.getScheduler().runTaskLater(
            com.sky4th.equipment.EquipmentAffix.instance,
            Runnable {
                val playerUUID = player.uniqueId
                val absorptionEffect = player.getPotionEffect(PotionEffectType.ABSORPTION)
                val currentAbsorption = player.absorptionAmount
                if (currentAbsorption != 0.0 ){
                    if (absorptionEffect != null) {
                        // 根据吸收效果等级保留吸收值（每个等级保留4点吸收值）
                        val absorptionLevel = absorptionEffect.amplifier + 1
                        val absorptionToKeep = absorptionLevel * 4.0
                        player.absorptionAmount = absorptionToKeep.coerceAtMost(currentAbsorption)
                    } else {
                        // 如果没有吸收buff，清除吸收值
                        val finalAbsorptionAmount = (currentAbsorption - absorptionAmount).coerceAtLeast(0.0)
                        player.absorptionAmount = finalAbsorptionAmount
                    }
                }
            },
            SHIELD_DURATION * 20L  // 转换为tick（20 tick = 1秒）
        )
    }

    override fun onRemove(player: Player) {
    }
}
