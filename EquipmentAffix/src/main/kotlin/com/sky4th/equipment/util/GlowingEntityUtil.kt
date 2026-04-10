package com.sky4th.equipment.util

import fr.skytasul.glowingentities.GlowingEntities
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.ChatColor
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.ConcurrentHashMap

/**
 * 实体发光效果工具类
 * 用于实现对指定玩家显示实体的发光效果
 * 使用 GlowingEntities 库实现
 */
object GlowingEntityUtil {

    // GlowingEntities 实例，需要在插件启动时初始化
    private lateinit var glowingApi: GlowingEntities

    // 存储每个实体的发光效果状态
    private val glowStates = ConcurrentHashMap<Int, MutableMap<Player, GlowState>>()

    /**
     * 初始化 GlowingEntities API
     * 必须在插件 onEnable 时调用一次
     */
    fun initialize(plugin: JavaPlugin) {
        if (!::glowingApi.isInitialized) {
            glowingApi = GlowingEntities(plugin)
        }
    }

    /**
     * 关闭并清理资源
     * 应在插件 onDisable 时调用
     */
    fun disable() {
        if (::glowingApi.isInitialized) {
            glowingApi.disable()
        }
        glowStates.clear()
    }

    /**
     * 发光状态数据类
     */
    private data class GlowState(
        val color: Color,
        val task: org.bukkit.scheduler.BukkitTask
    )

    /**
     * 将 Bukkit Color 转换为最接近的 ChatColor（用于 GlowingEntities）
     */
    @Suppress("DEPRECATION")
    private fun colorToChatColor(color: Color): ChatColor {
        // 尝试匹配标准颜色
        val rgb = color.asRGB()

        // 预定义的颜色映射
        val colorMap = mapOf(
            org.bukkit.Color.WHITE.asRGB() to ChatColor.WHITE,
            org.bukkit.Color.YELLOW.asRGB() to ChatColor.YELLOW,
            org.bukkit.Color.LIME.asRGB() to ChatColor.GREEN,
            org.bukkit.Color.AQUA.asRGB() to ChatColor.AQUA,
            org.bukkit.Color.RED.asRGB() to ChatColor.RED,
            org.bukkit.Color.PURPLE.asRGB() to ChatColor.DARK_PURPLE,
            org.bukkit.Color.BLUE.asRGB() to ChatColor.BLUE,
            org.bukkit.Color.fromRGB(0, 0, 0).asRGB() to ChatColor.BLACK,
            org.bukkit.Color.fromRGB(128, 128, 128).asRGB() to ChatColor.DARK_GRAY,
            org.bukkit.Color.fromRGB(165, 42, 42).asRGB() to ChatColor.DARK_RED,
            org.bukkit.Color.fromRGB(0, 100, 0).asRGB() to ChatColor.DARK_GREEN,
            org.bukkit.Color.fromRGB(0, 0, 139).asRGB() to ChatColor.DARK_BLUE,
            org.bukkit.Color.fromRGB(0, 191, 255).asRGB() to ChatColor.DARK_AQUA,
            org.bukkit.Color.fromRGB(255, 105, 180).asRGB() to ChatColor.LIGHT_PURPLE,
            org.bukkit.Color.fromRGB(255, 255, 0).asRGB() to ChatColor.GOLD,
            org.bukkit.Color.fromRGB(192, 192, 192).asRGB() to ChatColor.GRAY
        )

        // 尝试精确匹配
        colorMap[rgb]?.let { return it }

        // 无精确匹配，返回最接近的颜色（根据RGB值）
        return when {
            color.red > color.green && color.red > color.blue -> ChatColor.RED
            color.green > color.red && color.green > color.blue -> ChatColor.GREEN
            color.blue > color.red && color.blue > color.green -> ChatColor.BLUE
            color.red > 200 && color.green > 200 && color.blue < 100 -> ChatColor.YELLOW
            color.red > 200 && color.green < 100 && color.blue > 200 -> ChatColor.LIGHT_PURPLE
            color.red < 100 && color.green > 200 && color.blue > 200 -> ChatColor.AQUA
            color.red > 150 && color.green > 150 && color.blue > 150 -> ChatColor.WHITE
            color.red < 100 && color.green < 100 && color.blue < 100 -> ChatColor.BLACK
            else -> ChatColor.GRAY
        }
    }

