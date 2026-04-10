package sky4th.dungeon.monster.event

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import sky4th.dungeon.monster.core.MonsterMetadata
import sky4th.dungeon.monster.head.MonsterHeadFactory
import sky4th.dungeon.monster.head.MonsterHeadConfig
import sky4th.dungeon.container.ContainerLootService
import sky4th.dungeon.config.ConfigManager
import kotlin.random.Random

/**
 * 怪物死亡事件监听器
 * 
 * 功能：
 * - 禁止怪物掉落任何掉落物
 * - 在怪物死亡位置生成对应怪物的头颅
 */
object MonsterDeathListener : Listener {

    private lateinit var plugin: JavaPlugin
    private lateinit var containerLootService: ContainerLootService
    private lateinit var configManager: ConfigManager
    private val monsterIdKey: NamespacedKey by lazy { NamespacedKey(plugin, "monster_id") }
    private val random = Random.Default

    /**
     * 从世界名称中提取地牢ID
     * 世界名称格式：{dungeonId}_{instanceId}_world
     */
    private fun extractDungeonId(worldName: String): String? {
        // 移除实例ID和_world后缀
        return worldName.substringBeforeLast('_').substringBeforeLast('_')
    }

    fun init(plugin: JavaPlugin, containerLootService: ContainerLootService, configManager: ConfigManager) {
        this.plugin = plugin
        this.containerLootService = containerLootService
        this.configManager = configManager
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        val entity = event.entity

        // 只处理通过MonsterRegistry生成的怪物
        val monsterId = MonsterMetadata.getMonsterId(entity) ?: return

        // 清空所有掉落物
        event.drops.clear()

        // 禁止经验掉落
        event.droppedExp = 0

        // 在死亡位置生成对应怪物的头颅方块
        val dungeonId = extractDungeonId(entity.world.name)
        val headItem = MonsterHeadFactory.getMonsterHead(monsterId, dungeonId)
        if (headItem != null) {
            // 获取头颅方块类型
            val headBlockType = getHeadBlockType(headItem)
            if (headBlockType != null) {
                // 寻找合适的位置放置头颅
                val location = findSuitableLocation(entity.location, headBlockType)
                if (location != null) {
                    // 在找到的位置放置头颅方块
                    val block = location.block
                    block.type = headBlockType

                    // 保存怪物ID到方块中
                    saveMonsterIdToBlock(block, monsterId)
                    
                    // 如果是自定义玩家头颅，设置其纹理
                    if (headItem.type == Material.PLAYER_HEAD) {
                        setCustomHeadTexture(block, headItem)
                    }
                }
            }
        }
    }

    /**
     * 从头颅物品中获取方块类型
     */
    private fun getHeadBlockType(headItem: ItemStack): Material? {
        return when (headItem.type) {
            Material.ZOMBIE_HEAD, Material.ZOMBIE_WALL_HEAD -> Material.ZOMBIE_HEAD
            Material.SKELETON_SKULL, Material.SKELETON_WALL_SKULL -> Material.SKELETON_SKULL
            Material.CREEPER_HEAD, Material.CREEPER_WALL_HEAD -> Material.CREEPER_HEAD
            Material.PLAYER_HEAD, Material.PLAYER_WALL_HEAD -> Material.PLAYER_HEAD
            Material.DRAGON_HEAD, Material.DRAGON_WALL_HEAD -> Material.DRAGON_HEAD
            Material.PIGLIN_HEAD, Material.PIGLIN_WALL_HEAD -> Material.PIGLIN_HEAD
            else -> null
        }
    }

    /**
     * 寻找合适的位置放置头颅
     * @param startLocation 起始位置（怪物死亡位置）
     * @param headBlockType 头颅方块类型
     * @return 合适的位置，如果没有找到则返回null
     */
    private fun findSuitableLocation(startLocation: org.bukkit.Location, headBlockType: Material): org.bukkit.Location? {
        val world = startLocation.world ?: return null
        
        // 首先检查起始位置是否可以放置头颅
        if (canPlaceHeadAt(startLocation, headBlockType) && hasSupportBelow(startLocation)) {
            return startLocation
        }
        
        // 如果起始位置不能放置，则在周围搜索（先水平，再向上）
        val searchRadius = 3

        // 优先级1：水平搜索（同一高度，从中心向外）
        for (radius in 0..searchRadius) {
            for (x in -radius..radius) {
                for (z in -radius..radius) {
                    // 跳过中心点（已在起始位置检查）
                    if (radius == 0) continue

                    val location = startLocation.clone().add(x.toDouble(), 0.0, z.toDouble())
                    // 如果是薄层方块（地毯、草等），可以直接替换，不需要下方支撑
                    val block = location.block
                    val needsSupport = !isReplaceable(block.type)
                    if (canPlaceHeadAt(location, headBlockType) && (!needsSupport || hasSupportBelow(location))) {
                        return location
                    }
                }
            }
        }

        // 优先级2：向上搜索（从中心向外）
        for (y in 1..2) {
            for (radius in 0..searchRadius) {
                for (x in -radius..radius) {
                    for (z in -radius..radius) {
                        // 跳过正上方
                        if (radius == 0 && y == 1) continue

                        val location = startLocation.clone().add(x.toDouble(), y.toDouble(), z.toDouble())
                        // 如果是薄层方块（地毯、草等），可以直接替换，不需要下方支撑
                        val block = location.block
                        val needsSupport = !isReplaceable(block.type)
                        if (canPlaceHeadAt(location, headBlockType) && (!needsSupport || hasSupportBelow(location))) {
                            return location
                        }
                    }
                }
            }
        }

        // 没有找到合适的位置
        return null
    }

