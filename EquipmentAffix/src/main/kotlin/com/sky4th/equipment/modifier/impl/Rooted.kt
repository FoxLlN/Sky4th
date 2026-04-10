package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import com.sky4th.equipment.data.NBTEquipmentDataManager
import com.sky4th.equipment.modifier.manager.UnifiedModifierManager
import com.sky4th.equipment.modifier.manager.handlers.RootedHandler
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack

/**
 * 生根词条
 * 效果：站立不动时获得减伤和抗性提升
 *
 * 1级：每层提供3%减伤和2%抗击退
 * 2级：每层提供4%减伤和3%抗击退
 * 3级：每层提供5%减伤和4%抗击退
 *
 * 静止3秒后开始叠加层数，每秒叠加1层，最多5层
 * 移动时开始衰退层数，每秒衰退1层
 */
class Rooted : com.sky4th.equipment.modifier.ConfiguredModifier("rooted") {

    companion object {
        // 每级配置：(减伤百分比, 抗击退百分比)
        private val CONFIG = arrayOf(
            0.03 to 0.02,  // 1级：3%减伤，2%抗击退
            0.04 to 0.03,  // 2级：4%减伤，3%抗击退
            0.05 to 0.04,  // 3级：5%减伤，4%抗击退
        )

        // 静止距离阈值：1格
        private const val STATIONARY_DISTANCE_THRESHOLD = 0.01
        // 衰减配置
        private val DECAY_CHECK_TICKS = 1 * 20L  // 衰减间隔：1秒（20tick）
    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(
            EntityDamageEvent::class.java,
            PlayerMoveEvent::class.java
        )

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        when (event) {
            is EntityDamageEvent -> handleDamage(event, player, item, level, playerRole)
            is PlayerMoveEvent -> handlePlayerMove(event, player, item)
        }
    }

    /**
     * 处理伤害事件
     */
    private fun handleDamage(
        event: EntityDamageEvent,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 只处理作为防御者的事件
        if (playerRole != PlayerRole.DEFENDER) return

        // 检查伤害类型是否可以基础减伤
        if (!com.sky4th.equipment.util.DamageTypeUtil.isBasicReduction(event)) {
            return
        }

        val config = CONFIG.getOrNull(level - 1) ?: return
        val (damageReduction, knockbackResistance) = config

        // 使用系统的资源充能系统获取当前层数
        val currentLayers = NBTEquipmentDataManager.getAffixResource(item, getAffixId())
        if (currentLayers <= 0) return

        // 计算减伤比例
        val reductionMultiplier = 1.0 - (damageReduction * currentLayers)
        if (reductionMultiplier < 1.0) {
            // 应用减伤
            event.damage = event.damage * reductionMultiplier
        }
    }

    /**
     * 处理玩家移动事件
     * 检测玩家是否移动，移动时直接处理衰退逻辑
     */
    private fun handlePlayerMove(
        event: PlayerMoveEvent,
        player: Player,
        item: ItemStack
    ) {
        // 计算移动距离（只计算水平移动）
        val deltaX = event.to.x - event.from.x
        val deltaZ = event.to.z - event.from.z
        val distanceSquared = deltaX * deltaX + deltaZ * deltaZ

        // 判断是否移动（比较平方值避免开方）
        if (distanceSquared >= STATIONARY_DISTANCE_THRESHOLD * STATIONARY_DISTANCE_THRESHOLD) {
            val rootedData = UnifiedModifierManager.getAffixData(player, getAffixId()) as? RootedHandler.RootedData ?: return

            // 移动时立即重置静止时间
            rootedData.resetStationaryTicks()

            // 使用缓存的层数
            val currentLayers = rootedData.currentLayers
            if (currentLayers > 0) {
                // 检查是否可以衰减（有CD）
                val currentTime = System.currentTimeMillis()
                if (currentTime - rootedData.lastDecayTick >= 500) {
                    rootedData.lastDecayTick = currentTime
                    // 衰退1层
                    val newLayers = (currentLayers - 1).coerceAtLeast(0)
                    NBTEquipmentDataManager.setAffixResource(item, getAffixId(), newLayers)
                    // 更新缓存和抗击退属性
                    rootedData.currentLayers = newLayers
                    RootedHandler().updateKnockbackResistance(item, newLayers)
                }
            }
        }
    }

    override fun onInit(player: Player, item: ItemStack, level: Int) {
        // 初始化玩家数据，读取当前层数到缓存
        var currentLayers = NBTEquipmentDataManager.getAffixResource(item, getAffixId())

        // 创建RootedData并注册到UnifiedModifierManager
        val rootedData = RootedHandler.RootedData(
            uuid = player.uniqueId,
            currentLayers = currentLayers,
            item = item
        )
        UnifiedModifierManager.addPlayerAffix(player, getAffixId(), rootedData)
    }

    override fun onRemove(player: Player) {
        // 从UnifiedModifierManager中移除RootedData
        UnifiedModifierManager.removePlayerAffix(player, getAffixId())
    }
}
