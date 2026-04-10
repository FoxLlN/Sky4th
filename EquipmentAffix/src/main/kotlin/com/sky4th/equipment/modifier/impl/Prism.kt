package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import com.sky4th.equipment.data.NBTEquipmentDataManager
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack
import kotlin.math.min

/**
 * 棱镜词条
 * 效果：累计棱镜护盾，折射弹射物攻击到周围敌人
 * 
 * 1级：每25秒累计1层棱镜护盾，最多1层，可以折射一次弹射物伤害到周围另一个敌人
 * 2级：每20秒累计1层棱镜护盾，最多2层，可以折射一次弹射物伤害到周围另一个敌人
 * 3级：每15秒累计1层棱镜护盾，最多3层，可以折射一次弹射物伤害到周围另一个敌人
 */
class Prism : com.sky4th.equipment.modifier.ConfiguredModifier("prism") {

    companion object {
        // 折射搜索半径
        private const val REFLECT_RADIUS = 5.0

        // 折射伤害比例
        private const val REFLECT_DAMAGE_RATIO = 0.8
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
        // 只处理弹射物伤害事件
        if (event !is EntityDamageByEntityEvent) return
        if (playerRole != PlayerRole.DEFENDER) return

        // 检查是否是弹射物造成的伤害
        val projectile = event.damager
        if (!org.bukkit.entity.Projectile::class.java.isInstance(projectile)) return

        // 使用系统的资源充能系统获取当前层数
        val currentLayers = NBTEquipmentDataManager.getAffixResource(item, getAffixId())
        if (currentLayers <= 0) return

        // 消耗1层棱镜护盾
        NBTEquipmentDataManager.consumeAffixResource(item, getAffixId(), 1)

        // 取消原伤害
        event.isCancelled = true

        // 查找周围可以折射的目标
        val nearbyEntities = player.getNearbyEntities(REFLECT_RADIUS, REFLECT_RADIUS, REFLECT_RADIUS)
        val targets = nearbyEntities.filter { entity ->
            // 排除自己
            entity != player &&
            // 只选择活体实体
            entity is org.bukkit.entity.LivingEntity
        }

        // 如果有目标，选择最近的一个进行折射
        if (targets.isNotEmpty()) {
            val target = targets.minByOrNull { it.location.distance(player.location) } ?: return

            // 计算折射伤害
            val originalDamage = event.damage
            val reflectedDamage = originalDamage * REFLECT_DAMAGE_RATIO

            // 在玩家位置生成新的弹射物射向目标
            val world = player.world
            val spawnLocation = player.location.clone().add(0.0, 1.5, 0.0)

            // 计算从玩家到目标的向量
            val direction = target.location.clone().subtract(spawnLocation).toVector().normalize()

            // 根据原弹射物类型生成对应的新弹射物
            val reflectedProjectile: org.bukkit.entity.Projectile = when (projectile) {
                is org.bukkit.entity.Arrow -> {
                    val arrow = world.spawnArrow(spawnLocation, direction, 1.5f, 0.0f)
                    arrow.damage = reflectedDamage
                    arrow.shooter = player
                    arrow.isCritical = projectile.isCritical
                    arrow
                }
                is org.bukkit.entity.SpectralArrow -> {
                    val arrow = world.spawnArrow(spawnLocation, direction, 1.5f, 0.0f) as org.bukkit.entity.SpectralArrow
                    arrow.damage = reflectedDamage
                    arrow.shooter = player
                    arrow
                }
                is org.bukkit.entity.TippedArrow -> {
                    val arrow = world.spawnArrow(spawnLocation, direction, 1.5f, 0.0f) as org.bukkit.entity.TippedArrow
                    arrow.damage = reflectedDamage
                    arrow.shooter = player
                    arrow
                }
                is org.bukkit.entity.Trident -> {
                    val trident = world.spawn(spawnLocation, org.bukkit.entity.Trident::class.java)
                    trident.velocity = direction.multiply(1.5)
                    trident.damage = reflectedDamage
                    trident.shooter = player
                    trident
                }
                is org.bukkit.entity.Fireball -> {
                    val fireball = world.spawn(spawnLocation, org.bukkit.entity.Fireball::class.java)
                    fireball.velocity = direction.multiply(1.0)
                    fireball.shooter = player
                    fireball.yield = (reflectedDamage / 5.0).toFloat()
                    fireball
                }
                is org.bukkit.entity.SmallFireball -> {
                    val fireball = world.spawn(spawnLocation, org.bukkit.entity.SmallFireball::class.java)
                    fireball.velocity = direction.multiply(1.0)
                    fireball.shooter = player
                    fireball.yield = (reflectedDamage / 10.0).toFloat()
                    fireball
                }
                is org.bukkit.entity.WitherSkull -> {
                    val skull = world.spawn(spawnLocation, org.bukkit.entity.WitherSkull::class.java)
                    skull.velocity = direction.multiply(1.0)
                    skull.shooter = player
                    skull
                }
                is org.bukkit.entity.Egg -> {
                    val egg = world.spawn(spawnLocation, org.bukkit.entity.Egg::class.java)
                    egg.velocity = direction.multiply(1.5)
                    egg.shooter = player
                    egg
                }
                is org.bukkit.entity.Snowball -> {
                    val snowball = world.spawn(spawnLocation, org.bukkit.entity.Snowball::class.java)
                    snowball.velocity = direction.multiply(1.5)
                    snowball.shooter = player
                    snowball
                }
                is org.bukkit.entity.EnderPearl -> {
                    val pearl = world.spawn(spawnLocation, org.bukkit.entity.EnderPearl::class.java)
                    pearl.velocity = direction.multiply(1.5)
                    pearl.shooter = player
                    pearl
                }
                is org.bukkit.entity.FishHook -> {
                    val hook = world.spawn(spawnLocation, org.bukkit.entity.FishHook::class.java)
                    hook.velocity = direction.multiply(1.5)
                    hook.shooter = player
                    hook
                }
                else -> {
                    // 默认使用箭矢
                    val arrow = world.spawnArrow(spawnLocation, direction, 1.5f, 0.0f)
                    arrow.damage = reflectedDamage
                    arrow.shooter = player
                    arrow
                }
            }
        }
    }

    override fun onInit(player: Player, item: ItemStack, level: Int) {
        // 创建PrismData并注册到UnifiedModifierManager
        val prismData = com.sky4th.equipment.modifier.manager.handlers.PrismHandler.PrismData(
            uuid = player.uniqueId,
            level = level,
            item = item
        )
        com.sky4th.equipment.modifier.manager.UnifiedModifierManager.addPlayerAffix(player, getAffixId(), prismData)
    }

    override fun onRemove(player: Player) {
        // 从统一管理器中移除棱镜词条数据
        com.sky4th.equipment.modifier.manager.UnifiedModifierManager.removePlayerAffix(player, getAffixId())
    }
}
