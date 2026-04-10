package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/**
 * 置换词条
 * 效果：命中目标后与命中目标交换位置
 * 传送消耗15点耐久，冷却30秒
 */
class Swap : com.sky4th.equipment.modifier.ConfiguredModifier("swap") {

    companion object {
        // 冷却时间键
        private val COOLDOWN_KEY = NamespacedKey("equipment_affix", "swap_cooldown")
        // 耐久度消耗
        private const val DURABILITY_COST = 15
        // 冷却时间（秒）
        private const val COOLDOWN_SECONDS = 30
    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(EntityDamageByEntityEvent::class.java)

   
    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 只处理实体攻击实体事件，且玩家必须是攻击者
        if (event !is EntityDamageByEntityEvent || playerRole != PlayerRole.ATTACKER) {
            return
        }

        // 检查是否是弹射物造成的伤害
        val damager = event.damager
        if (!org.bukkit.entity.Projectile::class.java.isInstance(damager)) {
            return
        }
        val projectile = damager as org.bukkit.entity.Projectile

        // 确保弹射物的射击者是玩家
        if (projectile.shooter !is Player || projectile.shooter != player) {
            return
        }

        // 检查是否被盾牌完全格挡（伤害为0）
        if (event.damage <= 0) {
            return
        }

        // 获取受害者
        val targetEntity = event.entity

        // 只能与生物实体交换位置
        if (targetEntity !is LivingEntity) return

        // 检查是否在冷却中
        if (isOnCooldown(player)) return

        // 检查耐久度是否足够
        if (!hasEnoughDurability(item)) return

        // 获取玩家位置
        val playerLocation = player.location.clone()

        // 获取目标实体位置
        val targetLocation = targetEntity.location.clone()

        // 消耗耐久度
        consumeDurability(item)

        // 交换位置
        swapPositions(player, playerLocation, targetEntity, targetLocation)

        // 设置冷却时间
        setCooldown(player)

        // 播放传送特效
        playTeleportEffect(playerLocation)
        playTeleportEffect(targetLocation)
    }

    /**
     * 检查是否在冷却中
     */
    private fun isOnCooldown(player: Player): Boolean {
        val currentTime = System.currentTimeMillis() / 1000
        val lastUsed = player.persistentDataContainer.get(COOLDOWN_KEY, PersistentDataType.LONG) ?: 0
        return currentTime - lastUsed < COOLDOWN_SECONDS
    }

    /**
     * 设置冷却时间
     */
    private fun setCooldown(player: Player) {
        player.persistentDataContainer.set(
            COOLDOWN_KEY,
            PersistentDataType.LONG,
            System.currentTimeMillis() / 1000
        )
    }

    /**
     * 检查耐久度是否足够
     */
    private fun hasEnoughDurability(item: ItemStack): Boolean {
        val meta = item.itemMeta ?: return false
        if (meta.isUnbreakable) return true

        if (meta !is org.bukkit.inventory.meta.Damageable) return false

        val damageable = meta
        val currentDamage = damageable.damage

        // 优先使用元数据中的最大耐久度，如果没有则使用物品类型的默认耐久度
        val maxDurability = if (damageable.hasMaxDamage()) {
            damageable.maxDamage
        } else {
            item.type.maxDurability.toInt()
        }

        return currentDamage + DURABILITY_COST <= maxDurability
    }

    /**
     * 消耗耐久度
     */
    private fun consumeDurability(item: ItemStack) {
        val meta = item.itemMeta ?: return
        if (meta.isUnbreakable) return

        if (meta !is org.bukkit.inventory.meta.Damageable) return

        val damageable = meta
        val currentDamage = damageable.damage
        damageable.damage = currentDamage + DURABILITY_COST
        item.itemMeta = meta
    }

    /**
     * 交换两个实体的位置
     */
    private fun swapPositions(
        player: Player,
        playerLocation: org.bukkit.Location,
        targetEntity: LivingEntity,
        targetLocation: org.bukkit.Location
    ) {
        // 保留玩家的朝向
        val newPlayerLocation = targetLocation.clone()
        newPlayerLocation.yaw = player.location.yaw
        newPlayerLocation.pitch = player.location.pitch

        // 保留目标实体的朝向
        val newTargetLocation = playerLocation.clone()
        newTargetLocation.yaw = targetEntity.location.yaw
        newTargetLocation.pitch = targetEntity.location.pitch

        // 使用 Paper API 的异步传送
        player.teleportAsync(
            newPlayerLocation,
            org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN,
            io.papermc.paper.entity.TeleportFlag.EntityState.RETAIN_PASSENGERS
        ).thenAccept { success ->
            if (success) {
                playTeleportEffect(newPlayerLocation)
            }
        }.exceptionally { ex ->
            null
        }

        // 传送目标实体
        targetEntity.teleportAsync(
            newTargetLocation,
            org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN,
            io.papermc.paper.entity.TeleportFlag.EntityState.RETAIN_PASSENGERS
        ).thenAccept { success ->
            if (success) {
                playTeleportEffect(newTargetLocation)
            }
        }.exceptionally { ex ->
            null
        }
    }

    /**
     * 播放传送特效
     */
    private fun playTeleportEffect(location: org.bukkit.Location) {
        // 播放传送音效
        location.world?.playSound(
            location,
            org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT,
            1.0f,
            1.0f
        )

        // 生成紫色粒子效果
        location.world?.spawnParticle(
            org.bukkit.Particle.PORTAL,
            location,
            30,
            0.5,
            1.0,
            0.5,
            0.1
        )
    }

    override fun onRemove(player: Player) {
        // 不清理冷却数据，防止玩家通过换弓反复触发
    }
}
