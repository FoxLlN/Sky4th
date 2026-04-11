package sky4th.economy.event

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * 经济变动事件
 * 
 * 当玩家的信用点发生变动时触发此事件
 * 
 * @property player 玩家对象
 * @property amount 变动金额（正数表示增加，负数表示减少）
 * @property balance 变动后的余额
 * @property reason 变动原因
 * @property cause 变动原因类型
 */
class EconomyChangeEvent(
    val player: Player,
    val amount: Double,
    val balance: Double,
    val reason: String,
    val cause: Cause
) : Event() {

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }

    override fun getHandlers(): HandlerList = getHandlerList()

    /**
     * 变动原因类型
     */
    enum class Cause {
        /** 奖励 */
        REWARD,
        /** 消费 */
        CHARGE,
        /** 转账 */
        TRANSFER,
        /** 系统调整 */
        SYSTEM,
        /** 其他 */
        OTHER
    }
}
