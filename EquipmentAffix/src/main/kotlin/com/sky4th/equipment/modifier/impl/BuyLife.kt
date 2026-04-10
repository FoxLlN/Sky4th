
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.data.NBTEquipmentDataManager
import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.inventory.ItemStack
import java.util.concurrent.ConcurrentHashMap

/**
 * 买命词条
 * 效果：生命归零时可以消耗大量绿宝石复活
 * 1级：消耗128个绿宝石复活并恢复30%血量，冷却10分钟
 * 2级：消耗96个绿宝石复活并恢复40%血量，冷却10分钟
 * 3级：消耗64个绿宝石复活并恢复50%血量，冷却10分钟
 */
class BuyLife : com.sky4th.equipment.modifier.ConfiguredModifier("buy_life") {

    companion object {
        // 每级配置：(恢复血量百分比, 消耗绿宝石数量)
        private val CONFIG = arrayOf(
            0.30 to 128,  // 1级：恢复30%，消耗128
            0.40 to 96,  // 2级：恢复40%，消耗96
            0.50 to 64   // 3级：恢复50%，消耗64
        )

        // 冷却时间（秒）- 10分钟
        private const val COOLDOWN_TIME = 10 * 60L

        // 存储每个玩家上次复活的时间戳
        // Key: 玩家UUID
        // Value: 上次复活时间戳（秒）
        private val cooldownMap = ConcurrentHashMap<java.util.UUID, Long>()
    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(PlayerDeathEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 只处理玩家死亡事件
        if (event !is PlayerDeathEvent) {
            return
        }

        // 检查冷却时间
        val currentTime = System.currentTimeMillis() / 1000
        val lastTriggerTime = cooldownMap[player.uniqueId] ?: 0

        // 如果在冷却时间内，不触发效果
        if (currentTime - lastTriggerTime < COOLDOWN_TIME) {
            return
        }

        // 获取当前等级的配置
        val (healthPercent, emeraldCost) = CONFIG.getOrNull(level - 1) ?: return

        // 获取当前存储的绿宝石数量
        val currentEmeralds = NBTEquipmentDataManager.getAffixResource(item, "buy_life")

        // 如果绿宝石数量不足，不触发复活
        if (currentEmeralds < emeraldCost) {
            return
        }

        // 消耗绿宝石
        NBTEquipmentDataManager.consumeAffixResource(item, "buy_life", emeraldCost)

        // 更新冷却时间
        cooldownMap[player.uniqueId] = currentTime

        // 恢复玩家生命值
        val maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH)?.value ?: 20.0
        val restoreHealth = maxHealth * healthPercent

        // 取消死亡事件
        event.isCancelled = true

        // 恢复生命值
        player.health = restoreHealth

        // 清空死亡消息
        event.deathMessage(null)

        // 播放复活特效
        player.world.spawnParticle(Particle.CLOUD, player.location.add(0.0, 1.0, 0.0), 50, 0.5, 0.5, 0.5, 0.1)
        player.world.playSound(player.location, Sound.ITEM_TOTEM_USE, 1.0f, 1.0f)
    }

    override fun onRemove(player: Player) {
        // 当词条被移除时，清理冷却记录
        cooldownMap.remove(player.uniqueId)
    }
}
