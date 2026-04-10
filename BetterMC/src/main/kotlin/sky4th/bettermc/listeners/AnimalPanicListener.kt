package sky4th.bettermc.listeners

import sky4th.bettermc.command.FeatureManager
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Animals
import org.bukkit.entity.Entity
import org.bukkit.entity.Mob
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.scheduler.BukkitRunnable
import sky4th.bettermc.BetterMC
import sky4th.bettermc.config.ConfigManager
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 动物惊慌监听器
 *
 * 当动物受到攻击时，周围的其他动物会向远离攻击者的方向逃跑
 */
class AnimalPanicListener : Listener {

    // 记录当前处于恐慌状态的动物及其任务，避免重复启动
    private val panickingTasks = ConcurrentHashMap<UUID, BukkitRunnable>()

    // 性能保护：最大同时恐慌的动物数量
    private val maxPanicAnimals = 30

    // 性能保护：动态更新间隔（根据恐慌动物数量调整）
    private var dynamicUpdateInterval = 5L

    @EventHandler
    fun onAnimalDamaged(event: EntityDamageByEntityEvent) {
        if (!FeatureManager.isFeatureEnabled("animal-panic")) return
        val animal = event.entity as? Animals ?: return
        val attacker = event.damager

        // 性能保护：如果当前恐慌动物过多，只让被攻击的动物恐慌
        val currentPanicCount = panickingTasks.size

        // 先让受到攻击的动物进入恐慌状态
        startPanic(animal, attacker)

        // 如果已经超过限制，不再添加周围动物
        if (currentPanicCount >= maxPanicAnimals) {
            return
        }

        // 获取周围动物
        val nearby = animal.world.getNearbyEntities(
            animal.location,
            ConfigManager.panicRadius,
            ConfigManager.panicRadius,
            ConfigManager.panicRadius
        )
            .filterIsInstance<Animals>()
            .filter { it != animal }

        // 为周围动物启动恐慌，但不超过最大限制
        val remainingSlots = maxPanicAnimals - currentPanicCount - 1
        nearby.take(remainingSlots).forEach { other ->
            startPanic(other, attacker)
        }
    }

    /**
     * 让动物进入恐慌状态
     */
    private fun startPanic(animal: Animals, attacker: Entity?) {
        // 如果动物已在恐慌中，取消旧任务（重新开始）
        panickingTasks.remove(animal.uniqueId)?.cancel()

        // Animals 已经继承 Mob，可以直接使用

        val startTime = System.currentTimeMillis()
        val durationMs = ConfigManager.panicDurationTicks * 50L

        // 立即计算第一个逃跑目标点并开始移动
        val initialTarget = getEscapeTarget(animal, attacker)
        if (initialTarget != null) {
            animal.pathfinder.moveTo(initialTarget, ConfigManager.panicSpeedMultiplier)
        }

        // 创建恐慌任务
        val task = object : BukkitRunnable() {
            override fun run() {
                // 检查有效性
                if (!animal.isValid || animal.isDead) {
                    cleanup(animal)
                    return
                }

                // 检查是否超时
                if (System.currentTimeMillis() - startTime >= durationMs) {
                    cleanup(animal)
                    return
                }

                // 计算逃跑目标点
                val target = getEscapeTarget(animal, attacker) ?: return

                // 使用 Paper Pathfinder 移动
                animal.pathfinder.moveTo(target, ConfigManager.panicSpeedMultiplier)
            }
        }

        // 根据当前恐慌动物数量动态调整更新间隔
        val interval = calculateDynamicInterval()

        // 使用动态间隔更新目标
        task.runTaskTimer(BetterMC.instance, interval, interval)
        panickingTasks[animal.uniqueId] = task

        // 更新动态间隔
        updateDynamicInterval()
    }

