
package sky4th.dungeon.loadout.repair.animation

import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask

/**
 * 维修动画基类
 * 所有维修动画实现都应继承此类
 */
abstract class RepairAnimation(
    protected val plugin: Plugin,
    protected val player: Player,
    protected val targetItem: ItemStack,
    protected val repairItem: ItemStack,
    protected val config: RepairAnimationConfig
) {
    protected var animationTask: BukkitTask? = null
    protected var tick = 0
    protected var isRunning = false

    /**
     * 开始动画
     */
    fun start() {
        if (isRunning) return
        isRunning = true
        tick = 0

        // 初始化动画
        initializeAnimation()

        // 启动动画任务
        animationTask = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            tick++
            updateAnimation()

            // 检查是否完成
            if (tick >= config.durationTicks) {
                completeAnimation()
            }
        }, 1L, 1L)
    }

    /**
     * 停止动画
     */
    fun stop() {
        if (!isRunning) return
        isRunning = false

        animationTask?.cancel()
        animationTask = null

        // 清理动画
        cleanupAnimation()
    }

    /**
     * 初始化动画（创建显示实体等）
     */
    protected abstract fun initializeAnimation()

    /**
     * 更新动画（每tick调用一次）
     */
    protected abstract fun updateAnimation()

    /**
     * 动画完成时的处理
     */
    protected open fun completeAnimation() {
        stop()
    }

    /**
     * 清理动画（移除实体等）
     */
    protected abstract fun cleanupAnimation()

    /**
     * 在指定位置生成粒子效果
     */
    protected fun spawnParticle(location: Location, particle: Particle, count: Int = 1) {
        location.world?.spawnParticle(particle, location, count)
    }

    /**
     * 在指定位置播放音效
     */
    protected fun playSound(location: Location, sound: org.bukkit.Sound, volume: Float = 1.0f, pitch: Float = 1.0f) {
        location.world?.playSound(location, sound, volume, pitch)
    }

    /**
     * 获取玩家视线前方的位置
     * @param distance 距离（格）
     */
    protected fun getPlayerEyeLocation(distance: Double = 1.5): Location {
        val playerLoc = player.location
        val eyeHeight = 1.62
        return playerLoc.clone().add(
            playerLoc.direction.clone().multiply(distance).add(
                org.bukkit.util.Vector(0.0, eyeHeight, 0.0)
            )
        )
    }
}
