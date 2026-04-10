package sky4th.dungeon.container

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.Particle
import org.bukkit.inventory.ItemStack
import sky4th.dungeon.config.Region
import sky4th.dungeon.Dungeon
import sky4th.core.api.LanguageAPI
import kotlin.math.sqrt

/**
 * 容器搜索辅助类
 *
 * 包含容器搜索管理器需要的辅助方法
 */
object ContainerSearchHelper {
    private val plugin = Dungeon.instance

    /**
     * 构建容器轴对齐包围盒 12 条边上的采样点（用于粒子描边）。
     */
    fun buildOutlinePoints(world: World, region: Region): List<Location> {
        val points = mutableListOf<Location>()
        if (region.minX > region.maxX || region.minY > region.maxY || region.minZ > region.maxZ) return points

        val x0 = region.minX.toDouble()
        val x1 = region.maxX + 1.0
        val y0 = region.minY.toDouble()
        val y1 = region.maxY + 1.0
        val z0 = region.minZ.toDouble()
        val z1 = region.maxZ + 1.0

        fun addLine(ax: Double, ay: Double, az: Double, bx: Double, by: Double, bz: Double) {
            val dx = bx - ax
            val dy = by - ay
            val dz = bz - az
            val length = sqrt(dx * dx + dy * dy + dz * dz)
            if (length <= 0.0001) return
            val step = 0.35
            val steps = (length / step).toInt().coerceAtLeast(1)
            for (i in 0..steps) {
                val t = i.toDouble() / steps
                points.add(Location(world, ax + dx * t, ay + dy * t, az + dz * t))
            }
        }

        addLine(x0, y1, z0, x1, y1, z0)
        addLine(x1, y1, z0, x1, y1, z1)
        addLine(x1, y1, z1, x0, y1, z1)
        addLine(x0, y1, z1, x0, y1, z0)
        addLine(x0, y0, z0, x1, y0, z0)
        addLine(x1, y0, z0, x1, y0, z1)
        addLine(x1, y0, z1, x0, y0, z1)
        addLine(x0, y0, z1, x0, y0, z0)
        addLine(x0, y0, z0, x0, y1, z0)
        addLine(x1, y0, z0, x1, y1, z0)
        addLine(x1, y0, z1, x1, y1, z1)
        addLine(x0, y0, z1, x0, y1, z1)

        return points
    }

    /**
     * 计算位置到区域的距离平方
     */
    fun distanceSquaredToRegion(location: Location, region: Region): Double {
        val world = location.world ?: return Double.MAX_VALUE
        if (world.name != region.world) return Double.MAX_VALUE
        val x = location.x
        val z = location.z
        val dx = when {
            x < region.minX -> region.minX - x
            x > region.maxX -> x - region.maxX
            else -> 0.0
        }
        val dz = when {
            z < region.minZ -> region.minZ - z
            z > region.maxZ -> z - region.maxZ
            else -> 0.0
        }
        return dx * dx + dz * dz
    }

    /**
     * 查找最近的容器
     * @param location 位置
     * @param containers 容器配置映射（复合键 -> 配置）
     * @param searchedContainers 已搜索容器集合（复合键集合）
     * @param maxDistance 最大距离
     * @return 容器复合键，未找到返回null
     */
    fun findNearestContainer(
        location: Location,
        containers: Map<String, sky4th.dungeon.config.ContainerConfig>,
        searchedContainers: Set<String>,
        maxDistance: Double
    ): String? {
        if (containers.isEmpty()) return null
        var resultKey: String? = null
        var bestDistSq = maxDistance * maxDistance
        for ((fullKey, cfg) in containers) {
            // 只考虑已搜索的容器
            if (!searchedContainers.contains(fullKey)) continue
            val distSq = distanceSquaredToRegion(location, cfg.region)
            if (distSq <= bestDistSq) {
                bestDistSq = distSq
                resultKey = fullKey
            }
        }
        return resultKey
    }

    /**
     * 创建搜索条物品
     * @param searchSeconds 搜索时间（秒），默认为5秒
     * @return 搜索条物品
     */
    fun createSearchBar(searchSeconds: Int = 5): ItemStack {
        val item = ItemStack(Material.WHITE_STAINED_GLASS_PANE)
        item.editMeta { meta ->
            meta.displayName(LanguageAPI.getComponent(plugin, "search.ui.search-bar", "seconds" to searchSeconds))
            
            val lore = listOf(
                LanguageAPI.toComponent(plugin, ""),
                LanguageAPI.getComponent(plugin, "search.ui.search-time", "seconds" to searchSeconds)
            )
            meta.lore(lore)
        }
        return item
    }
}