    /**
     * 为指定玩家设置实体的发光效果
     * @param entity 要发光的实体
     * @param player 能看到发光效果的玩家
     * @param duration 持续时间（秒），0表示永久
     * @param color 发光颜色，默认为白色
     */
    fun setGlowForPlayer(
        entity: Entity,
        player: Player,
        duration: Long = 0,
        color: Color = Color.WHITE
    ) {
        val entityId = entity.entityId

        // 获取或创建该实体的发光状态映射
        val entityGlowStates = glowStates.getOrPut(entityId) { mutableMapOf() }

        // 取消该玩家对该实体的旧任务
        entityGlowStates[player]?.task?.cancel()

        // 将 Color 转换为 ChatColor
        val chatColor = colorToChatColor(color)

        // 设置发光（仅对指定玩家可见）
        glowingApi.setGlowing(entity, player, chatColor)

        // 如果持续时间大于0，创建定时任务在持续时间结束后取消发光
        if (duration > 0) {
            val task = Bukkit.getScheduler().runTaskLater(
                JavaPlugin.getProvidingPlugin(GlowingEntityUtil::class.java),
                Runnable {
                    // 清除该玩家对该实体的发光
                    glowingApi.unsetGlowing(entity, player)
                    entityGlowStates.remove(player)
                    if (entityGlowStates.isEmpty()) {
                        glowStates.remove(entityId)
                    }
                },
                duration * 20L
            )

            // 保存状态
            entityGlowStates[player] = GlowState(color, task)
        } else {
            // 永久发光，不创建定时任务
            entityGlowStates[player] = GlowState(color, Bukkit.getScheduler().runTaskLater(
                JavaPlugin.getProvidingPlugin(GlowingEntityUtil::class.java),
                Runnable {},
                Long.MAX_VALUE
            ))
        }
    }

    /**
     * 刷新实体的发光效果
     * @param entity 要发光的实体
     * @param player 能看到发光效果的玩家
     * @param duration 持续时间（秒），0表示永久
     */
    fun refreshGlowForPlayer(
        entity: Entity,
        player: Player,
        duration: Long = 0
    ) {
        val entityId = entity.entityId
        val entityGlowStates = glowStates[entityId] ?: return

        val currentState = entityGlowStates[player] ?: return

        // 取消旧任务
        currentState.task.cancel()

        // 重新设置发光（会刷新持续时间）
        val chatColor = colorToChatColor(currentState.color)
        glowingApi.setGlowing(entity, player, chatColor)

        // 如果持续时间大于0，创建新的定时任务
        if (duration > 0) {
            val task = Bukkit.getScheduler().runTaskLater(
                JavaPlugin.getProvidingPlugin(GlowingEntityUtil::class.java),
                Runnable {
                    // 清除该玩家对该实体的发光
                    glowingApi.unsetGlowing(entity, player)
                    entityGlowStates.remove(player)
                    if (entityGlowStates.isEmpty()) {
                        glowStates.remove(entityId)
                    }
                },
                duration * 20L
            )

            entityGlowStates[player] = GlowState(currentState.color, task)
        } else {
            // 永久发光
            entityGlowStates[player] = GlowState(currentState.color, Bukkit.getScheduler().runTaskLater(
                JavaPlugin.getProvidingPlugin(GlowingEntityUtil::class.java),
                Runnable {},
                Long.MAX_VALUE
            ))
        }
    }

    /**
     * 取消实体对指定玩家的发光效果
     */
    fun removeGlowForPlayer(entity: Entity, player: Player) {
        val entityId = entity.entityId
        val entityGlowStates = glowStates[entityId] ?: return

        val state = entityGlowStates.remove(player)
        state?.task?.cancel()

        if (state != null) {
            // 清除该玩家对该实体的发光
            glowingApi.unsetGlowing(entity, player)
        }

        if (entityGlowStates.isEmpty()) {
            glowStates.remove(entityId)
        }
    }

    /**
     * 取消实体对所有玩家的发光效果
     */
    fun removeAllGlowEffects(entity: Entity) {
        val entityId = entity.entityId
        val entityGlowStates = glowStates.remove(entityId) ?: return

        entityGlowStates.forEach { (player, state) ->
            state.task.cancel()
            // 清除该玩家对该实体的发光
            glowingApi.unsetGlowing(entity, player)
        }
    }

    /**
     * 取消指定玩家的所有发光效果
     */
    fun removeAllGlowEffectsForPlayer(player: Player) {
        val iterator = glowStates.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val entityId = entry.key
            val entityGlowStates = entry.value

            val state = entityGlowStates.remove(player)
            if (state != null) {
                state.task.cancel()
                // 需要获取实体，可能已卸载，但 GlowingEntities 内部会处理不存在的实体
                Bukkit.getOnlinePlayers().forEach { p ->
                    p.world.entities.find { it.entityId == entityId }?.let { entity ->
                        // 清除该玩家对该实体的发光
                        glowingApi.unsetGlowing(entity, player)
                    }
                }
            }

            if (entityGlowStates.isEmpty()) {
                iterator.remove()
            }
        }
    }
}
