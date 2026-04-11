package sky4th.core.economy

import org.bukkit.entity.Player
import java.util.*

/**
 * 经济系统提供者接口（economy 包唯一契约）
 *
 * 实现类负责真实读写余额（如 CoreEconomyProvider 用 PlayerService 信用点）。
 * 注册与未注册时的默认行为由 EconomyService 统一处理，对外通过 EconomyAPI 使用。
 */
interface EconomyProvider {

    fun getBalance(player: Player): Double
    fun getBalance(uuid: UUID): Double
    fun hasEnough(player: Player, amount: Double): Boolean
    fun hasEnough(uuid: UUID, amount: Double): Boolean
    fun deposit(player: Player, amount: Double): Double
    fun deposit(uuid: UUID, amount: Double): Double
    fun withdraw(player: Player, amount: Double): Boolean
    fun withdraw(uuid: UUID, amount: Double): Boolean
    fun forceWithdraw(player: Player, amount: Double): Double
    fun forceWithdraw(uuid: UUID, amount: Double): Double
    fun setBalance(player: Player, amount: Double)
    fun setBalance(uuid: UUID, amount: Double)
    fun getCurrencyName(): String
    fun getCurrencyNamePlural(): String
    fun format(amount: Double): String
}
