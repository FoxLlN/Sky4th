
package sky4th.bettervillage.command.impl

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import sky4th.bettervillage.BetterVillage
import sky4th.bettervillage.manager.VillagerManager
import sky4th.bettervillage.util.LanguageUtil.sendLang

/**
 * 村民命令处理
 * 
 * 子命令：
 * - upgrade [level]: 升级最近的村民
 * - refresh: 刷新最近的村民交易
 * - info: 查看最近的村民信息
 */
class Villager {

    fun execute(sender: CommandSender, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendLang(BetterVillage.instance, "command.player_only")
            return true
        }

        if (args.isEmpty()) {
            sender.sendLang(BetterVillage.instance, "command.villager.usage")
            return true
        }

        when (args[0].lowercase()) {
            "refresh" -> handleRefresh(sender)
            "info" -> handleInfo(sender)
            else -> {
                sender.sendLang(BetterVillage.instance, "command.villager.unknown_subcommand", "subcommand" to args[0])
                sender.sendLang(BetterVillage.instance, "command.villager.usage")
            }
        }

        return true
    }

    /**
     * 处理刷新命令
     */
    private fun handleRefresh(player: Player) {
        // 获取最近的村民
        val villager = getNearestVillager(player) ?: run {
            player.sendLang(BetterVillage.instance, "villager.not_found")
            return
        }

        // 刷新交易
        VillagerManager.refreshTrades(player, villager)
    }

    /**
     * 处理信息查询命令
     */
    private fun handleInfo(player: Player) {
        // 获取最近的村民
        val villager = getNearestVillager(player) ?: run {
            player.sendLang(BetterVillage.instance, "villager.not_found")
            return
        }

        // 获取村民信息
        VillagerManager.getVillagerInfo(player, villager)
    }

    /**
     * 获取玩家最近的村民
     * 
     * @param player 玩家
     * @return 最近的村民，如果未找到则返回null
     */
    private fun getNearestVillager(player: Player): Villager? {
        val nearbyEntities = player.getNearbyEntities(10.0, 10.0, 10.0)
        return nearbyEntities.filterIsInstance<Villager>().minByOrNull { 
            it.location.distance(player.location) 
        }
    }

    fun tabComplete(sender: CommandSender, args: Array<String>): List<String>? {
        if (args.size == 1) {
            return listOf("upgrade", "refresh", "info").filter { 
                it.startsWith(args[0], ignoreCase = true) 
            }
        }

        if (args.size == 2 && args[0].equals("upgrade", ignoreCase = true)) {
            return listOf("1", "2", "3", "4", "5").filter { 
                it.startsWith(args[1]) 
            }
        }

        return null
    }
}
