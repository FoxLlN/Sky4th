
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.data.NBTEquipmentDataManager
import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack

/**
 * 翠刃词条
 * 效果：可以存储绿宝石，攻击时消耗存储提升伤害
 * 1级：消耗4个绿宝石增加20%伤害，最多存储64个
 * 2级：消耗8个绿宝石增加25%伤害，最多存储128个
 * 3级：消耗16个绿宝石增加30%伤害，最多存储256个
 */
class EmeraldEdge : com.sky4th.equipment.modifier.ConfiguredModifier("emerald_edge") {

    companion object {
        // 每级消耗的绿宝石数量
        private val CONSUME_AMOUNT = intArrayOf(4, 8, 16)
        // 每级增加的伤害百分比
        private val DAMAGE_BONUS = doubleArrayOf(0.20, 0.25, 0.30)
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
        // 只处理实体伤害事件，且玩家必须是攻击者
        if (event !is EntityDamageByEntityEvent || playerRole != PlayerRole.ATTACKER) {
            return
        }

        // 获取受击者
        val victim = event.entity
        if (victim !is LivingEntity) {
            return
        }

        // 获取当前等级的消耗数量和伤害加成
        val levelIndex = level - 1
        if (levelIndex !in 0..2) return

        val consumeAmount = CONSUME_AMOUNT[levelIndex]
        val damageBonus = DAMAGE_BONUS[levelIndex]

        // 检查装备中是否有足够的绿宝石资源
        val emeraldAmount = NBTEquipmentDataManager.getAffixResource(item, "emerald_edge")
        if (emeraldAmount < consumeAmount) {
            return
        }

        // 消耗绿宝石资源
        NBTEquipmentDataManager.consumeAffixResource(item, "emerald_edge", consumeAmount)

        // 计算额外伤害
        val extraDamage = event.damage * damageBonus

        // 增加伤害
        event.damage += extraDamage
    }

    override fun onRemove(player: Player) {}
}
