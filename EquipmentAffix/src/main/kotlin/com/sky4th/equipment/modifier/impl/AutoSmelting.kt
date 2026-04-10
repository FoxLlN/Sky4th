
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.inventory.ItemStack

/**
 * 自动冶炼词条
 * 效果：挖掘方块可以自动冶炼成成品，挖掘掉落物可以自动冶炼成成品
 */
class AutoSmelting : com.sky4th.equipment.modifier.ConfiguredModifier("auto_smelting") {

    companion object {
        // 冶炼结果映射
        private val SMELTING_RESULTS = mapOf(
            // 主世界矿石
            Material.COAL_ORE to Material.COAL,
            Material.DEEPSLATE_COAL_ORE to Material.COAL,
            Material.IRON_ORE to Material.IRON_INGOT,
            Material.DEEPSLATE_IRON_ORE to Material.IRON_INGOT,
            Material.COPPER_ORE to Material.COPPER_INGOT,
            Material.DEEPSLATE_COPPER_ORE to Material.COPPER_INGOT,
            Material.GOLD_ORE to Material.GOLD_INGOT,
            Material.DEEPSLATE_GOLD_ORE to Material.GOLD_INGOT,
            Material.LAPIS_ORE to Material.LAPIS_LAZULI,
            Material.DEEPSLATE_LAPIS_ORE to Material.LAPIS_LAZULI,
            Material.REDSTONE_ORE to Material.REDSTONE,
            Material.DEEPSLATE_REDSTONE_ORE to Material.REDSTONE,
            Material.DIAMOND_ORE to Material.DIAMOND,
            Material.DEEPSLATE_DIAMOND_ORE to Material.DIAMOND,
            Material.EMERALD_ORE to Material.EMERALD,
            Material.DEEPSLATE_EMERALD_ORE to Material.EMERALD,
            // 下界矿石
            Material.NETHERRACK to Material.NETHER_BRICK,
            Material.NETHER_QUARTZ_ORE to Material.QUARTZ,
            Material.NETHER_GOLD_ORE to Material.GOLD_INGOT,
            Material.ANCIENT_DEBRIS to Material.NETHERITE_SCRAP,
            // 原始矿
            Material.RAW_IRON to Material.IRON_INGOT,
            Material.RAW_COPPER to Material.COPPER_INGOT,
            Material.RAW_GOLD to Material.GOLD_INGOT,
            // 平滑
            Material.QUARTZ_BLOCK to Material.SMOOTH_QUARTZ,
            Material.RED_SANDSTONE to Material.SMOOTH_RED_SANDSTONE,
            Material.SANDSTONE to Material.SMOOTH_SANDSTONE,
            Material.BASALT to Material.SMOOTH_BASALT,
            // 沙子
            Material.SAND to Material.GLASS,
            Material.RED_SAND to Material.GLASS,
            // 粘土
            Material.CLAY_BALL to Material.BRICK,
            Material.CLAY to Material.TERRACOTTA,
            // 陶瓦
            Material.WHITE_TERRACOTTA to Material.WHITE_GLAZED_TERRACOTTA,
            Material.LIGHT_GRAY_TERRACOTTA to Material.LIGHT_GRAY_GLAZED_TERRACOTTA,
            Material.GRAY_TERRACOTTA to Material.GRAY_GLAZED_TERRACOTTA,
            Material.BLACK_TERRACOTTA to Material.BLACK_GLAZED_TERRACOTTA,
            Material.BROWN_TERRACOTTA to Material.BROWN_GLAZED_TERRACOTTA,
            Material.RED_TERRACOTTA to Material.RED_GLAZED_TERRACOTTA,
            Material.ORANGE_TERRACOTTA to Material.ORANGE_GLAZED_TERRACOTTA,
            Material.YELLOW_TERRACOTTA to Material.YELLOW_GLAZED_TERRACOTTA,
            Material.LIME_TERRACOTTA to Material.LIME_GLAZED_TERRACOTTA,
            Material.GREEN_TERRACOTTA to Material.GREEN_GLAZED_TERRACOTTA,
            Material.CYAN_TERRACOTTA to Material.CYAN_GLAZED_TERRACOTTA,
            Material.LIGHT_BLUE_TERRACOTTA to Material.LIGHT_BLUE_GLAZED_TERRACOTTA,
            Material.BLUE_TERRACOTTA to Material.BLUE_GLAZED_TERRACOTTA,
            Material.PURPLE_TERRACOTTA to Material.PURPLE_GLAZED_TERRACOTTA,
            Material.MAGENTA_TERRACOTTA to Material.MAGENTA_GLAZED_TERRACOTTA,
            Material.PINK_TERRACOTTA to Material.PINK_GLAZED_TERRACOTTA,
            // 仙人掌
            Material.CACTUS to Material.GREEN_DYE,
            Material.CHORUS_FRUIT to Material.POPPED_CHORUS_FRUIT,
            // 海绵
            Material.WET_SPONGE to Material.SPONGE,
            // 原石
            Material.COBBLESTONE to Material.STONE,
            Material.STONE to Material.SMOOTH_STONE,
            Material.COBBLED_DEEPSLATE to Material.DEEPSLATE,
            // 原木
            Material.OAK_LOG to Material.CHARCOAL,
            Material.SPRUCE_LOG to Material.CHARCOAL,
            Material.BIRCH_LOG to Material.CHARCOAL,
            Material.JUNGLE_LOG to Material.CHARCOAL,
            Material.ACACIA_LOG to Material.CHARCOAL,
            Material.DARK_OAK_LOG to Material.CHARCOAL,
            Material.CRIMSON_STEM to Material.CHARCOAL,
            Material.WARPED_STEM to Material.CHARCOAL,
            // 去皮原木
            Material.STRIPPED_OAK_LOG to Material.CHARCOAL,
            Material.STRIPPED_SPRUCE_LOG to Material.CHARCOAL,
            Material.STRIPPED_BIRCH_LOG to Material.CHARCOAL,
            Material.STRIPPED_JUNGLE_LOG to Material.CHARCOAL,
            Material.STRIPPED_ACACIA_LOG to Material.CHARCOAL,
            Material.STRIPPED_DARK_OAK_LOG to Material.CHARCOAL,
            Material.STRIPPED_CRIMSON_STEM to Material.CHARCOAL,
            Material.STRIPPED_WARPED_STEM to Material.CHARCOAL
        )

        // 需要特殊处理的原木和木头（横向放置的原木）
        private val LOG_MATERIALS = setOf(
            Material.OAK_WOOD,
            Material.SPRUCE_WOOD,
            Material.BIRCH_WOOD,
            Material.JUNGLE_WOOD,
            Material.ACACIA_WOOD,
            Material.DARK_OAK_WOOD,
            Material.CRIMSON_HYPHAE,
            Material.WARPED_HYPHAE,
            Material.STRIPPED_OAK_WOOD,
            Material.STRIPPED_SPRUCE_WOOD,
            Material.STRIPPED_BIRCH_WOOD,
            Material.STRIPPED_JUNGLE_WOOD,
            Material.STRIPPED_ACACIA_WOOD,
            Material.STRIPPED_DARK_OAK_WOOD,
            Material.STRIPPED_CRIMSON_HYPHAE,
            Material.STRIPPED_WARPED_HYPHAE
        )
    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(BlockDropItemEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 处理方块掉落物事件
        if (event !is BlockDropItemEvent || playerRole != PlayerRole.SELF) {
            return
        }

        // 收集需要冶炼的掉落物
        val itemsToSmelt = event.items.filter { drop ->
            val material = drop.itemStack.type
            // 检查是否在冶炼映射中
            material in SMELTING_RESULTS || material in LOG_MATERIALS
        }

        // 如果没有需要冶炼的物品，直接返回
        if (itemsToSmelt.isEmpty()) {
            return
        }

        // 移除原有的掉落物
        event.items.removeAll(itemsToSmelt)

        // 冶炼并生成新的物品
        itemsToSmelt.forEach { drop ->
            val material = drop.itemStack.type
            val amount = drop.itemStack.amount
            val smeltedMaterial = SMELTING_RESULTS[material] ?: Material.CHARCOAL

            // 生成冶炼后的物品（每次最多生成64个）
            var remaining = amount
            while (remaining > 0) {
                val dropAmount = minOf(remaining, 64)
                val smeltedItem = ItemStack(smeltedMaterial, dropAmount)
                event.block.location.world?.dropItemNaturally(event.block.location, smeltedItem)
                remaining -= dropAmount
            }
        }
    }
}
