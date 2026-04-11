package sky4th.core.event

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * 玩家游玩时间每满1小时触发的事件
 * 
 * @property player 玩家对象
 * @property fromHour 起始小时数（例如：从0小时到1小时，fromHour=0）
 * @property toHour 目标小时数（例如：从0小时到1小时，toHour=1）
 * @property timeType 时间类型（DAILY=每日时长，CURRENT_LIFE=存活时长，TOTAL=总时长）
 */
class PlayTimeHourEvent(
    val player: Player,
    val fromHour: Int,
    val toHour: Int,
    val timeType: TimeType
) : Event() {

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }

    override fun getHandlers(): HandlerList = getHandlerList()

    /**
     * 时间类型枚举
     */
    enum class TimeType {
        /** 每日游玩时长 */
        DAILY,
        /** 当前存活时长 */
        CURRENT_LIFE,
        /** 总游玩时长 */
        TOTAL
    }
}
