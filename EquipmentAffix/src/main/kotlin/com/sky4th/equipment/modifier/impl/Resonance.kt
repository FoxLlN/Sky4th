package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import com.sky4th.equipment.modifier.manager.UnifiedModifierManager
import com.sky4th.equipment.modifier.manager.handlers.ResonanceHandler
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemStack

/**
 * 共振词条
 * 效果：连续破坏同种方块时，可以加快挖掘速度
 * 连续挖掘同种方块时，每块叠加一层"共振"，每层挖掘速度+3%/5%/7%，最多叠加3层。
 * 中断（切换方块或停止挖掘）后每2秒衰减1层
 */
class Resonance : com.sky4th.equipment.modifier.ConfiguredModifier("resonance") {

    // 最大层数（按等级）
    private val MAX_LAYERS = 3.0

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(BlockBreakEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 处理方块破坏事件
        if (event !is BlockBreakEvent || playerRole != PlayerRole.SELF) {
            return
        }

        // 获取或创建玩家的共振数据
        val recordBlockMining = UnifiedModifierManager.getAffixData(player, getAffixId()) as? ResonanceHandler.ResonanceData ?: return
        // 记录玩家挖掘方块
        val blockType = event.block.type.name
        recordBlockMining(player, blockType, recordBlockMining)
    }

    override fun onInit(player: Player, item: ItemStack, level: Int) {
        // 在词条初始化时创建共振数据并注册到统一管理器
        val resonanceData = ResonanceHandler.ResonanceData(
            uuid = player.uniqueId,
            level = level,
            item = item
        )
        UnifiedModifierManager.addPlayerAffix(player, getAffixId(), resonanceData)
    }

    override fun onRemove(player: Player) {
        // 当词条被移除时，从统一管理器中移除玩家的共振词条
        UnifiedModifierManager.removePlayerAffix(player, getAffixId())
    }

    /**
     * 记录玩家挖掘方块
     * @param player 玩家
     * @param blockType 方块类型
     */
    fun recordBlockMining(player: Player, blockType: String, resonanceData: ResonanceHandler.ResonanceData) {
        // 如果挖掘的是同种方块，增加层数
        if (resonanceData.lastBlockType == blockType) {
            val maxLayers = MAX_LAYERS
            if (resonanceData.currentLayers < maxLayers) {
                resonanceData.currentLayers++
                // 更新挖掘速度加成
                ResonanceHandler().updateSpeedBonus(player, resonanceData)
            }
        } else {
            // 如果挖掘的是不同方块，重置层数
            resonanceData.currentLayers = 1
            resonanceData.lastBlockType = blockType
            // 更新挖掘速度加成
            ResonanceHandler().updateSpeedBonus(player, resonanceData)
        }

        // 更新最后挖掘时间
        resonanceData.lastMiningTime = System.currentTimeMillis()
    }
}
