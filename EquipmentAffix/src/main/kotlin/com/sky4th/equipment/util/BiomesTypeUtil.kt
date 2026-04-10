package com.sky4th.equipment.util

import org.bukkit.Location
import org.bukkit.block.Biome

/**
 * 生物群系工具类
 * 用于区分不同类型的生物群系
 */
object BiomesTypeUtil {

    /**
     * 生物群系类型枚举
     */
    enum class BiomesType {
        /** 自然生物群系 */
        NATURAL,
        /** 河流生物群系 */
        RIVER,
        /** 海洋生物群系 */
        OCEAN,
        /** 沙漠生物群系 */
        DESERT,
        /** 恶地生物群系 */
        BADLANDS,
        /** 冰雪生物群系 */
        SNOWY,
        /** 丛林生物群系 */
        JUNGLE,
        /** 山地生物群系 */
        MOUNTAIN,
        /** 沼泽生物群系 */
        SWAMP,
        /** 洞穴生物群系 */
        CAVE,
        /** 下界生物群系 */
        NETHER,
        /** 末地生物群系 */
        THE_END,
        /** 其他生物群系 */
        OTHER
    }

    // 定义自然生物群系
    private val NATURAL_BIOMES = setOf(
        Biome.PLAINS,
        Biome.FOREST,
        Biome.TAIGA,
        Biome.BEACH,
        Biome.SUNFLOWER_PLAINS,
        Biome.FLOWER_FOREST,
        Biome.BIRCH_FOREST,
        Biome.DARK_FOREST,
        Biome.SNOWY_TAIGA,
        Biome.SNOWY_PLAINS,
        Biome.ICE_SPIKES,
        Biome.JUNGLE,
        Biome.SPARSE_JUNGLE,
        Biome.BAMBOO_JUNGLE,
        Biome.SAVANNA,
        Biome.SAVANNA_PLATEAU,
        Biome.WINDSWEPT_SAVANNA,
        Biome.WINDSWEPT_HILLS,
        Biome.WINDSWEPT_FOREST,
        Biome.MEADOW,
        Biome.GROVE,
        Biome.SNOWY_SLOPES,
        Biome.FROZEN_PEAKS,
        Biome.JAGGED_PEAKS,
        Biome.STONY_PEAKS,
        Biome.LUSH_CAVES,
        Biome.DRIPSTONE_CAVES
    )

    // 定义河流生物群系
    private val RIVER_BIOMES = setOf(
        Biome.RIVER,
        Biome.FROZEN_RIVER
    )

    // 定义海洋生物群系
    private val OCEAN_BIOMES = setOf(
        Biome.OCEAN,
        Biome.DEEP_OCEAN,
        Biome.FROZEN_OCEAN,
        Biome.DEEP_FROZEN_OCEAN,
        Biome.COLD_OCEAN,
        Biome.DEEP_COLD_OCEAN,
        Biome.DEEP_LUKEWARM_OCEAN,
        Biome.LUKEWARM_OCEAN,
        Biome.WARM_OCEAN
    )

    // 定义沙漠生物群系
    private val DESERT_BIOMES = setOf(
        Biome.DESERT
    )

    // 定义恶地生物群系
    private val BADLANDS_BIOMES = setOf(
        Biome.BADLANDS,
        Biome.ERODED_BADLANDS,
        Biome.WOODED_BADLANDS
    )

    // 定义冰雪生物群系
    private val SNOWY_BIOMES = setOf(
        Biome.SNOWY_PLAINS,
        Biome.SNOWY_TAIGA,
        Biome.SNOWY_SLOPES,
        Biome.FROZEN_PEAKS,
        Biome.FROZEN_RIVER,
        Biome.FROZEN_OCEAN,
        Biome.DEEP_FROZEN_OCEAN,
        Biome.ICE_SPIKES
    )

    // 定义丛林生物群系
    private val JUNGLE_BIOMES = setOf(
        Biome.JUNGLE,
        Biome.SPARSE_JUNGLE,
        Biome.BAMBOO_JUNGLE
    )

