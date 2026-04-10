package sky4th.bettermc.command.impl

import sky4th.bettermc.command.BetterMCContext
import sky4th.bettermc.command.FeatureManager
import org.bukkit.command.CommandSender
import sky4th.core.api.LanguageAPI
import sky4th.bettermc.util.LanguageUtil.sendLang
import sky4th.bettermc.BetterMC

/** 子命令：/bettermc list */
fun runList(sender: CommandSender) {
    val ctx = BetterMCContext.getOrThrow()
    if (!sender.hasPermission("bettermc.admin")) {
        sender.sendLang(BetterMC.instance, "command.no-permission")
        return
    }

    sender.sendLang(BetterMC.instance, "command.list.header")
    FeatureManager.getFeatures().forEach { (key, descriptionKey) ->
        val statusKey = if (FeatureManager.isFeatureEnabled(key)) "command.list.enabled" else "command.list.disabled"
        val status = LanguageAPI.getText(BetterMC.instance, statusKey)
        val description = LanguageAPI.getText(BetterMC.instance, descriptionKey)  // 在这里本地化
        
        // 使用 sendLang 发送消息
        sender.sendLang(BetterMC.instance, "command.list.format", 
            "key" to key, 
            "description" to description,
            "status" to status)
    }
}
