
package com.sky4th.equipment.util.projectile

import com.sky4th.equipment.EquipmentAffix
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 投掷物路径管理器
 * 统一管理所有投掷物的路径追踪，每tick检测一次
 */
object ProjectilePathManager {
    // 跟踪的投掷物映射
    private val trackedProjectiles = ConcurrentHashMap<UUID, TrackedProjectile>()

    // 统一检测任务
    private var trackingTask: org.bukkit.scheduler.BukkitTask? = null

    /**
     * 投掷物数据类
     */
    data class TrackedProjectile(
        val projectileId: UUID,
        val projectile: Projectile,
        val owner: Player,
        val sourceItem: org.bukkit.inventory.ItemStack,
        val damage: Double,
        val damagedEntities: MutableSet<UUID> = mutableSetOf(),
        val listeners: MutableList<ProjectilePathListener> = mutableListOf(),
        var startTime: Long = System.currentTimeMillis(),
        var startLocation: org.bukkit.Location? = projectile.location.clone(),
        var detectionRadius: Double = 1.0,
        var maxTrackTime: Long = 300L,
        var maxTrackDistance: Double = 100.0,
        var debug: Boolean = false
    )

    /**
     * 路径监听器接口
     * 用于处理投掷物路径上的事件
     */
    interface ProjectilePathListener {
        /**
         * 当检测到路径上的实体时调用
         * @param projectile 投掷物
         * @param entity 路径上的实体
         * @param distance 距离投掷物的距离
         * @param isFront 是否在投掷物前方
         */
        fun onEntityDetected(
            projectile: Projectile,
            entity: LivingEntity,
            distance: Double,
            isFront: Boolean
        )

        /**
         * 当投掷物命中实体时调用
         * @param projectile 投掷物
         * @param entity 被命中的实体
         */
        fun onEntityHit(projectile: Projectile, entity: LivingEntity) {}

        /**
         * 当投掷物落地或消失时调用
         * @param projectile 投掷物
         * @param trackedProjectile 追踪的投掷物数据
         */
        fun onProjectileEnd(projectile: Projectile, trackedProjectile: TrackedProjectile) {}
    }

    /**
     * 初始化管理器
     */
    fun initialize() {
        if (trackingTask == null) {
            trackingTask = object : BukkitRunnable() {
                override fun run() {
                    updateAllProjectiles()
                }
            }.runTaskTimer(EquipmentAffix.instance, 0L, 1L)
        }
    }

    /**
     * 关闭管理器
     */
    fun shutdown() {
        trackingTask?.cancel()
        trackingTask = null
        trackedProjectiles.clear()
    }

    /**
     * 更新所有投掷物
     */
    private fun updateAllProjectiles() {
        val currentTime = System.currentTimeMillis()

        // 使用迭代器以便安全删除
        val iterator = trackedProjectiles.values.iterator()

        while (iterator.hasNext()) {
            val trackedProjectile = iterator.next()
            val projectile = trackedProjectile.projectile

            // 检查是否超过最大追踪时间
            val elapsedTime = (currentTime - trackedProjectile.startTime) / 50 // 转换为tick
            if (elapsedTime >= trackedProjectile.maxTrackTime) {
                endTracking(projectile, trackedProjectile)
                iterator.remove()
                continue
            }

            // 检查投掷物是否有效
            if (!projectile.isValid) {
                endTracking(projectile, trackedProjectile)
                iterator.remove()
                continue
            }

            // 检查投掷物是否落地
            if (projectile.isOnGround || projectile.location.block.type.isSolid) {
                endTracking(projectile, trackedProjectile)
                iterator.remove()
                continue
            }

            // 检查是否超过最大追踪距离
            val startLocation = trackedProjectile.startLocation
            if (startLocation != null && projectile.location.distance(startLocation) > trackedProjectile.maxTrackDistance) {
                endTracking(projectile, trackedProjectile)
                iterator.remove()
                continue
            }

            // 检测路径上的实体
            detectEntitiesOnPath(projectile, trackedProjectile)

            // 如果开启了debug模式，在当前位置生成绿色粒子
            if (trackedProjectile.debug) {
                spawnDebugParticle(projectile.location)
            }
        }
    }

