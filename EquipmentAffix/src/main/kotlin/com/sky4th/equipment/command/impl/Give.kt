package com.sky4th.equipment.command.impl

import com.sky4th.equipment.EquipmentAffix
import com.sky4th.equipment.manager.EquipmentManager
import com.sky4th.equipment.util.LanguageUtil.sendLang
import org.bukkit.entity.Player

/**
 * 子命令：/equipment give
 */
fun runGive(plugin: EquipmentAffix, sender: Player, args: Array<out String>) {
    if (args.size < 2) {
        sender.sendLang(plugin, "command.equipment.give.usage")
        return
    }

    // 检查是否是 affix 子命令
    if (args[1].lowercase() == "affix") {
        if (args.size < 3) {
            sender.sendLang(plugin, "command.equipment.give.affix-usage")
            return
        }
        
        val affixId = args[2]
        // 尝试创建词条锻造模版
        val template = com.sky4th.equipment.loader.AffixTemplateLoader.getAffixTemplate(affixId)
        
        if (template != null) {
            sender.inventory.addItem(template)
            // 使用LegacyComponentSerializer正确序列化Component，获取纯文本名称
            val templateName = template.itemMeta?.displayName()?.let {
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(it)
            } ?: affixId
            sender.sendLang(plugin, "command.equipment.give.affix-success", "template_name" to templateName)
        } else {
            sender.sendLang(plugin, "command.equipment.give.affix-not-found", "affix_id" to affixId)
        }
        return
    }

    val equipmentId = args[1]
    val item = EquipmentManager.createEquipment(equipmentId)

    if (item == null) {
        sender.sendLang(plugin, "command.equipment.give.not-found",
            "equipment_id" to equipmentId)
        return
    }

    sender.inventory.addItem(item)
    // 使用LegacyComponentSerializer正确序列化Component，获取纯文本名称
    val equipmentName = item.itemMeta?.displayName()?.let {
        net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(it)
    } ?: equipmentId
    sender.sendLang(plugin, "command.equipment.give.success",
        "equipment_name" to equipmentName)
}
