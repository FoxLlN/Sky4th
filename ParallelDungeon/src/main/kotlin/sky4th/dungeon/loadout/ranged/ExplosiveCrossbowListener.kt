package sky4th.dungeon.loadout.ranged

import sky4th.dungeon.command.DungeonContext
import sky4th.dungeon.Dungeon
import sky4th.dungeon.loadout.equipment.LoadoutSetSkillState
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Arrow
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.entity.SpectralArrow
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
 * 爆破弩（explosive_crossbow）：原版弩无附魔，配合爆炸箭；仅命中生物时在命中处产生爆炸
 * （中心约 14 点伤害、范围衰减），不破坏方块、不起火，含特效与音效；落地不炸，爆炸后移除箭矢，箭矢直接伤害取消。
 */
class ExplosiveCrossbowListener(private val plugin: JavaPlugin) : Listener {

    private val shopIdKey: NamespacedKey by lazy { NamespacedKey(plugin, "loadout_shop_id") }
    private val metaKey = "ExplosiveArrow"

    private fun isExplosiveCrossbow(item: ItemStack?): Boolean =
        item?.type == Material.CROSSBOW &&
            item.itemMeta?.persistentDataContainer?.get(shopIdKey, PersistentDataType.STRING) == EXPLOSIVE_CROSSBOW_ID

    /** 发射时：副本内玩家持爆破弩，且射出的为光灵箭（爆炸箭），才给弹道打标 */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onProjectileLaunch(event: ProjectileLaunchEvent) {
        val proj = event.entity
        val shooter = proj.shooter as? Player ?: return
        val ctx = DungeonContext.get() ?: return
        if (!ctx.playerManager.isPlayerInDungeon(shooter)) return
        val main = shooter.inventory.itemInMainHand
        if (!isExplosiveCrossbow(main)) return
        // 仅当实际射出的是光灵箭（本插件配置为爆炸箭原型）时，才视为爆炸箭
        if (proj !is SpectralArrow) return
        proj.setMetadata(metaKey, FixedMetadataValue(plugin, true))
    }

    /** 仅命中生物时爆炸；落地（只命中方块）不炸，只移除箭矢 */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onProjectileHit(event: ProjectileHitEvent) {
        val proj = event.entity
        if (!proj.hasMetadata(metaKey)) return
        val hitEntity = event.hitEntity
        if (hitEntity != null) {
            val loc = hitEntity.location
            val world = loc.world ?: return
            val shooter = (proj.shooter as? Player)?.takeIf { p -> DungeonContext.get()?.playerManager?.isPlayerInDungeon(p) == true }
            val hadYouxiaBoost = shooter != null && LoadoutSetSkillState.youxiaRangedBoost.remove(shooter.uniqueId)
            if (hadYouxiaBoost) Dungeon.instance.refreshSidebar(shooter)
            val activeMult = 1.60 // 游侠主动 +60% 伤害
            val centerDamage = 18.0
            val explosionRadius = 5.0
            world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1f, 1f)
            world.spawnParticle(Particle.EXPLOSION, loc.x, loc.y, loc.z, 1, 0.0, 0.0, 0.0, 0.0)
            for (e in world.getNearbyEntities(loc, explosionRadius, explosionRadius, explosionRadius)) {
                if (e !is LivingEntity) continue
                val dist = e.location.distance(loc)
                if (dist >= explosionRadius) continue
                var damage = centerDamage * (1.0 - dist / explosionRadius)
                if (e == hitEntity && hadYouxiaBoost) damage *= activeMult
                if (damage > 0) e.damage(damage, proj)
                e.velocity = Vector(0.0, 0.0, 0.0)
            }
        }
        if (proj.isValid) proj.remove()
    }

    /** 取消爆炸箭的直接伤害，由爆炸统一造成伤害 */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        if (damager !is Arrow) return
        if (!damager.hasMetadata(metaKey)) return
        event.isCancelled = true
    }

    companion object {
        const val EXPLOSIVE_CROSSBOW_ID = "explosive_crossbow"
    }
}