    /**
     * 计算逃离攻击者的目标点
     * @return 目标位置，如果无法计算则返回 null
     */
    private fun getEscapeTarget(animal: Animals, attacker: Entity?): Location? {
        val animalLoc = animal.location

        // 基础方向：从攻击者指向动物（远离攻击者）
        val baseDir = if (attacker != null && attacker.isValid && attacker.world == animal.world) {
            animalLoc.toVector().subtract(attacker.location.toVector())
        } else {
            // 无攻击者或攻击者无效时使用随机方向
            org.bukkit.util.Vector(Math.random() - 0.5, 0.0, Math.random() - 0.5)
        }

        // 如果方向向量长度为0（可能动物和攻击者重合），则使用随机方向
        if (baseDir.lengthSquared() < 0.01) {
            baseDir.setX(Math.random() - 0.5).setZ(Math.random() - 0.5)
        }

        // 归一化并添加随机扰动（使逃跑路径更自然）
        val dir = baseDir.normalize()
        val angle = (Math.random() - 0.5) * Math.PI / 3 // 弧度
        dir.rotateAroundY(angle)

        // 尝试多个距离点，找到最安全的逃跑目标
        val maxAttempts = 5
        for (attempt in 0 until maxAttempts) {
            // 计算目标点：当前坐标 + 方向 * 逃跑距离
            val distance = ConfigManager.panicDistance * (1.0 - attempt * 0.15) // 逐渐缩短距离
            var target = animalLoc.clone().add(dir.clone().multiply(distance))

            // 高度调整：找到安全的高度位置
            target = findSafeHeight(target, animalLoc)

            // 检查目标点是否安全
            if (isSafeTarget(target, animal.world)) {
                return target
            }

            // 如果当前方向不安全，稍微调整方向再试
            val newAngle = ((attempt + 1) * Math.PI / 6) * (if (attempt % 2 == 0) 1 else -1)
            dir.rotateAroundY(newAngle)
        }

        // 如果所有尝试都失败，返回一个相对安全的默认位置
        return animalLoc.clone().add(dir.multiply(ConfigManager.panicDistance * 0.5))
    }

    /**
     * 为目标位置找到安全的高度
     */
    private fun findSafeHeight(target: Location, reference: Location): Location {
        val world = target.world
        val x = target.blockX
        val z = target.blockZ

        // 从目标高度开始向下搜索，找到第一个非固体方块
        for (y in target.blockY downTo maxOf(reference.blockY - 5, world.minHeight)) {
            val block = world.getBlockAt(x, y, z)
            val above = world.getBlockAt(x, y + 1, z)
            if (block.type.isSolid && !above.type.isSolid) {
                target.y = (y + 1).toDouble()
                return target
            }
        }

        // 如果没找到合适的，向上搜索
        for (y in target.blockY..minOf(reference.blockY + 5, world.maxHeight)) {
            val block = world.getBlockAt(x, y, z)
            val above = world.getBlockAt(x, y + 1, z)
            if (!block.type.isSolid && !above.type.isSolid) {
                target.y = y.toDouble()
                return target
            }
        }

        // 保持原高度
        return target
    }

    /**
     * 检查目标位置是否安全
     */
    private fun isSafeTarget(target: Location, world: org.bukkit.World): Boolean {
        val block = target.block
        val above = world.getBlockAt(target.blockX, target.blockY + 1, target.blockZ)
        val below = world.getBlockAt(target.blockX, target.blockY - 1, target.blockZ)

        // 目标点不能在固体方块内，且上方必须有空间
        if (block.type.isSolid) return false
        if (above.type.isSolid) return false

        // 下方必须有固体方块支撑（除非是水生生物）
        if (!below.type.isSolid && block.type != Material.WATER) {
            return false
        }

        // 避免危险方块（岩浆、仙人掌等）
        val dangerousBlocks = setOf(
            org.bukkit.Material.LAVA,
            org.bukkit.Material.FIRE,
            org.bukkit.Material.CAMPFIRE,
            org.bukkit.Material.SOUL_FIRE,
            org.bukkit.Material.MAGMA_BLOCK,
            org.bukkit.Material.CACTUS
        )

        if (block.type in dangerousBlocks || below.type in dangerousBlocks) {
            return false
        }

        return true
    }

    /**
     * 清理动物的恐慌状态
     */
    private fun cleanup(animal: Animals) {
        panickingTasks.remove(animal.uniqueId)?.cancel()
        // 清理后更新动态间隔
        updateDynamicInterval()
    }

    /**
     * 根据当前恐慌动物数量计算动态更新间隔
     * 动物越多，更新间隔越长，以平衡性能
     */
    private fun calculateDynamicInterval(): Long {
        val count = panickingTasks.size
        return when {
            count <= 5 -> 5L      // 少量动物：5 tick (0.25秒)
            count <= 15 -> 10L    // 中等数量：10 tick (0.5秒)
            count <= 25 -> 15L    // 较多动物：15 tick (0.75秒)
            else -> 20L           // 大量动物：20 tick (1秒)
        }
    }

    /**
     * 更新动态间隔
     */
    private fun updateDynamicInterval() {
        dynamicUpdateInterval = calculateDynamicInterval()
    }
}