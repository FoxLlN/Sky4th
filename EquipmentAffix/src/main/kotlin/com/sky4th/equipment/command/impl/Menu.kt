
package com.sky4th.equipment.command.impl

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import com.sky4th.equipment.util.LanguageUtil.sendLang

/**
 * menu子命令实现
 * 打开指定的UI页面
 */
fun runMenu(plugin: com.sky4th.equipment.EquipmentAffix, sender: CommandSender, args: Array<out String>) {
    if (sender !is Player) {
        sender.sendLang(plugin, "command.player-only")
        return
    }
    if (args.size < 2) {
        sender.sendLang(plugin, "command.menu.usage")
        return
    }
    val pageId = args[1]
    sky4th.core.api.UIAPI.openUI(sender, pageId)
}
