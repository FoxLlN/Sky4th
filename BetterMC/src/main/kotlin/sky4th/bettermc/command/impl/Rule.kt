package sky4th.bettermc.command.impl

import sky4th.bettermc.command.BetterMCContext
import sky4th.bettermc.command.FeatureManager
import org.bukkit.command.CommandSender
import sky4th.bettermc.util.LanguageUtil.sendLang
import sky4th.bettermc.BetterMC

/** 子命令：/bettermc rule <功能> <true|false> */
fun runRule(sender: CommandSender, args: Array<out String>) {
    val ctx = BetterMCContext.getOrThrow()
    if (!sender.hasPermission("bettermc.admin")) {
        sender.sendLang(BetterMC.instance, "command.no-permission")
        return
    }

    if (args.size < 3) {
        sender.sendLang(BetterMC.instance, "command.rule.usage")
        sender.sendLang(BetterMC.instance, "command.rule.hint")
        return
    }

    val feature = args[1].lowercase()
    if (!FeatureManager.getFeatures().containsKey(feature)) {
        sender.sendLang(BetterMC.instance, "command.rule.not-found", "feature" to feature)
        sender.sendLang(BetterMC.instance, "command.rule.hint")
        return
    }

    val enabled = when (args[2].lowercase()) {
        "true", "on", "yes", "1" -> true
        "false", "off", "no", "0" -> false
        else -> {
            sender.sendLang(BetterMC.instance, "command.rule.invalid-value", "value" to args[2])
            sender.sendLang(BetterMC.instance, "command.rule.valid-values")
            return
        }
    }

    FeatureManager.setFeatureEnabled(feature, enabled)
    FeatureManager.saveFeatureToggles()
    val messageKey = if (enabled) "command.rule.enabled" else "command.rule.disabled"
    sender.sendLang(BetterMC.instance, messageKey, "feature" to feature)
}
