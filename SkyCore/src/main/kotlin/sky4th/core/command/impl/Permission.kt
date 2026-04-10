package sky4th.core.command.impl

import sky4th.core.api.PlayerPermissionsAPI
import sky4th.core.command.SkyCoreContext
import org.bukkit.command.CommandSender

/** 子命令：/sky permission <add|remove> <玩家> <权限名> */
fun runPermission(sender: CommandSender, args: Array<out String>) {
    val ctx = SkyCoreContext.getOrThrow()
    if (!sender.hasPermission("skycore.permission")) {
        sender.sendMessage("§c你没有权限执行此操作！")
        return
    }

    if (args.size < 2) {
        sender.sendMessage("§c用法: /sky permission <add|remove> <玩家> <权限名>")
        return
    }

    when (args[1].lowercase()) {
        "add" -> {
            if (args.size < 4) {
                sender.sendMessage("§c用法: /sky permission add <玩家> <权限名>")
                return
            }

            val targetName = args[2]
            val permission = args[3]
            val targetPlayer = sender.server.getPlayer(targetName)

            if (targetPlayer == null) {
                sender.sendMessage("§c找不到玩家: $targetName")
                return
            }

            val success = PlayerPermissionsAPI.addPermission(targetPlayer, permission)
            if (success) {
                sender.sendMessage("§a已成功给玩家 ${targetPlayer.name} 添加权限: $permission")
            } else {
                sender.sendMessage("§e玩家 ${targetPlayer.name} 已拥有权限: $permission")
            }
        }
        "remove" -> {
            if (args.size < 4) {
                sender.sendMessage("§c用法: /sky permission remove <玩家> <权限名>")
                return
            }

            val targetName = args[2]
            val permission = args[3]
            val targetPlayer = sender.server.getPlayer(targetName)

            if (targetPlayer == null) {
                sender.sendMessage("§c找不到玩家: $targetName")
                return
            }

            val success = PlayerPermissionsAPI.removePermission(targetPlayer, permission)
            if (success) {
                sender.sendMessage("§a已成功移除玩家 ${targetPlayer.name} 的权限: $permission")
            } else {
                sender.sendMessage("§e玩家 ${targetPlayer.name} 没有权限: $permission")
            }
        }
        else -> {
            sender.sendMessage("§c用法: /sky permission <add|remove> <玩家> <权限名>")
        }
    }
}
