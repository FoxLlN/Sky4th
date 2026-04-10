
package sky4th.dungeon.player

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import sky4th.dungeon.Dungeon
import sky4th.dungeon.team.TeamManager
import sky4th.dungeon.util.LanguageUtil.sendLangSys
import sky4th.dungeon.util.LanguageUtil.sendLangTeam
import sky4th.dungeon.util.LanguageUtil.sendLangBroad
import sky4th.core.api.LanguageAPI
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import net.kyori.adventure.text.Component

/**
 * 倒地玩家数据类
 */
data class DownedPlayer(
    val playerUuid: UUID,
    val playerName: String,
    val downedTime: Long = System.currentTimeMillis(),
    var isRescued: Boolean = false,
    var isGivingUp: Boolean = false,
    var rescuerUuid: UUID? = null,
    var rescueStartTime: Long = 0,
    var deathCount: Int = 0,
    var bossBar: BossBar? = null,
    var rescueBossBar: BossBar? = null,
    var rescueTask: org.bukkit.scheduler.BukkitTask? = null,
    var killerName: String? = null,  // 击倒者名称
    var weaponName: String? = null   // 武器名称（如果是玩家击倒）
)

/**
 * 倒地玩家管理器
 * 负责管理倒地玩家的状态和救援逻辑
 */
