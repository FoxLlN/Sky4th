package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import com.sky4th.equipment.data.NBTEquipmentDataManager
import com.sky4th.equipment.modifier.manager.UnifiedModifierManager
import com.sky4th.equipment.modifier.manager.handlers.GaleWindHandler
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack

/**
 * 疾风词条
 * 效果：移动时累计疾风层数，下次攻击释放造成额外伤害
 *
 * 1级：每移动10格叠加1层，最多5层，每层增加3%伤害
 * 2级：每移动8格叠加1层，最多5层，每层增加4%伤害
 * 3级：每移动6格叠加1层，最多5层，每层增加5%伤害
 * 
 * 停止移动1秒后开始掉层数，每秒掉1层
 */
class GaleWind : com.sky4th.equipment.modifier.ConfiguredModifier("gale_wind") {

    companion object {
        // 每级配置：(增伤)
        private val DAMAGE_BONUS_PER_LAYER = doubleArrayOf(0.03, 0.04, 0.05)

        // 每级配置：(每层所需格数, 最大层数, 每层所需格数的倒数)
        private val MOVEMENT_CONFIG = arrayOf(
            10 to 5,  // 1级：10格，5层
            8 to 5,     // 2级：8格，5层
            6 to 5      // 3级：6格，5层
        )
    }

    // 不再需要单独的玩家数据缓存，所有数据统一存储在 GaleWindData 中
    // 监听玩家移动事件、攻击事件和世界切换事件
    override fun getEventTypes(): List<Class<out Event>> =
        listOf(
            PlayerMoveEvent::class.java,
            EntityDamageByEntityEvent::class.java
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
            is EntityDamageByEntityEvent -> handleDamage(event, player, item, level, playerRole)
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
        // 获取GaleWindData
        val galeWindData = UnifiedModifierManager.getAffixData(player, getAffixId()) as? GaleWindHandler.GaleWindData ?: return

        // 获取配置
        val config = MOVEMENT_CONFIG.getOrNull(level - 1) ?: return
        val (blocksPerLayer, maxLayers) = config

        // 计算移动距离的平方（只计算水平移动）
        val deltaX = event.to.x - event.from.x
        val deltaZ = event.to.z - event.from.z
        val distanceSquared = deltaX * deltaX + deltaZ * deltaZ

        // 如果移动距离很小（例如只是转头），忽略（比较平方值避免开方）
        if (distanceSquared < 0.01) return
        
        galeWindData.updateMoveTime()

        // 累加移动距离（只在需要时才开方）
        galeWindData.totalDistance += Math.sqrt(distanceSquared)
        
        // 计算可以叠加的层数
        val newLayers = (galeWindData.totalDistance / blocksPerLayer).toInt()
        
        // 如果有新层数可以叠加
        if (newLayers > 0) {
            // 获取当前层数
            val currentLayers = galeWindData.currentLayers
            val finalLayers = (currentLayers + newLayers).coerceAtMost(maxLayers)
            
            // 更新层数
            if (finalLayers > currentLayers) {
                NBTEquipmentDataManager.setAffixResource(item, getAffixId(), finalLayers)
                galeWindData.currentLayers = finalLayers

                // 如果达到最大层数，清空累计移动距离；否则保留余数
                if (finalLayers >= maxLayers) {
                    galeWindData.totalDistance = 0.0
                } else {
                    galeWindData.totalDistance = galeWindData.totalDistance % blocksPerLayer
                }
            } else if (currentLayers >= maxLayers) {
                // 如果当前已经是最大层数，也清空累计移动距离，防止无限累计
                galeWindData.totalDistance = 0.0
            }
        }
    }

    /**
     * 处理攻击事件
     */
    private fun handleDamage(
        event: EntityDamageByEntityEvent,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 只处理攻击事件
        if (playerRole != PlayerRole.ATTACKER) return

        // 检查是否是玩家攻击
        if (event.damager != player) return

        // 获取GaleWindData
        val galeWindData = UnifiedModifierManager.getAffixData(player, getAffixId()) as? GaleWindHandler.GaleWindData ?: return

        // 获取当前疾风层数
        val currentLayers = galeWindData.currentLayers
        if (currentLayers <= 0) return

        // 获取每层伤害加成
        val damageBonusPerLayer = DAMAGE_BONUS_PER_LAYER.getOrNull(level - 1) ?: return
        if (damageBonusPerLayer <= 0) return

        // 计算额外伤害
        val baseDamage = event.damage
        val totalBonus = baseDamage * damageBonusPerLayer * currentLayers
        event.damage = baseDamage + totalBonus

        // 消耗所有层数
        NBTEquipmentDataManager.setAffixResource(item, getAffixId(), 0)
        galeWindData.currentLayers = 0
    }

    override fun onInit(player: Player, item: ItemStack, level: Int) {
        // 从物品的NBT中读取当前层数
        val currentLayers = NBTEquipmentDataManager.getAffixResource(item, getAffixId())

        // 创建GaleWindData并注册到UnifiedModifierManager
        val galeWindData = GaleWindHandler.GaleWindData(
            uuid = player.uniqueId,
            currentLayers = currentLayers,
            totalDistance = 0.0,
            item = item
        )
        UnifiedModifierManager.addPlayerAffix(player, getAffixId(), galeWindData)
    }

    override fun onRemove(player: Player) {
        // 从UnifiedModifierManager中移除GaleWindData
        UnifiedModifierManager.removePlayerAffix(player, getAffixId())
    }
}
