package sky4th.bettermc.listeners

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.projectiles.ProjectileSource
import sky4th.bettermc.command.FeatureManager
import sky4th.bettermc.config.ConfigManager

/**
 * 玩家部位受击监听器
 *
 * 根据玩家被击中的部位调整伤害倍率
 * 头部伤害倍率1.5，躯干1.0，腿部0.7
 * 近战攻击时不与跳劈叠加，如果跳劈则走跳劈逻辑
 */
class PlayerHitboxListener : Listener {

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH)
    fun onPlayerDamaged(event: EntityDamageByEntityEvent) {
        // 检查功能是否启用
        if (!FeatureManager.isFeatureEnabled("player-hitbox")) return

        // 检查受害者是否是玩家
        val victim = event.entity as? Player ?: return

        // 检查是否是近战攻击（攻击者是实体）或远程攻击（攻击者是弹射物）
        val isProjectile = event.damager is org.bukkit.entity.Projectile
        val isMelee = !isProjectile && event.damager is org.bukkit.entity.LivingEntity

        if (!isProjectile && !isMelee) return

        // 检查是否是跳劈（暴击）
        val isCriticalHit = isMelee && isCriticalHit(event.damager as org.bukkit.entity.LivingEntity)

        // 如果是跳劈，则不应用部位伤害倍率
        if (isCriticalHit) return

        // 获取击中部位
        val hitPart = getHitPart(event)

        // 根据部位应用伤害倍率
        val damageMultiplier = when (hitPart) {
            HitPart.HEAD -> ConfigManager.headDamageMultiplier
            HitPart.BODY -> ConfigManager.bodyDamageMultiplier
            HitPart.LEGS -> ConfigManager.legsDamageMultiplier
        }

        // 应用伤害倍率
        event.damage = event.damage * damageMultiplier
    }

    /**
     * 判断是否是跳劈（暴击）
     */
    private fun isCriticalHit(attacker: org.bukkit.entity.LivingEntity): Boolean {
        // 检查攻击者是否在空中且在下落
        return attacker is Player && 
               !attacker.isOnGround && 
               attacker.velocity.y < 0
    }

    /**
     * 获取被击中的部位
     */
    private fun getHitPart(event: EntityDamageByEntityEvent): HitPart {
        val victim = event.entity as Player
        val damager = event.damager
        
        // 获取被攻击者的碰撞箱
        val victimBox = victim.boundingBox
        
        // 获取击中位置的精确坐标
        val hitPosition = if (damager is org.bukkit.entity.Projectile) {
            // 如果是弹射物，直接获取弹射物位置
            val projectilePos = damager.location.toVector()
            // 打印射箭的命中点信息
            projectilePos
        } else {
            // 如果是实体攻击，使用光线追踪法获取击中位置
            val eyeLocation = (damager as org.bukkit.entity.LivingEntity).eyeLocation
            val rayStart = eyeLocation.toVector()
            val rayDirection = eyeLocation.direction
            
            // 进行光线追踪，最大距离设为攻击距离（比如4格）
            val result = victimBox.rayTrace(rayStart, rayDirection, 4.0)
            
            if (result != null) {
                result.hitPosition
            } else {
                // 如果光线追踪失败，直接返回BODY
                // 失败情况极少（只有攻击者视角完全偏离时才会发生）
                return HitPart.BODY
            }
        }
        
        // 获取被攻击者的位置和碰撞箱信息来做相对高度判断
        val victimMinY = victimBox.minY // 碰撞箱最低点 (脚)
        val victimMaxY = victimBox.maxY // 碰撞箱最高点 (头)
        val hitY = hitPosition.y
        
        // 获取玩家的scale属性
        val scale = victim.getAttribute(org.bukkit.attribute.Attribute.GENERIC_SCALE)?.value ?: 1.0
        
        // 根据玩家身高1.75格的比例来划分部位
        // 腿部：0.7格 (0.7 / 1.75 = 40%)
        // 身体：1.37 - 0.7 = 0.745格 (0.745 / 1.75 = 38.2%)
        // 头部：1.75 - 1.37 = 0.38格 (0.38 / 1.75 = 21.7%)
        val totalHeight = victimMaxY - victimMinY
        val legHeight = (0.7 / 1.75) * totalHeight * scale
        val bodyHeight = ((1.37 - 0.7) / 1.75) * totalHeight * scale
        
        // 计算阈值
        val legThreshold = victimMinY + legHeight // 腿部区域顶部
        val headThreshold = victimMaxY - ((1.75 - 1.37) / 1.75) * totalHeight * scale // 头部区域底部
        
        val hitPart = when {
            hitY >= headThreshold -> HitPart.HEAD
            hitY <= legThreshold -> HitPart.LEGS
            else -> HitPart.BODY
        }
        return hitPart
    }

    /**
     * 击中部位枚举
     */
    private enum class HitPart {
        HEAD,
        BODY,
        LEGS
    }
}
