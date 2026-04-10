
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/**
 * 压强词条
 * 效果：在水中攻击水中生物附加伤害
 * 1级：附加2点溺水伤害
 * 2级：附加3点溺水伤害
 * 3级：附加4点溺水伤害
 */
class Pressure : com.sky4th.equipment.modifier.ConfiguredModifier("pressure") {

    companion object {
        // 每级的额外溺水伤害
        private val CONFIG = doubleArrayOf(2.0, 3.0, 4.0)
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
        if (event !is EntityDamageByEntityEvent || playerRole != PlayerRole.ATTACKER) return
        
        if (event.cause == EntityDamageEvent.DamageCause.DROWNING) return

        // 获取受击者
        val victim = event.entity
        if (victim !is LivingEntity) {
            return
        }

        // 检查玩家是否在水中
        if (!player.isInWater) {
            return
        }

        // 检查目标是否在水中
        if (!victim.isInWater) {
            return
        }

        // 获取当前等级的伤害加成
        val drowningDamage = if (level - 1 in 0..2) CONFIG[level - 1] else return

        // 创建溺水伤害(溺水是无来源伤害)
        val damageSource = DamageSource.builder(DamageType.DROWN).build()

        // 附加溺水伤害
        victim.damage(drowningDamage, damageSource)

    }
}
