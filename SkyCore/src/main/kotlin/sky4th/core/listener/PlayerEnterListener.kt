package sky4th.core.listener

import sky4th.core.api.PlayerAPI
import sky4th.core.api.PlayerAttributesAPI
import sky4th.core.api.PlayerPermissionsAPI
import sky4th.core.api.MarkAPI
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

/**
 * Core 玩家进出服监听器
 * - 登录：更新最后登录时间（供新人保护、在线时长等逻辑使用）
 * - 退出：从缓存移除并落盘，避免内存累积
 * - 屏蔽原版玩家加入和死亡消息（未来将自定义这些消息）
 */
class PlayerEnterListener : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        // 屏蔽原版玩家加入消息
        event.joinMessage(null)
        
        if (!PlayerAPI.isAvailable()) return
        try {
            PlayerAPI.updateLoginTime(event.player)
        } catch (_: Exception) { /* 未初始化时忽略 */ }

        // 设置最大伤害吸收属性（首次加入时）
        try {
            setMaxAbsorption(event.player)
        } catch (_: Exception) { /* 设置失败时忽略 */ }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        // 屏蔽原版玩家退出消息
        event.quitMessage(null)
        
        if (!PlayerAPI.isAvailable()) return
        try {
            PlayerAPI.removeFromCache(event.player.uniqueId)
        } catch (_: Exception) { /* 未初始化时忽略 */ }

        // 清理权限缓存
        if (PlayerPermissionsAPI.isAvailable()) {
            try {
                PlayerPermissionsAPI.removeFromCache(event.player)
            } catch (_: Exception) { /* 未初始化时忽略 */ }
        }

        // 清理属性缓存
        if (PlayerAttributesAPI.isAvailable()) {
            try {
                PlayerAttributesAPI.removeFromCache(event.player.uniqueId)
            } catch (_: Exception) { /* 未初始化时忽略 */ }
        }
    }
    
    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        // 屏蔽原版玩家死亡消息
        event.deathMessage(null)

        // 清理玩家标记
        if (MarkAPI.isAvailable()) {
            try {
                MarkAPI.removeMark(event.player)
                // 移除玩家的自定义名字标签
                sky4th.core.mark.MarkManager.removePlayerNameTag(event.player)
            } catch (_: Exception) { /* 未初始化时忽略 */ }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR)
    fun onPlayerDamage(event: EntityDamageEvent) {
        val player = event.entity
        if (player !is org.bukkit.entity.Player) return

        // 获取初始伤害
        val initialDamage = event.damage

        // 获取玩家属性
        val armor = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ARMOR)?.value ?: 0.0
        val toughness = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ARMOR_TOUGHNESS)?.value ?: 0.0
        val damageCause = event.cause

        // 获取伤害来源（如果有）
        val damager = if (event is EntityDamageByEntityEvent) {
            event.damager
        } else {
            null
        }

        // 获取最终伤害
        val finalDamage = event.finalDamage

        // 打印调试信息
        println("[Damage Debug] 玩家: ${player.name}")
        println("[Damage Debug] 伤害原因: $damageCause")
        if (damager != null) {
            println("[Damage Debug] 伤害来源: ${damager.name}")
        } else {
            println("[Damage Debug] 伤害来源: 无来源伤害")
        }
        println("[Damage Debug] 初始伤害: $initialDamage")
        println("[Damage Debug] 护甲值: $armor")
        println("[Damage Debug] 韧性: $toughness")
        println("[Damage Debug] 最终伤害: $finalDamage")
    }

    /**
     * 设置玩家的最大伤害吸收为20点（10颗心）
     * @param player 玩家
     */
    private fun setMaxAbsorption(player: org.bukkit.entity.Player) {
        val absorptionAttribute = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_ABSORPTION) ?: return

        // 检查是否已经设置过最大伤害吸收
        val hasModifier = absorptionAttribute.modifiers.any { 
            it.key.namespace == "sky4th" && it.key.key == "max_absorption"
        }

        if (!hasModifier) {
            // 创建最大伤害吸收修饰符
            val modifier = org.bukkit.attribute.AttributeModifier(
                org.bukkit.NamespacedKey("skycore", "max_absorption"),
                20.0,  // 设置为20点（10颗心）
                org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER,
                org.bukkit.inventory.EquipmentSlotGroup.ANY
            )
            absorptionAttribute.addModifier(modifier)
        }
    }
}
