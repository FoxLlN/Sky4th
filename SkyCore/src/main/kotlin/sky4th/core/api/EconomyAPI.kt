package sky4th.core.api

import sky4th.core.economy.EconomyProvider
import sky4th.core.economy.EconomyService
import org.bukkit.entity.Player
import java.util.*

/**
 * 经济系统对外 API（唯一入口）
 *
 * 所有调用统一委托给 EconomyService，未注册时读操作返回默认值、写操作可抛异常。
 * 提供者由 SkyCore 在初始化时自动注册（CoreEconomyProvider），子插件通过本 API 使用经济。
 */
object EconomyAPI {

    fun isAvailable(): Boolean = EconomyService.isRegistered()
    fun getProvider(): EconomyProvider? = EconomyService.getProvider()
    fun registerProvider(provider: EconomyProvider) = EconomyService.registerProvider(provider)
    fun unregisterProvider() = EconomyService.unregisterProvider()

    fun getBalance(player: Player): Double = EconomyService.getBalance(player)
    fun getBalance(uuid: UUID): Double = EconomyService.getBalance(uuid)
    fun hasEnough(player: Player, amount: Double): Boolean = EconomyService.hasEnough(player, amount)
    fun hasEnough(uuid: UUID, amount: Double): Boolean = EconomyService.hasEnough(uuid, amount)
    fun deposit(player: Player, amount: Double): Double = EconomyService.deposit(player, amount)
    fun deposit(uuid: UUID, amount: Double): Double = EconomyService.deposit(uuid, amount)
    fun withdraw(player: Player, amount: Double): Boolean = EconomyService.withdraw(player, amount)
    fun withdraw(uuid: UUID, amount: Double): Boolean = EconomyService.withdraw(uuid, amount)
    fun forceWithdraw(player: Player, amount: Double): Double = EconomyService.forceWithdraw(player, amount)
    fun forceWithdraw(uuid: UUID, amount: Double): Double = EconomyService.forceWithdraw(uuid, amount)

    fun setBalance(player: Player, amount: Double) {
        if (!EconomyService.isRegistered()) throw IllegalStateException("经济系统未注册")
        EconomyService.setBalance(player, amount)
    }

    fun setBalance(uuid: UUID, amount: Double) {
        if (!EconomyService.isRegistered()) throw IllegalStateException("经济系统未注册")
        EconomyService.setBalance(uuid, amount)
    }

    fun require(player: Player, amount: Double, action: () -> Unit): Boolean =
        EconomyService.require(player, amount, action)

    fun getCurrencyName(): String = EconomyService.getCurrencyName()
    fun getCurrencyNamePlural(): String = EconomyService.getCurrencyNamePlural()
    fun format(amount: Double): String = EconomyService.format(amount)
}
