package com.sky4th.equipment.modifier.manager.handlers

import com.sky4th.equipment.data.NBTEquipmentDataManager
import com.sky4th.equipment.modifier.manager.AffixData
import com.sky4th.equipment.modifier.manager.AffixHandler
import com.sky4th.equipment.util.AttributeModifierUtil
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player

/**
 * 共振词条处理器
 * 负责处理共振词条的挖掘速度加成逻辑
 *
 * 功能：
 * 1. 挖掘同种方块时增加层数，不同方块时重置层数
 * 2. 2秒未挖掘时衰减1层
 * 3. 根据层数提供挖掘速度加成
 *
 * 配置：
 * 1级：每层3%加成，最大3层
 * 2级：每层5%加成，最大3层
 * 3级：每层7%加成，最大3层
 */
class ResonanceHandler : AffixHandler {

    // 词条修饰符的命名空间键
    private val RESONANCE_MODIFIER_KEY = NamespacedKey("equipment_affix", "resonance")

    // 每层提供的挖掘速度加成百分比（按等级）
    private val BONUS_PER_LAYER = listOf(0.03, 0.05, 0.07)  // 3%, 5%, 7%

    /**
     * 共振词条数据
     */
    class ResonanceData(
        val uuid: java.util.UUID,
        var level: Int,
        var item: org.bukkit.inventory.ItemStack,
        var currentLayers: Int = 0,
        var lastBlockType: String? = null,
        var lastMiningTime: Long = System.currentTimeMillis(),
        var lastBonus: Double = 0.0
    ) : AffixData() {
        override val interval: Int = 40  // 每40 tick（2秒）执行一次
    }

    override fun process(player: Player, data: AffixData) {
        val resonanceData = data as? ResonanceData ?: return

        // 计算距离上次挖掘的时间差（毫秒）
        val currentTime = System.currentTimeMillis()
        val timeSinceLastMining = currentTime - resonanceData.lastMiningTime

        // 如果超过2秒没有挖掘，衰减1层
        if (timeSinceLastMining >= 2000 && resonanceData.currentLayers > 0) {
            resonanceData.currentLayers--
            resonanceData.lastMiningTime = currentTime

            // 更新挖掘速度加成
            updateSpeedBonus(player, resonanceData)
        }
    }

    /**
     * 更新挖掘速度加成
     * @param player 玩家
     * @param resonanceData 共振数据
     */
    fun updateSpeedBonus(player: Player, resonanceData: ResonanceData) {
        // 确保level在有效范围内（1-3）
        val effectiveLevel = resonanceData.level.coerceIn(1, 3) - 1  // 转换为0-2的索引

        // 计算挖掘速度加成
        val bonusPerLayer = BONUS_PER_LAYER.getOrNull(effectiveLevel) ?: return
        val speedBonus = resonanceData.currentLayers * bonusPerLayer

        // 如果加成值没有变化，直接返回
        if (speedBonus == resonanceData.lastBonus) {
            return
        }

        // 更新缓存的加成值
        resonanceData.lastBonus = speedBonus

        // 根据加成值更新或移除修饰符
        if (speedBonus > 0) {
            // 只有当加成大于0时才更新修饰符
            val mainHandItem = player.inventory.itemInMainHand
            if (containsResonanceModifier(mainHandItem)) {
                AttributeModifierUtil.updateItemAttribute(
                    mainHandItem,
                    Attribute.PLAYER_BLOCK_BREAK_SPEED,
                    RESONANCE_MODIFIER_KEY,
                    speedBonus,
                    AttributeModifier.Operation.ADD_NUMBER,
                    org.bukkit.inventory.EquipmentSlotGroup.MAINHAND
                )
            }
        } else {
            // 当加成小于等于0时，移除属性修饰符
            removeResonanceModifier(player)
        }
    }

    /**
     * 移除共振修饰符
     * @param player 玩家
     */
    private fun removeResonanceModifier(player: Player) {
        val mainHandItem = player.inventory.itemInMainHand
        if (containsResonanceModifier(mainHandItem)) {
            AttributeModifierUtil.removeItemAttributeModifier(
                mainHandItem,
                Attribute.PLAYER_BLOCK_BREAK_SPEED,
                RESONANCE_MODIFIER_KEY
            )
        }
    }

    /**
     * 检查物品是否含有共振词条
     * @param item 物品
     * @return 如果物品含有共振词条返回true，否则返回false
     */
    private fun containsResonanceModifier(item: org.bukkit.inventory.ItemStack?): Boolean {
        if (item == null || item.type.isAir) {
            return false
        }

        // 检查是否是装备
        if (!NBTEquipmentDataManager.isEquipment(item)) {
            return false
        }

        // 检查材质效果是否是共振
        val materialEffect = NBTEquipmentDataManager.getMaterialEffect(item)
        if (materialEffect == "resonance") {
            return true
        }

        // 检查词条是否包含共振
        val affixes = NBTEquipmentDataManager.getAffixes(item)
        return affixes.containsKey("resonance")
    }
}