    /**
     * 开始追踪投掷物
     * @param projectile 要追踪的投掷物
     * @param owner 投掷者
     * @param sourceItem 来源物品
     * @param damage 基础伤害
     * @param listener 路径监听器
     * @param detectionRadius 检测范围，默认为1.0
     * @param maxTrackTime 最大追踪时间（tick），默认为300
     * @param maxTrackDistance 最大追踪距离（方块），默认为100.0
     * @param debug 是否开启调试模式，开启后会在路径上生成绿色粒子
     * @return 追踪的投掷物数据
     */
    fun trackProjectile(
        projectile: Projectile,
        owner: Player,
        sourceItem: org.bukkit.inventory.ItemStack,
        damage: Double,
        listener: ProjectilePathListener? = null,
        detectionRadius: Double = 1.0,
        maxTrackTime: Long = 300L,
        maxTrackDistance: Double = 100.0,
        debug: Boolean = false
    ): TrackedProjectile {
        // 创建投掷物数据
        val trackedProjectile = TrackedProjectile(
            projectileId = projectile.uniqueId,
            projectile = projectile,
            owner = owner,
            sourceItem = sourceItem,
            damage = damage,
            detectionRadius = detectionRadius,
            maxTrackTime = maxTrackTime,
            maxTrackDistance = maxTrackDistance,
            debug = debug
        )

        // 添加监听器
        if (listener != null) {
            trackedProjectile.listeners.add(listener)
        }

        // 存储投掷物数据
        trackedProjectiles[projectile.uniqueId] = trackedProjectile

        // 如果开启了debug模式，在起始位置生成绿色粒子
        if (trackedProjectile.debug) {
            spawnDebugParticle(projectile.location)
        }

        return trackedProjectile
    }

    /**
     * 添加监听器到已追踪的投掷物
     * @param projectileId 投掷物ID
     * @param listener 路径监听器
     * @return 是否成功添加
     */
    fun addListener(projectileId: UUID, listener: ProjectilePathListener): Boolean {
        val trackedProjectile = trackedProjectiles[projectileId] ?: return false
        trackedProjectile.listeners.add(listener)
        return true
    }

    /**
     * 移除监听器
     * @param projectileId 投掷物ID
     * @param listener 路径监听器
     * @return 是否成功移除
     */
    fun removeListener(projectileId: UUID, listener: ProjectilePathListener): Boolean {
        val trackedProjectile = trackedProjectiles[projectileId] ?: return false
        return trackedProjectile.listeners.remove(listener)
    }

    /**
     * 获取投掷物的追踪数据
     * @param projectileId 投掷物ID
     * @return 追踪数据，如果不存在则返回null
     */
    fun getTrackedProjectile(projectileId: UUID): TrackedProjectile? {
        return trackedProjectiles[projectileId]
    }

    /**
     * 停止追踪投掷物
     * @param projectileId 投掷物ID
     */
    fun stopTracking(projectileId: UUID) {
        val trackedProjectile = trackedProjectiles.remove(projectileId)
        trackedProjectile?.let {
            endTracking(it.projectile, it)
        }
    }

    /**
     * 检测路径上的实体
     */
    private fun detectEntitiesOnPath(
        projectile: Projectile,
        trackedProjectile: TrackedProjectile
    ) {
        val projectileLocation = projectile.location

        // 获取范围内的所有实体
        val nearbyEntities = projectile.world.getNearbyEntities(
            projectileLocation,
            trackedProjectile.detectionRadius,
            trackedProjectile.detectionRadius,
            trackedProjectile.detectionRadius
        )

        for (entity in nearbyEntities) {
            // 只处理活体生物
            if (entity !is LivingEntity) continue

            // 跳过投掷者自己
            if (entity.uniqueId == trackedProjectile.owner.uniqueId) continue

            // 跳过已经受伤害的实体
            if (entity.uniqueId in trackedProjectile.damagedEntities) continue

            // 计算距离
            val distance = entity.location.distance(projectileLocation)

            // 检查实体是否在路径上（使用向量点积判断）
            val toEntity = entity.location.toVector().subtract(projectileLocation.toVector())
            val projectileDirection = projectile.velocity.normalize()

            // 计算点积，判断是否在前方
            val dotProduct = toEntity.normalize().dot(projectileDirection)

            // 判断是否在前方
            val isFront = dotProduct > 0

            // 只对在前方的实体通知监听器并标记为已受伤害
            if (isFront) {
                // 通知所有监听器
                trackedProjectile.listeners.forEach { listener ->
                    listener.onEntityDetected(projectile, entity, distance, isFront)
                }

                // 标记该实体已受伤害
                trackedProjectile.damagedEntities.add(entity.uniqueId)
            }
        }
    }

