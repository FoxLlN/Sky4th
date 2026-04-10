package sky4th.core.economy

import org.bukkit.entity.Player
import java.util.*

/**
 * 经济服务（内部单例）
 *
 * 职责：持有唯一 EconomyProvider、注册/注销、所有调用统一经此委托并处理未注册时的默认值。
 * 不对外暴露；对外统一使用 sky4th.core.api.EconomyAPI。
 */
object EconomyService {
    private var provider: EconomyProvider? = null

    fun registerProvider(p: EconomyProvider) {
        provider = p
    }

    fun unregisterProvider() {
        provider = null
    }

    fun getProvider(): EconomyProvider? = provider
    fun isRegistered(): Boolean = provider != null

    fun getBalance(player: Player): Double = provider?.getBalance(player) ?: 0.0
    fun getBalance(uuid: UUID): Double = provider?.getBalance(uuid) ?: 0.0
    fun hasEnough(player: Player, amount: Double): Boolean = provider?.hasEnough(player, amount) ?: false
    fun hasEnough(uuid: UUID, amount: Double): Boolean = provider?.hasEnough(uuid, amount) ?: false
    fun deposit(player: Player, amount: Double): Double = provider?.deposit(player, amount) ?: 0.0
    fun deposit(uuid: UUID, amount: Double): Double = provider?.deposit(uuid, amount) ?: 0.0
    fun withdraw(player: Player, amount: Double): Boolean = provider?.withdraw(player, amount) ?: false
    fun withdraw(uuid: UUID, amount: Double): Boolean = provider?.withdraw(uuid, amount) ?: false

    fun setBalance(player: Player, amount: Double) {
        provider?.setBalance(player, amount)
    }

    fun setBalance(uuid: UUID, amount: Double) {
        provider?.setBalance(uuid, amount)
    }

    fun require(player: Player, amount: Double, action: () -> Unit): Boolean {
        if (hasEnough(player, amount) && withdraw(player, amount)) {
            action()
            return true
        }
        return false
    }

    fun getCurrencyName(): String = provider?.getCurrencyName() ?: "信用点"
    fun getCurrencyNamePlural(): String = provider?.getCurrencyNamePlural() ?: "信用点"
    fun format(amount: Double): String = provider?.format(amount) ?: "%.2f".format(amount)
}
