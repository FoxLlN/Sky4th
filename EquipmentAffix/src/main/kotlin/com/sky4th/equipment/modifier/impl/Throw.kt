
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.data.NBTEquipmentDataManager
import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.Material
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 飞掷词条
 * 效果：可以无限投掷，每次投掷消耗5点耐久，伤害为60/70/80%
 * 投掷时取消原版投掷事件，生成一个物品实体按照玩家视角投掷
 * 物品落地或击中时消失，不可拾取
 */
class Throw : com.sky4th.equipment.modifier.ConfiguredModifier("throw") {

    companion object {
        // 每次投掷消耗的耐久
        private const val DURABILITY_COST = 5

        // 每级的伤害比例
        private val DAMAGE_PERCENTAGE = doubleArrayOf(0.60, 0.70, 0.80)

        // 跟踪投掷三叉戟的映射（用于检测击中）
        private val thrownItems = ConcurrentHashMap<UUID, ThrownItemData>()

        // 跟踪投掷三叉戟的伤害数据
        data class ThrownItemData(
            val itemId: UUID,
            val player: UUID,
            val weaponItem: ItemStack,
            val damagePercentage: Double
        )
    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(
            ProjectileLaunchEvent::class.java,
            ProjectileHitEvent::class.java,
            EntityDamageByEntityEvent::class.java
        )

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        when (event) {
            is ProjectileLaunchEvent -> {
                // 阻止原版三叉戟投掷
                if (event.entity is org.bukkit.entity.Trident) {
                    val trident = event.entity as org.bukkit.entity.Trident
                    if (trident.shooter is Player && trident.shooter == player) {
                        // 检查是否是持有该词条的装备
                        if (trident.itemStack == item) {
                            // 取消原版投掷事件
                            event.isCancelled = true

                            // 获取当前等级的伤害比例
                            val damagePercent = if (level - 1 in 0..2) DAMAGE_PERCENTAGE[level - 1] else 0.75

                            // 消耗耐久
                            consumeDurability(item, player)
                            // 生成物品投掷
                            throwItem(player, item.clone(), damagePercent)
                        }
                    }
                }
            }

            is ProjectileHitEvent -> {
                // 三叉戟落地或击中时消失
                if (event.entity is org.bukkit.entity.Trident) {
                    val hitTrident = event.entity as org.bukkit.entity.Trident
                    val itemData = thrownItems[hitTrident.uniqueId]
                    if (itemData != null) {
                        // 移除三叉戟
                        hitTrident.remove()
                        thrownItems.remove(hitTrident.uniqueId)
                    }
                }
            }

            is EntityDamageByEntityEvent -> {
                // 处理投掷三叉戟造成的伤害
                if (event.damager is org.bukkit.entity.Trident) {
                    val thrownTrident = event.damager as org.bukkit.entity.Trident
                    val itemData = thrownItems[thrownTrident.uniqueId]
                    if (itemData != null) {
                        // 计算伤害
                        val baseDamage = event.damage
                        val newDamage = baseDamage * itemData.damagePercentage

                        // 设置新伤害
                        event.damage = newDamage

                        // 移除三叉戟
                        thrownTrident.remove()
                        thrownItems.remove(thrownTrident.uniqueId)
                    }
                }
            }
        }
    }

    /**
     * 消耗装备耐久
     */
    private fun consumeDurability(item: ItemStack, player: Player) {
        val meta = item.itemMeta ?: return
        if (meta is org.bukkit.inventory.meta.Damageable) {
            val currentDamage = meta.damage

            var maxDamage = 0
            // 获取最大耐久
            if (meta.hasMaxDamage()) {
                // 如果元数据中有耐久度信息，直接使用
                maxDamage = meta.maxDamage
            } else {
                // 如果元数据中没有耐久度信息，尝试从物品类型获取默认耐久度
                val material = item.type
                if (material.isItem && material.maxDurability > 0) {
                    maxDamage = material.maxDurability.toInt()
                } else {
                    return // 物品没有耐久度，无需处理
                }
            }

            // 增加伤害值（消耗耐久）
            val newDamage = (currentDamage + DURABILITY_COST).coerceAtMost(maxDamage)
            meta.damage = newDamage

            // 应用新的耐久
            item.itemMeta = meta

            // 如果耐久耗尽，播放破损效果
            if (newDamage >= maxDamage) {
                player.world.playSound(player.location, org.bukkit.Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f)
            }
        }
    }

    /**
     * 生成三叉戟投掷
     */
    private fun throwItem(player: Player, item: ItemStack, damagePercentage: Double) {
        // 获取玩家视线方向
        val direction = player.eyeLocation.direction.normalize()

        // 获取玩家手部位置
        val spawnLocation = player.eyeLocation.add(direction.clone().multiply(0.5))

        // 创建三叉戟实体
        val trident = player.world.spawnEntity(spawnLocation, org.bukkit.entity.EntityType.TRIDENT) as org.bukkit.entity.Trident

        // 设置三叉戟的物品
        trident.itemStack = item

        // 设置投掷者为玩家
        trident.shooter = player

        // 设置三叉戟不可拾取（通过设置pickupStatus）
        trident.pickupStatus = org.bukkit.entity.AbstractArrow.PickupStatus.DISALLOWED

        // 设置投掷速度
        val velocity = direction.multiply(1.5)
        trident.velocity = velocity

        // 添加到跟踪列表
        val thrownData = ThrownItemData(
            itemId = trident.uniqueId,
            player = player.uniqueId,
            weaponItem = item,
            damagePercentage = damagePercentage
        )
        thrownItems[trident.uniqueId] = thrownData
    }
}
