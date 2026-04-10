package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.player.PlayerToggleSprintEvent
import org.bukkit.inventory.ItemStack

/**
 * 冲刺词条
 * 效果：疾跑速度增加 4%/8%/12%
 * 
 * 1级：疾跑速度增加 4%
 * 2级：疾跑速度增加 8%
 * 3级：疾跑速度增加 12%
 */
class Sprint : com.sky4th.equipment.modifier.ConfiguredModifier("sprint") {

    companion object {
        // 词条修饰符的命名空间键
        private val SPRINT_MODIFIER_KEY = NamespacedKey("equipment_affix", "sprint")

        // 每级的速度加成
        private val SPEED_BONUS = doubleArrayOf(0.04, 0.08, 0.12)
    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(PlayerToggleSprintEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        if (event !is PlayerToggleSprintEvent || playerRole != PlayerRole.SELF) {
            return
        }

        val isNowSprinting = event.isSprinting // true=开始疾跑，false=停止疾跑

        if (isNowSprinting) {
            // 开始疾跑，添加速度修饰符
            applySpeedModifier(item, level)
        } else {
            // 停止疾跑，移除速度修饰符
            removeSpeedModifier(item)
        }
    }

    /**
     * 应用速度修饰符
     */
    private fun applySpeedModifier(item: ItemStack, level: Int) {
        val speedBonus = SPEED_BONUS.getOrNull(level - 1) ?: return
        
        // 使用工具类更新玩家属性
        com.sky4th.equipment.util.AttributeModifierUtil.updateItemAttribute(
            item,
            Attribute.GENERIC_MOVEMENT_SPEED,
            SPRINT_MODIFIER_KEY,
            speedBonus,
            AttributeModifier.Operation.ADD_SCALAR,
            org.bukkit.inventory.EquipmentSlotGroup.FEET
        )
    }

    /**
     * 移除速度修饰符
     */
    private fun removeSpeedModifier(item: ItemStack) {
        // 使用工具类移除玩家属性
        com.sky4th.equipment.util.AttributeModifierUtil.removeItemAttributeModifier(
            item,
            Attribute.GENERIC_MOVEMENT_SPEED,
            SPRINT_MODIFIER_KEY
        )
    }

    override fun onRemove(player: Player) {
    }
    
    // 当词条进入活跃状态时，初始化检测疾跑状态
    override fun onInit(player: Player, item: ItemStack, level: Int) {
        if (player.isSprinting) {
            applySpeedModifier(item, level)
        } else {
            removeSpeedModifier(item)
        }
    }
}
