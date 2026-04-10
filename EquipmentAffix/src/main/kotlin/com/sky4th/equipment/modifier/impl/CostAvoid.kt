package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.data.NBTEquipmentDataManager
import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack

/**
 * 消灾词条
 * 效果：受到伤害时优先存储的绿宝石抵挡伤害
 * 1级：消耗4个绿宝石抵消1点伤害，最多抵消10点，最多存储64个
 * 2级：消耗3个绿宝石抵消1点伤害，最多抵消15点，最多存储128个
 * 3级：消耗2个绿宝石抵消1点伤害，最多抵消20点，最多存储256个
 */
class CostAvoid : com.sky4th.equipment.modifier.ConfiguredModifier("cost_avoid") {

    companion object {
        // 每级配置：(消耗绿宝石数量, 最大抵消伤害, 最大存储数量)
        private val CONFIG = arrayOf(
            Triple(4, 10, 64),    // 1级：4绿宝石抵消1伤害，最多10伤害，存储64
            Triple(3, 15, 128),   // 2级：3绿宝石抵消1伤害，最多15伤害，存储128
            Triple(2, 20, 256)    // 3级：2绿宝石抵消1伤害，最多20伤害，存储256
        )
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
        // 只处理实体伤害事件，且玩家必须是受害者
        if (event !is EntityDamageEvent || playerRole != PlayerRole.DEFENDER) {
            return
        }

        // 获取当前等级的配置
        val (emeraldPerDamage, maxReduceDamage, maxStorage) = CONFIG.getOrNull(level - 1) ?: return

        // 获取当前存储的绿宝石数量
        val currentEmeralds = NBTEquipmentDataManager.getAffixResource(item, "cost_avoid")

        // 如果没有绿宝石，直接返回
        if (currentEmeralds <= 0) {
            return
        }

        // 计算可以抵消的伤害量
        val damage = event.damage
        val canReduceDamage = minOf(damage, maxReduceDamage.toDouble())

        // 计算需要的绿宝石数量
        val neededEmeralds = (canReduceDamage * emeraldPerDamage).toInt()

        // 计算实际可以使用的绿宝石数量
        val actualEmeralds = minOf(neededEmeralds, currentEmeralds)

        // 计算实际抵消的伤害量
        val actualReduceDamage = actualEmeralds.toDouble() / emeraldPerDamage

        // 消耗绿宝石
        NBTEquipmentDataManager.consumeAffixResource(item, "cost_avoid", actualEmeralds)

        // 减少伤害
        event.damage -= actualReduceDamage
    }

    override fun onRemove(player: Player) {}
}
