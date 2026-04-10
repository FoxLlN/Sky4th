package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack
import org.bukkit.event.player.PlayerInteractEvent

/**
 * 果腹词条
 * 效果：右键空气时，消耗工具10%耐久度恢复2点饥饿度(最少10点耐久)
 */
class Satiate : com.sky4th.equipment.modifier.ConfiguredModifier("satiate") {
    override fun getEventTypes(): List<Class<out Event>> =
        listOf(PlayerInteractEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 确保事件是PlayerInteractEvent
        if (event !is PlayerInteractEvent || playerRole != PlayerRole.SELF) {
            return
        }
        // 防止双击触发：只处理主手点击
        if (event.hand != org.bukkit.inventory.EquipmentSlot.HAND) {
            return
        }

        // 确保是右键点击空气
        if (event.action != org.bukkit.event.block.Action.RIGHT_CLICK_AIR) return

        // 获取物品的耐久度
        val itemMeta = item.itemMeta ?: return
        val damageable = itemMeta as? org.bukkit.inventory.meta.Damageable ?: return
        val maxDurability = item.type.maxDurability
        val currentDamage = damageable.damage
        val durability = maxDurability - currentDamage

        // 确保耐久度足够（至少10点）
        if (durability < 10) return

        // 计算要消耗的耐久度（10%，最少10点）
        val durabilityCost = maxOf(10, (durability * 0.1).toInt())

        // 检查玩家是否需要恢复饥饿值
        if (player.foodLevel >= 18) return

        // 恢复2点饥饿度
        player.foodLevel = minOf(20, player.foodLevel + 2)

        // 消耗耐久度
        damageable.damage = currentDamage + durabilityCost
        item.itemMeta = itemMeta

        // 播放音效和粒子效果
        player.playSound(player.location, Sound.ENTITY_PLAYER_BURP, 1.0f, 1.0f)
        player.spawnParticle(Particle.HEART, player.location.add(0.0, 1.0, 0.0), 1, 0.0, 0.0, 0.0, 0.0)
    }
}
