package sky4th.core.command.impl

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import sky4th.core.api.MarkAPI
import sky4th.core.command.SkyCoreContext
import org.bukkit.Bukkit

/**
 * 子命令：/sky mark <玩家|@nearest> <物品> [持续时间(秒)]
 * 给玩家或最近的实体添加一个标签
 */
fun runMark(sender: CommandSender, args: Array<out String>) {
    val ctx = SkyCoreContext.getOrThrow()
    if (!sender.hasPermission("skycore.mark")) {
        sender.sendMessage("§c你没有权限执行此操作！")
        return
    }

    if (args.size < 3) {
        sender.sendMessage("§c用法: /sky mark <玩家|@nearest> <物品> [持续时间(秒)]")
        sender.sendMessage("§7示例: /sky mark Steve DIAMOND 10")
        sender.sendMessage("§7示例: /sky mark @nearest REDSTONE 5")
        return
    }

    val targetArg = args[1]
    val materialName = args[2]
    val duration = if (args.size > 3) {
        args[3].toLongOrNull() ?: 10L
    } else {
        10L
    }

    // 解析物品类型
    val material = try {
        Material.valueOf(materialName.uppercase())
    } catch (e: IllegalArgumentException) {
        sender.sendMessage("§c无效的物品类型: $materialName")
        return
    }

    // 获取目标实体
    val target: LivingEntity = when {
        targetArg.equals("@nearest", ignoreCase = true) -> {
            if (sender !is Player) {
                sender.sendMessage("§c只有玩家可以使用 @nearest 参数")
                return
            }
            val nearest = sender.getNearbyEntities(10.0, 10.0, 10.0)
                .filterIsInstance<LivingEntity>()
                .filter { it != sender }
                .minByOrNull { it.location.distance(sender.location) }

            if (nearest == null) {
                sender.sendMessage("§c附近没有找到实体")
                return
            }
            nearest
        }
        else -> {
            val player = Bukkit.getPlayer(targetArg)
            if (player == null) {
                sender.sendMessage("§c找不到玩家: $targetArg")
                return
            }
            player
        }
    }

    // 创建标记
    val markId = "mark_${target.uniqueId}_${System.currentTimeMillis()}"
    val itemStack = org.bukkit.inventory.ItemStack(material)
    val displayId = MarkAPI.createMark(target, markId, itemStack, showToAllPlayers = true, duration = duration)

    if (displayId == -1) {
        sender.sendMessage("§c创建标记失败")
        return
    }

    sender.sendMessage("§a已为 §e${target.name} §a添加标记: §b${material.name}")
    sender.sendMessage("§7持续时间: §e${duration}秒")
    // 注意：过期时间的处理由MarkManager内部自动管理，无需在此处设置定时任务
}
