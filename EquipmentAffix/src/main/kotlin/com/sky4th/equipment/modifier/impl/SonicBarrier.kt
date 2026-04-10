
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.Particle
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector

/**
 * 音波屏障词条
 * 效果：受到攻击时，对周围所有敌人造成一次击退效果
 * 1级：击退1格，冷却10秒
 * 2级：击退2格，冷却10秒
 * 3级：击退3格，冷却10秒
 */
class SonicBarrier : com.sky4th.equipment.modifier.ConfiguredModifier("sonic_barrier") {

    companion object {
        // 每级击退距离（格）
        private val CONFIG = doubleArrayOf(0.5, 0.6, 0.7)
        // 冷却时间（秒）
        private const val COOLDOWN_SECONDS = 10L
        // 存储每个玩家的上次触发时间
        private val cooldownMap = mutableMapOf<org.bukkit.entity.Player, Long>()
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

        // 检查冷却时间
        val currentTime = System.currentTimeMillis()
        val lastTriggerTime = cooldownMap[player] ?: 0L
        val cooldownMillis = COOLDOWN_SECONDS * 1000

        if (currentTime - lastTriggerTime < cooldownMillis) {
            return
        }

        // 更新冷却时间
        cooldownMap[player] = currentTime

        // 获取当前等级的击退距离
        val knockbackDistance = if (level - 1 in 0..2) CONFIG[level - 1] else return

        // 获取玩家周围2格内的所有实体
        val nearbyEntities = player.world.getNearbyEntities(player.location, 2.0, 2.0, 2.0)

        if (nearbyEntities.isEmpty()) return
        // 对每个周围的实体执行击退
        for (entity in nearbyEntities) {
            // 跳过玩家自己和非敌对实体
            if (entity === player || entity !is LivingEntity) {
                continue
            }

            // 计算从玩家指向实体的向量
            val direction = entity.location.toVector().subtract(player.location.toVector()).normalize()

            // 应用击退效果（减少y轴速度）
            val velocity = direction.multiply(knockbackDistance)
            velocity.y = 0.1  // 给予很小的向上的力
            entity.velocity = velocity
        }
        // 在玩家位置生成音波扩散效果
        playSonicWaveBurst(player)
    }

    /**
     * 在玩家位置播放音波爆发效果
     */
    private fun playSonicWaveBurst(player: Player) {
        val location = player.location.clone().add(0.0, 1.0, 0.0)

        // 生成少量环形音波效果
        for (angle in 0 until 360 step 60) {
            val radians = Math.toRadians(angle.toDouble())
            val radius = 1.5

            val x = location.x + Math.cos(radians) * radius
            val z = location.z + Math.sin(radians) * radius

            player.world.spawnParticle(
                Particle.SONIC_BOOM,
                x,
                location.y,
                z,
                1,
                0.0,
                0.0,
                0.0,
                0.0
            )
        }
    }
}
