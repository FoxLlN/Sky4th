
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.data.NBTEquipmentDataManager
import com.sky4th.equipment.modifier.config.PlayerRole
import com.sky4th.equipment.util.PlayereffectUtil
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/**
 * 雷击词条
 * 效果：消耗存储的铜块资源召唤闪电攻击目标
 * 1级：消耗1个铜块造成6点闪电伤害
 * 2级：消耗1个铜块造成9点闪电伤害
 * 3级：消耗1个铜块造成12点闪电伤害
 * 冷却时间：2秒
 */
class LightningStrike : com.sky4th.equipment.modifier.ConfiguredModifier("lightning_strike") {

    companion object {
        // 冷却时间键
        private val COOLDOWN_KEY = NamespacedKey("equipment_affix", "lightning_strike_cooldown")
        // 每级造成的闪电伤害值
        private val CONFIG = doubleArrayOf(6.0, 9.0, 12.0)
        // 冷却时间（秒）
        private const val COOLDOWN_SECONDS = 2
    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(
            EntityDamageByEntityEvent::class.java
        )

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        val plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("EquipmentAffix") ?: return

        // 处理实体伤害事件
        if (event is EntityDamageByEntityEvent) {
            // 只处理攻击者角色
            if (playerRole != PlayerRole.ATTACKER) {
                return
            }
            
            if (event.cause == EntityDamageEvent.DamageCause.LIGHTNING) return

            // 检查是否在冷却中
            if (isOnCooldown(player)) {
                return
            }

            // 检查装备中是否有铜块资源
            val copperAmount = NBTEquipmentDataManager.getAffixResource(item, "lightning_strike")
            if (copperAmount <= 0) {
                return
            }

            // 获取受害者
            val victim = event.entity
            if (victim !is LivingEntity) {
                return
            }

            // 触发雷击效果
            triggerLightningStrike(player, victim, level, item)
        }
    }

    /**
     * 检查玩家是否在冷却中
     */
    private fun isOnCooldown(player: Player): Boolean {
        val currentTime = System.currentTimeMillis() / 1000
        val lastUsed = player.persistentDataContainer.get(COOLDOWN_KEY, PersistentDataType.LONG) ?: 0
        return currentTime - lastUsed < COOLDOWN_SECONDS
    }

    /**
     * 消耗装备中存储的铜块资源
     */
    private fun consumeCopper(item: ItemStack): Boolean {
        val currentAmount = NBTEquipmentDataManager.getAffixResource(item, "lightning_strike")
        if (currentAmount > 0) {
            NBTEquipmentDataManager.setAffixResource(item, "lightning_strike", currentAmount - 1)
            return true
        }
        return false
    }

    /**
     * 触发雷击效果
     */
    private fun triggerLightningStrike(player: Player, target: LivingEntity, level: Int, item: ItemStack) {
        val plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("EquipmentAffix") ?: return

        // 获取当前等级的伤害值
        val damage = if (level - 1 in 0..2) CONFIG[level - 1] else return
        // 消耗一个铜块资源
        if (!consumeCopper(item)) {
            return
        }

        // 在目标位置生成有伤害的闪电
        val world = target.world
        world.strikeLightning(target.location)
        // 定义伤害范围（例如半径 3 格）
        val radius = 3.0

        // 获取范围内的所有实体
        val entities = world.getNearbyEntities(target.location, radius, radius, radius)

        val damageSource = DamageSource.builder(DamageType.LIGHTNING_BOLT)
            .withDirectEntity(player)          // 直接来源：玩家（比如闪电是玩家召唤的）
            .build()

        // 对每个实体造成额外伤害（原版雷电伤害为5点，额外伤害为 damage - 5）
        val extraDamage = (damage - 5).coerceAtLeast(0.0)  // 确保额外伤害不为负数

        for (entity in entities) {
            if (entity is LivingEntity) {
                entity.damage(extraDamage, damageSource)       
            }
        }

        // 播放特效
        PlayereffectUtil.playSound(target, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8, 1.0, 0.8, 1.2)

        // 设置冷却时间
        player.persistentDataContainer.set(COOLDOWN_KEY, PersistentDataType.LONG, System.currentTimeMillis() / 1000)
    }
}
