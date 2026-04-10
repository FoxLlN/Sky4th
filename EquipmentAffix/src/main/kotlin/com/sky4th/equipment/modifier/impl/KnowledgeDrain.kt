package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerExpChangeEvent
import org.bukkit.inventory.ItemStack

/**
 * 博学词条
 * 效果：增加玩家获得的经验值
 * 等级1：经验增加5%
 * 等级2：经验增加10%
 * 等级3：经验增加15%
 */
class KnowledgeDrain : com.sky4th.equipment.modifier.ConfiguredModifier("knowledge_drain") {
    
    companion object {
        // 创建NamespacedKey
        private val KNOWLEDGE_DRAIN_FRACTION_KEY = NamespacedKey("sky_core", "knowledge_drain_fraction")
        // 每级增加经验值比例 0.05/0.1/0.15
        private val CONFIG = doubleArrayOf(0.05, 0.10, 0.15)
    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(PlayerExpChangeEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 处理经验变化事件
        if (event !is PlayerExpChangeEvent || playerRole != PlayerRole.SELF) {
            return
        }

        // 根据等级计算经验增加比例
        val bonusPercentage = if (level - 1 in 0..2) CONFIG[level - 1] else return

        // 如果没有经验增加，直接返回
        if (bonusPercentage <= 0.0) {
            return
        }

        // 计算增加后的经验值
        val originalExp = event.amount
        val bonusExp = originalExp * bonusPercentage
        
        // 获取玩家的PDC
        val pdc = player.persistentDataContainer
        
        // 获取之前存储的小数部分
        val storedFraction = if (pdc.has(KNOWLEDGE_DRAIN_FRACTION_KEY, org.bukkit.persistence.PersistentDataType.DOUBLE)) {
            pdc.get(KNOWLEDGE_DRAIN_FRACTION_KEY, org.bukkit.persistence.PersistentDataType.DOUBLE) ?: 0.0
        } else {
            0.0
        }
        
        // 计算总的经验值（包括之前存储的小数部分）
        val totalBonusExp = bonusExp + storedFraction
        
        // 分离整数和小数部分
        val integerPart = totalBonusExp.toInt()
        val fractionPart = totalBonusExp - integerPart
        
        // 保留小数点后两位，避免浮点数精度问题
        val roundedFraction = Math.round(fractionPart * 100.0) / 100.0
        
        // 存储小数部分到PDC
        pdc.set(KNOWLEDGE_DRAIN_FRACTION_KEY, org.bukkit.persistence.PersistentDataType.DOUBLE, roundedFraction)
        
        // 设置新的经验值
        event.amount = originalExp + integerPart
    }
}