    /**
     * 结束追踪
     */
    private fun endTracking(projectile: Projectile, trackedProjectile: TrackedProjectile) {
        // 通知所有监听器
        trackedProjectile.listeners.forEach { listener ->
            listener.onProjectileEnd(projectile, trackedProjectile)
        }
    }

    /**
     * 清空所有追踪
     */
    fun clearAll() {
        trackedProjectiles.clear()
    }

    /**
     * 生成调试粒子
     * 使用绿色粒子，持续5分钟（6000 ticks）
     * @param location 位置
     */
    private fun spawnDebugParticle(location: org.bukkit.Location) {
        location.world.spawnParticle(
            org.bukkit.Particle.HAPPY_VILLAGER,
            location,
            1,
            0.0,
            0.0,
            0.0,
            0.0
        )
    }
}

/**
 * 投掷物路径监听器适配器
 * 提供空实现，方便子类只重写需要的方法
 */
abstract class ProjectilePathListenerAdapter : ProjectilePathManager.ProjectilePathListener {
    override fun onEntityDetected(
        projectile: Projectile,
        entity: LivingEntity,
        distance: Double,
        isFront: Boolean
    ) {
        // 默认空实现
    }

    override fun onEntityHit(projectile: Projectile, entity: LivingEntity) {
        // 默认空实现
    }

    override fun onProjectileEnd(projectile: Projectile, trackedProjectile: ProjectilePathManager.TrackedProjectile) {
        // 默认空实现
    }
}

/**
 * 投掷物路径效果工具类
 * 提供常用的路径效果方法
 */
object ProjectilePathEffects {
    /**
     * 播放水花效果
     * @param location 位置
     * @param count 粒子数量
     */
    fun playSplashEffect(location: org.bukkit.Location, count: Int = 5) {
        location.world.spawnParticle(
            org.bukkit.Particle.SPLASH,
            location,
            count,
            0.3,
            0.3,
            0.3,
            0.1
        )

        location.world.playSound(
            location,
            org.bukkit.Sound.ENTITY_PLAYER_SPLASH,
            0.5f,
            1.0f
        )
    }

    /**
     * 播放火焰效果
     * @param location 位置
     * @param count 粒子数量
     */
    fun playFireEffect(location: org.bukkit.Location, count: Int = 5) {
        location.world.spawnParticle(
            org.bukkit.Particle.FLAME,
            location,
            count,
            0.3,
            0.3,
            0.3,
            0.1
        )

        location.world.playSound(
            location,
            org.bukkit.Sound.ENTITY_BLAZE_SHOOT,
            0.5f,
            1.0f
        )
    }

    /**
     * 播放闪电效果
     * @param location 位置
     */
    fun playLightningEffect(location: org.bukkit.Location) {
        location.world.spawnParticle(
            org.bukkit.Particle.ELECTRIC_SPARK,
            location,
            10,
            0.5,
            0.5,
            0.5,
            0.1
        )

        location.world.playSound(
            location,
            org.bukkit.Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
            0.5f,
            1.0f
        )
    }

    /**
     * 播放魔法效果
     * @param location 位置
     * @param count 粒子数量
     */
    fun playMagicEffect(location: org.bukkit.Location, count: Int = 10) {
        location.world.spawnParticle(
            org.bukkit.Particle.PORTAL,
            location,
            count,
            0.3,
            0.3,
            0.3,
            0.1
        )

        location.world.playSound(
            location,
            org.bukkit.Sound.ENTITY_ELDER_GUARDIAN_CURSE,
            0.5f,
            1.0f
        )
    }
}
