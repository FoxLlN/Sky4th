package sky4th.dungeon.loadout.weapon

import org.bukkit.attribute.Attribute
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.plugin.java.JavaPlugin

/**
 * 烈焰效果（锋利1 击退2 火焰附加 铁剑）：
 * 攻击附带火焰附加，命中目标被点燃。总伤8点伤害（有bug 目前会缺失一点伤害）
 * 火焰被灭（水/雨/细雪等）则立即停止额外伤害。
 * 额外伤害走 [EntityDamageEvent] 的 FIRE_TICK，与原版火焰灼烧一致，会被保护/火焰保护等减伤。
 */
class LieYanEffectHandler : LoadoutWeaponEffectHandler {
    override val loadoutId: String = "flame_blade"

    /** 点燃时长（tick），80 = 4 秒 */
    private val fireTicks = 80

    /** 每秒额外伤害（仅当目标仍在燃烧时结算），走伤害系统可被保护减伤  4*(1+edps)*/
    private val extraDamagePerSecond = 1.0

    override fun processHits(
        attacker: Player,
        hits: List<WeaponHit>,
        plugin: JavaPlugin
    ) {
        for (hit in hits) {
            if (!hit.victim.isValid || hit.victim.isDead) continue
            hit.victim.fireTicks = fireTicks.coerceAtLeast(hit.victim.fireTicks)
            scheduleFireExtraDamage(plugin, hit.victim)
        }
    }

    /**
     * 每秒检查一次：若目标仍在燃烧则造成 [extraDamagePerSecond] 伤害，共 4 次。
     * 火焰被灭（原版机制：水/雨/细雪等）则不再造成额外伤害。
     * 通过 FIRE_TICK 伤害事件造成伤害，与原版火焰灼烧一样可被保护/火焰保护减伤。
     */
    private fun scheduleFireExtraDamage(plugin: JavaPlugin, victim: LivingEntity) {
        val fireDamageSource = DamageSource.builder(DamageType.ON_FIRE).build()
        val runnable = object : Runnable {
            var count = 0
            override fun run() {
                if (count >= 4) return
                if (!victim.isValid || victim.isDead || victim.fireTicks <= 0) return
                val event = EntityDamageEvent(
                    victim,
                    EntityDamageEvent.DamageCause.FIRE_TICK,
                    fireDamageSource,
                    extraDamagePerSecond
                )
                plugin.server.pluginManager.callEvent(event)
                if (!event.isCancelled()) {
                    val maxHp = victim.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: 20.0
                    val finalDamage = event.finalDamage
                    victim.health = (victim.health - finalDamage).coerceIn(0.0, maxHp)
                    // lastDamageCause 已弃用，移除设置
                }
                count++
                if (count < 4) plugin.server.scheduler.runTaskLater(plugin, this, 20L)
            }
        }
        plugin.server.scheduler.runTaskLater(plugin, runnable, 20L)
    }
}
