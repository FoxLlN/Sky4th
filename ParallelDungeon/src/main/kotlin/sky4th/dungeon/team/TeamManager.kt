
package sky4th.dungeon.team

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import sky4th.dungeon.util.LanguageUtil.sendLangSys
import sky4th.dungeon.Dungeon
import sky4th.dungeon.model.DungeonInstance
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 队伍数据类
 */
data class Team(
    val teamId: String,
    val instanceFullId: String,
    val leader: UUID,
    val members: MutableSet<UUID>,
    val maxMembers: Int = 3,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * 检查队伍是否已满
     */
    fun isFull(): Boolean = members.size >= maxMembers

    /**
     * 检查玩家是否在队伍中
     */
    fun hasMember(playerUuid: UUID): Boolean = members.contains(playerUuid)

    /**
     * 获取队伍成员数量
     */
    fun getMemberCount(): Int = members.size

    /**
     * 添加成员
     */
    fun addMember(playerUuid: UUID): Boolean {
        if (isFull() || hasMember(playerUuid)) return false
        members.add(playerUuid)
        return true
    }

    /**
     * 移除成员
     */
    fun removeMember(playerUuid: UUID): Boolean {
        if (!hasMember(playerUuid)) return false
        members.remove(playerUuid)
        return true
    }
}

/**
 * 玩家状态数据类
 */
data class PlayerState(
    val playerUuid: UUID,
    var teamId: String? = null,
    var deathCount: Int = 0,
    var isDowned: Boolean = false,
    var isHelping: Boolean = false,
    var downedLocation: Location? = null,
    var downedTime: Long = 0,
    var isDead: Boolean = false
)

/**
 * 队伍邀请数据类
 */
