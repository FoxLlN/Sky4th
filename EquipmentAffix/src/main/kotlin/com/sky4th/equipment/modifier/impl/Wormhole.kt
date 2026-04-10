
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.Event

import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/**
 * 虫洞词条
 * 效果：攻击未命中后传送到箭矢落点
 * 传送消耗10点耐久，冷却30秒
 */
class Wormhole : com.sky4th.equipment.modifier.ConfiguredModifier("wormhole") {

    companion object {
        // 冷却时间键
        private val COOLDOWN_KEY = NamespacedKey("equipment_affix", "wormhole_cooldown")
        // 耐久度消耗
        private const val DURABILITY_COST = 10
        // 冷却时间（秒）
        private const val COOLDOWN_SECONDS = 30
    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(
            ProjectileHitEvent::class.java
        )

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        if (event !is ProjectileHitEvent) return

        // 检查是否命中了实体
        if (event.hitEntity != null) return

        // 检查是否在冷却中
        if (isOnCooldown(player)) return

        // 检查耐久度是否足够
        if (!hasEnoughDurability(item)) return

        val targetLocation = event.hitBlock?.location ?: return

        val safeLocation = findSafeTeleportLocation(targetLocation) ?: return
        
        // 消耗耐久度
        consumeDurability(item)

        teleportToHitLocation(player, safeLocation)

        // 设置冷却时间
        setCooldown(player)

        // 播放传送特效
        playTeleportEffect(player)
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
     * 传送到命中位置（支持乘客）
     */
    private fun teleportToHitLocation(player: Player, teleportLocation: org.bukkit.Location) {
        // 保留玩家的朝向
        teleportLocation.yaw = player.location.yaw
        teleportLocation.pitch = player.location.pitch

        // 使用 Paper API 的异步传送，保留乘客
        player.teleportAsync(
            teleportLocation,
            org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN,
            io.papermc.paper.entity.TeleportFlag.EntityState.RETAIN_PASSENGERS
        ).thenAccept { success ->
            org.bukkit.Bukkit.getScheduler().runTask(
                com.sky4th.equipment.EquipmentAffix.instance,
                Runnable {
                    if (success) {
                        playTeleportEffect(player)
                    }
                }
            )
        }.exceptionally { ex ->
            null
        }
    }

    /**
     * 在指定位置附近搜索安全传送位置
     * 安全位置：地面有方块，上方有2格空格
     */
    private fun findSafeTeleportLocation(location: org.bukkit.Location): org.bukkit.Location? {
        val world = location.world ?: return null
        val centerX = location.blockX
        val centerY = location.blockY
        val centerZ = location.blockZ

        // 搜索范围：水平方向1格，垂直方向1格
        val horizontalRange = 1
        val verticalRange = 1

        // 从中心开始向外搜索
        for (dx in -horizontalRange..horizontalRange) {
            for (dz in -horizontalRange..horizontalRange) {
                for (dy in -verticalRange..verticalRange) {
                    val x = centerX + dx
                    val y = centerY + dy
                    val z = centerZ + dz

                    // 检查位置是否安全
                    if (isSafeLocation(world, x, y, z)) {
                        // 返回安全位置（玩家站在方块上，所以y+1）
                        val safeLocation = org.bukkit.Location(world, x.toDouble() + 0.5, y.toDouble() + 1.0, z.toDouble() + 0.5)
                        return safeLocation
                    }
                }
            }
        }

        return null
    }

    /**
     * 检查指定位置是否安全
     * 安全位置：地面有方块，上方有2格空格
     */
    private fun isSafeLocation(world: org.bukkit.World, x: Int, y: Int, z: Int): Boolean {
        // 检查地面是否有方块
        val groundBlock = world.getBlockAt(x, y, z)
        if (!groundBlock.type.isSolid) {
            return false
        }

        // 检查上方2格是否为空
        val block1 = world.getBlockAt(x, y + 1, z)
        val block2 = world.getBlockAt(x, y + 2, z)

        return !block1.type.isSolid && !block2.type.isSolid
    }


    /**
     * 播放传送特效
     */
    private fun playTeleportEffect(player: Player) {
        val location = player.location

        // 播放传送音效
        player.world.playSound(
            location,
            org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT,
            1.0f,
            1.0f
        )

        // 生成紫色粒子效果
        player.world.spawnParticle(
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
