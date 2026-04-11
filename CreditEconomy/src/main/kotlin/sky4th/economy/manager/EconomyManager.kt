package sky4th.economy.manager

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import sky4th.core.api.EconomyAPI
import sky4th.economy.event.EconomyChangeEvent
import java.util.*

/**
 * 经济管理器
 * 
 * 负责处理所有经济变动操作，所有经济操作都通过 SkyCoreAPI 进行
 * 提供统一的经济操作接口，便于管理和扩展
 * 每次经济变动都会触发 EconomyChangeEvent
 */
object EconomyManager {

    /**
     * 触发经济变动事件
     */
    private fun fireEvent(
        player: Player,
        amount: Double,
        balance: Double,
        reason: String,
        cause: EconomyChangeEvent.Cause
    ) {
        Bukkit.getPluginManager().callEvent(
            EconomyChangeEvent(
                player = player,
                amount = amount,
                balance = balance,
                reason = reason,
                cause = cause
            )
        )
    }

    /**
     * 检查经济系统是否可用
     */
    fun isAvailable(): Boolean = EconomyAPI.isAvailable()

    /**
     * 获取玩家余额
     * @param player 玩家对象
     * @return 玩家余额
     */
    fun getBalance(player: Player): Double {
        return EconomyAPI.getBalance(player)
    }

    /**
     * 获取玩家余额
     * @param uuid 玩家UUID
     * @return 玩家余额
     */
    fun getBalance(uuid: UUID): Double {
        return EconomyAPI.getBalance(uuid)
    }

    /**
     * 检查玩家余额是否足够
     * @param player 玩家对象
     * @param amount 金额
     * @return 是否足够
     */
    fun hasEnough(player: Player, amount: Double): Boolean {
        return EconomyAPI.hasEnough(player, amount)
    }

    /**
     * 检查玩家余额是否足够
     * @param uuid 玩家UUID
     * @param amount 金额
     * @return 是否足够
     */
    fun hasEnough(uuid: UUID, amount: Double): Boolean {
        return EconomyAPI.hasEnough(uuid, amount)
    }

    /**
     * 给玩家存款
     * @param player 玩家对象
     * @param amount 金额
     * @return 实际存入的金额
     */
    fun deposit(player: Player, amount: Double): Double {
        val oldBalance = getBalance(player)
        val actual = EconomyAPI.deposit(player, amount)
        if (actual > 0) {
            fireEvent(
                player = player,
                amount = actual,
                balance = oldBalance + actual,
                reason = "存款",
                cause = EconomyChangeEvent.Cause.OTHER
            )
        }
        return actual
    }

    /**
     * 给玩家存款
     * @param uuid 玩家UUID
     * @param amount 金额
     * @return 实际存入的金额
     */
    fun deposit(uuid: UUID, amount: Double): Double {
        return EconomyAPI.deposit(uuid, amount)
    }

    /**
     * 从玩家取款
     * @param player 玩家对象
     * @param amount 金额
     * @return 是否成功取款
     */
    fun withdraw(player: Player, amount: Double): Boolean {
        if (!hasEnough(player, amount)) {
            return false
        }
        val oldBalance = getBalance(player)
        val success = EconomyAPI.withdraw(player, amount)
        if (success) {
            fireEvent(
                player = player,
                amount = -amount,
                balance = oldBalance - amount,
                reason = "取款",
                cause = EconomyChangeEvent.Cause.OTHER
            )
        }
        return success
    }

    /**
     * 从玩家取款
     * @param uuid 玩家UUID
     * @param amount 金额
     * @return 是否成功取款
     */
    fun withdraw(uuid: UUID, amount: Double): Boolean {
        return EconomyAPI.withdraw(uuid, amount)
    }

    /**
     * 设置玩家余额
     * @param player 玩家对象
     * @param amount 金额
     */
    fun setBalance(player: Player, amount: Double) {
        val oldBalance = getBalance(player)
        EconomyAPI.setBalance(player, amount)
        fireEvent(
            player = player,
            amount = amount - oldBalance,
            balance = amount,
            reason = "系统调整",
            cause = EconomyChangeEvent.Cause.SYSTEM
        )
    }

