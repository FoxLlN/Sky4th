package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.Material
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack

/**
 * 对决词条
 * 效果：对使用远程武器的生物（主手拿着弓/弩/三叉戟）伤害增加
 * 1级：+15%伤害
 * 2级：+20%伤害
 * 3级：+25%伤害
 */
class Duel : com.sky4th.equipment.modifier.ConfiguredModifier("duel") {

    companion object {
        // 每级配置：伤害加成比例
        private val CONFIG = arrayOf(
            0.15,  // 15%伤害加成
            0.20,  // 20%伤害加成
            0.25   // 25%伤害加成
        )

        // 远程武器类型
        private val RANGED_WEAPONS = setOf(
            Material.BOW,
            Material.CROSSBOW,
            Material.TRIDENT
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
        // 只处理实体伤害事件，且玩家必须是攻击者
        if (event !is EntityDamageByEntityEvent || playerRole != PlayerRole.ATTACKER) {
            return
        }

        // 获取受害者
        val victim = event.entity
        if (victim !is LivingEntity) {
            return
        }

        // 检查受害者是否使用远程武器
        val victimWeapon = victim.equipment?.itemInMainHand
        if (victimWeapon == null || victimWeapon.type !in RANGED_WEAPONS) {
            return
        }

        // 获取当前等级的配置
        val damageBonus = CONFIG.getOrNull(level - 1) ?: return

        // 计算额外伤害
        val extraDamage = event.damage * damageBonus

        // 增加伤害
        event.damage = event.damage + extraDamage
    }

    override fun onRemove(player: Player) {
    }
}
