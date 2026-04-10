package sky4th.bettermc.listeners

import sky4th.bettermc.command.FeatureManager
import org.bukkit.entity.EntityType
import org.bukkit.entity.IronGolem
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityDamageEvent
import sky4th.bettermc.config.ConfigManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 铁傀儡改进监听器
 * 
 * 功能：
 * 1. 免疫掉落伤害
 * 2. 免疫自然伤害（防水防火）
 * 3. 基础减伤20%
 * 4. 短时间内连续攻击时减伤逐步增加到70%
 */
class IronGolemListener : Listener {

    // 记录铁傀儡的连续攻击历史
    private val attackHistory = ConcurrentHashMap<UUID, MutableList<Long>>()

    // 攻击记录的有效时间窗口（毫秒）
    private val attackWindowMs: Long
        get() = (ConfigManager.ironGolemAttackWindowSeconds * 1000).toLong()

    // 达到最大减伤所需的连续攻击次数
    private val maxReductionHits: Int
        get() = ConfigManager.ironGolemMaxReductionHits

    // 基础减伤（20%）
    private val baseReduction: Double
        get() = ConfigManager.ironGolemBaseReduction

    // 最大减伤（70%）
    private val maxReduction: Double
        get() = ConfigManager.ironGolemMaxReduction

    @EventHandler
    fun onIronGolemDamage(event: EntityDamageEvent) {
        if (!FeatureManager.isFeatureEnabled("iron-golem")) return

        val entity = event.entity

        // 只处理铁傀儡
        if (entity.type != EntityType.IRON_GOLEM) return

        val golem = entity as? IronGolem ?: return

        // 1. 免疫掉落伤害
        if (event.cause == EntityDamageEvent.DamageCause.FALL) {
            event.isCancelled = true
            return
        }

        // 2. 免疫自然伤害（防水防火）
        if (event.cause in listOf(
                EntityDamageEvent.DamageCause.DROWNING,
                EntityDamageEvent.DamageCause.FIRE,
                EntityDamageEvent.DamageCause.FIRE_TICK,
                EntityDamageEvent.DamageCause.LAVA
            )) {
            event.isCancelled = true
            return
        }

        // 计算减伤比例
        val reduction = calculateDamageReduction(golem)

        // 应用减伤
        val originalDamage = event.damage
        event.damage = originalDamage * (1 - reduction)
        // 记录攻击时间
        recordAttack(golem)
    }

    /**
     * 计算铁傀儡的减伤比例
     * @return 减伤比例（0.0-1.0）
     */
    private fun calculateDamageReduction(golem: IronGolem): Double {
        val uuid = golem.uniqueId
        val history = attackHistory[uuid] ?: return baseReduction

        // 清理过期的攻击记录
        val currentTime = System.currentTimeMillis()
        val iterator = history.iterator()
        while (iterator.hasNext()) {
            if (currentTime - iterator.next() > attackWindowMs) {
                iterator.remove()
            }
        }

        // 如果没有有效记录，返回基础减伤
        if (history.isEmpty()) {
            attackHistory.remove(uuid)
            return baseReduction
        }

        // 根据连续攻击次数计算减伤
        val hitCount = history.size
        val reductionProgress = (hitCount - 1).coerceAtMost(maxReductionHits - 1).toDouble() / (maxReductionHits - 1)

        return baseReduction + (maxReduction - baseReduction) * reductionProgress
    }

    /**
     * 记录铁傀儡受到攻击的时间
     */
    private fun recordAttack(golem: IronGolem) {
        val uuid = golem.uniqueId
        val currentTime = System.currentTimeMillis()

        val history = attackHistory.getOrPut(uuid) { mutableListOf<Long>() }
        history.add(currentTime)

        // 清理过期的记录
        val iterator = history.iterator()
        while (iterator.hasNext()) {
            if (currentTime - iterator.next() > attackWindowMs) {
                iterator.remove()
            }
        }

        // 如果列表为空（清理后），移除键值对
        if (history.isEmpty()) {
            attackHistory.remove(uuid)
        }
    }

    /**
     * 阻止玩家通过搭建铁块和南瓜头生成铁傀儡
     */
    @EventHandler
    fun onCreatureSpawn(event: CreatureSpawnEvent) {
        if (!FeatureManager.isFeatureEnabled("iron-golem")) return
        // 只处理铁傀儡
        if (event.entityType != EntityType.IRON_GOLEM) return

        // 检查生成原因是否是玩家搭建
        if (event.spawnReason == CreatureSpawnEvent.SpawnReason.BUILD_IRONGOLEM) {
            event.isCancelled = true
        }
    }
}
