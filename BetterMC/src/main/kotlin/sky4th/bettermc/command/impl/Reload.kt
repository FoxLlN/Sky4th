package sky4th.bettermc.command.impl

import sky4th.bettermc.command.BetterMCContext
import sky4th.bettermc.command.FeatureManager
import sky4th.bettermc.config.ConfigManager
import org.bukkit.command.CommandSender
import sky4th.core.api.LanguageAPI
import sky4th.bettermc.util.LanguageUtil.sendLang
import sky4th.bettermc.BetterMC

/** 子命令：/bettermc reload */
fun runReload(sender: CommandSender) {
    val ctx = BetterMCContext.getOrThrow()
    if (!sender.hasPermission("bettermc.admin")) {
        sender.sendLang(BetterMC.instance, "command.no-permission")
        return
    }

    ConfigManager.reloadConfig()
    FeatureManager.loadFeatureToggles()
    sender.sendLang(BetterMC.instance, "command.reload-success")
}
