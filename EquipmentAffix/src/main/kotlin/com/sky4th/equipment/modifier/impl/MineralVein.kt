package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import com.sky4th.equipment.util.BlockTypeUtil
import com.sky4th.equipment.util.PlayereffectUtil
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * 矿脉词条
 * 效果：挖掘自然生成方块时，有5%概率额外获得粗矿（燧石，煤炭，粗铁，粗铜，粗金）
 */
class MineralVein : com.sky4th.equipment.modifier.ConfiguredModifier("mineral_vein") {

    // 定义粗矿列表
    private val rawOres = listOf(
        Material.FLINT,           // 燧石
        Material.COAL,            // 煤炭
        Material.RAW_IRON,        // 粗铁
        Material.RAW_COPPER,      // 粗铜
        Material.RAW_GOLD         // 粗金
    )

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

        // 只处理自然生成的方块（非玩家放置）
        // 通过检查方块是否被玩家放置过来判断
        if (event.block.hasMetadata("player_placed")) {
            return
        }

        // 5%概率触发
        if (Random.nextDouble() >= 0.05) return

        // 获取方块位置
        val loc = event.block.location

        // 按概率随机选择一种粗矿：燧石25%，煤炭25%，粗铁30%，粗铜15%，粗金5%
        val random = Random.nextDouble() * 100
        val randomOre = when {
            random < 25 -> Material.FLINT           // 25%
            random < 50 -> Material.COAL            // 25% (25-50)
            random < 80 -> Material.RAW_IRON        // 30% (50-80)
            random < 95 -> Material.RAW_COPPER      // 15% (80-95)
            else -> Material.RAW_GOLD               // 5% (95-100)
        }

        // 额外掉落粗矿
        loc.world?.dropItemNaturally(loc, ItemStack(randomOre, 1))
    }
}
