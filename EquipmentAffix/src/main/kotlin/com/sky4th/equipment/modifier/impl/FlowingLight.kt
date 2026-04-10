package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import com.sky4th.equipment.data.NBTEquipmentDataManager
import com.sky4th.equipment.modifier.manager.UnifiedModifierManager
import com.sky4th.equipment.modifier.manager.handlers.FlowingLightHandler
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * 流光词条
 * 效果：移动时累计流光层数，受到攻击有概率消耗层数抵挡伤害
 * 
 * 1级：每移动20格叠加1层，最多5层，每次有20%概率闪避本次伤害
 * 2级：每移动10格叠加1层，最多5层，每次有25%概率闪避本次伤害
 * 3级：每移动10格叠加1层，最多10层，每次有30%概率闪避本次伤害
 *
 * 停止移动1秒后开始掉层数，每秒掉1层
 */
class FlowingLight : com.sky4th.equipment.modifier.ConfiguredModifier("flowing_light") {

    companion object {
        // 每级配置：（闪避概率)
        private val DODGE_BONUS_PER_LAYER = doubleArrayOf(0.20, 0.25, 0.30)
        // 每级配置：(移动格数, 最大层数, 每层所需格数的倒数)
        private val CONFIG = arrayOf(
            20 to 5,  // 1级：20格，5层，20%闪避
            15 to 5,  // 2级：15格，5层，25%闪避
            10 to 10  // 3级：10格，10层，30%闪避
        )
    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(
            PlayerMoveEvent::class.java,
            EntityDamageEvent::class.java
        )

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        when (event) {
            is PlayerMoveEvent -> handlePlayerMove(event, player, item, level)
            is EntityDamageEvent -> handleDamage(event, player, item, level, playerRole)
        }
    }

    /**
     * 处理玩家移动事件
     */
    private fun handlePlayerMove(
        event: PlayerMoveEvent,
        player: Player,
        item: ItemStack,
        level: Int
    ) {
        // 获取FlowingLightData
        val flowingLightData = UnifiedModifierManager.getAffixData(player, getAffixId()) as? FlowingLightHandler.FlowingLightData ?: return

        // 获取配置
        val config = CONFIG.getOrNull(level - 1) ?: return
        val (blocksPerLayer, maxLayers) = config
        
        // 计算移动距离的平方（只计算水平移动）
        val deltaX = event.to.x - event.from.x
        val deltaZ = event.to.z - event.from.z
        val distanceSquared = deltaX * deltaX + deltaZ * deltaZ

        // 如果移动距离很小（例如只是转头），忽略（比较平方值避免开方）
        if (distanceSquared < 0.01) return

        flowingLightData.updateMoveTime()

        // 累加移动距离（只在需要时才开方）
        flowingLightData.totalDistance += Math.sqrt(distanceSquared)
        // 计算可以叠加的层数
        val newLayers = (flowingLightData.totalDistance / blocksPerLayer).toInt()

        // 如果有新层数可以叠加
        if (newLayers > 0) {
            // 使用缓存的层数，减少NBT读取
            val currentLayers = flowingLightData.currentLayers
            val finalLayers = (currentLayers + newLayers).coerceAtMost(maxLayers)

            // 更新层数
            if (finalLayers > currentLayers) {
                NBTEquipmentDataManager.setAffixResource(item, getAffixId(), finalLayers)
                flowingLightData.currentLayers = finalLayers

                // 如果达到最大层数，清空累计移动距离；否则保留余数
                if (finalLayers >= maxLayers) {
                    flowingLightData.totalDistance = 0.0
                } else {
                    flowingLightData.totalDistance = flowingLightData.totalDistance % blocksPerLayer
                }
            } else if (currentLayers >= maxLayers) {
                // 如果当前已经是最大层数，也清空累计移动距离，防止无限累计
                flowingLightData.totalDistance = 0.0
            }
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

        // 获取配置
        val dodgeChance = DODGE_BONUS_PER_LAYER.getOrNull(level - 1) ?: return

        // 获取FlowingLightData
        val flowingLightData = UnifiedModifierManager.getAffixData(player, getAffixId()) as? FlowingLightHandler.FlowingLightData ?: return

        // 获取当前层数（使用缓存）
        val currentLayers = flowingLightData.currentLayers
        if (currentLayers <= 0) return

        // 判定是否闪避
        if (Random.nextDouble() < dodgeChance) {
            // 使用系统的资源充能系统消耗1层
            NBTEquipmentDataManager.consumeAffixResource(item, getAffixId(), 1)
            flowingLightData.currentLayers = (currentLayers - 1).coerceAtLeast(0)

            // 取消伤害
            event.isCancelled = true

            // 生成粒子效果
            val location = player.location.clone().add(0.0, 1.0, 0.0)
            for (i in 0 until 10) {
                val offsetX = (Math.random() - 0.5) * 1.5
                val offsetY = Math.random() * 2.0
                val offsetZ = (Math.random() - 0.5) * 1.5

                player.world.spawnParticle(
                    org.bukkit.Particle.END_ROD,
                    location.x + offsetX,
                    location.y + offsetY,
                    location.z + offsetZ,
                    1,
                    0.0, 0.0, 0.0,
                    0.0
                )
            }
        }
    }

    override fun onInit(player: Player, item: ItemStack, level: Int) {
        // 初始化玩家数据
        var currentLayers = NBTEquipmentDataManager.getAffixResource(item, getAffixId())

        // 创建FlowingLightData并注册到UnifiedModifierManager
        val flowingLightData = FlowingLightHandler.FlowingLightData(
            uuid = player.uniqueId,
            currentLayers = currentLayers,
            totalDistance = 0.0,
            item = item  // 直接存储物品引用
        )
        UnifiedModifierManager.addPlayerAffix(player, getAffixId(), flowingLightData)
    }

    override fun onRemove(player: Player) {
        // 从UnifiedModifierManager中移除FlowingLightData
        UnifiedModifierManager.removePlayerAffix(player, getAffixId())
    }
}
