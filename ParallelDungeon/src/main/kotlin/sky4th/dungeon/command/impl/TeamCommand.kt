
package sky4th.dungeon.command.impl

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import sky4th.dungeon.Dungeon
import sky4th.dungeon.command.DungeonContext
import sky4th.dungeon.player.PlayerManager
import sky4th.dungeon.util.LanguageUtil.sendLang
import sky4th.dungeon.util.LanguageUtil.sendLangTeam
import sky4th.core.api.LanguageAPI

/**
 * 队伍命令实现
 */
object TeamCommand {

    /**
     * 邀请玩家加入队伍
     */
    fun runInvite(sender: Player, targetName: String) {
        val ctx = DungeonContext.get() ?: run {
            return
        }

        val teamManager = ctx.teamManager ?: run {
            sender.sendLang(ctx.plugin, "team.not-available")
            return
        }

        val playerManager = ctx.playerManager

        // 检查发送者是否在地牢中
        if (!playerManager.isPlayerInDungeon(sender)) {
            sender.sendLang(ctx.plugin, "command.not-in-dungeon")
            return
        }

        // 检查目标玩家是否存在
        val target = Bukkit.getPlayer(targetName)
        if (target == null || !target.isOnline) {
            sender.sendLang(ctx.plugin, "team.player-not-found", "player" to targetName)
            return
        }

        // 检查目标玩家是否在地牢中
        if (!playerManager.isPlayerInDungeon(target)) {
            sender.sendLang(ctx.plugin, "team.player-not-in-dungeon", "player" to targetName)
            return
        }

        // 检查是否在同一地牢实例中
        val senderInstance = playerManager.getPlayerInstance(sender)
        val targetInstance = playerManager.getPlayerInstance(target)
        if (senderInstance?.getFullId() != targetInstance?.getFullId()) {
            sender.sendLang(ctx.plugin, "team.player-not-in-instance", "player" to targetName)
            return
        }

        if (sender == target) {
            sender.sendLang(ctx.plugin, "team.cannot-invite-self")
            return
        }

        // 发送邀请
        teamManager.sendInvite(sender, target)
        
        // 发送带点击按钮的邀请消息
        val acceptButton = Component.text(LanguageAPI.getText(ctx.plugin, "team.invite-buttons-accept"))
            .clickEvent(ClickEvent.callback { audience ->
                if (audience is Player) { 
                    teamManager.acceptInvite(audience)
                }
            })
        
        val declineButton = Component.text(LanguageAPI.getText(ctx.plugin, "team.invite-buttons-decline"))
            .clickEvent(ClickEvent.callback { audience ->
                if (audience is Player) {
                    teamManager.declineInvite(audience)
                }
            })
        
        val inviteMessage = Component.text()
            .append(Component.text(LanguageAPI.getPrefix(ctx.plugin, "prefix-team")))
            .append(Component.text(LanguageAPI.getText(ctx.plugin, "team.invite-received", "player" to sender.name)))
            .append(Component.newline())
            .append(Component.text(LanguageAPI.getPrefix(ctx.plugin)))
            .append(Component.text(" "))
            .append(acceptButton)
            .append(Component.text(" "))
            .append(declineButton)
        
        target.sendMessage(inviteMessage)
    }

    /**
     * 查看队伍信息
     */
    fun runInfo(sender: Player) {
        val ctx = DungeonContext.get() ?: run {
            return
        }

        val teamManager = ctx.teamManager ?: run {
            sender.sendLang(ctx.plugin, "team.not-available")
            return
        }

        val playerManager = ctx.playerManager

        // 检查玩家是否在地牢中
        if (!playerManager.isPlayerInDungeon(sender)) {
            sender.sendLang(ctx.plugin, "command.not-in-dungeon")
            return
        }

        // 获取玩家的队伍
        val team = teamManager.getPlayerTeam(sender.uniqueId)
        if (team == null) {
            sender.sendLang(ctx.plugin, "team.not-in-team")
            return
        }

        // 显示队伍信息
        sender.sendLangTeam(ctx.plugin, "team.info.header")
        sender.sendLangTeam(ctx.plugin, "team.info.id", "id" to team.teamId)
        sender.sendLangTeam(ctx.plugin, "team.info.members", "count" to team.getMemberCount(), "max" to team.maxMembers)

        // 显示成员列表
        team.members.forEach { memberUuid ->
            val member = Bukkit.getPlayer(memberUuid)
            if (member != null && member.isOnline) {
                val state = teamManager.getPlayerState(memberUuid)
                val status = when {
                    state?.isHelping == true -> "§a救援中"
                    state?.isDowned == true -> "§c倒地"
                    state?.isDead == true -> "§4死亡"
                    else -> "§a正常"
                }
                sender.sendLangTeam(ctx.plugin, "team.info.member", 
                    "player" to member.name,
                    "status" to status
                )
            }
        }

        sender.sendLangTeam(ctx.plugin, "team.info.footer")
    }
}
