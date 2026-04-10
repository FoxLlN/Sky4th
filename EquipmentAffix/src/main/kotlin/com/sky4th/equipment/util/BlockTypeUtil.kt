package com.sky4th.equipment.util

import org.bukkit.Material
import org.bukkit.block.Block

/**
 * 方块类型工具类
 * 用于区分不同类型的方块
 */
object BlockTypeUtil {

    /**
     * 方块类型枚举
     */
    enum class BlockType {
        /** 主世界矿物类型 */
        OVERWORLDORE,
        /** 地狱矿物类型 */
        NETHERORE,
        /** 石质方块类型 */
        STONE,
        /** 自然类方块类型 */
        NATURAL,
        /** 功能类方块 */
        FUNCTIONAL,
        /** 容器类方块 */
        CONTAINER,
        /** 原木类型 - 包括所有原版原木 */
        LOG,
        /** 其他类型 */
        OTHER
    }

    /**
     * 主世界矿物列表
     */
    private val overworldOres = setOf(
        // 基础矿物
        Material.COAL_ORE,
        Material.DEEPSLATE_COAL_ORE,
        Material.IRON_ORE,
        Material.DEEPSLATE_IRON_ORE,
        Material.COPPER_ORE,
        Material.DEEPSLATE_COPPER_ORE,
        Material.GOLD_ORE,
        Material.DEEPSLATE_GOLD_ORE,
        Material.LAPIS_ORE,
        Material.DEEPSLATE_LAPIS_ORE,
        Material.REDSTONE_ORE,
        Material.DEEPSLATE_REDSTONE_ORE,
        Material.DIAMOND_ORE,
        Material.DEEPSLATE_DIAMOND_ORE,
        Material.EMERALD_ORE,
        Material.DEEPSLATE_EMERALD_ORE,
    )

     /**
     * 地狱矿物列表(除下届合金)
     */
    private val netherOres = setOf(
        // 特殊矿物
        Material.NETHER_QUARTZ_ORE,
        Material.NETHER_GOLD_ORE
    )

    /**
     * 石质方块列表（仅主世界，包括石头、石砖变种）
     */
    private val stoneBlocks = setOf(
        // 基础石头
        Material.STONE,     
        Material.COBBLESTONE,
        Material.MOSSY_COBBLESTONE,
        Material.INFESTED_COBBLESTONE,
        Material.GRANITE,
        Material.POLISHED_GRANITE,
        Material.DIORITE,
        Material.POLISHED_DIORITE,
        Material.ANDESITE,
        Material.POLISHED_ANDESITE,
        // 深板岩系列
        Material.DEEPSLATE,
        Material.COBBLED_DEEPSLATE,
        Material.POLISHED_DEEPSLATE,
        Material.TUFF,
        Material.CALCITE
    )

    /**
     * 自然类方块列表
     */
    private val naturalBlocks = setOf(
        // 自然方块
        Material.GRASS_BLOCK,
        Material.PODZOL,
        Material.MYCELIUM,
        Material.DIRT,
        Material.COARSE_DIRT,
        Material.ROOTED_DIRT,
        Material.MUD,
        Material.CLAY,
        Material.GRAVEL,
        Material.SAND,
        Material.RED_SAND,
        Material.ICE,
        Material.PACKED_ICE,
        Material.BLUE_ICE,
        Material.SNOW_BLOCK,
        Material.SNOW,
        Material.MOSS_BLOCK,
        Material.MOSS_CARPET,
        Material.NETHERRACK,
        Material.SOUL_SAND,
        Material.SOUL_SOIL
    )

    /**
     * 功能类方块列表
     */
    private val functionalBlocks = setOf(
        // 工作台和熔炉
        Material.CRAFTING_TABLE,
        Material.FURNACE,
        Material.BLAST_FURNACE,
        Material.SMOKER,
        // 附魔和酿造
        Material.ENCHANTING_TABLE,
        Material.BREWING_STAND,
        Material.RESPAWN_ANCHOR,
        // 传送门
        Material.NETHER_PORTAL,
        Material.END_PORTAL,
        Material.END_GATEWAY,
        // 信标和信标
        Material.BEACON,
        Material.CONDUIT,
        // 铁砧
        Material.ANVIL,
        Material.CHIPPED_ANVIL,
        Material.DAMAGED_ANVIL,
        // 磨石
        Material.GRINDSTONE,
        // 织布机
        Material.LOOM,
        // 切石机
        Material.STONECUTTER,
        // 锻造台
        Material.SMITHING_TABLE,
        // 制图台
        Material.CARTOGRAPHY_TABLE,
        // 其他功能方块
        Material.CAMPFIRE,
        Material.SOUL_CAMPFIRE,
        Material.TARGET,
        Material.LODESTONE,
        Material.LIGHTNING_ROD
    )

