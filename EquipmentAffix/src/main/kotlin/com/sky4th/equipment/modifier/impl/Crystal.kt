package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import com.sky4th.equipment.util.BlockTypeUtil
import com.sky4th.equipment.util.PlayereffectUtil
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.Location
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * 双晶词条
 * 效果：挖掘时10%概率双倍掉落（不与时运叠加）
 */
class Crystal : com.sky4th.equipment.modifier.ConfiguredModifier("crystal") {

    // 临时存储需要双倍掉落的方块位置（玩家UUID -> 方块位置列表）
    private val pendingDoubleDrops = ConcurrentHashMap<UUID, MutableSet<Location>>()

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(BlockBreakEvent::class.java, BlockDropItemEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        if (playerRole != PlayerRole.SELF) {
            return
        }

        when (event) {
            is BlockBreakEvent -> {
                if (!BlockTypeUtil.isOre(event.block) && !BlockTypeUtil.isNatural(event.block)) return      
                
                // 检查是否有时运附魔，如果有则不触发
                val fortuneLevel = item.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.FORTUNE)
                if (fortuneLevel > 0) return

                // 10%概率触发
                if (Random.nextDouble() >= 0.1) return

                // 标记该方块需要双倍掉落
                val playerUuid = player.uniqueId
                val blockLocation = event.block.location
                synchronized(pendingDoubleDrops) {
                    val locations = pendingDoubleDrops.getOrPut(playerUuid) { mutableSetOf() }
                    locations.add(blockLocation)
                }
            }
            
            is BlockDropItemEvent -> {
                // 检查该方块是否被标记为需要双倍掉落
                val playerUuid = player.uniqueId
                val blockLocation = event.block.location
                
                val playerPendingDrops = synchronized(pendingDoubleDrops) {
                    pendingDoubleDrops[playerUuid]
                }
                
                if (playerPendingDrops == null || blockLocation !in playerPendingDrops) {
                    return
                }
                // 双倍掉落 - 将event.items中的物品数量加倍
                event.items.forEach { itemEntity ->
                    val itemStack = itemEntity.itemStack
                    itemStack.amount *= 2
                }

                // 播放特效
                PlayereffectUtil.playCircleParticle(player, Particle.HAPPY_VILLAGER, 10)
                PlayereffectUtil.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3, 0.5, 1.0, 1.2)

                // 移除标记
                synchronized(pendingDoubleDrops) {
                    playerPendingDrops.remove(blockLocation)
                    if (playerPendingDrops.isEmpty()) {
                        pendingDoubleDrops.remove(playerUuid)
                    }
                }
            }
            
            else -> return
        }
    }
}