    /**
     * 设置玩家余额
     * @param uuid 玩家UUID
     * @param amount 金额
     */
    fun setBalance(uuid: UUID, amount: Double) {
        EconomyAPI.setBalance(uuid, amount)
    }

    /**
     * 条件执行操作（余额足够时扣除并执行操作）
     * @param player 玩家对象
     * @param amount 金额
     * @param action 要执行的操作
     * @return 是否成功执行
     */
    fun require(player: Player, amount: Double, action: () -> Unit): Boolean {
        return EconomyAPI.require(player, amount, action)
    }

    /**
     * 获取货币名称
     * @return 货币名称
     */
    fun getCurrencyName(): String {
        return EconomyAPI.getCurrencyName()
    }

    /**
     * 获取货币复数名称
     * @return 货币复数名称
     */
    fun getCurrencyNamePlural(): String {
        return EconomyAPI.getCurrencyNamePlural()
    }

    /**
     * 格式化金额显示
     * @param amount 金额
     * @return 格式化后的字符串
     */
    fun format(amount: Double): String {
        return EconomyAPI.format(amount)
    }

    /**
     * 转账操作
     * @param from 转出玩家
     * @param to 转入玩家
     * @param amount 金额
     * @return 是否成功转账
     */
    fun transfer(from: Player, to: Player, amount: Double): Boolean {
        if (!hasEnough(from, amount)) {
            return false
        }
        if (!withdraw(from, amount)) {
            return false
        }
        val deposited = deposit(to, amount)
        if (deposited > 0) {
            // 触发转账事件
            fireEvent(
                player = from,
                amount = -amount,
                balance = getBalance(from),
                reason = "转账给 ${to.name}",
                cause = EconomyChangeEvent.Cause.TRANSFER
            )
            fireEvent(
                player = to,
                amount = deposited,
                balance = getBalance(to),
                reason = "来自 ${from.name} 的转账",
                cause = EconomyChangeEvent.Cause.TRANSFER
            )
        }
        return true
    }

    /**
     * 转账操作
     * @param fromUuid 转出玩家UUID
     * @param toUuid 转入玩家UUID
     * @param amount 金额
     * @return 是否成功转账
     */
    fun transfer(fromUuid: UUID, toUuid: UUID, amount: Double): Boolean {
        if (!hasEnough(fromUuid, amount)) {
            return false
        }
        if (!withdraw(fromUuid, amount)) {
            return false
        }
        deposit(toUuid, amount)
        return true
    }

    /**
     * 批量存款
     * @param players 玩家列表
     * @param amount 金额
     * @return 实际存入的总金额
     */
    @JvmName("batchDepositPlayers")
    fun batchDeposit(players: List<Player>, amount: Double): Double {
        var total = 0.0
        players.forEach { player ->
            total += deposit(player, amount)
        }
        return total
    }

    /**
     * 批量存款
     * @param uuids 玩家UUID列表
     * @param amount 金额
     * @return 实际存入的总金额
     */
    @JvmName("batchDepositUuids")
    fun batchDeposit(uuids: List<UUID>, amount: Double): Double {
        var total = 0.0
        uuids.forEach { uuid ->
            total += deposit(uuid, amount)
        }
        return total
    }

    /**
     * 批量取款
     * @param players 玩家列表
     * @param amount 金额
     * @return 成功取款的总金额
     */
    @JvmName("batchWithdrawPlayers")
    fun batchWithdraw(players: List<Player>, amount: Double): Double {
        var total = 0.0
        players.forEach { player ->
            if (withdraw(player, amount)) {
                total += amount
            }
        }
        return total
    }

    /**
     * 批量取款
     * @param uuids 玩家UUID列表
     * @param amount 金额
     * @return 成功取款的总金额
     */
    @JvmName("batchWithdrawUuids")
    fun batchWithdraw(uuids: List<UUID>, amount: Double): Double {
        var total = 0.0
        uuids.forEach { uuid ->
            if (withdraw(uuid, amount)) {
                total += amount
            }
        }
        return total
    }

