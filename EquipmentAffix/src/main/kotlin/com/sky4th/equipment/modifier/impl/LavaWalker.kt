package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import com.sky4th.equipment.util.AttributeModifierUtil
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.NamespacedKey
import org.bukkit.World

/**
 * 熔岩行者词条
 * 效果：
 * 在下界时，移动速度增加 5%/10%/15%
 */
class LavaWalker : com.sky4th.equipment.modifier.ConfiguredModifier("lava_walker") {

    companion object {
        // 词条修饰符的命名空间键
        private val LAVA_WALKER_MODIFIER_KEY = NamespacedKey("equipment_affix", "lava_walker")
        // 每级的移动速度加成
        private val SPEED_BONUS = doubleArrayOf(0.05, 0.10, 0.15)  // 1级: 5%, 2级: 10%, 3级: 15%
    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(PlayerChangedWorldEvent::class.java)

    override fun onInit(player: Player, item: ItemStack, level: Int) {
        updateLavaWalkerModifier(player, item, level)
    }

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        when (event) {
            is PlayerChangedWorldEvent -> {
                // 处理玩家切换世界事件
                if (playerRole != PlayerRole.SELF) return

                updateLavaWalkerModifier(player, item, level)
            }
        }
    }

    private fun updateLavaWalkerModifier(player: Player, item: ItemStack, level: Int) {
        // 检查玩家是否在下界
        if (player.world.environment == World.Environment.NETHER) {
            // 根据等级设置移动速度加成
            val cappedSpeedBonus = if (level - 1 in 0..2) SPEED_BONUS[level - 1] else return
            AttributeModifierUtil.updateItemAttribute(
                item,
                Attribute.GENERIC_MOVEMENT_SPEED,
                LAVA_WALKER_MODIFIER_KEY,
                cappedSpeedBonus,
                AttributeModifier.Operation.ADD_SCALAR,
                org.bukkit.inventory.EquipmentSlotGroup.LEGS
            )
        } else {
            AttributeModifierUtil.removeItemAttributeModifier(
                item,
                Attribute.GENERIC_MOVEMENT_SPEED,
                LAVA_WALKER_MODIFIER_KEY
            )
        }
    }
}
