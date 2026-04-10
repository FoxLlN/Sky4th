package com.sky4th.equipment.modifier.manager.handlers

import com.sky4th.equipment.modifier.manager.AffixHandler
import com.sky4th.equipment.modifier.manager.AffixData
import com.sky4th.equipment.util.GlowingEntityUtil
import org.bukkit.Color
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import java.lang.ref.WeakReference

/**
 * 回声定位词条处理器
 * 负责处理回声定位词条的发光效果逻辑
 *
 * 功能：
 * 1. 玩家潜行时，周围3格内的生物会发光
 * 2. 发光效果持续2秒
 * 3. 每2秒检查一次玩家潜行状态
 *
 * 注意：
 * - 配置数据由回声定位词条类管理
 * - 玩家数据由UnifiedModifierManager管理
 * - 本处理器只负责发光效果逻辑
 */
class EchoLocationHandler : AffixHandler {

    // 检测范围（3格）
    private val DETECTION_RANGE = 3.0

    // 发光颜色（青色，适合回声定位）
    private val GLOW_COLOR = Color.fromRGB(0, 255, 255)

    // 发光持续时间（1秒）(脱离后继续发光1秒)
    private val GLOW_DURATION = 1L

    /**
     * 回声定位词条数据
     */
    class EchoLocationData(
        val uuid: java.util.UUID,
        val glowingEntities: MutableSet<Int> = mutableSetOf(),
        var item: org.bukkit.inventory.ItemStack  // 直接存储物品引用
    ) : AffixData() {
        override val interval: Int = 10  // 每10 tick（0.5秒）检查一次
    }

    override fun process(player: Player, data: AffixData) {
        val echoData = data as? EchoLocationData ?: return
        val glowingEntities = echoData.glowingEntities

        // 获取周围3格内的所有生物
        val nearbyEntities = player.getNearbyEntities(DETECTION_RANGE, DETECTION_RANGE, DETECTION_RANGE)
            .filter { it is LivingEntity && it !== player && it !is org.bukkit.entity.ItemDisplay && it !is org.bukkit.entity.TextDisplay }

        // 对每个生物设置发光效果
        nearbyEntities.forEach { entity ->
            val entityId = entity.entityId

            // 如果该实体尚未在集合中，则设置发光
            if (!glowingEntities.contains(entityId)) {
                GlowingEntityUtil.setGlowForPlayer(entity, player, GLOW_DURATION, GLOW_COLOR)
                glowingEntities.add(entityId)
            } else {
                // 实体已在集合中，刷新发光效果
                GlowingEntityUtil.refreshGlowForPlayer(entity, player, GLOW_DURATION)
            }
        }
    }
}
