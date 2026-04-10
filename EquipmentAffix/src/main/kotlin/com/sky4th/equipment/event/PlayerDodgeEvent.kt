package com.sky4th.equipment.event

import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.event.entity.EntityDamageEvent

/**
 * 玩家闪避成功事件
 * 当玩家成功闪避攻击时触发
 */
class PlayerDodgeEvent(
    val player: Player,
    val damageEvent: EntityDamageEvent,
    val dodgeChance: Double
) : Event(), Cancellable {
    
    private var cancelled = false

    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        private val handlerList = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return handlerList
        }
    }
    
    override fun isCancelled(): Boolean = cancelled
    
    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    /**
     * 获取闪避前的伤害值
     */
    fun getOriginalDamage(): Double = damageEvent.damage

    /**
     * 获取伤害类型
     */
    fun getDamageCause(): EntityDamageEvent.DamageCause = damageEvent.cause
}
