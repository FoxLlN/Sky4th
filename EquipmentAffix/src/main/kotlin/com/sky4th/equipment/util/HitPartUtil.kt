
package com.sky4th.equipment.util

import org.bukkit.attribute.Attribute
import org.bukkit.event.entity.EntityDamageByEntityEvent

/**
 * 命中部位检测工具类
 */
object HitPartUtil {

    /**
     * 获取攻击击中的部位
     * @param event 实体伤害事件
     * @return 命中的部位
     */
    fun getHitPart(event: EntityDamageByEntityEvent): HitPart {
        val victim = event.entity as? org.bukkit.entity.Player ?: return HitPart.BODY
        val damager = event.damager

        // 获取被攻击者的碰撞箱
        val victimBox = victim.boundingBox

        // 获取击中位置的精确坐标
        val hitPosition = if (damager is org.bukkit.entity.Projectile) {
            // 如果是弹射物，直接获取弹射物位置
            damager.location.toVector()
        } else {
            // 如果是实体攻击，使用光线追踪法获取击中位置
            val eyeLocation = (damager as? org.bukkit.entity.LivingEntity)?.eyeLocation ?: return HitPart.BODY
            val rayStart = eyeLocation.toVector()
            val rayDirection = eyeLocation.direction
            // 进行光线追踪，最大距离设为攻击距离（比如4格）
            val result = victimBox.rayTrace(rayStart, rayDirection, 4.0)
            if (result != null) {
                result.hitPosition
            } else {
                // 如果光线追踪失败，直接返回BODY
                return HitPart.BODY
            }
        }

        // 获取被攻击者的位置和碰撞箱信息来做相对高度判断
        val victimMinY = victimBox.minY // 碰撞箱最低点 (脚)
        val victimMaxY = victimBox.maxY // 碰撞箱最高点 (头)
        val hitY = hitPosition.y

        // 获取玩家的scale属性
        val scale = victim.getAttribute(Attribute.GENERIC_SCALE)?.value ?: 1.0

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

        // 判断命中部位
        return when {
            hitY >= headThreshold -> HitPart.HEAD
            hitY >= legThreshold -> HitPart.BODY
            else -> HitPart.LEG
        }
    }
}
