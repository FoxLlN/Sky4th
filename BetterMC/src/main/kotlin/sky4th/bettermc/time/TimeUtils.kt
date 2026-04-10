package sky4th.bettermc.time

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.GameRule
import org.bukkit.World
import java.util.UUID

object TimeUtils {

    private val config get() = sky4th.bettermc.config.ConfigManager

    /**
     * 获取主世界
     */
    fun getDefaultWorld(): World? {
        return Bukkit.getWorlds().firstOrNull { it.environment == World.Environment.NORMAL }
    }
    
    /**
     * 计算快进比例（0.0-1.0），基于睡觉玩家比例
     */
    fun getFastForwardRatio(world: World?): Double {
        world ?: return 0.0

        val totalPlayers = world.players.size
        if (totalPlayers == 0) return 0.0

        val sleepingPlayers = world.players.count { it.isSleeping }
        return sleepingPlayers.toDouble() / totalPlayers.toDouble()
    }

    /**
     * 获取白天每tick的时间增量
     */
    fun getDayIncrementPerTick(): Double {
        val dayLengthGameTicks = config.dayTime * 60 * 20
        return 12000.0 / dayLengthGameTicks
    }

    /**
     * 获取夜间每tick的时间增量
     */
    fun getNightIncrementPerTick(): Double {
        val nightLengthGameTicks = config.nightTime * 60 * 20
        return 12000.0 / nightLengthGameTicks
    }

    /**
     * 获取快进模式每tick的时间增量
     */
    fun getFastForwardIncrementPerTick(): Double {
        val fastForwardGameTicks = config.fastForwardTime * 20
        return 12000.0 / fastForwardGameTicks
    }
}