    /**
     * 检查指定位置是否可以放置头颅
     * @param location 要检查的位置
     * @param headBlockType 头颅方块类型
     * @return 如果可以放置返回true，否则返回false
     */
    private fun canPlaceHeadAt(location: org.bukkit.Location, headBlockType: Material): Boolean {
        val block = location.block
        
        // 检查该位置是否已经是头颅
        if (isHeadBlock(block.type)) {
            return false
        }
        
        // 检查该位置是否为空气或可替换的方块（不要求下方有支撑）
        if (block.type.isAir || isReplaceable(block.type)) {
            return true
        }
        
        return false
    }

    /**
     * 检查指定位置下方是否有支撑
     * @param location 要检查的位置
     * @return 如果下方有支撑返回true，否则返回false
     */
    private fun hasSupportBelow(location: org.bukkit.Location): Boolean {
        val world = location.world ?: return false
        val below = location.clone().subtract(0.0, 1.0, 0.0).block

        // 检查下方方块是否是固体方块（非空气、非液体）
        // 允许楼梯、半砖等作为支撑
        return !below.type.isAir && 
               !isLiquid(below.type) &&
               below.type.isSolid
    }

    /**
     * 检查指定方块是否可替换
     * 只替换空气、液体和薄层方块（如地毯、雪层等），不替换板砖、楼梯、珊瑚、海泡菜等
     */
    private fun isReplaceable(material: Material): Boolean {
        // 空气
        if (material == Material.AIR) return true
        
        // 液体
        if (material == Material.WATER) return true
        if (material == Material.LAVA) return true
        
        // 薄层方块（类似地毯、雪层等）
        if (material.name.endsWith("_CARPET")) return true
        if (material == Material.SNOW) return true
        if (material == Material.SHORT_GRASS) return true
        if (material == Material.FERN) return true
        if (material == Material.DEAD_BUSH) return true
        if (material == Material.SEAGRASS) return true
        if (material == Material.TALL_SEAGRASS) return true
        if (material == Material.VINE) return true
        if (material.name.endsWith("_LEAVES")) return true
        if (material.name.contains("PRESSURE_PLATE")) return true
        if (material == Material.LILY_PAD) return true
        if (material == Material.MOSS_CARPET) return true
        
        // 不替换板砖、楼梯、珊瑚、海泡菜等占比较大的方块
        return false
    }

    /**
     * 检查指定方块是否为液体
     */
    private fun isLiquid(material: Material): Boolean {
        return material == Material.WATER ||
               material == Material.LAVA ||
               material.name.contains("FLOWING_")
    }

    /**
     * 检查指定方块是否为头颅方块
     */
    private fun isHeadBlock(material: Material): Boolean {
        return material == Material.ZOMBIE_HEAD ||
               material == Material.SKELETON_SKULL ||
               material == Material.CREEPER_HEAD ||
               material == Material.PLAYER_HEAD ||
               material == Material.DRAGON_HEAD ||
               material == Material.PIGLIN_HEAD ||
               material == Material.ZOMBIE_WALL_HEAD ||
               material == Material.SKELETON_WALL_SKULL ||
               material == Material.CREEPER_WALL_HEAD ||
               material == Material.PLAYER_WALL_HEAD ||
               material == Material.DRAGON_WALL_HEAD ||
               material == Material.PIGLIN_WALL_HEAD
    }

    /**
     * 设置自定义玩家头颅的纹理
     */
    private fun setCustomHeadTexture(block: Block, headItem: ItemStack) {
        try {
            val meta = headItem.itemMeta ?: return
            
            // 获取 SkullMeta 中的 GameProfile
            val profileField = meta.javaClass.getDeclaredField("profile")
            profileField.isAccessible = true
            val profile = profileField.get(meta) ?: return
            
            // 获取方块状态并设置 profile
            val blockState = block.state
            val skullState = blockState as? org.bukkit.block.Skull ?: return
            
            // 设置方块状态中的 profile
            val skullProfileField = skullState.javaClass.getDeclaredField("profile")
            skullProfileField.isAccessible = true
            skullProfileField.set(skullState, profile)
            
            // 更新方块状态
            skullState.update(true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 保存怪物ID到方块中
     */
    private fun saveMonsterIdToBlock(block: Block, monsterId: String) {
        try {
            val blockState = block.state
            if (blockState !is org.bukkit.block.TileState) {
                return
            }
            val dataContainer = blockState.persistentDataContainer
            dataContainer.set(monsterIdKey, PersistentDataType.STRING, monsterId)
            blockState.update()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
