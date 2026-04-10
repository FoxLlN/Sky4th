package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.Material
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * 坚毅词条
 * 效果：受伤有概率获得抗性提升，并且下次攻击伤害提升
 * 1级：10%概率获得抗性提升，持续3秒，下次攻击伤害+10%
 * 2级：12%概率获得抗性提升，持续4秒，下次攻击伤害+15%
 * 3级：15%概率获得抗性提升，持续5秒，下次攻击伤害+20%
 */
class Tenacity : com.sky4th.equipment.modifier.ConfiguredModifier("tenacity") {

    companion object {
        // 拥有坚毅效果的玩家列表（使用UUID避免内存泄漏）
        private val tenacityPlayers = ConcurrentHashMap<UUID, Int>()

        // 每级配置：(触发概率，持续时间，伤害提升)
        private val CONFIG = arrayOf(
            Triple(0.10, 3, 0.10),  // 10%概率，持续3秒，伤害+10%
            Triple(0.12, 4, 0.15),  // 12%概率，持续4秒，伤害+15%
            Triple(1.0, 5, 0.20)   // 15%概率，持续5秒，伤害+20%
        )
    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(EntityDamageByEntityEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        if (event !is EntityDamageByEntityEvent) return

        // 处理受伤事件（玩家是受害者）
        if (playerRole == PlayerRole.DEFENDER) {
            handleDefend(event, player, level)
        }
        // 处理攻击事件（玩家是攻击者）
        else if (playerRole == PlayerRole.ATTACKER) {
            handleAttack(event, player, level)
        }
    }

    /**
     * 处理受伤事件
     */
    private fun handleDefend(event: EntityDamageByEntityEvent, player: Player, level: Int) {
        val (chance, duration, _) = CONFIG.getOrNull(level - 1) ?: return

        // 检查是否触发概率
        if (Random.nextDouble() > chance) return

        // 添加坚毅效果到列表
        tenacityPlayers[player.uniqueId] = level

        // 添加抗性提升效果（Resistance I）
        val resistanceEffect = PotionEffect(
            PotionEffectType.RESISTANCE,
            duration * 20,  // 转换为tick
            0,  // Resistance I
            true,  // 不显示粒子
            true   // 不显示图标
        )
        player.addPotionEffect(resistanceEffect)
    }

    /**
     * 处理攻击事件
     */
    private fun handleAttack(event: EntityDamageByEntityEvent, player: Player, level: Int) {
        // 检查是否在列表中
        if (!hasTenacityEffect(player)) return

        // 检查是否有抗性提升效果
        if (player.hasPotionEffect(PotionEffectType.RESISTANCE)) {
            val damageBoost = getDamageBoost(player) ?: return

            // 应用伤害提升
            val originalDamage = event.damage
            val bonusDamage = originalDamage * damageBoost
            event.damage = originalDamage + bonusDamage
        }

        // 从列表中移除
        tenacityPlayers.remove(player.uniqueId)
    }

    override fun onRemove(player: Player) {
        // 移除坚毅效果
        tenacityPlayers.remove(player.uniqueId)
    }

    /**
     * 检查玩家是否有坚毅效果
     */
    private fun hasTenacityEffect(player: Player): Boolean {
        return tenacityPlayers.containsKey(player.uniqueId)
    }

    /**
     * 获取玩家的坚毅伤害提升
     */
    private fun getDamageBoost(player: Player): Double? {
        val level = tenacityPlayers[player.uniqueId] ?: return null
        val (_, _, damageBoost) = CONFIG.getOrNull(level - 1) ?: return null
        return damageBoost
    }
}
