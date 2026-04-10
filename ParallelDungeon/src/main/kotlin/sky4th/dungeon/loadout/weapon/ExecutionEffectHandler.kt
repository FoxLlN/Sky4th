package sky4th.dungeon.loadout.weapon

import sky4th.dungeon.monster.core.MonsterLevel
import sky4th.dungeon.monster.core.MonsterMetadata
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

/**
 * 处决效果（锋利4 铁剑）：
 * - 对任意敌人第一次伤害 +60%
 * - 对精英及以上怪物（ELITE/ENHANCED/LEGENDARY）取最高倍率 +100%（总计200%）
 */
class ExecutionEffectHandler(private val plugin: JavaPlugin) : LoadoutWeaponEffectHandler {
    override val loadoutId: String = "execution"

    /** 记录每个玩家对每个怪物的首次攻击状态 */
    private val firstHitTracker: MutableMap<UUID, MutableMap<String, Boolean>> = mutableMapOf()

    /** 记录怪物的实体ID，用于检测怪物重生 */
    private val monsterEntityIds: MutableMap<String, Int> = mutableMapOf()

    /** 普通怪物的伤害加成：160% */
    private val normalBonusMultiplier = 1.6

    /** 精英及以上怪物的伤害加成：200% */
    private val eliteBonusMultiplier = 2.0

    override fun processHits(
        attacker: Player,
        hits: List<WeaponHit>,
        plugin: JavaPlugin
    ) {
        val attackerId = attacker.uniqueId

        // 初始化该玩家的首次攻击记录
        if (!firstHitTracker.containsKey(attackerId)) {
            firstHitTracker[attackerId] = mutableMapOf()
        }

        for (hit in hits) {
            val victim = hit.victim
            val victimId = victim.uniqueId
            val monsterId = MonsterMetadata.getMonsterId(victim)

            // 如果不是怪物，跳过
            if (monsterId == null) continue

            // 检查怪物是否重生（实体ID是否改变）
            val currentEntityId = victim.entityId
            val storedEntityId = monsterEntityIds[monsterId]
            val isRespawned = storedEntityId != null && storedEntityId != currentEntityId

            // 如果怪物重生了，重置所有玩家对该怪物的首次攻击记录
            if (isRespawned) {
                firstHitTracker.values.forEach { it.remove(monsterId) }
                monsterEntityIds[monsterId] = currentEntityId
            } else if (storedEntityId == null) {
                // 首次遇到这个怪物，记录其实体ID
                monsterEntityIds[monsterId] = currentEntityId
            }

            // 检查是否是首次攻击
            val isFirstHit = !firstHitTracker[attackerId]!!.getOrDefault(monsterId, false)

            // 只有首次攻击才触发加成
            if (isFirstHit) {
                // 标记为已攻击
                firstHitTracker[attackerId]!![monsterId] = true

                // 计算伤害加成
                val monsterLevel = MonsterMetadata.getMonsterLevel(victim)
                val isEliteOrAbove = monsterLevel == MonsterLevel.ELITE || 
                                   monsterLevel == MonsterLevel.ENHANCED || 
                                   monsterLevel == MonsterLevel.LEGENDARY

                // 对精英及以上怪物取最高倍率
                val bonusMultiplier = if (isEliteOrAbove) {
                    eliteBonusMultiplier
                } else {
                    normalBonusMultiplier
                }

                // 应用伤害加成
                val baseDamage = hit.damage
                val extraDamage = baseDamage * (bonusMultiplier - 1.0)
                
                // 应用额外伤害到怪物
                if (extraDamage > 0 && victim.isValid && !victim.isDead) {
                    val maxHp = victim.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: 20.0
                    val healthBefore = victim.health
                    val newHealth = (healthBefore - extraDamage).coerceIn(0.0, maxHp)
                    victim.health = newHealth
                }
            }
        }
    }

    /**
     * 清理已死亡或离线玩家的记录，避免内存泄漏
     */
    fun cleanup() {
        // 清理离线玩家的记录
        val iterator = firstHitTracker.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val player = plugin.server.getPlayer(entry.key)
            if (player == null || !player.isOnline) {
                iterator.remove()
            }
        }

        // 清理已死亡怪物的记录
        val deadMonsters = mutableSetOf<String>()
        monsterEntityIds.keys.forEach { monsterId ->
            // 通过 monsterId 查找对应的实体
            var isDead = true
            for (player in plugin.server.onlinePlayers) {
                val world = player.world
                for (entity in world.entities) {
                    if (entity is LivingEntity) {
                        val entityMonsterId = MonsterMetadata.getMonsterId(entity)
                        if (entityMonsterId == monsterId) {
                            isDead = entity.isDead
                            break
                        }
                    }
                }
                if (!isDead) break
            }
            if (isDead) {
                deadMonsters.add(monsterId)
            }
        }

        // 从所有玩家的记录中移除已死亡的怪物
        deadMonsters.forEach { monsterId ->
            firstHitTracker.values.forEach { it.remove(monsterId) }
            monsterEntityIds.remove(monsterId)
        }
    }
}
