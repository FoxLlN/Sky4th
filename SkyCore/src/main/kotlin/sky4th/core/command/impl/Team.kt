package sky4th.core.command.impl

import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import sky4th.core.command.SkyCoreContext
import sky4th.core.mark.MarkManager

/**
 * 子命令：/sky team <join|leave|create> [队伍名]
 * 自定义组队指令，替代原版的/team指令
 * 加入队伍后自动隐藏队伍标签，只显示自定义名字标签
 */
fun runTeam(sender: CommandSender, args: Array<out String>) {
    val ctx = SkyCoreContext.getOrThrow()
    if (sender !is Player) {
        sender.sendMessage("§c只有玩家可以执行此命令！")
        return
    }

    if (args.size < 2) {
        sender.sendMessage("§c用法: /sky team <join|leave|create> [队伍名]")
        sender.sendMessage("§7示例: /sky team join red")
        sender.sendMessage("§7示例: /sky team leave")
        sender.sendMessage("§7示例: /sky team create blue")
        return
    }

    val action = args[1].lowercase()
    val scoreboard = sender.server.scoreboardManager.mainScoreboard

    when (action) {
        "join" -> {
            if (args.size < 3) {
                sender.sendMessage("§c请指定要加入的队伍名称")
                return
            }
            val teamName = args[2]
            val team = scoreboard.getTeam(teamName)
            if (team == null) {
                sender.sendMessage("§c队伍不存在: $teamName")
                return
            }

            // 将玩家加入队伍
            team.addEntry(sender.name)
            sender.sendMessage("§a已加入队伍: §e$teamName")

            // 为玩家创建自定义名字标签（隐藏原版队伍标签）
            sender.isCustomNameVisible = false
        }
        "leave" -> {
            // 获取玩家当前所在的队伍
            val team = scoreboard.getEntryTeam(sender.name)
            if (team == null) {
                sender.sendMessage("§c你不在任何队伍中")
                return
            }

            // 将玩家从队伍中移除
            team.removeEntry(sender.name)
            sender.sendMessage("§a已离开队伍: §e${team.name}")
        }
        "create" -> {
            if (args.size < 3) {
                sender.sendMessage("§c请指定要创建的队伍名称")
                return
            }
            val teamName = args[2]

            // 检查队伍是否已存在
            if (scoreboard.getTeam(teamName) != null) {
                sender.sendMessage("§c队伍已存在: $teamName")
                return
            }

            // 创建新队伍
            val team = scoreboard.registerNewTeam(teamName)
            team.addEntry(sender.name)
            sender.sendMessage("§a已创建队伍: §e$teamName")

            // 为玩家创建自定义名字标签（隐藏原版队伍标签）
            sender.isCustomNameVisible = false
        }
        else -> {
            sender.sendMessage("§c未知的操作: $action")
            sender.sendMessage("§7可用操作: join, leave, create")
        }
    }
}
