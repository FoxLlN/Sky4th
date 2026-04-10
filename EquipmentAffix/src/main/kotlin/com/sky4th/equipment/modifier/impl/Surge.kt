
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import com.sky4th.equipment.util.projectile.ProjectilePathManager
import com.sky4th.equipment.util.projectile.ProjectilePathListenerAdapter
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Trident
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 潮涌词条
 * 效果：投掷三叉戟会对路径上周围的敌人造成伤害
 * 1级：投掷时对路径上所有敌人造成50%武器伤害
 * 2级：投掷时对路径上所有敌人造成60%武器伤害
 * 3级：投掷时对路径上所有敌人造成70%武器伤害
 */
class Surge : com.sky4th.equipment.modifier.ConfiguredModifier("surge") {

    companion object {
        // 每级的伤害比例
        private val DAMAGE_PERCENTAGE = doubleArrayOf(0.40, 0.50, 0.60)

        // 检测范围（以三叉戟为中心的半径）
        private const val DETECTION_RADIUS = 1.0

        // 跟踪投掷三叉戟的映射
        private val thrownTridents = ConcurrentHashMap<UUID, TridentData>()

        // 三叉戟数据
        data class TridentData(
            val tridentId: UUID,
            val player: Player,
            val weaponItem: ItemStack,
            val damagePercentage: Double,
            val damagedEntities: MutableSet<UUID> = mutableSetOf()
        )

        /**
         * 播放潮涌特效
         */
        fun playSurgeEffect(location: org.bukkit.Location) {
            // 播放水花粒子
            location.world.spawnParticle(
                Particle.SPLASH,
                location,
                5,
                0.3,
                0.3,
                0.3,
                0.1
            )

            // 播放声音
            location.world.playSound(
                location,
                Sound.ENTITY_PLAYER_SPLASH,
                0.5f,
                1.0f
            )
        }

        /**
         * 获取三叉戟的基础伤害
         */
        fun getTridentDamage(item: ItemStack): Double {
            // 尝试从装备属性获取攻击伤害
            val attributes = com.sky4th.equipment.attributes.EquipmentAttributes.fromItemStack(item)

            // 如果有自定义攻击伤害，使用自定义值
            val meta = item.itemMeta
            if (meta != null && meta.hasAttributeModifiers()) {
                val attribute = meta.getAttributeModifiers(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE)
                if (attribute != null && attribute.isNotEmpty()) {
                    // 获取第一个攻击伤害修饰符的值
                    val modifier = attribute.firstOrNull()
                    if (modifier != null) {
                        return modifier.amount + 1.0 // 基础伤害1.0 + 修饰符值
                    }
                }
            }

            // 默认三叉戟伤害为9.0
            return 9.0
        }
    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(
            ProjectileLaunchEvent::class.java,
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
                // 处理三叉戟投掷
                if (event.entity is Trident) {
                    val trident = event.entity as Trident
                    if (trident.shooter is Player && trident.shooter == player) {
                        // 检查是否是持有该词条的装备
                        if (trident.itemStack == item) {
                            // 获取当前等级的伤害比例
                            val damagePercent = if (level - 1 in 0..2) DAMAGE_PERCENTAGE[level - 1] else 0.50

                            // 创建三叉戟数据并开始跟踪
                            val tridentData = TridentData(
                                tridentId = trident.uniqueId,
                                player = player,
                                weaponItem = item,
                                damagePercentage = damagePercent
                            )
                            thrownTridents[trident.uniqueId] = tridentData

                            // 使用 ProjectilePathManager 追踪三叉戟
                            val baseDamage = getTridentDamage(item)
                            val surgeDamage = baseDamage * damagePercent

                            ProjectilePathManager.trackProjectile(
                                projectile = trident,
                                owner = player,
                                sourceItem = item,
                                damage = surgeDamage,
                                listener = SurgePathListener(tridentData),
                                detectionRadius = DETECTION_RADIUS,
                                debug = true
                            )
                        }
                    }
                }
            }

            is EntityDamageByEntityEvent -> {
                // 处理三叉戟直接命中造成的伤害
                if (event.damager is Trident) {
                    val trident = event.damager as Trident
                    val tridentData = thrownTridents[trident.uniqueId]
                    if (tridentData != null) {
                        // 获取基础伤害（武器的攻击伤害）
                        val baseDamage = getTridentDamage(tridentData.weaponItem)

                        // 计算新伤害
                        val newDamage = baseDamage * tridentData.damagePercentage

                        // 设置新伤害
                        event.damage = newDamage

                        // 标记该实体已受伤害，避免重复计算
                        event.entity.uniqueId.let { tridentData.damagedEntities.add(it) }
                    }
                }
            }
        }
    }

    /**
     * 潮涌路径监听器
     * 处理三叉戟路径上的实体检测和伤害
     */
    private class SurgePathListener(private val tridentData: TridentData) : ProjectilePathListenerAdapter() {

        override fun onEntityDetected(
            projectile: org.bukkit.entity.Projectile,
            entity: LivingEntity,
            distance: Double,
            isFront: Boolean
        ) {
            // 只处理在前方的实体
            if (!isFront) return

            // 获取基础伤害
            val baseDamage = Surge.getTridentDamage(tridentData.weaponItem)
            val surgeDamage = baseDamage * tridentData.damagePercentage

            // 造成伤害
            entity.damage(surgeDamage, tridentData.player)

            // 播放粒子效果
            Surge.playSurgeEffect(entity.location)
        }

        override fun onProjectileEnd(
            projectile: org.bukkit.entity.Projectile,
            trackedProjectile: ProjectilePathManager.TrackedProjectile
        ) {
            // 清理三叉戟数据
            thrownTridents.remove(tridentData.tridentId)
        }
    }


}
