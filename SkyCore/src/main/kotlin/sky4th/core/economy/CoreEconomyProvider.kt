package sky4th.core.economy

import org.bukkit.entity.Player
import sky4th.core.service.PlayerService
import java.util.*

/**
 * Core 默认经济提供者（EconomyProvider 的默认实现）
 *
 * 数据来源：PlayerService → PlayerData.economy（信用点等），无额外存储。
 * 在 SkyCore 初始化数据库与玩家服务后自动注册，地牢/任务等可直接用 EconomyAPI，
 * CreditEconomy 仅需在此基础上提供命令与扩展，无需再注册提供者。
 */
class CoreEconomyProvider(private val playerService: PlayerService) : EconomyProvider {

    override fun getBalance(player: Player): Double {
        return playerService.getPlayerData(player).economy.credits
    }

    override fun getBalance(uuid: UUID): Double {
        return playerService.getPlayerData(uuid)?.economy?.credits ?: 0.0
    }

    override fun hasEnough(player: Player, amount: Double): Boolean {
        return playerService.getPlayerData(player).economy.hasEnough(amount)
    }

    override fun hasEnough(uuid: UUID, amount: Double): Boolean {
        val data = playerService.getPlayerData(uuid) ?: return false
        return data.economy.hasEnough(amount)
    }

    override fun deposit(player: Player, amount: Double): Double {
        if (amount <= 0) return 0.0
        val data = playerService.getPlayerData(player)
        val actual = data.economy.addCredits(amount)
        playerService.savePlayerData(data)
        return actual
    }

    override fun deposit(uuid: UUID, amount: Double): Double {
        if (amount <= 0) return 0.0
        val data = playerService.getPlayerData(uuid) ?: return 0.0
        val actual = data.economy.addCredits(amount)
        playerService.savePlayerData(data)
        return actual
    }

    override fun withdraw(player: Player, amount: Double): Boolean {
        if (amount <= 0) return true
        val data = playerService.getPlayerData(player)
        val ok = data.economy.removeCredits(amount)
        if (ok) playerService.savePlayerData(data)
        return ok
    }

    override fun withdraw(uuid: UUID, amount: Double): Boolean {
        if (amount <= 0) return true
        val data = playerService.getPlayerData(uuid) ?: return false
        val ok = data.economy.removeCredits(amount)
        if (ok) playerService.savePlayerData(data)
        return ok
    }

    override fun forceWithdraw(player: Player, amount: Double): Double {
        if (amount <= 0) return 0.0
        val data = playerService.getPlayerData(player)
        val actual = data.economy.forceRemoveCredits(amount)
        playerService.savePlayerData(data)
        return actual
    }

    override fun forceWithdraw(uuid: UUID, amount: Double): Double {
        if (amount <= 0) return 0.0
        val data = playerService.getPlayerData(uuid) ?: return 0.0
        val actual = data.economy.forceRemoveCredits(amount)
        playerService.savePlayerData(data)
        return actual
    }

    override fun setBalance(player: Player, amount: Double) {
        val data = playerService.getPlayerData(player)
        data.economy.credits = amount.coerceAtLeast(0.0)
        playerService.savePlayerData(data)
    }

    override fun setBalance(uuid: UUID, amount: Double) {
        val data = playerService.getPlayerData(uuid) ?: return
        data.economy.credits = amount.coerceAtLeast(0.0)
        playerService.savePlayerData(data)
    }

    override fun getCurrencyName(): String = "信用点"
    override fun getCurrencyNamePlural(): String = "信用点"
    override fun format(amount: Double): String = "%.2f".format(amount)
}
