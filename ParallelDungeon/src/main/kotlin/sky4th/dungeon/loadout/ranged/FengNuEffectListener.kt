package sky4th.dungeon.loadout.ranged

import sky4th.dungeon.command.DungeonContext
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Arrow
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Vector

/**
 * 风弹弩（wind_crossbow）：快速装填II 弩，伤害减半；命中/箭矢落点生成风弹，将周围实体击退。
 */
class FengNuEffectListener(private val plugin: JavaPlugin) : Listener {

    private val shopIdKey: NamespacedKey by lazy { NamespacedKey(plugin, "loadout_shop_id") }
    private val metaKey = "FengNu"

    private fun isFengNuCrossbow(item: ItemStack?): Boolean =
        item?.type == Material.CROSSBOW &&
            item.itemMeta?.persistentDataContainer?.get(shopIdKey, PersistentDataType.STRING) == FENG_NU_ID

    /** 发射时：若为副本内玩家持风弹弩，给弹道打标 */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onProjectileLaunch(event: ProjectileLaunchEvent) {
        val proj = event.entity
        val shooter = proj.shooter as? Player ?: return
        val ctx = DungeonContext.get() ?: return
        if (!ctx.playerManager.isPlayerInDungeon(shooter)) return
        val main = shooter.inventory.itemInMainHand
        if (!isFengNuCrossbow(main)) return
        proj.setMetadata(metaKey, FixedMetadataValue(plugin, true))
    }

    /** 命中/落点：在落点生成风弹（1.21 风弹粒子+音效），击退周围实体 */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onProjectileHit(event: ProjectileHitEvent) {
        val proj = event.entity
        if (!proj.hasMetadata(metaKey)) return
        val loc = event.hitBlock?.location?.add(0.5, 0.5, 0.5)
            ?: event.hitEntity?.location
            ?: proj.location
        applyWindBlast(loc, proj.shooter as? LivingEntity)
    }

    private fun applyWindBlast(center: org.bukkit.Location, exclude: LivingEntity?) {
        val world = center.world ?: return
        // 1.21 风弹碰撞特效：粒子 + 音效
        playWindBurstEffect(world, center)
        val radius = 4.0
        val strength = 1.4
        for (e in world.getNearbyEntities(center, radius, radius, radius)) {
            if (e !is LivingEntity) continue
            if (e === exclude) continue
            val dir = e.location.toVector().subtract(center.toVector()).normalize()
            val dist = e.location.distance(center)
            if (dist < 0.1) continue
            val falloff = 1.0 - (dist / radius) * 0.5
            e.velocity = dir.multiply(strength * falloff)
        }
    }

    /** 1.21 风弹命中时的粒子与音效（与原版风弹碰撞一致） */
    private fun playWindBurstEffect(world: org.bukkit.World, center: org.bukkit.Location) {
        val x = center.x
        val y = center.y
        val z = center.z
        world.spawnParticle(Particle.GUST_EMITTER_SMALL, x, y, z, 1, 0.0, 0.0, 0.0, 0.0)
        world.spawnParticle(Particle.GUST, x, y, z, 12, 0.5, 0.5, 0.5, 0.15)
        try {
            world.playSound(center, Sound.ENTITY_WIND_CHARGE_WIND_BURST, SoundCategory.PLAYERS, 1f, (0.7 + Math.random() * 0.35).toFloat())
        } catch (_: Throwable) {
            // 低版本无此音效时忽略
        }
    }

    companion object {
        const val FENG_NU_ID = "wind_crossbow"
    }
}
