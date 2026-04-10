
package com.sky4th.equipment.modifier

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.*
import com.sky4th.equipment.event.PlayerDodgeEvent
import org.bukkit.event.player.*
import org.bukkit.event.block.*
import org.bukkit.event.inventory.*
import org.bukkit.event.enchantment.*

/**
 * 词条事件分发器
 * 监听所有游戏事件，并将它们转发给ModifierManager处理
 */
class ModifierEventDispatcher : Listener {

    // 实体事件
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityDamage(event: EntityDamageEvent) {
        ModifierManager.instance.handleEvent(event)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerDodge(event: PlayerDodgeEvent) {
        ModifierManager.instance.handleEvent(event)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityDeath(event: EntityDeathEvent) {
        ModifierManager.instance.handleEvent(event)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityShootBow(event: EntityShootBowEvent) {
        ModifierManager.instance.handleEvent(event)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onProjectileHit(event: ProjectileHitEvent) {
        ModifierManager.instance.handleEvent(event)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityRegainHealth(event: EntityRegainHealthEvent) {
        ModifierManager.instance.handleEvent(event)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityPotionEffect(event: EntityPotionEffectEvent) {
        ModifierManager.instance.handleEvent(event)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityTarget(event: EntityTargetEvent) {
        ModifierManager.instance.handleEvent(event)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityTame(event: EntityTameEvent) {
        ModifierManager.instance.handleEvent(event)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityTeleport(event: EntityTeleportEvent) {
        ModifierManager.instance.handleEvent(event)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerPickupItem(event: EntityPickupItemEvent) {
        if (event.entity is org.bukkit.entity.Player) {
            ModifierManager.instance.handleEvent(event)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerPickupItem(event: EntityToggleGlideEvent) {
        if (event.entity is org.bukkit.entity.Player) {
            ModifierManager.instance.handleEvent(event)
        }
    }

    // 玩家事件
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerMove(event: PlayerMoveEvent) {
        ModifierManager.instance.handleEvent(event)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerChangedWorld(event: PlayerChangedWorldEvent) {
        ModifierManager.instance.handleEvent(event)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerItemBreak(event: PlayerItemBreakEvent) {
        ModifierManager.instance.handleEvent(event)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerItemConsume(event: PlayerItemConsumeEvent) {
        ModifierManager.instance.handleEvent(event)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerItemDamage(event: PlayerItemDamageEvent) {
        ModifierManager.instance.handleEvent(event)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerExpChangeEvent(event: PlayerExpChangeEvent) {
        ModifierManager.instance.handleEvent(event)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerLevelChange(event: PlayerLevelChangeEvent) {
        ModifierManager.instance.handleEvent(event)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerDropItem(event: PlayerToggleSneakEvent) {
        ModifierManager.instance.handleEvent(event)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerDropItem(event: PlayerToggleSprintEvent) {
        ModifierManager.instance.handleEvent(event)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        ModifierManager.instance.handleEvent(event)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        ModifierManager.instance.handleEvent(event)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerShearEntity(event: PlayerShearEntityEvent) {
        ModifierManager.instance.handleEvent(event)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerFish(event: PlayerFishEvent) {
        ModifierManager.instance.handleEvent(event)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerBedEnter(event: PlayerBedEnterEvent) {
        ModifierManager.instance.handleEvent(event)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerBedLeave(event: PlayerBedLeaveEvent) {
        ModifierManager.instance.handleEvent(event)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerClose(event: InventoryCloseEvent) {
        ModifierManager.instance.handleEvent(event)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerClose(event: InventoryOpenEvent) {
        ModifierManager.instance.handleEvent(event)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerOpen(event: ProjectileLaunchEvent) {
        ModifierManager.instance.handleEvent(event)
    }

    // 方块事件
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockBreak(event: BlockBreakEvent) {
        ModifierManager.instance.handleEvent(event)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockPlace(event: BlockPlaceEvent) {
        ModifierManager.instance.handleEvent(event)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockDamage(event: BlockDamageEvent) {
        ModifierManager.instance.handleEvent(event)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockDropItem(event: BlockDropItemEvent) {
        ModifierManager.instance.handleEvent(event)
    }

    // 附魔事件
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEnchantItem(event: EnchantItemEvent) {
        ModifierManager.instance.handleEvent(event)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPrepareItemEnchant(event: PrepareItemEnchantEvent) {
        ModifierManager.instance.handleEvent(event)
    }
}
