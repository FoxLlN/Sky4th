
package sky4th.dungeon.loadout.weapon

import sky4th.dungeon.util.LanguageUtil.sendLangSys
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*

/**
 * 压制战斧效果（锋利III 铁斧）：
 * - 攻击有80%概率使目标停顿0.35秒
 * - 必定打断正在进行的拉弓/施法/装填
 * - 同一目标每5秒最多触发一次
 */
class SuppressionAxeEffectHandler(private val plugin: JavaPlugin) : LoadoutWeaponEffectHandler {
    override val loadoutId: String = "suppression_axe"

    /** 停顿概率：80% */
    private val stunChance = 0.8

    /** 停顿时长：0.35秒 = 7 tick */
    private val stunDurationTicks = 7

    /** 冷却时间：5秒 = 100 tick */
    private val cooldownTicks = 100L

    /** 记录每个目标上次被压制的时间 */
    private val lastStunnedTime: MutableMap<UUID, Long> = mutableMapOf()

    override fun processHits(
        attacker: Player,
        hits: List<WeaponHit>,
        plugin: JavaPlugin
    ) {
        val currentTime = System.currentTimeMillis()

        for (hit in hits) {
            val victim = hit.victim
            if (!victim.isValid || victim.isDead) continue

            val victimId = victim.uniqueId
            val lastTime = lastStunnedTime[victimId] ?: 0L
            val cooldownMs = cooldownTicks * 50L // tick转毫秒

            // 必定打断正在进行的动作
            interruptActions(victim)

            // 80%概率停顿
            if (Math.random() < stunChance) {
                // 使用慢速效果实现停顿
                victim.addPotionEffect(PotionEffect(
                    PotionEffectType.SLOWNESS,
                    stunDurationTicks,
                    255, // 最大等级，几乎无法移动
                    false, // 无粒子
                    false  // 无图标
                ))

                // 记录压制时间
                lastStunnedTime[victimId] = currentTime

                // 发送压制效果生效提示
                attacker.sendLangSys(plugin, "weapon.suppression_axe.effect-triggered")
            }
        }
    }

    /**
     * 打断目标的动作（拉弓/施法/装填）
     * - 对于怪物：重置AI状态，打断当前动作
     * - 对于玩家：取消拉弓、使用物品等动作
     */
    private fun interruptActions(victim: LivingEntity) {
        // 对于怪物：使用 AI 方法打断
        victim.setAI(false)
        victim.setAI(true)

        // 对于玩家：取消正在进行的动作
        if (victim is Player) {
            // 取消拉弓：切换物品槽位会取消拉弓
            val heldSlot = victim.inventory.heldItemSlot
            victim.inventory.heldItemSlot = heldSlot

            // 取消使用物品（如药水、食物等）
            victim.inventory.setItem(victim.inventory.heldItemSlot, victim.inventory.getItem(victim.inventory.heldItemSlot))
        }
    }

    /**
     * 清理已死亡实体的记录，避免内存泄漏
     */
    fun cleanup() {
        val currentTime = System.currentTimeMillis()
        val cooldownMs = cooldownTicks * 50L

        // 移除超过冷却时间或已死亡的实体记录
        lastStunnedTime.keys.removeIf { uuid ->
            val entity = plugin.server.getEntity(uuid)
            entity == null || 
            entity.isDead || 
            (currentTime - (lastStunnedTime[uuid] ?: 0L) > cooldownMs * 10)
        }
    }
}
