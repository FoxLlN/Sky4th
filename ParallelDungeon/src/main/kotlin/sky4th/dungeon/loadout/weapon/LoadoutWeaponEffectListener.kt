package sky4th.dungeon.loadout.weapon

import sky4th.dungeon.command.DungeonContext
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

/**
 * 收集本 tick 内配装武器的近战命中，tick 末按 loadoutId 分发给 [LoadoutWeaponEffectRegistry] 中的处理器。
 * 仅处理副本内玩家的主手攻击；主手物品需带 loadout_shop_id。
 */
class LoadoutWeaponEffectListener(private val plugin: JavaPlugin) : Listener {

    private val shopIdKey: NamespacedKey by lazy { NamespacedKey(plugin, "loadout_shop_id") }

    /** 本 tick 内已参与收集的 attacker，已安排 end-of-tick 处理 */
    private val scheduledThisTick: MutableSet<UUID> = mutableSetOf()

    /** attacker -> 本 tick 命中记录 (loadoutId, victim, finalDamage, isMainTarget) */
    private data class HitRecord(val loadoutId: String, val victim: LivingEntity, val damage: Double, val isMainTarget: Boolean)
    private val buffer: MutableMap<UUID, MutableList<HitRecord>> = mutableMapOf()

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        if (damager !is Player) return
        val victim = event.entity
        if (victim !is LivingEntity) return
        val ctx = DungeonContext.get() ?: return
        if (!ctx.playerManager.isPlayerInDungeon(damager)) return
        if (event.cause != EntityDamageEvent.DamageCause.ENTITY_ATTACK && event.cause != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) return
        val mainHand: ItemStack = damager.inventory.itemInMainHand
        val loadoutId = mainHand.itemMeta?.persistentDataContainer?.get(shopIdKey, PersistentDataType.STRING) ?: return

        val finalDamage = event.finalDamage
        val isMainTarget = event.cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK
        val list = buffer.getOrPut(damager.uniqueId) { mutableListOf() }
        list.add(HitRecord(loadoutId, victim, finalDamage, isMainTarget))
        if (scheduledThisTick.add(damager.uniqueId)) {
            plugin.server.scheduler.runTaskLater(plugin, Runnable { processAttacker(damager.uniqueId) }, 1L)
        }
    }

    private fun processAttacker(attackerUuid: UUID) {
        scheduledThisTick.remove(attackerUuid)
        val list = buffer.remove(attackerUuid) ?: return
        val attacker = plugin.server.getPlayer(attackerUuid) ?: return
        if (!attacker.isOnline) return
        val ctx = DungeonContext.get() ?: return
        if (!ctx.playerManager.isPlayerInDungeon(attacker)) return

        val byLoadoutId = list.groupBy { it.loadoutId }
        for ((loadoutId, rawHits) in byLoadoutId) {
            val handler = LoadoutWeaponEffectRegistry.get(loadoutId)
            if (handler != null) {
                val hits = rawHits.map { r -> WeaponHit(r.victim, r.damage, r.isMainTarget) }
                handler.processHits(attacker, hits, plugin)
            }
        }
    }
}
