
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID

/**
 * 应激词条
 * 效果：受到攻击时，获得速度I/II/III，持续2/3/4秒，冷却10秒
 * 
 * 1级：获得速度I，持续2秒
 * 2级：获得速度II，持续3秒
 * 3级：获得速度III，持续4秒
 */
class Adrenaline : com.sky4th.equipment.modifier.ConfiguredModifier("adrenaline") {

    companion object {
        // 存储每个玩家的冷却时间结束时间戳
        private val cooldowns = mutableMapOf<UUID, Long>()

        // 冷却时间（毫秒）：10秒
        private const val COOLDOWN_MS = 10000L

        // 每级配置：(速度等级, 持续时间秒)
        private val CONFIG = arrayOf(
            1 to 2,  // 1级：速度I，持续2秒
            2 to 3,  // 2级：速度II，持续3秒
            3 to 4   // 3级：速度III，持续4秒
        )
    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(EntityDamageEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 只处理实体伤害事件，且玩家必须是防御者
        if (event !is EntityDamageEvent || playerRole != PlayerRole.DEFENDER) {
            return
        }

        // 获取当前等级的配置
        val (speedLevel, duration) = CONFIG.getOrNull(level - 1) ?: return

        // 检查冷却时间
        val currentTime = System.currentTimeMillis()
        val lastUseTime = cooldowns[player.uniqueId] ?: 0
        if (currentTime - lastUseTime < COOLDOWN_MS) {
            return
        }

        // 更新冷却时间
        cooldowns[player.uniqueId] = currentTime

        // 给玩家添加速度效果
        val speedEffect = PotionEffect(
            PotionEffectType.SPEED,
            duration * 20,  // 转换为tick
            speedLevel - 1,  // PotionEffect等级从0开始
            false,           // 不显示粒子
            true            // 不显示图标
        )
        player.addPotionEffect(speedEffect)
    }

    override fun onRemove(player: Player) {
    }
}