class DownedPlayerManager(
    private val plugin: sky4th.dungeon.Dungeon,
    private val teamManager: TeamManager
) {
    // 倒地玩家列表 (playerUuid -> DownedPlayer)
    private val downedPlayers: ConcurrentHashMap<UUID, DownedPlayer> = ConcurrentHashMap()

    // 刚刚放弃救援的玩家列表（用于防止放弃后再次倒地）
    private val recentlyGivenUp: ConcurrentHashMap<UUID, Long> = ConcurrentHashMap()

    // 正在处理死亡的玩家列表（用于防止无限递归）
    private val dyingPlayers: ConcurrentHashMap<UUID, Boolean> = ConcurrentHashMap()

    // 保存放弃救援玩家的击倒者信息（用于播报死亡消息）
    private val giveUpPlayerKillers: ConcurrentHashMap<UUID, Pair<String?, String?>> = ConcurrentHashMap()

    // 倒地持续时间（毫秒）
    private val downDuration = teamManager.config.downDuration * 1000L

    // 救援持续时间（毫秒）
    private val helpDuration = teamManager.config.helpDuration * 1000L

    // 呼救距离
    private val helpDistance = teamManager.config.helpDistance

    /**
     * 将玩家设置为倒地状态
     */
    fun setPlayerDowned(player: Player, killerName: String? = null, weaponName: String? = null): Boolean {
        val playerUuid = player.uniqueId

        // 检查玩家是否正在处理死亡（防止无限递归）
        if (dyingPlayers.containsKey(playerUuid)) {
            return false
        }

        // 检查玩家是否已经倒地
        if (downedPlayers.containsKey(playerUuid)) {
            return false
        }

        // 获取玩家当前死亡次数
        val playerState = teamManager.getPlayerState(playerUuid)
        val deathCount = playerState?.deathCount ?: 0

        // 如果死亡次数已达上限，直接死亡
        if (deathCount >= teamManager.config.maxDeathCount) {
            return false
        }

        // 检查玩家所在的队伍是否还有其他存活玩家
        val playerTeam = teamManager.getPlayerTeam(playerUuid)
        if (playerTeam != null && playerTeam.getMemberCount() > 1) {
            // 多人队伍，检查是否还有其他存活玩家
            val hasAliveTeammate = playerTeam.members.any { memberUuid ->
                memberUuid != playerUuid && !(teamManager.getPlayerState(memberUuid)?.isDead ?: false)
            }

            // 如果没有其他存活队友，直接死亡
            if (!hasAliveTeammate) {
                // 标记玩家为正在处理死亡
                dyingPlayers[playerUuid] = true
                // 增加死亡次数
                increaseDeathCount(playerUuid)
                // 标记玩家为死亡状态
                if (playerState != null) {
                    playerState.isDead = true
                }
                // 检查队伍是否所有成员都已死亡，如果是则解散队伍
                teamManager.checkAndDisbandTeamIfAllDead(playerTeam.teamId)
                // 清除标记
                dyingPlayers.remove(playerUuid)
                return false
            }
        }

        // 增加死亡次数
        increaseDeathCount(playerUuid)

        // 再次检查死亡次数是否超过上限
        val newDeathCount = teamManager.getPlayerState(playerUuid)?.deathCount ?: 0
        
        // 发送死亡次数更新消息
        if (newDeathCount >= teamManager.config.maxDeathCount) {
            // 死亡次数已达上限，直接死亡
            player.sendLangSys(plugin, "downed.death-count-exceeded")
            // 标记玩家为正在处理死亡
            dyingPlayers[playerUuid] = true
            // 标记玩家为死亡状态
            if (playerState != null) {
                playerState.isDead = true
            }
            // 清除标记
            dyingPlayers.remove(playerUuid)
            return false
        }

        // 创建倒地玩家数据
        val downedPlayer = DownedPlayer(
            playerUuid = playerUuid,
            playerName = player.name,
            deathCount = newDeathCount,
            killerName = killerName,
            weaponName = weaponName
        )

        // 添加到倒地玩家列表
        downedPlayers[playerUuid] = downedPlayer

        // 更新PlayerState的倒地状态
        if (playerState != null) {
            playerState.isDowned = true
        }

        // 应用倒地效果
        applyDownedEffects(player)

        // 发送倒地提示
        player.sendLangSys(plugin, "downed.downed")

        // 发送呼救/放弃救援选择按钮
        sendHelpOrGiveUpMessage(player)

        // 启动倒地超时检查
        startDownedTimeoutCheck(playerUuid)

        return true
    }

    /**
     * 应用倒地效果
     */
    private fun applyDownedEffects(player: Player) {
        // 设置游戏模式为冒险模式
        player.gameMode = org.bukkit.GameMode.ADVENTURE

        // 设置玩家为趴下状态（一格高）
        player.setPose(org.bukkit.entity.Pose.SWIMMING, true);

        // 应用非常缓慢效果（爬行速度）
        player.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, Int.MAX_VALUE, 5, false, false))

        // 应用虚弱效果
        player.addPotionEffect(PotionEffect(PotionEffectType.WEAKNESS, Int.MAX_VALUE, 5, false, false))

        // 设置生命值为20（20滴血），不锁定血量
        player.health = 20.0

        // 创建并显示BossBar倒计时
        val bossBar = Bukkit.createBossBar("倒地倒计时: ${downDuration / 1000}秒", BarColor.RED, BarStyle.SEGMENTED_10)
        bossBar.progress = 1.0
        bossBar.addPlayer(player)
        bossBar.isVisible = true

        // 保存BossBar到倒地玩家数据
        val downedPlayer = downedPlayers[player.uniqueId]!!
        downedPlayer.bossBar = bossBar

        // 清除周围怪物对倒地玩家的仇恨
        clearMonsterAggro(player)
    }

    /**
     * 清除周围怪物对倒地玩家的仇恨
     */
    private fun clearMonsterAggro(player: Player) {
        // 获取玩家附近的怪物（扩大范围到64格，确保清除所有可能的仇恨）
        val nearbyEntities = player.location.getNearbyEntities(64.0, 64.0, 64.0)
        for (entity in nearbyEntities) {
            if (entity is org.bukkit.entity.Mob) {
                // 检查怪物是否将倒地玩家作为目标
                if (entity.target == player) {
                    // 清除怪物对倒地玩家的仇恨
                    entity.target = null
                    // 重置怪物的AI目标
                    entity.setAI(false)
                    entity.setAI(true)
                }
            }
        }
    }

    /**
     * 移除倒地效果
     */
    private fun removeDownedEffects(player: Player) {
        // 移除所有负面效果
        player.removePotionEffect(PotionEffectType.SLOWNESS)
        player.removePotionEffect(PotionEffectType.WEAKNESS)

        // 恢复玩家状态
        player.setPose(org.bukkit.entity.Pose.SWIMMING, false);

        // 只在玩家活着的情况下恢复生命值
        if (player.health > 0) {
            val maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH)?.value ?: 20.0
            player.health = maxHealth * 0.2 // 恢复到20%生命值
        }

        // 移除所有BossBar
        val downedPlayer = downedPlayers[player.uniqueId]
        if (downedPlayer != null) {
            // 移除倒地BossBar
            if (downedPlayer.bossBar != null) {
                downedPlayer.bossBar?.removePlayer(player)
                downedPlayer.bossBar = null
            }
            // 移除救援BossBar
            if (downedPlayer.rescueBossBar != null) {
                downedPlayer.rescueBossBar?.removePlayer(player)
                downedPlayer.rescueBossBar = null
            }
        }
    }

    /**
     * 发送呼救/放弃救援选择按钮
     */
    private fun sendHelpOrGiveUpMessage(player: Player) {
        // 发送选择按钮

        // 使用组件消息发送可点击的按钮
        val helpMessage = Component.text(LanguageAPI.getText(plugin, "downed.help-button"))
            .clickEvent(net.kyori.adventure.text.event.ClickEvent.callback { audience ->
                if (audience is Player) {
                    handleDownedHelp(audience)
                }
            })
            .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(net.kyori.adventure.text.Component.text(LanguageAPI.getText(plugin, "downed.help-message"))))

        val giveUpMessage = Component.text(LanguageAPI.getText(plugin, "downed.give-up-button"))
            .clickEvent(net.kyori.adventure.text.event.ClickEvent.callback { audience ->
                if (audience is Player) {
                    handleDownedGiveUp(audience)
                }
            })
            .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(net.kyori.adventure.text.Component.text(LanguageAPI.getText(plugin, "downed.give-up-message"))))

        val inviteMessage = Component.text()
            .append(Component.text(LanguageAPI.getPrefix(plugin, "prefix-sys")))
            .append(helpMessage)
            .append(Component.text(" "))
            .append(giveUpMessage)
        
        player.sendMessage(inviteMessage)
    }

    /**
     * 启动倒地超时检查
     */
    private fun startDownedTimeoutCheck(playerUuid: UUID) {
        // 启动BossBar倒计时更新任务
        val updateTask = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            val downedPlayer = downedPlayers[playerUuid] ?: return@Runnable

            // 如果玩家已经被救援或放弃，则取消任务
            if (downedPlayer.isRescued || downedPlayer.isGivingUp) {
                return@Runnable
            }

            // 计算剩余时间
            val elapsedTime = System.currentTimeMillis() - downedPlayer.downedTime
            val remainingTime = downDuration - elapsedTime

            // 更新BossBar
            if (downedPlayer.bossBar != null) {
                val remainingSeconds = (remainingTime / 1000).coerceAtLeast(0)
                val progress = remainingTime.toDouble() / downDuration.toDouble()
                downedPlayer.bossBar?.setTitle("倒地倒计时: $remainingSeconds 秒")
                downedPlayer.bossBar?.setProgress(progress.coerceAtLeast(0.0))
            }

            // 检查是否超时
            if (elapsedTime >= downDuration) {
                // 超时，玩家放弃救援
                val player = Bukkit.getPlayer(playerUuid)
                if (player != null && player.isOnline) {
                    giveUp(player)
                }
            }
        }, 0L, 20L) // 每秒更新一次
    }

    /**
     * 玩家放弃救援（直接触发死亡）
     */
    fun giveUp(player: Player) {
        val playerUuid = player.uniqueId
        val downedPlayer = downedPlayers[playerUuid] ?: return

        // 标记为放弃
        downedPlayer.isGivingUp = true

        // 记录刚刚放弃救援的玩家（防止再次倒地）
        recentlyGivenUp[playerUuid] = System.currentTimeMillis()

        // 更新PlayerState的倒地状态和死亡状态
        val playerState = teamManager.getPlayerState(playerUuid)
        if (playerState != null) {
            playerState.isDowned = false
            playerState.isDead = true
        }

        // 取消救援任务（如果正在进行）
        downedPlayer.rescueTask?.cancel()
        downedPlayer.rescueTask = null

        // 移除救援BossBar
        downedPlayer.rescueBossBar?.removeAll()
        downedPlayer.rescueBossBar = null

        // 移除倒地效果
        removeDownedEffects(player)

        // 保存击倒者信息（用于播报死亡消息）
        giveUpPlayerKillers[playerUuid] = Pair(downedPlayer.killerName, downedPlayer.weaponName)

        // 从倒地列表中移除
        downedPlayers.remove(playerUuid)

        // 发送放弃消息
        player.sendLangSys(plugin, "team.give-up")

        // 触发真正的死亡事件（死亡次数已经在倒地时增加过了）
        // 不直接设置玩家生命值为0，而是让PlayerDeathListener处理
        // 标记玩家为正在处理死亡（防止无限递归）
        dyingPlayers[playerUuid] = true
        // 设置玩家生命值为0，触发死亡事件
        player.health = 0.0
        // 清除标记
        dyingPlayers.remove(playerUuid)
    }

    /**
     * 检查玩家是否刚刚放弃救援
     * @return true如果玩家在最近5秒内放弃过救援
     */
    fun wasGivingUp(playerUuid: UUID): Boolean {
        val giveUpTime = recentlyGivenUp[playerUuid] ?: return false
        val currentTime = System.currentTimeMillis()

        // 如果放弃时间超过5秒，则清除记录
        if (currentTime - giveUpTime > 5000) {
            recentlyGivenUp.remove(playerUuid)
            return false
        }

        return true
    }

    /**
     * 获取放弃救援玩家的击倒者信息
     * @return 击倒者名称和武器名称的Pair
     */
    fun getGiveUpPlayerKiller(playerUuid: UUID): Pair<String?, String?> {
        return giveUpPlayerKillers[playerUuid] ?: Pair(null, null)
    }

    /**
     * 清除放弃救援玩家的击倒者信息
     */
    fun clearGiveUpPlayerKiller(playerUuid: UUID) {
        giveUpPlayerKillers.remove(playerUuid)
    }

    /**
     * 强制杀死玩家并踢出地牢，跳过倒地逻辑
     * 这个方法不要求玩家在倒地列表中，可以直接调用
     */
    fun forceKillAndKick(player: Player) {
        val playerUuid = player.uniqueId

        // 如果玩家在倒地列表中，先清除倒地状态
        val downedPlayer = downedPlayers[playerUuid]
        if (downedPlayer != null) {
            // 取消救援任务（如果正在进行）
            downedPlayer.rescueTask?.cancel()
            downedPlayer.rescueTask = null

            // 移除救援BossBar
            downedPlayer.rescueBossBar?.removeAll()
            downedPlayer.rescueBossBar = null

            // 移除倒地效果
            removeDownedEffects(player)

            // 从倒地列表中移除
            downedPlayers.remove(playerUuid)
        }

        // 记录刚刚放弃救援的玩家（防止再次倒地）
        recentlyGivenUp[playerUuid] = System.currentTimeMillis()

        // 更新PlayerState的倒地状态和死亡状态
        val playerState = teamManager.getPlayerState(playerUuid)
        if (playerState != null) {
            playerState.isDowned = false
            playerState.isDead = true
        }

        // 标记玩家为正在处理死亡（防止无限递归）
        dyingPlayers[playerUuid] = true

        // 设置玩家生命值为0，触发死亡事件
        player.health = 0.0

        // 清除标记
        dyingPlayers.remove(playerUuid)
    }

    /**
     * 增加玩家死亡次数
     */
    private fun increaseDeathCount(playerUuid: UUID) {
        val playerState = teamManager.getPlayerState(playerUuid) ?: return
        playerState.deathCount++
    }

    /**
     * 检查是否可以救援
     */
    fun canRescue(rescuer: Player, downedPlayer: Player): Boolean {
        val rescuerUuid = rescuer.uniqueId
        val downedUuid = downedPlayer.uniqueId
        // 检查倒地玩家是否在倒地列表中
        val downed = downedPlayers[downedUuid] ?: return false
        // 检查是否已经在救援中
        if (downed.rescuerUuid != null) {
            return false
        }
        // 检查救援者是否在倒地状态
        if (downedPlayers.containsKey(rescuerUuid)) {
            return false
        }
        // 获取倒地玩家和救援者的队伍
        val downedTeam = teamManager.getPlayerTeam(downedUuid)
        val rescuerTeam = teamManager.getPlayerTeam(rescuerUuid)
        // 检查倒地玩家的队伍类型
        val isDownedSolo = downedTeam == null || downedTeam.getMemberCount() == 1
        val isRescuerSolo = rescuerTeam == null || rescuerTeam.getMemberCount() == 1
        
        if (isDownedSolo) {
            // 单人队伍成员倒地：独狼或本队未满的玩家可以救援
            if (!isRescuerSolo && rescuerTeam.isFull()) {
                // 救援者不是独狼且队伍满员，不能救援
                return false
            }
        } else {
            // 多人队伍成员倒地
            // 多人队伍不能救助多人队伍队员
            if (!isRescuerSolo && rescuerTeam.teamId != downedTeam.teamId) {
                return false
            }
            // 检查救援者是否是队友
            if (!downedTeam.hasMember(rescuerUuid)) {
                return false
            }
        }
        return true
    }


    /**
     * 开始救援
     */
    fun startRescue(rescuer: Player, downedPlayer: Player): Boolean {
        val rescuerUuid = rescuer.uniqueId
        val downedUuid = downedPlayer.uniqueId

        // 检查倒地玩家是否在倒地列表中
        val downed = downedPlayers[downedUuid] ?: return false

        // 检查是否已经在救援中
        if (downed.rescuerUuid != null) {
            rescuer.sendLangSys(plugin, "downed.already-being-rescued")
            return false
        }

        // 检查救援者是否在倒地状态
        if (downedPlayers.containsKey(rescuerUuid)) {
            rescuer.sendLangSys(plugin, "downed.cannot-rescue-while-downed")
            return false
        }

        // 设置救援者
        downed.rescuerUuid = rescuerUuid
        downed.rescueStartTime = System.currentTimeMillis()

        // 更新救援者的状态
        val rescuerState = teamManager.getPlayerState(rescuerUuid)
        if (rescuerState != null) {
            rescuerState.isHelping = true
        }

        // 创建并显示救援进度BossBar（给救援者和倒地玩家）
        val rescueBossBar = Bukkit.createBossBar("救援倒计时: ${teamManager.config.helpDuration}秒", BarColor.GREEN, BarStyle.SEGMENTED_10)
        rescueBossBar.progress = 0.0
        rescueBossBar.addPlayer(rescuer)
        rescueBossBar.addPlayer(downedPlayer)
        rescueBossBar.isVisible = true
        downed.rescueBossBar = rescueBossBar

        // 隐藏倒地BossBar（因为现在显示的是救援BossBar）
        if (downed.bossBar != null) {
            downed.bossBar?.isVisible = false
        }

        // 发送救援开始消息
        rescuer.sendLangTeam(plugin, "team.rescue-start", "player" to downedPlayer.name)
        downedPlayer.sendLangTeam(plugin, "downed.being-rescued", "player" to rescuer.name)

        // 启动救援进度检查
        startRescueProgressCheck(rescuerUuid, downedUuid)

        return true
    }

    /**
     * 启动救援进度检查
     */
    private fun startRescueProgressCheck(rescuerUuid: UUID, downedUuid: UUID) {
        val downed = downedPlayers[downedUuid] ?: return
        
        // 创建并保存救援任务
        val task = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            val currentDowned = downedPlayers[downedUuid] ?: run {
                // 倒地玩家已被移除，取消任务并清理BossBar
                downed.rescueTask?.cancel()
                downed.rescueTask = null
                downed.rescueBossBar?.removeAll()
                downed.rescueBossBar = null
                return@Runnable
            }

            // 检查救援是否完成
            val elapsedTime = System.currentTimeMillis() - currentDowned.rescueStartTime
            if (elapsedTime >= helpDuration) {
                // 救援完成
                completeRescue(rescuerUuid, downedUuid)
                return@Runnable
            }

            // 检查救援者是否还在附近
            val rescuer = Bukkit.getPlayer(rescuerUuid)
            val downedPlayer = Bukkit.getPlayer(downedUuid)

            if (rescuer == null || !rescuer.isOnline || downedPlayer == null || !downedPlayer.isOnline) {
                // 救援者或倒地玩家离线，取消救援
                cancelRescue(downedUuid)
                return@Runnable
            }

            // 检查距离
            val distance = rescuer.location.distance(downedPlayer.location)
            if (distance > 5) {
                // 距离过远，取消救援
                rescuer.sendLangTeam(plugin, "downed.rescue-cancel-distance")
                downedPlayer.sendLangTeam(plugin, "downed.rescue-cancel-distance")
                cancelRescue(downedUuid)
                return@Runnable
            }

            // 更新救援进度BossBar
            val remainingTime = (helpDuration - elapsedTime) / 1000
            val progressDouble = elapsedTime.toDouble() / helpDuration.toDouble()
            currentDowned.rescueBossBar?.setTitle("救援倒计时: ${remainingTime}秒")
            currentDowned.rescueBossBar?.setProgress(progressDouble.coerceAtMost(1.0))

            // 发送救援进度消息（每20%发送一次）
            val progressPercent = (progressDouble * 100).toInt()
            if (progressPercent % 20 == 0) {
                rescuer.sendLangTeam(plugin, "downed.rescue-progress", "progress" to "$progressPercent%")
            }
        }, 20L, 20L) // 每秒检查一次
        
        // 保存任务引用
        downed.rescueTask = task
    }

    /**
     * 完成救援
     */
    private fun completeRescue(rescuerUuid: UUID, downedUuid: UUID) {
        val downed = downedPlayers.remove(downedUuid) ?: return

        // 取消救援任务
        downed.rescueTask?.cancel()
        downed.rescueTask = null

        // 移除救援BossBar
        downed.rescueBossBar?.removeAll()
        downed.rescueBossBar = null

        // 标记为已救援
        downed.isRescued = true

        // 更新救援者的状态
        val rescuerState = teamManager.getPlayerState(rescuerUuid)
        if (rescuerState != null) {
            rescuerState.isHelping = false
        }

        // 获取玩家
        val rescuer = Bukkit.getPlayer(rescuerUuid)
        val downedPlayer = Bukkit.getPlayer(downedUuid)

        if (downedPlayer != null && downedPlayer.isOnline) {
            // 更新PlayerState的倒地状态
            val playerState = teamManager.getPlayerState(downedUuid)
            if (playerState != null) {
                playerState.isDowned = false
            }

            // 移除倒地效果
            removeDownedEffects(downedPlayer)

            // 发送救援完成消息
            downedPlayer.sendLangTeam(plugin, "downed.rescued", "player" to (rescuer?.name ?: "Unknown"))
        }

        if (rescuer != null && rescuer.isOnline) {
            // 发送救援完成消息
            rescuer.sendLangTeam(plugin, "team.rescue-complete", "player" to (downedPlayer?.name ?: "Unknown"))

            // 如果倒地玩家是独狼，加入救援者的队伍
            val rescuerTeam = teamManager.getPlayerTeam(rescuerUuid)
            val downedTeam = teamManager.getPlayerTeam(downedUuid)
            val isDownedSolo = downedTeam == null || downedTeam.getMemberCount() == 1

            if (isDownedSolo && rescuerTeam != null && downedPlayer != null) {
                // 倒地玩家是独狼，加入救援者的队伍
                if (downedTeam != null) {
                    teamManager.removePlayerFromTeam(downedUuid)
                }
                teamManager.addPlayerToTeam(downedPlayer, rescuerTeam.teamId)
            }
        }
    }

    /**
     * 取消救援
     */
    private fun cancelRescue(downedUuid: UUID) {
        val downed = downedPlayers[downedUuid] ?: return

        // 取消救援任务
        downed.rescueTask?.cancel()
        downed.rescueTask = null

        // 移除救援BossBar
        downed.rescueBossBar?.removeAll()
        downed.rescueBossBar = null


        // 恢复倒地BossBar的显示
        if (downed.bossBar != null) {
            downed.bossBar?.isVisible = true
        }

        // 更新救援者的状态
        val rescuerUuid = downed.rescuerUuid
        if (rescuerUuid != null) {
            val rescuerState = teamManager.getPlayerState(rescuerUuid)
            if (rescuerState != null) {
                rescuerState.isHelping = false
            }
        }

        // 重置救援者
        downed.rescuerUuid = null
        downed.rescueStartTime = 0
    }

    /**
     * 检查玩家是否倒地
     */
    fun isPlayerDowned(playerUuid: UUID): Boolean {
        return downedPlayers.containsKey(playerUuid)
    }

    /**
     * 获取倒地玩家数据
     */
    fun getDownedPlayer(playerUuid: UUID): DownedPlayer? {
        return downedPlayers[playerUuid]
    }

    /**
     * 获取所有倒地玩家
     */
    fun getAllDownedPlayers(): List<DownedPlayer> {
        return downedPlayers.values.toList()
    }

    /**
     * 清理玩家数据
     */
    fun clearPlayerData(playerUuid: UUID) {
        downedPlayers.remove(playerUuid)
    }

    /**
     * 清理所有数据
     */
    fun clearAllData() {
        // 在清除数据前，先移除所有倒地玩家的效果
        downedPlayers.values.forEach { downedPlayer ->
            val player = Bukkit.getPlayer(downedPlayer.playerUuid)
            if (player != null && player.isOnline) {
                // 取消救援任务（如果正在进行）
                downedPlayer.rescueTask?.cancel()
                downedPlayer.rescueTask = null
                // 移除救援BossBar
                downedPlayer.rescueBossBar?.removeAll()
                downedPlayer.rescueBossBar = null
                // 移除倒地BossBar
                downedPlayer.bossBar?.removeAll()
                downedPlayer.bossBar = null
                // 移除倒地效果
                removeDownedEffects(player)
                // 更新PlayerState的倒地状态
                val playerState = teamManager.getPlayerState(downedPlayer.playerUuid)
                if (playerState != null) {
                    playerState.isDowned = false
                }
            }
        }
        // 清除所有倒地玩家数据
        downedPlayers.clear()
    }

    /**
     * 清理指定实例中的倒地玩家数据
     * @param instanceFullId 实例完整ID
     */
    fun clearForInstance(instanceFullId: String) {
        // 获取玩家管理器
        val playerManager = teamManager.playerManager

        // 找出所有在该实例中的倒地玩家
        val playersToRemove = downedPlayers.filter { (playerUuid, _) ->
            val player = Bukkit.getPlayer(playerUuid)
            if (player != null && player.isOnline) {
                val instance = playerManager.getPlayerInstance(player)
                instance?.getFullId() == instanceFullId
            } else {
                false
            }
        }.keys.toList()

        // 清理这些倒地玩家
        playersToRemove.forEach { playerUuid ->
            val downedPlayer = downedPlayers[playerUuid]
            if (downedPlayer != null) {
                val player = Bukkit.getPlayer(playerUuid)
                if (player != null && player.isOnline) {
                    // 取消救援任务（如果正在进行）
                    downedPlayer.rescueTask?.cancel()
                    downedPlayer.rescueTask = null
                    // 移除救援BossBar
                    downedPlayer.rescueBossBar?.removeAll()
                    downedPlayer.rescueBossBar = null
                    // 移除倒地BossBar
                    downedPlayer.bossBar?.removeAll()
                    downedPlayer.bossBar = null
                    // 移除倒地效果
                    removeDownedEffects(player)
                    // 更新PlayerState的倒地状态
                    val playerState = teamManager.getPlayerState(playerUuid)
                    if (playerState != null) {
                        playerState.isDowned = false
                    }
                }
                // 从倒地列表中移除
                downedPlayers.remove(playerUuid)
            }
        }
    }

    /**
     * 玩家死亡时清理倒地状态
     */
    fun onPlayerDeath(player: Player) {
        val playerUuid = player.uniqueId
        val downedPlayer = downedPlayers[playerUuid] ?: return

        // 标记为放弃
        downedPlayer.isGivingUp = true

        // 更新PlayerState的倒地状态和死亡状态
        val playerState = teamManager.getPlayerState(playerUuid)
        if (playerState != null) {
            playerState.isDowned = false
            playerState.isDead = true
        }

        // 取消救援任务（如果正在进行）
        downedPlayer.rescueTask?.cancel()
        downedPlayer.rescueTask = null
        // 移除救援BossBar
        downedPlayer.rescueBossBar?.removeAll()
        downedPlayer.rescueBossBar = null

        // 移除倒地BossBar
        if (downedPlayer.bossBar != null) {
            downedPlayer.bossBar?.removeAll()
            downedPlayer.bossBar = null
        }

        // 从倒地列表中移除（在移除倒地效果之前）
        downedPlayers.remove(playerUuid)

        // 移除倒地效果
        removeDownedEffects(player)

        // 检查玩家所在的队伍是否所有成员都已死亡，如果是则解散队伍
        val playerTeam = teamManager.getPlayerTeam(playerUuid)
        if (playerTeam != null) {
            teamManager.checkAndDisbandTeamIfAllDead(playerTeam.teamId)
        }
    }

    /**
     * 处理呼救
     */
    private fun handleDownedHelp(player: Player) {
        // 检查玩家是否倒地
        if (!isPlayerDowned(player.uniqueId)) {
            player.sendLangSys(plugin, "downed.downed")
            return
        }

        // 发送呼救消息
        val location = player.location
        val locationText = "X:${location.blockX} Y:${location.blockY} Z:${location.blockZ}"
        val playerUuid = player.uniqueId

        // 获取倒地玩家的队伍
        val downedTeam = teamManager.getPlayerTeam(playerUuid)

        if (downedTeam == null || downedTeam.getMemberCount() == 1) {
            // 单人队伍成员倒地：只发送给周围30格内的独狼或队伍未满的玩家
            val nearbyPlayers = teamManager.getNearbyPlayers(player, 30)

            if (nearbyPlayers.isNullOrEmpty()) {
                player.sendLangSys(plugin, "downed.no-nearby-players")
                return
            }

            // 筛选出可以救援的玩家（独狼或队伍未满）
            val eligiblePlayers = nearbyPlayers.filter { nearbyPlayer ->
                val nearbyTeam = teamManager.getPlayerTeam(nearbyPlayer.uniqueId)
                // 独狼玩家或队伍未满的玩家可以接收求救信息
                nearbyTeam == null || nearbyTeam.getMemberCount() == 1 || !nearbyTeam.isFull()
            }

            // 发送呼救消息给符合条件的玩家
            eligiblePlayers.forEach { nearbyPlayer ->
                nearbyPlayer.sendLangBroad(plugin, "team.help-received", 
                    "player" to player.name,
                    "x" to location.blockX,
                    "y" to location.blockY,
                    "z" to location.blockZ
                )
            }

            // 发送呼救成功消息给倒地玩家
            player.sendLangSys(plugin, "team.help-sent-solo")
        } else {
            // 多人队伍成员倒地：发送给队友（不发给周围玩家）
            val teammates = downedTeam.members.filter { it != playerUuid }

            if (teammates.isEmpty()) {
                player.sendLangSys(plugin, "downed.no-teammates")
                return
            }

            // 发送呼救消息给队友
            teammates.forEach { teammateUuid ->
                val teammate = org.bukkit.Bukkit.getPlayer(teammateUuid)
                if (teammate != null && teammate.isOnline) {
                    teammate.sendLangTeam(plugin, "team.help-received",
                        "player" to player.name,
                        "x" to location.blockX,
                        "y" to location.blockY,
                        "z" to location.blockZ
                    )
                }
            }

            // 发送呼救成功消息给倒地玩家
            player.sendLangSys(plugin, "team.help-sent-team")
        }
    }

    /**
     * 处理放弃救援
     */
    private fun handleDownedGiveUp(player: Player) {
        // 检查玩家是否倒地
        if (!isPlayerDowned(player.uniqueId)) {
            player.sendLangSys(plugin, "downed.downed")
            return
        }
        // 放弃救援
        giveUp(player)
    }

}