    /**
     * 容器类方块列表
     */
    private val containerBlocks = setOf(
        Material.CHEST,
        Material.TRAPPED_CHEST,
        Material.ENDER_CHEST,
        Material.BARREL,
        Material.SHULKER_BOX,
        Material.WHITE_SHULKER_BOX,
        Material.ORANGE_SHULKER_BOX,
        Material.MAGENTA_SHULKER_BOX,
        Material.LIGHT_BLUE_SHULKER_BOX,
        Material.YELLOW_SHULKER_BOX,
        Material.LIME_SHULKER_BOX,
        Material.PINK_SHULKER_BOX,
        Material.GRAY_SHULKER_BOX,
        Material.LIGHT_GRAY_SHULKER_BOX,
        Material.CYAN_SHULKER_BOX,
        Material.PURPLE_SHULKER_BOX,
        Material.BLUE_SHULKER_BOX,
        Material.BROWN_SHULKER_BOX,
        Material.GREEN_SHULKER_BOX,
        Material.RED_SHULKER_BOX,
        Material.BLACK_SHULKER_BOX
    )

    /**
     * 原木列表（包括所有原版原木）
     */
    private val logBlocks = setOf(
        // 橡木
        Material.OAK_LOG,
        // 云杉木
        Material.SPRUCE_LOG,
        // 白桦木
        Material.BIRCH_LOG,
        // 丛林木
        Material.JUNGLE_LOG,
        // 金合欢木
        Material.ACACIA_LOG,
        // 深色橡木
        Material.DARK_OAK_LOG,
        // 绯红木
        Material.CRIMSON_STEM,
        Material.CRIMSON_HYPHAE,
        // 诡异木
        Material.WARPED_STEM,
        Material.WARPED_HYPHAE,
        // 去皮原木
        Material.STRIPPED_OAK_LOG,
        Material.STRIPPED_SPRUCE_LOG,
        Material.STRIPPED_BIRCH_LOG,
        Material.STRIPPED_JUNGLE_LOG,
        Material.STRIPPED_ACACIA_LOG,
        Material.STRIPPED_DARK_OAK_LOG,
        Material.STRIPPED_CRIMSON_STEM,
        Material.STRIPPED_CRIMSON_HYPHAE,
        Material.STRIPPED_WARPED_STEM,
        Material.STRIPPED_WARPED_HYPHAE,
        // 木头（横向放置的原木）
        Material.OAK_WOOD,
        Material.SPRUCE_WOOD,
        Material.BIRCH_WOOD,
        Material.JUNGLE_WOOD,
        Material.ACACIA_WOOD,
        Material.DARK_OAK_WOOD,
        Material.CRIMSON_STEM,
        Material.WARPED_STEM,
        // 去皮木头
        Material.STRIPPED_OAK_WOOD,
        Material.STRIPPED_SPRUCE_WOOD,
        Material.STRIPPED_BIRCH_WOOD,
        Material.STRIPPED_JUNGLE_WOOD,
        Material.STRIPPED_ACACIA_WOOD,
        Material.STRIPPED_DARK_OAK_WOOD,
        Material.STRIPPED_CRIMSON_STEM,
        Material.STRIPPED_WARPED_STEM
    )

    /**
     * 获取方块类型
     * @param block 要判断的方块
     * @return 方块类型
     */
    fun getBlockType(block: Block): BlockType {
        val material = block.type
        return when {
            material in overworldOres -> BlockType.OVERWORLDORE
            material in netherOres -> BlockType.NETHERORE
            material in logBlocks -> BlockType.LOG
            material in naturalBlocks -> BlockType.NATURAL
            material in functionalBlocks -> BlockType.FUNCTIONAL
            material in containerBlocks -> BlockType.CONTAINER
            material in stoneBlocks -> BlockType.STONE
            else -> BlockType.OTHER
        }
    }

    /**
     * 判断是否为矿物
     * @param block 要判断的方块
     * @return 是否为矿物
     */
    fun isOre(block: Block): Boolean {
        return getBlockType(block) == BlockType.OVERWORLDORE || getBlockType(block) == BlockType.NETHERORE
    }

    /**
     * 判断是否为石质方块
     * @param block 要判断的方块
     * @return 是否为石质方块
     */
    fun isStone(block: Block): Boolean {
        return getBlockType(block) == BlockType.STONE
    }

    /**
     * 判断是否为自然方块
     * @param block 要判断的方块
     * @return 是否为自然方块
     */
    fun isNatural(block: Block): Boolean {
        return getBlockType(block) == BlockType.NATURAL
    }

    /**
     * 判断是否为功能类方块
     * @param block 要判断的方块
     * @return 是否为功能类方块
     */
    fun isFunctional(block: Block): Boolean {
        return getBlockType(block) == BlockType.FUNCTIONAL
    }

    /**
     * 判断是否为容器类方块
     * @param block 要判断的方块
     * @return 是否为容器类方块
     */
    fun isContainer(block: Block): Boolean {
        return getBlockType(block) == BlockType.CONTAINER
    }

    /**
     * 判断是否为原木
     * @param block 要判断的方块
     * @return 是否为原木
     */
    fun isLog(block: Block): Boolean {
        return getBlockType(block) == BlockType.LOG
    }
}