    // 定义山地生物群系
    private val MOUNTAIN_BIOMES = setOf(
        Biome.WINDSWEPT_HILLS,
        Biome.WINDSWEPT_FOREST,
        Biome.WINDSWEPT_SAVANNA,
        Biome.MEADOW,
        Biome.GROVE,
        Biome.SNOWY_SLOPES,
        Biome.FROZEN_PEAKS,
        Biome.JAGGED_PEAKS,
        Biome.STONY_PEAKS
    )

    // 定义沼泽生物群系
    private val SWAMP_BIOMES = setOf(
        Biome.SWAMP,
        Biome.MANGROVE_SWAMP
    )

    // 定义洞穴生物群系
    private val CAVE_BIOMES = setOf(
        Biome.LUSH_CAVES,
        Biome.DRIPSTONE_CAVES,
        Biome.DEEP_DARK
    )

    // 定义下界生物群系
    private val NETHER_BIOMES = setOf(
        Biome.NETHER_WASTES,
        Biome.SOUL_SAND_VALLEY,
        Biome.CRIMSON_FOREST,
        Biome.WARPED_FOREST,
        Biome.BASALT_DELTAS
    )

    // 定义末地生物群系
    private val THE_END_BIOMES = setOf(
        Biome.THE_END,
        Biome.END_MIDLANDS,
        Biome.END_HIGHLANDS,
        Biome.END_BARRENS,
        Biome.SMALL_END_ISLANDS
    )

    /**
     * 判断是否为自然生物群系
     */
    fun isNaturalBiome(location: Location): Boolean {
        return NATURAL_BIOMES.contains(location.block.biome)
    }

    /**
     * 判断是否为河流生物群系
     */
    fun isRiverBiome(location: Location): Boolean {
        return RIVER_BIOMES.contains(location.block.biome)
    }

    /**
     * 判断是否为海洋生物群系
     */
    fun isOceanBiome(location: Location): Boolean {
        return OCEAN_BIOMES.contains(location.block.biome)
    }

    /**
     * 判断是否为沙漠生物群系
     */
    fun isDesertBiome(location: Location): Boolean {
        return DESERT_BIOMES.contains(location.block.biome)
    }

    /**
     * 判断是否为恶地生物群系
     */
    fun isBadlandsBiome(location: Location): Boolean {
        return BADLANDS_BIOMES.contains(location.block.biome)
    }

    /**
     * 判断是否为冰雪生物群系
     */
    fun isSnowyBiome(location: Location): Boolean {
        return SNOWY_BIOMES.contains(location.block.biome)
    }

    /**
     * 判断是否为丛林生物群系
     */
    fun isJungleBiome(location: Location): Boolean {
        return JUNGLE_BIOMES.contains(location.block.biome)
    }

    /**
     * 判断是否为山地生物群系
     */
    fun isMountainBiome(location: Location): Boolean {
        return MOUNTAIN_BIOMES.contains(location.block.biome)
    }

    /**
     * 判断是否为沼泽生物群系
     */
    fun isSwampBiome(location: Location): Boolean {
        return SWAMP_BIOMES.contains(location.block.biome)
    }

    /**
     * 判断是否为洞穴生物群系
     */
    fun isCaveBiome(location: Location): Boolean {
        return CAVE_BIOMES.contains(location.block.biome)
    }

    /**
     * 判断是否为下界生物群系
     */
    fun isNetherBiome(location: Location): Boolean {
        return NETHER_BIOMES.contains(location.block.biome)
    }

    /**
     * 判断是否为末地生物群系
     */
    fun isTheEndBiome(location: Location): Boolean {
        return THE_END_BIOMES.contains(location.block.biome)
    }

    /**
     * 判断是否为水上生物群系（河流或海洋）
     */
    fun isWaterBiome(location: Location): Boolean {
        return isRiverBiome(location) || isOceanBiome(location)
    }
}