    /**
     * 发放奖励
     * @param player 玩家对象
     * @param amount 金额
     * @param reason 奖励原因
     * @return 实际发放的金额
     */
    fun reward(player: Player, amount: Double, reason: String = "奖励"): Double {
        val oldBalance = getBalance(player)
        val actual = EconomyAPI.deposit(player, amount)
        if (actual > 0) {
            player.sendMessage("§a[经济] §e您获得了 §f${format(actual)} ${getCurrencyName()} §e($reason)")
            fireEvent(
                player = player,
                amount = actual,
                balance = oldBalance + actual,
                reason = reason,
                cause = EconomyChangeEvent.Cause.REWARD
            )
        }
        return actual
    }

    /**
     * 发放奖励
     * @param uuid 玩家UUID
     * @param amount 金额
     * @param reason 奖励原因
     * @return 实际发放的金额
     */
    fun reward(uuid: UUID, amount: Double, reason: String = "奖励"): Double {
        val actual = deposit(uuid, amount)
        Bukkit.getPlayer(uuid)?.let { player ->
            if (actual > 0) {
                player.sendMessage("§a[经济] §e您获得了 §f${format(actual)} ${getCurrencyName()} §e($reason)")
            }
        }
        return actual
    }

    /**
     * 扣除费用
     * @param player 玩家对象
     * @param amount 金额
     * @param reason 扣费原因
     * @param rate 扣费比例（0.0-1.0），默认1.0表示全额扣除
     * @return 是否成功扣除
     */
    fun charge(player: Player, amount: Double, reason: String = "消费", rate: Double = 1.0): Boolean {
        // 计算实际扣费金额
        val actualAmount = amount * rate

        if (!hasEnough(player, actualAmount)) {
            player.sendMessage("§c[经济] §e余额不足！需要 §f${format(actualAmount)} ${getCurrencyName()} §e(原价: ${format(amount)}, 比例: ${(rate * 100).toInt()}%)")
            return false
        }
        val oldBalance = getBalance(player)
        val success = EconomyAPI.withdraw(player, actualAmount)
        if (success) {
            val rateText = if (rate < 1.0) " §e(扣费比例: ${(rate * 100).toInt()}%)" else ""
            player.sendMessage("§c[经济] §e您消费了 §f${format(actualAmount)} ${getCurrencyName()}$rateText §e($reason)")
            fireEvent(
                player = player,
                amount = -actualAmount,
                balance = oldBalance - actualAmount,
                reason = reason,
                cause = EconomyChangeEvent.Cause.CHARGE
            )
        }
        return success
    }

    /**
     * 强制扣除费用（不检查余额，允许负余额）
     * @param player 玩家对象
     * @param amount 金额
     * @param reason 扣费原因
     * @param rate 扣费比例（0.0-1.0），默认1.0表示全额扣除
     * @return 实际扣除的金额
     */
    fun forceCharge(player: Player, amount: Double, reason: String = "强制消费", rate: Double = 1.0): Double {
        // 计算实际扣费金额
        val actualAmount = amount * rate
        val oldBalance = getBalance(player)
        // 调用 SkyCore 的 forceWithdraw 进行实际扣费
        val deducted = EconomyAPI.forceWithdraw(player, actualAmount)
        
        if (deducted > 0) {
            fireEvent(
                player = player,
                amount = -deducted,
                balance = oldBalance - deducted,
                reason = reason,
                cause = EconomyChangeEvent.Cause.CHARGE
            )
        }
        return deducted
    }

    /**
     * 扣除费用
     * @param uuid 玩家UUID
     * @param amount 金额
     * @param reason 扣费原因
     * @return 是否成功扣除
     */
    fun charge(uuid: UUID, amount: Double, reason: String = "消费"): Boolean {
        val player = Bukkit.getPlayer(uuid)
        if (!hasEnough(uuid, amount)) {
            player?.sendMessage("§c[经济] §e余额不足！需要 §f${format(amount)} ${getCurrencyName()}")
            return false
        }
        if (withdraw(uuid, amount)) {
            player?.sendMessage("§c[经济] §e您消费了 §f${format(amount)} ${getCurrencyName()} §e($reason)")
            return true
        }
        return false
    }
}
