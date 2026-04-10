package sky4th.core.command.impl

import sky4th.core.api.EquipmentAPI
import sky4th.core.command.SkyCoreContext
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
// EquipmentRegistry由EquipmentAffix插件提供，不应在SkyCore中直接引用

/** 子命令：/sky equipment create */
fun runEquipment(sender: CommandSender, args: Array<out String>) {
    val ctx = SkyCoreContext.getOrThrow()
    if (!sender.hasPermission("skycore.equipment")) {
        sender.sendMessage("§c你没有权限执行此操作！")
        return
    }

    if (args.size < 2) {
        sender.sendMessage("§c用法: /sky equipment <create>")
        return
    }

    when (args[1].lowercase()) {
        "create" -> {
            if (sender !is Player) {
                sender.sendMessage("§c只有玩家可以使用此命令！")
                return
            }
            if (args.size < 3) {
                sender.sendMessage("§c用法: /sky equipment create <装备ID>")
                return
            }

            val equipmentId = args[2]
            val equipment = EquipmentAPI.createEquipment(equipmentId)

            if (equipment != null) {
                sender.inventory.addItem(equipment)
                sender.sendMessage("§a成功创建装备: §e${equipmentId}")
            } else {
                // 检查是否是词条模板请求 (格式: affixId_template 或 affixId_模板)
                if (equipmentId.endsWith("_template") || equipmentId.endsWith("_模板")) {
                    val templateId = equipmentId.removeSuffix("_template").removeSuffix("_模板")
                    val template = EquipmentAPI.createAffixTemplate(templateId)
                    
                    if (template != null) {
                        sender.inventory.addItem(template)
                        sender.sendMessage("§a成功创建词条模板: §e${templateId}")
                    } else {
                        sender.sendMessage("§c词条模板不存在: §e$templateId")
                    }
                } else {
                    sender.sendMessage("§c装备不存在: §e$equipmentId")
                }
            }
        }
        else -> {
            sender.sendMessage("§c用法: /sky equipment create")
        }
    }
}
