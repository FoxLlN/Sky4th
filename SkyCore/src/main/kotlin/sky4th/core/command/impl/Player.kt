package sky4th.core.command.impl

import sky4th.core.api.PlayerAPI
import sky4th.core.api.PlayerAttributesAPI
import sky4th.core.command.SkyCoreContext
import org.bukkit.command.CommandSender

/** 子命令：/sky player <玩家> <info|economy|attributes> */
fun runPlayer(sender: CommandSender, args: Array<out String>) {
    val ctx = SkyCoreContext.getOrThrow()
    if (!sender.hasPermission("skycore.player")) {
        sender.sendMessage("§c你没有权限执行此操作！")
        return
    }

    if (args.size < 2) {
        sender.sendMessage("§c用法: /sky player <玩家> <info|economy|attributes>")
        return
    }

    val targetName = args[1]
    val targetPlayer = sender.server.getPlayer(targetName)

    if (targetPlayer == null) {
        sender.sendMessage("§c找不到玩家: $targetName")
        return
    }

    val player = targetPlayer
    val playerData = PlayerAPI.getPlayerData(player)

    when (args.getOrNull(2)?.lowercase()) {
        "info" -> {
            sender.sendMessage("§6===== §b玩家信息: ${player.name} §6=====")
            sender.sendMessage("§aUUID: §e${player.uniqueId}")
            sender.sendMessage("§a用户名: §e${playerData.identity.username}")
            sender.sendMessage("§a首次登录: §e${playerData.identity.firstLogin}")
            sender.sendMessage("§a最后登录: §e${playerData.identity.lastLogin}")
            sender.sendMessage("§a游戏时长: §e${playerData.identity.playTime.toMinutes()} 分钟")
        }
        "economy" -> {
            sender.sendMessage("§6===== §b经济信息: ${player.name} §6=====")
            sender.sendMessage("§a信用点: §e${String.format("%.2f", playerData.economy.credits)}")
            sender.sendMessage("§a今日获得: §e${String.format("%.2f", playerData.economy.dailyEarned)}")
            sender.sendMessage("§a今日消费: §e${String.format("%.2f", playerData.economy.dailySpent)}")
            sender.sendMessage("§a每日上限: §e${String.format("%.2f", playerData.economy.dailyLimit)}")
        }
        "attributes" -> {
            val attributes = PlayerAttributesAPI.getAttributes(player.uniqueId)
            sender.sendMessage("§6===== §b玩家属性: ${player.name} §6=====")
            sender.sendMessage("§a最大生命值: §e${String.format("%.1f", attributes.maxHealth)}")
            sender.sendMessage("§a护甲值: §e${String.format("%.1f", attributes.armor)}")
            sender.sendMessage("§a闪避率: §e${String.format("%.2f%%", attributes.dodge * 100)}")
            sender.sendMessage("§a抗击退: §e${String.format("%.2f%%", attributes.knockbackResistance * 100)}")
            sender.sendMessage("§a饥饿消耗倍率: §e${String.format("%.2f", attributes.hungerConsumptionMultiplier)}x")
            sender.sendMessage("§a移动速度倍率: §e${String.format("%.2f", attributes.movementSpeedMultiplier)}x")
            sender.sendMessage("§a经验获取加成: §e${String.format("%.2f", attributes.expGainMultiplier)}x")
            sender.sendMessage("§a交易折扣: §e${String.format("%.2f%%", attributes.tradeDiscount * 100)}")
            sender.sendMessage("§a锻造成功率: §e${String.format("%.2f%%", attributes.forgingSuccessRate * 100)}")
            if (attributes.talents.isNotEmpty()) {
                sender.sendMessage("§a天赋: §e${attributes.talents.joinToString(", ")}")
            } else {
                sender.sendMessage("§a天赋: §7无")
            }
        }
        else -> {
            sender.sendMessage("§c用法: /sky player <玩家> <info|economy|attributes>")
        }
    }
}
