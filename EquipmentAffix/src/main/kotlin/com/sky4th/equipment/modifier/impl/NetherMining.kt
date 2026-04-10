
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import com.sky4th.equipment.util.AttributeModifierUtil
import com.sky4th.equipment.util.PlayereffectUtil
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import kotlin.random.Random

/**
 * 炽热采集词条
 * 效果：
 * 1. 在下界时，挖掘速度增加 5%/10%/15%
 * 2. 在下界时，有 5%/10%/15% 概率额外掉落 1 个下界石英
 */
class NetherMining : com.sky4th.equipment.modifier.ConfiguredModifier("nether_mining") {

    companion object {
        // 词条修饰符的命名空间键
        private val NETHER_MINING_MODIFIER_KEY = NamespacedKey("equipment_affix", "nether_mining")
        // 每级的效率加成
        private val SPEED_BONUS = doubleArrayOf(0.15, 0.20, 0.25)
        // 每级额外下界石英概率
        private val DROP_CHANCE = doubleArrayOf(0.05, 0.10, 0.15)  // 1级: 5%, 2级: 10%, 3级: 15%
    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(PlayerChangedWorldEvent::class.java, BlockDropItemEvent::class.java)

    override fun onInit(player: Player, item: ItemStack, level: Int) {
        updateNetherMiningModifier(player, item, level)
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

                updateNetherMiningModifier(player, item, level)
            }

            is BlockDropItemEvent -> {
                // 处理方块掉落物事件，有概率额外掉落下界石英
                if (playerRole != PlayerRole.SELF) return

                // 检查玩家是否在下界
                if (player.world.environment != World.Environment.NETHER) {
                    return
                }

                // 根据等级计算额外掉落概率
                val dropChance = if (level - 1 in 0..2) DROP_CHANCE[level - 1] else return

                // 检查是否触发额外掉落
                //if (Random.nextDouble() >= dropChance) return
                // 额外掉落1个下界石英
                val quartz = ItemStack(Material.QUARTZ, 1)
                event.block.location.world?.dropItemNaturally(event.block.location, quartz)
                // 播放特效
                PlayereffectUtil.playCircleParticle(player, Particle.WAX_OFF, 10)
                PlayereffectUtil.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3, 0.5, 1.0, 1.2)
            }
        }
    }

    private fun updateNetherMiningModifier(player: Player, item: ItemStack, level: Int) {
        // 检查玩家是否切换到下界
        if (player.world.environment == World.Environment.NETHER) {
            // 根据等级设置挖掘速度加成
            val cappedSpeedBonus = if (level - 1 in 0..2) SPEED_BONUS[level - 1] else return
            AttributeModifierUtil.updateItemAttribute(
                item,
                Attribute.PLAYER_BLOCK_BREAK_SPEED,
                NETHER_MINING_MODIFIER_KEY,
                cappedSpeedBonus,
                AttributeModifier.Operation.ADD_NUMBER,
                org.bukkit.inventory.EquipmentSlotGroup.MAINHAND
            )
        } else {
            AttributeModifierUtil.removeItemAttributeModifier(
                item,
                Attribute.PLAYER_BLOCK_BREAK_SPEED,
                NETHER_MINING_MODIFIER_KEY
            )
        }
    }
}
