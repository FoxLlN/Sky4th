package com.sky4th.equipment.modifier.manager.handlers

import com.sky4th.equipment.data.NBTEquipmentDataManager
import com.sky4th.equipment.modifier.manager.AffixHandler
import com.sky4th.equipment.modifier.manager.AffixData
import com.sky4th.equipment.util.AttributeModifierUtil
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import java.lang.ref.WeakReference

/**
 * 生根词条处理器
 * 负责处理生根层数的叠加和衰退逻辑
 *
 * 功能：
 * 1. 静止3秒后开始叠加层数，每秒叠加1层，最多5层
 * 2. 移动时开始衰退层数，每秒衰退2层
 * 3. 更新抗击退属性
 *
 * 注意：
 * - 配置数据（CONFIG）由Rooted词条类管理
 * - 玩家数据由UnifiedModifierManager管理
 * - 本处理器只负责叠加、衰退和属性更新逻辑
 */
class RootedHandler : AffixHandler {

    // 抗击退属性修改键
    private val ROOTED_KNOCKBACK_KEY = NamespacedKey("equipment_affix", "rooted_knockback")

    // 最大层数配置
    private val MAX_LAYERS = 5

    // 叠加配置
    private val STACK_CHECK_TICKS = 1 * 20L  // 叠加间隔：1秒（20tick）
    private val INITIAL_WAIT_TICKS = 3 * 20L  // 初始等待：3秒（60tick）

    /**
     * 生根词条数据
     */
    class RootedData(
        val uuid: java.util.UUID,
        var stationaryTicks: Long = 0L,  // 静止时间（tick）
        var lastStackTick: Long = 0L,  // 上次叠加时间（tick）
        var lastDecayTick: Long = 0L,  // 上次衰减时间（时间戳）
        var currentLayers: Int = 0,  // 缓存的当前层数，减少NBT读取
        var item: org.bukkit.inventory.ItemStack  // 直接存储物品引用
    ) : AffixData() {

        override val interval: Int = 5  // 每5 tick检查一次

        /**
         * 重置静止时间
         */
        fun resetStationaryTicks() {
            stationaryTicks = 0L
            lastStackTick = 0L
        }

        /**
         * 重置衰减时间
         */
        fun resetDecayTick() {
            lastDecayTick = System.currentTimeMillis()
        }
    }

    override fun process(player: Player, data: AffixData) {
        val rootedData = data as? RootedData ?: return

        // 获取物品
        val item = rootedData.item

        // 使用缓存的层数
        val currentLayers = rootedData.currentLayers

        // 增加静止时间
        rootedData.stationaryTicks += rootedData.interval

        // 检查是否需要叠加
        if (rootedData.stationaryTicks >= INITIAL_WAIT_TICKS) {
            rootedData.lastStackTick += rootedData.interval
            if (rootedData.lastStackTick >= STACK_CHECK_TICKS) {
                rootedData.lastStackTick = 0L

                // 如果未达到最大层数，叠加1层
                if (currentLayers < MAX_LAYERS) {
                    val newLayers = currentLayers + 1
                    NBTEquipmentDataManager.setAffixResource(item, "rooted", newLayers)
                    // 更新缓存和抗击退属性
                    rootedData.currentLayers = newLayers
                    updateKnockbackResistance(item, newLayers)
                }
            }
        }
    }

    /**
     * 更新物品的抗击退属性
     * @param item 装备物品
     * @param layers 当前层数
     */
    fun updateKnockbackResistance(item: org.bukkit.inventory.ItemStack, layers: Int) {
        // 获取词条等级
        val affixes = NBTEquipmentDataManager.getAffixes(item)
        val level = affixes["rooted"] ?: return

        // 获取该等级的抗击退百分比
        val knockbackPercent = when (level) {
            1 -> 0.02
            2 -> 0.03
            3 -> 0.04
            else -> return
        }

        // 计算总抗击退值（层数 × 每层百分比）
        val totalKnockback = knockbackPercent * layers

        if (totalKnockback > 0) {
            // 更新物品的抗击退属性
            AttributeModifierUtil.updateItemAttribute(
                item,
                Attribute.GENERIC_KNOCKBACK_RESISTANCE,
                ROOTED_KNOCKBACK_KEY,
                totalKnockback,
                AttributeModifier.Operation.ADD_NUMBER,
                org.bukkit.inventory.EquipmentSlotGroup.FEET
            )
        } else {
            // 如果没有层数，移除抗击退属性
            AttributeModifierUtil.removeItemAttributeModifier(
                item,
                Attribute.GENERIC_KNOCKBACK_RESISTANCE,
                ROOTED_KNOCKBACK_KEY
            )
        }
    }
}
