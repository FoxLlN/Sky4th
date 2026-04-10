package sky4th.bettermc.time

import net.kyori.adventure.text.Component
import org.bukkit.GameRule
import org.bukkit.World
import org.bukkit.scheduler.BukkitRunnable

class TimeTickTask : BukkitRunnable() {

    companion object {
        private const val TICKS_PER_DAY = 24000L
        private const val TICKS_PER_HALF_DAY = 12000L
    }

    private var carry = 0.0
    private var customTime = 0L
    private var initialized = false
    private var baseDay = -1L

    override fun run() {
        val world = TimeUtils.getDefaultWorld() ?: return

        // 确保只处理主世界
        if (world.environment != org.bukkit.World.Environment.NORMAL) return

        // 检查是否启用了时间循环
        val cycleOn = world.getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE) ?: true

        if (!initialized) {
            val full = world.fullTime
            baseDay = (full / TICKS_PER_DAY) * TICKS_PER_DAY
            customTime = full % TICKS_PER_DAY
            initialized = true
        }

        val currentWorldTime = world.fullTime % TICKS_PER_DAY

        // 检测手动更改并同步时间
        if (kotlin.math.abs(currentWorldTime - customTime) > 1) {
            customTime = currentWorldTime
            carry = 0.0
        }

        if (cycleOn == true) {
            val increment = if (customTime < TICKS_PER_HALF_DAY) {
                // 白天：正常速度
                TimeUtils.getDayIncrementPerTick()
            } else {
                // 夜间：根据睡觉玩家比例调整速度
                val fastForwardRatio = TimeUtils.getFastForwardRatio(world)
                if (fastForwardRatio > 0) {
                    // 有玩家睡觉，根据比例加速
                    val nightIncrement = TimeUtils.getNightIncrementPerTick()
                    val fastForwardIncrement = TimeUtils.getFastForwardIncrementPerTick()
                    // 根据比例在正常夜间速度和最大快进速度之间插值
                    nightIncrement + (fastForwardIncrement - nightIncrement) * fastForwardRatio
                } else {
                    // 没有玩家睡觉，正常夜间速度
                    TimeUtils.getNightIncrementPerTick()
                }
            }

            carry += increment

            if (carry >= 1.0) {
                val ticksToAdd = carry.toLong()
                carry -= ticksToAdd

                var newTime = customTime + ticksToAdd
                if (newTime >= TICKS_PER_DAY) {
                    baseDay += TICKS_PER_DAY
                    newTime %= TICKS_PER_DAY
                }

                customTime = newTime
            }
        }

        world.fullTime = baseDay + customTime

        if (world.time > TICKS_PER_HALF_DAY && world.time < TICKS_PER_DAY) {
            // 夜间且玩家在睡觉时，每秒更新一次
            if (carry % 20 < 1 && TimeUtils.getFastForwardRatio(world) > 0) {
                broadcastSleepSpeed(world)
            }
        }
    }

    /**
     * 向正在睡觉的玩家广播当前夜间加速速率
     */
    private fun broadcastSleepSpeed(world: World) {
        val sleepingPlayers = world.players.filter { it.isSleeping }
        if (sleepingPlayers.isEmpty()) return
        
        val fastForwardRatio = TimeUtils.getFastForwardRatio(world)
        val speedPercentage = (fastForwardRatio * 100).toInt()
        
        sleepingPlayers.forEach { player ->
            player.sendActionBar(
                Component.text("当前夜间加速速率: $speedPercentage%")
            )
        }
    }

}
