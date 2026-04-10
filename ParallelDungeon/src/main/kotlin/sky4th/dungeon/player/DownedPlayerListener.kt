package sky4th.dungeon.player

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerToggleFlightEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.event.entity.EntityTargetEvent
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mob
import org.bukkit.Bukkit
import sky4th.dungeon.Dungeon
import java.util.*
import com.destroystokyo.paper.event.player.PlayerJumpEvent

/**
 * 倒地玩家事件监听器
 * 处理倒地玩家的移动限制和背包限制
 */
class DownedPlayerListener(
    private val downedPlayerManager: DownedPlayerManager
) : Listener {

    /**
     * 玩家点击背包事件
     * 限制倒地玩家操作背包
     */
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? org.bukkit.entity.Player ?: return
        val playerUuid = player.uniqueId

        // 检查玩家是否倒地
        if (!downedPlayerManager.isPlayerDowned(playerUuid)) {
            return
        }

        // 取消点击背包事件
        event.isCancelled = true
    }

    /**
     * 玩家扔物品事件
     * 限制倒地玩家扔物品
     */
    @EventHandler
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        val player = event.player
        val playerUuid = player.uniqueId

        // 检查玩家是否倒地
        if (!downedPlayerManager.isPlayerDowned(playerUuid)) {
            return
        }

        // 取消扔物品事件
        event.isCancelled = true
    }

    /**
     * 玩家切换主副手物品事件
     * 限制倒地玩家切换主副手物品
     */
    @EventHandler
    fun onPlayerSwapHandItems(event: PlayerSwapHandItemsEvent) {
        val player = event.player
        val playerUuid = player.uniqueId

        // 检查玩家是否倒地
        if (!downedPlayerManager.isPlayerDowned(playerUuid)) {
            return
        }

        // 取消切换主副手物品事件
        event.isCancelled = true
    }

    /**
     * 玩家切换物品栏物品事件
     * 限制倒地玩家切换物品栏物品
     */
    @EventHandler
    fun onPlayerItemHeld(event: PlayerItemHeldEvent) {
        val player = event.player
        val playerUuid = player.uniqueId

        // 检查玩家是否倒地
        if (!downedPlayerManager.isPlayerDowned(playerUuid)) {
            return
        }

        // 取消切换物品栏物品事件
        event.isCancelled = true
    }

    /*
     * 处理玩家跳跃事件 
     * 阻止倒地玩家跳跃
     */
    @EventHandler
    fun onPlayerJump(event: PlayerJumpEvent) {
        val player = event.player
        val playerUuid = player.uniqueId

        // 检查玩家是否倒地
        if (!downedPlayerManager.isPlayerDowned(playerUuid)) {
            return
        }

        // 取消跳跃事件
        event.isCancelled = true
    }

    /**
     * 玩家退出事件
     * 清理倒地玩家数据
     */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val playerUuid = player.uniqueId

        // 检查玩家是否倒地
        if (downedPlayerManager.isPlayerDowned(playerUuid)) {
            // 获取倒地玩家数据
            val downedPlayer = downedPlayerManager.getDownedPlayer(playerUuid)

            // 取消救援任务（如果正在进行）
            downedPlayer?.rescueTask?.cancel()

            // 移除救援BossBar
            downedPlayer?.rescueBossBar?.removeAll()

            // 移除倒地BossBar
            downedPlayer?.bossBar?.removeAll()
        }

        // 清理倒地玩家数据
        downedPlayerManager.clearPlayerData(playerUuid)
    }

    /**
     * 怪物目标选择事件
     * 阻止怪物将倒地玩家作为目标
     */
    @EventHandler
    fun onEntityTarget(event: EntityTargetEvent) {
        val target = event.target as? Player ?: return
        val playerUuid = target.uniqueId

        // 检查玩家是否倒地
        if (!downedPlayerManager.isPlayerDowned(playerUuid)) {
            return
        }

        // 检查目标选择者是否是怪物（非玩家）
        val entity = event.entity
        val isMonster = when (entity) {
            is Player -> false
            is LivingEntity -> entity.type.name != "PLAYER"
            else -> true
        }

        // 如果目标选择者是怪物，取消目标选择
        if (isMonster) {
            event.isCancelled = true
            // 清除怪物对倒地玩家的仇恨
            if (entity is Mob) {
                entity.target = null
            }
        }
    }

    /**
     * 玩家受到伤害事件
     * 阻止怪物攻击倒地玩家，但允许玩家攻击倒地玩家
     */
    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val victim = event.entity as? Player ?: return
        val playerUuid = victim.uniqueId

        // 检查玩家是否倒地
        if (!downedPlayerManager.isPlayerDowned(playerUuid)) {
            return
        }

        // 检查攻击者是否是怪物（非玩家）
        val damager = event.damager
        val isMonster = when (damager) {
            is Player -> false
            is LivingEntity -> damager.type.name != "PLAYER"
            else -> true
        }

        // 如果攻击者是怪物，取消伤害事件
        if (isMonster) {
            event.isCancelled = true
        }
    }

    /**
     * 玩家回血事件
     * 阻止倒地玩家回血
     */
    @EventHandler
    fun onEntityRegainHealth(event: EntityRegainHealthEvent) {
        val entity = event.entity
        if (entity !is org.bukkit.entity.Player) {
            return
        }

        val playerUuid = entity.uniqueId

        // 检查玩家是否倒地
        if (!downedPlayerManager.isPlayerDowned(playerUuid)) {
            return
        }

        // 取消回血事件
        event.isCancelled = true
    }

    /**
     * 玩家交互事件(交换副手键)
     * 处理按下F键开始救援
     */
    @EventHandler
    fun onPlayerInteract(event: PlayerSwapHandItemsEvent) {
        val player = event.player
        val playerUuid = player.uniqueId

        // 检查玩家是否倒地
        if (downedPlayerManager.isPlayerDowned(playerUuid)) {
            return
        }

        // 查找附近的倒地玩家
        val nearbyPlayers = player.location.world?.getNearbyPlayers(player.location, 2.0) ?: return
        var downedPlayer: Player? = null
        
        for (nearbyPlayer in nearbyPlayers) {
            if (nearbyPlayer.uniqueId == playerUuid) continue
            if (downedPlayerManager.isPlayerDowned(nearbyPlayer.uniqueId)) {
                // 检查是否可以救援
                if (downedPlayerManager.canRescue(player, nearbyPlayer)) {
                    downedPlayer = nearbyPlayer
                    break
                }
            }
        }
        
        if (downedPlayer == null) {
            return
        }
        
        // 取消副手交换事件
        event.isCancelled = true

        // 开始救援
        downedPlayerManager.startRescue(player, downedPlayer)
    }

}