data class TeamInvite(
    val inviter: UUID,
    val target: UUID,
    val teamId: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 队伍管理器
 * 负责管理地牢中的临时队伍系统
 */
class TeamManager(
    private val plugin: sky4th.dungeon.Dungeon,
    val config: TeamConfig,
    playerManager: sky4th.dungeon.player.PlayerManager?
) {
    private var _playerManager = playerManager
    val playerManager: sky4th.dungeon.player.PlayerManager
        get() = _playerManager!!
    
    fun setPlayerManager(manager: sky4th.dungeon.player.PlayerManager) {
        _playerManager = manager
    }
    // 队伍ID计数器
    private val teamIdCounter = AtomicInteger(0)

    // 所有队伍 (teamId -> Team)
    private val teams: ConcurrentHashMap<String, Team> = ConcurrentHashMap()

    // 玩家状态 (playerUuid -> PlayerState)
    private val playerStates: ConcurrentHashMap<UUID, PlayerState> = ConcurrentHashMap()

    // 队伍邀请 (playerUuid -> TeamInvite)
    private val invites: ConcurrentHashMap<UUID, TeamInvite> = ConcurrentHashMap()

    /**
     * 创建新队伍
     */
    fun createTeam(leader: Player, instance: DungeonInstance): Team? {
        val leaderUuid = leader.uniqueId

        // 检查玩家是否已经在队伍中
        if (playerStates[leaderUuid]?.teamId != null) {
            leader.sendLangSys(plugin, "team.already-in-team")
            return null
        }

        val teamId = "team_${teamIdCounter.incrementAndGet()}"
        val team = Team(
            teamId = teamId,
            instanceFullId = instance.getFullId(),
            leader = leaderUuid,
            members = mutableSetOf(leaderUuid),
            maxMembers = config.maxTeamSize
        )

        teams[teamId] = team

        // 获取或创建玩家状态
        val playerState = playerStates[leaderUuid]
        if (playerState != null) {
            // 如果玩家状态已存在（已经被resetPlayerState重置过），只更新teamId
            playerState.teamId = teamId
        } else {
            // 如果玩家状态不存在，创建新的
            playerStates[leaderUuid] = PlayerState(leaderUuid, teamId)
        }

        return team
    }

    /**
     * 解散队伍
     */
    fun disbandTeam(teamId: String): Boolean {
        val team = teams.remove(teamId) ?: return false

        // 清除所有成员的队伍信息
        team.members.forEach { memberUuid ->
            playerStates[memberUuid]?.teamId = null
        }

        return true
    }

    /**
     * 添加玩家到队伍
     */
    fun addPlayerToTeam(player: Player, teamId: String): Boolean {
        val playerUuid = player.uniqueId
        val team = teams[teamId] ?: return false

        // 检查玩家是否已经在队伍中
        if (playerStates[playerUuid]?.teamId != null) {
            player.sendLangSys(plugin, "team.already-in-team")
            return false
        }

        // 检查玩家是否已经死亡
        if (playerStates[playerUuid]?.isDead == true) {
            player.sendLangSys(plugin, "team.player-is-dead")
            return false
        }

        // 检查队伍是否已满
        if (team.isFull()) {
            player.sendLangSys(plugin, "team.full")
            return false
        }

        // 添加成员
        if (!team.addMember(playerUuid)) {
            return false
        }

        // 更新玩家状态（保留原有的死亡次数和倒地状态）
        val existingState = playerStates[playerUuid]
        playerStates[playerUuid] = PlayerState(
            playerUuid = playerUuid,
            teamId = teamId,
            deathCount = existingState?.deathCount ?: 0,
            isDowned = existingState?.isDowned ?: false,
            isHelping = existingState?.isHelping ?: false,
            downedLocation = existingState?.downedLocation,
            downedTime = existingState?.downedTime ?: 0,
            isDead = existingState?.isDead ?: false
        )

        return true
    }

    /**
     * 从队伍中移除玩家
     */
    fun removePlayerFromTeam(playerUuid: UUID): Boolean {
        val state = playerStates[playerUuid] ?: return false
        val teamId = state.teamId ?: return false

        val team = teams[teamId] ?: return false

        // 移除成员
        team.removeMember(playerUuid)

        // 更新玩家状态
        state.teamId = null

        // 如果队长离开，转移队长或解散队伍
        if (team.leader == playerUuid) {
            if (team.members.isEmpty()) {
                // 队伍为空，解散队伍
                disbandTeam(teamId)
            } else {
                // 转移队长给第一个成员
                val newLeader = team.members.first()
                teams[teamId] = team.copy(leader = newLeader)
            }
        } else if (team.members.isEmpty()) {
            // 队伍为空，解散队伍
            disbandTeam(teamId)
        }

        // 检查队伍是否所有成员都已死亡或退出地牢
        checkAndDisbandTeamIfAllDead(teamId)

        return true
    }

    /**
     * 发送队伍邀请
     */
    fun sendInvite(inviter: Player, target: Player): Boolean {
        val inviterUuid = inviter.uniqueId
        val targetUuid = target.uniqueId

        // 检查邀请者是否在队伍中
        val inviterState = playerStates[inviterUuid]
        val teamId = inviterState?.teamId
        if (teamId == null) {
            inviter.sendLangSys(plugin, "team.not-in-team")
            return false
        }

        // 检查目标玩家是否已经死亡
        if (playerStates[targetUuid]?.isDead == true) {
            inviter.sendLangSys(plugin, "team.player-is-dead")
            return false
        }

        // 检查队伍是否已满
        val team = teams[teamId]
        if (team != null && team.isFull()) {
            inviter.sendLangSys(plugin, "team.full")
            return false
        }

        // 创建邀请
        val invite = TeamInvite(inviterUuid, targetUuid, teamId)
        invites[targetUuid] = invite

        // 发送消息给邀请者
        inviter.sendLangSys(plugin, "team.invite-sent", "player" to target.name)
        // 注意：邀请消息和按钮由 TeamCommand.runInvite() 发送，这里不重复发送

        return true
    }

    /**
     * 接受邀请
     */
    fun acceptInvite(player: Player): Boolean {
        val playerUuid = player.uniqueId
        val invite = invites.remove(playerUuid) ?: return false

        // 检查邀请是否过期（30秒）
        if (System.currentTimeMillis() - invite.timestamp > 30000) {
            player.sendLangSys(plugin, "team.invite-expired")
            return false
        }

        // 检查玩家是否已经死亡
        if (playerStates[playerUuid]?.isDead == true) {
            player.sendLangSys(plugin, "team.player-is-dead")
            return false
        }

        // 获取目标队伍
        val team = teams[invite.teamId]
        if (team == null) {
            player.sendLangSys(plugin, "team.not-exist")
            return false
        }

        // 如果玩家已在队伍中，先离开原队伍
        val oldState = playerStates[playerUuid]
        if (oldState?.teamId != null) {
            removePlayerFromTeam(playerUuid)
        }

        // 添加到新队伍
        if (!addPlayerToTeam(player, invite.teamId)) {
            return false
        }

        player.sendLangSys(plugin, "team.invite-accepted")

        return true
    }

    /**
     * 拒绝邀请
     */
    fun declineInvite(player: Player): Boolean {
        val playerUuid = player.uniqueId
        val invite = invites.remove(playerUuid) ?: return false

        player.sendLangSys(plugin, "team.invite-declined")

        // 通知邀请者
        Bukkit.getPlayer(invite.inviter)?.let { inviter ->
            inviter.sendLangSys(plugin, "team.invite-declined-by", "player" to player.name)
        }

        return true
    }

    /**
     * 处理点击邀请消息
     */
    fun handleInviteClick(player: Player, action: String): Boolean {
        return when (action.lowercase()) {
            "accept" -> acceptInvite(player)
            "decline" -> declineInvite(player)
            else -> false
        }
    }

    /**
     * 获取玩家的队伍
     */
    fun getPlayerTeam(playerUuid: UUID): Team? {
        val state = playerStates[playerUuid] ?: return null
        val teamId = state.teamId ?: return null
        return teams[teamId]
    }

    /**
     * 获取玩家的状态
     */
    fun getPlayerState(playerUuid: UUID): PlayerState? {
        return playerStates[playerUuid]
    }

    /**
     * 重置玩家状态（用于进入新地牢时）
     */
    fun resetPlayerState(playerUuid: UUID) {
        val state = playerStates[playerUuid]
        if (state != null) {
            state.isDead = false
            state.isDowned = false
            state.isHelping = false
            state.downedLocation = null
            state.downedTime = 0
            state.deathCount = 0
            state.teamId = null  // 清理旧实例的 teamId
        }
    }

    /**
     * 获取队伍成员
     */
    fun getTeamMembers(teamId: String): List<Player> {
        val team = teams[teamId] ?: return emptyList()
        return team.members.mapNotNull { Bukkit.getPlayer(it) }.filter { it.isOnline }
    }

    /**
     * 获取附近的玩家
     */
    fun getNearbyPlayers(player: Player, distance: Int): List<Player> {
        val location = player.location
        val distanceSquared = distance * distance

        return Bukkit.getOnlinePlayers()
            .filter { it.uniqueId != player.uniqueId }
            .filter { it.world == location.world }
            .filter { it.location.distanceSquared(location) <= distanceSquared }
    }

    /**
     * 清理玩家数据（玩家退出地牢时调用）
     */
    fun clearPlayerData(playerUuid: UUID) {
        // 移除邀请
        invites.remove(playerUuid)

        // 获取玩家状态
        val playerState = playerStates[playerUuid]

        // 检查玩家所在的队伍是否所有成员都已死亡或退出地牢
        if (playerState != null) {
            val teamId = playerState.teamId
            if (teamId != null) {
                checkAndDisbandTeamIfAllDead(teamId)
            }
        }

        // 清理玩家的 teamId，但保留玩家状态
        if (playerState != null) {
            playerState.teamId = null
        }
    }

    /**
     * 检查队伍中所有成员是否都已死亡或退出地牢
     * 如果是，则解散队伍
     */
    fun checkAndDisbandTeamIfAllDead(teamId: String) {
        val team = teams[teamId] ?: return

        // 检查队伍中是否还有存活的成员
        val hasAliveMember = team.members.any { memberUuid ->
            val memberState = playerStates[memberUuid]
            memberState != null && !memberState.isDead
        }

        // 如果没有存活的成员，解散队伍
        if (!hasAliveMember) {
            disbandTeam(teamId)
        }
    }

    /**
     * 清理实例数据（实例关闭时调用）
     */
    fun clearInstanceData(instanceFullId: String) {
        // 找出该实例的所有队伍
        val instanceTeams = teams.filter { it.value.instanceFullId == instanceFullId }

        // 解散这些队伍
        instanceTeams.keys.forEach { teamId ->
            disbandTeam(teamId)
        }

        // 清理不在任何地牢中的玩家状态
        cleanupPlayersNotInDungeon()
    }

    /**
     * 清理不在任何地牢中的玩家状态
     * 只清理不在队伍中的玩家，避免影响正在游戏中的玩家
     */
    private fun cleanupPlayersNotInDungeon() {
        val toRemove = mutableListOf<UUID>()

        playerStates.forEach { (playerUuid, playerState) ->
            // 只清理不在队伍中的玩家
            if (playerState.teamId == null) {
                // 检查玩家是否在地牢中
                val player = Bukkit.getPlayer(playerUuid)
                if (player == null || !player.isOnline || !playerManager.isPlayerInDungeon(player)) {
                    toRemove.add(playerUuid)
                }
            }
        }

        // 移除符合条件的玩家状态
        toRemove.forEach { playerUuid ->
            playerStates.remove(playerUuid)
        }

        if (toRemove.isNotEmpty()) {
            plugin.logger.info("清理了 ${toRemove.size} 个不在地牢中的玩家状态")
        }
    }

    /**
     * 清理所有数据
     */
    fun clearAllData() {
        teams.clear()
        playerStates.clear()
        invites.clear()
    }
}
