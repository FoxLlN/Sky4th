
package sky4th.dungeon.container

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.event.block.Action
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType
import sky4th.dungeon.Dungeon
import sky4th.dungeon.config.ConfigManager
import sky4th.dungeon.config.ContainerConfig
import sky4th.dungeon.config.LootItemConfig
import sky4th.dungeon.config.Region
import sky4th.dungeon.loadout.equipment.LoadoutSetSkillState
import sky4th.dungeon.player.PlayerManager
import sky4th.dungeon.search.BaseSearchManager
import sky4th.dungeon.search.SearchLootService
import sky4th.dungeon.util.CreditsHeadUtil
import sky4th.dungeon.util.LanguageUtil.sendLangSys
import java.util.UUID
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * 容器搜索管理器
 *
 * 功能：
 * - 在配置的容器范围附近 1 格内显示"容器包裹"粒子效果
 * - 玩家在容器附近右键时打开搜索UI
 * - 搜索完成后物品直接显示在UI中，不再弹出掉落物
 * - 支持搜索进度保存，退出界面后可继续搜索
 * - 不显示BOSSBAR和搜索文本提示
 */
class ContainerSearchManager(
    plugin: JavaPlugin,
    private val configManager: ConfigManager,
    private val playerManager: PlayerManager,
    private val lootService: ContainerLootService,
    searchLootService: SearchLootService
) : BaseSearchManager(plugin, searchLootService) {

    // ========== 复合键辅助方法 ==========
    /**
     * 创建复合键：instanceFullId_containerId
     */
    private fun makeFullKey(instanceFullId: String, containerId: String): String {
        return "${instanceFullId}_$containerId"
    }
    
    /**
     * 从复合键中提取实例ID
     */
    private fun extractInstanceId(fullKey: String): String {
        return fullKey.substringBeforeLast("_")
    }
    
    /**
     * 从复合键中提取容器ID
     */
    private fun extractContainerId(fullKey: String): String {
        return fullKey.substringAfterLast("_")
    }

    // ========== 数据存储（使用复合键优化） ==========
    // 容器配置（复合键：instanceFullId_containerId -> config）
    private val containers: MutableMap<String, ContainerConfig> = mutableMapOf()
    // 容器中心点（复合键）
    private val containerCenters: MutableMap<String, Location> = mutableMapOf()
    // 容器轮廓点（复合键）
    private val containerOutlines: MutableMap<String, List<Location>> = mutableMapOf()
    // 已搜索容器（复合键集合）
    private val searchedContainers: MutableSet<String> = mutableSetOf()
    
    // 实例索引：用于快速批量操作（instanceFullId -> 容器ID集合）
    private val instanceContainerIds: MutableMap<String, MutableSet<String>> = mutableMapOf()

    /**
     * 容器全局状态数据
     * 记录容器的搜索状态，包括已找到的物品和搜索进度
     */
    private data class ContainerState(
        val containerId: String,
        override val lootList: List<Pair<LootItemConfig, Int>>, // 物品配置和搜索时间
        override val items: MutableList<ItemStack?> = MutableList(BaseSearchManager.ITEM_SLOTS.size) { null }, // 所有物品槽位的物品（null表示空）
        val foundItems: MutableList<ItemStack?> = MutableList(BaseSearchManager.ITEM_SLOTS.size) { null }, // 已找到的物品
        val storageItems: MutableList<ItemStack?> = MutableList(BaseSearchManager.ITEM_SLOTS.size) { null }, // 存储区域的物品
        override var currentItemIndex: Int = 0, // 当前正在搜索的物品索引
        override var progressSlotIndex: Int = 0, // 当前进度条槽位索引
        override var searchComplete: Boolean = false, // 搜索是否完成
        override var uiInitialized: Boolean = false // UI是否已初始化（是否已显示过占位符）
    ) : BaseSearchManager.SearchState

    // 容器状态（复合键：instanceFullId_containerId -> state）
    private val containerStates: MutableMap<String, ContainerState> = mutableMapOf()

    /**
     * 搜索上下文数据
     */
    private var particleTask: BukkitTask? = null
    private val random: Random = Random.Default

    /**
     * 移除容器的"已搜索"标记和状态，使其可再次被搜索（学者技能用）。
     * @param player 玩家
     * @param containerId 容器ID
     */
    fun resetContainer(player: Player, containerId: String) {
        val instanceFullId = playerManager.getCurrentInstanceFullId(player) ?: return
        val fullKey = makeFullKey(instanceFullId, containerId)
        searchedContainers.remove(fullKey)
        // 移除容器状态，这样下次打开时会重新初始化
        containerStates.remove(fullKey)
    }

    /**
     * 查找玩家范围内最近的一个已搜索过的容器，供学者技能刷新用。
     * @param player 玩家
     * @param maxDistance 最大距离
     * @return 容器ID
     */
    fun findNearestSearchedContainer(player: Player, maxDistance: Double): String? {
        val instanceFullId = playerManager.getCurrentInstanceFullId(player) ?: return null
        val containerIds = instanceContainerIds[instanceFullId] ?: return null
        
        var resultId: String? = null
        var bestDistSq = maxDistance * maxDistance
        for (containerId in containerIds) {
            val fullKey = makeFullKey(instanceFullId, containerId)
            if (!searchedContainers.contains(fullKey)) continue
            val config = containers[fullKey] ?: continue
            val distSq = distanceSquaredToRegion(player.location, config)
            if (distSq <= bestDistSq) {
                bestDistSq = distSq
                resultId = containerId
            }
        }
        return resultId
    }

    /**
     * 随机抽一个物品放入玩家背包（委托给掉落物服务），供命令等外部调用。
     */
    fun giveRandomLootToPlayer(player: Player, dungeonId: String, tier: String? = null): Boolean {
        return lootService.giveRandomLootToPlayer(dungeonId, player, tier)
    }

    /**
     * 在副本世界加载完成后，根据配置初始化所有容器的粒子与搜索状态。
     * @param world 世界
     * @param instanceFullId 实例完整ID
     * @param dungeonId 地牢ID
     */
    fun initForWorld(world: World, instanceFullId: String, dungeonId: String) {
 
        // 初始化该实例的索引
        instanceContainerIds[instanceFullId] = mutableSetOf()

        // 根据dungeonId获取对应的容器配置并初始化
        val dungeonConfig = configManager.getDungeonConfig(dungeonId)
        if (dungeonConfig != null) {
            for (cfg in dungeonConfig.containers) {
                val fullKey = makeFullKey(instanceFullId, cfg.id)

                // 创建新的Region对象,使用实例世界的实际名称
                val newRegion = Region(
                    cfg.region.minX,
                    cfg.region.minY,
                    cfg.region.minZ,
                    cfg.region.maxX,
                    cfg.region.maxY,
                    cfg.region.maxZ,
                    world.name  // 使用实例世界的实际名称,例如 "test_1" 而不是 "test"
                )

                // 创建新的ContainerConfig对象
                val newConfig = ContainerConfig(
                    cfg.id,
                    newRegion,
                    cfg.level,
                    cfg.name,
                    cfg.texture
                )

                containers[fullKey] = newConfig
                val region = newRegion
                val centerX = (region.minX + region.maxX) / 2.0 + 0.5
                val centerY = (region.minY + region.maxY) / 2.0 + 0.5
                val centerZ = (region.minZ + region.maxZ) / 2.0 + 0.5
                containerCenters[fullKey] = Location(world, centerX, centerY, centerZ)
                containerOutlines[fullKey] = buildOutlinePoints(world, region)
                instanceContainerIds[instanceFullId]?.add(cfg.id)
            }
        } else {
            plugin.logger.warning("未找到地牢配置: $dungeonId")
        }

        particleTask = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            if (containers.isEmpty()) return@Runnable

            // 玩家靠近容器时总是显示END_ROD粒子效果
            for (player in world.players) {
                val instanceFullId = playerManager.getCurrentInstanceFullId(player) ?: continue
                val containerIds = instanceContainerIds[instanceFullId] ?: continue
                
                var nearFullKey: String? = null
                var bestDistSq = 2.0 * 2.0
                
                for (containerId in containerIds) {
                    val fullKey = makeFullKey(instanceFullId, containerId)
                    val config = containers[fullKey] ?: continue
                    val distSq = distanceSquaredToRegion(player.location, config)
                    if (distSq < bestDistSq) {
                        bestDistSq = distSq
                        nearFullKey = fullKey
                    }
                }
                
                if (nearFullKey != null) {
                    val outline = containerOutlines[nearFullKey] ?: continue
                    for (point in outline) {
                        world.spawnParticle(Particle.END_ROD, point, 1, 0.0, 0.0, 0.0, 0.0)
                    }
                }
            }
        }, 20L, 10L)
    }

    /**
     * 清理指定实例的所有容器数据
     * @param instanceFullId 实例完整ID
     */
    fun clearForInstance(instanceFullId: String) {
        val containerIds = instanceContainerIds.remove(instanceFullId) ?: return
        for (containerId in containerIds) {
            val fullKey = makeFullKey(instanceFullId, containerId)
            containers.remove(fullKey)
            containerCenters.remove(fullKey)
            containerOutlines.remove(fullKey)
            containerStates.remove(fullKey)
            searchedContainers.remove(fullKey)
        }
    }

    private fun clearAll() {
        // 取消粒子任务
        particleTask?.cancel()
        particleTask = null

        // 取消所有正在进行的搜索任务
        playerSearches.values.forEach { context ->
            context.searchTask?.cancel()
            context.searchTask = null
        }

        // 清空所有容器状态
        containers.clear()
        containerCenters.clear()
        containerOutlines.clear()
        playerSearches.clear()
        searchedContainers.clear()
        containerStates.clear()
    }

    @EventHandler
    fun onWorldLoad(event: WorldLoadEvent) {
        val world = event.world
        // 检查世界名称是否符合地牢实例命名规范：dungeonId_instanceId
        val parts = world.name.split("_")
        if (parts.size >= 2) {
            val dungeonId = parts[0]
            val instanceId = parts[1]
            // 检查是否是有效的地牢配置
            if (configManager.getDungeonConfig(dungeonId) != null) {
                val instanceFullId = "${dungeonId}_${instanceId}"
                initForWorld(world, instanceFullId, dungeonId)
            }
        }
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND || event.action != Action.RIGHT_CLICK_BLOCK) return
        val clickedBlock = event.clickedBlock ?: return
        val player = event.player
        
        // 获取玩家所在的实例
        val instanceFullId = playerManager.getCurrentInstanceFullId(player) ?: return

        // 检查是否是配置的容器
        val containerId = findContainerAtLocation(clickedBlock.location, instanceFullId) ?: return

        event.isCancelled = true
        val fullKey = makeFullKey(instanceFullId, containerId)
        val containerConfig = containers[fullKey] ?: return
        // 检查容器是否有全局状态
        val containerState = containerStates[fullKey]

        if (containerState != null) {
            // 容器已有状态，检查是否可以获取搜索锁定
            if (!tryAcquireSearchLock(fullKey, player.uniqueId)) {
                // 无法获取锁定，说明有其他玩家正在搜索
                player.sendLangSys(plugin, "search.already-in-use")
                return
            }
            // 容器已有状态，直接打开UI显示当前状态
            openSearchUI(player, containerState)
            // 播放容器打开音效
            playContainerOpenSound(player, containerConfig.level)
            return
        }

        // 尝试获取搜索锁定
        if (!tryAcquireSearchLock(fullKey, player.uniqueId)) {
            // 无法获取锁定，说明有其他玩家正在搜索
            player.sendLangSys(plugin, "search.already-in-use")
            return
        }

        // 容器没有状态，开始新的搜索
        val itemCount = getItemCountForContainer(containerConfig)
        val qualityBoost = LoadoutSetSkillState.chikeQualityBoost.remove(player.uniqueId)

        val lootList = mutableListOf<Pair<LootItemConfig, Int>>()
        for (index in 0 until itemCount) {
            val useBoost = qualityBoost && index == 0
            // 从玩家当前所在的地牢实例获取dungeonId
            val dungeonId = instanceFullId.substringBefore("_")
            val (loot, baseSeconds) = lootService.pickRandomLoot(
                dungeonId,
                containerConfig.level,
                useBoost,
                debugPlayer = if (index == 0) player else null
            ) ?: continue
            val searchSeconds = if (hasXuezheTwoPiece(player)) (baseSeconds - 2).coerceAtLeast(1) else baseSeconds
            lootList.add(loot to searchSeconds)
        }

        if (lootList.isEmpty()) {
            player.sendLangSys(plugin, "search.no-loot")
            return
        }

        // 创建容器全局状态
        val newState = ContainerState(
            containerId = fullKey,
            lootList = lootList,
            items = MutableList(BaseSearchManager.ITEM_SLOTS.size) { null },
            currentItemIndex = 0,
            progressSlotIndex = 0,
            searchComplete = false,
            uiInitialized = false
        )
        containerStates[fullKey] = newState

        openSearchUI(player, newState)
        // 播放容器打开音效
        playContainerOpenSound(player, containerConfig.level)
    }

    

    override fun createInventoryTitle(searchId: String): String {
        // searchId格式为 instanceFullId_containerId，需要提取containerId
        val containerId = extractContainerId(searchId)
        val containerConfig = containers[searchId]
        return if (containerConfig?.name?.isNotEmpty() == true) {
            containerConfig.name
        } else {
            containerId
        } 
    }

    override fun getHeadItem(searchId: String): ItemStack? {
        val containerConfig = containers[searchId] ?: return null
        val containerTexture = containerConfig.texture
        if (containerTexture.isNotEmpty()) {
            return CreditsHeadUtil.createCustomHead(containerConfig.name, containerTexture)
        }
        return null
    }

    override fun getSearchState(searchId: String): SearchState? {
        return containerStates[searchId]
    }

    override fun getSearchId(state: SearchState): String {
        return (state as ContainerState).containerId
    }

    override fun createSearchTask(player: Player, context: SearchContext, state: SearchState) {
        val containerState = state as ContainerState
        val lootList = context.lootList
        val inventory = context.inventory ?: return

        val currentSearchTime = lootList[context.currentItemIndex].second
        val ticksPerSlot = (currentSearchTime * 20) / BaseSearchManager.PROGRESS_SLOTS_COUNT

        context.searchTask = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            if (!player.isOnline) {
                cancelSearch(player.uniqueId)
                return@Runnable
            }

            if (context.searchComplete) {
                return@Runnable
            }

            if (context.progressSlotIndex < BaseSearchManager.PROGRESS_SLOTS.size) {
                inventory.setItem(BaseSearchManager.PROGRESS_SLOTS[context.progressSlotIndex], createProgressItemInProgress())
                context.progressSlotIndex++
                containerState.progressSlotIndex = context.progressSlotIndex
                
                // 在进度条的2/4/6/8玻璃板后播放翻书音效
                if (context.progressSlotIndex == 2 || context.progressSlotIndex == 4 || 
                    context.progressSlotIndex == 6 || context.progressSlotIndex == 8) {
                    playPageTurnSound(player)
                }
            } else {
                // 进度条填满，当前物品搜索完成
                if (context.currentItemIndex < lootList.size) {
                    val loot = lootList[context.currentItemIndex].first
                    // 从实例完整ID中提取dungeonId
                    val dungeonId = extractInstanceId(context.searchId)
                    val item = lootService.createItemStack(dungeonId, loot)

                    context.items[context.currentItemIndex] = item
                    containerState.items[context.currentItemIndex] = item

                    val slot = BaseSearchManager.ITEM_SLOTS[context.currentItemIndex]
                    inventory.setItem(slot, item)
                    
                    // 播放物品发现音效
                    playItemFoundSound(player)

                    context.currentItemIndex++
                    containerState.currentItemIndex = context.currentItemIndex

                    if (context.currentItemIndex < lootList.size) {
                        BaseSearchManager.PROGRESS_SLOTS.forEach { inventory.setItem(it, createProgressItem(false)) }
                        context.progressSlotIndex = 0
                        containerState.progressSlotIndex = 0

                        val nextSearchTime = lootList[context.currentItemIndex].second
                        val nextTicksPerSlot = (nextSearchTime * 20) / BaseSearchManager.PROGRESS_SLOTS_COUNT

                        context.searchTask?.cancel()
                        createSearchTask(player, context, containerState)
                    } else {
                        context.searchComplete = true
                        containerState.searchComplete = true
                        // 标记容器为已搜索，供学者套技能使用
                        searchedContainers.add(containerState.containerId)
                        BaseSearchManager.PROGRESS_SLOTS.forEach { inventory.setItem(it, createProgressItem(true)) }
                    }
                }
            }
        }, ticksPerSlot.toLong(), ticksPerSlot.toLong())
    }

    @EventHandler
    fun onContainerInventoryClick(event: InventoryClickEvent) {
        
        val player = event.whoClicked as? Player ?: return
        val context = playerSearches[player.uniqueId] ?: return
        val containerState = containerStates[context.searchId] ?: return
        
        val slot = event.slot
        val inventory = event.clickedInventory ?: return
        
        if (!BaseSearchManager.ITEM_SLOTS.contains(slot)) return
        
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            val itemIndex = BaseSearchManager.ITEM_SLOTS.indexOf(slot)
            if (itemIndex >= 0) {
                containerState.items[itemIndex] = inventory.getItem(slot)?.clone()
            }
        }, 1L)
    }


    @EventHandler
    fun onContainerInventoryClose(event: InventoryCloseEvent) {
        
        val player = event.player as? Player ?: return
        val context = playerSearches[player.uniqueId] ?: return
        val containerState = containerStates[context.searchId] ?: return
        
        for (i in BaseSearchManager.ITEM_SLOTS.indices) {
            val slot = BaseSearchManager.ITEM_SLOTS[i]
            containerState.items[i] = event.inventory.getItem(slot)?.clone()
        }
        
        // 播放容器关闭音效
        val containerConfig = containers[context.searchId]
        if (containerConfig != null) {
            playContainerCloseSound(player, containerConfig.level)
        }
    }

    private fun findNearestContainer(location: Location, maxDistance: Double): String? {
        return ContainerSearchHelper.findNearestContainer(
            location,
            containers,
            searchedContainers,
            maxDistance
        )
    }

    private fun findContainerAtLocation(location: Location, instanceFullId: String): String? {
        val containerIds = instanceContainerIds[instanceFullId] ?: return null
        val world = location.world ?: return null
        for (containerId in containerIds) {
            val fullKey = makeFullKey(instanceFullId, containerId)
            val cfg = containers[fullKey] ?: continue
            val region = cfg.region
            if (world.name != region.world) continue
            // 检查位置是否在容器区域内
            if (location.blockX in region.minX..region.maxX &&
                location.blockY in region.minY..region.maxY &&
                location.blockZ in region.minZ..region.maxZ) {
                return containerId
            }
        }
        return null
    }

    /**
     * 根据容器的体积（方块数）决定本次可搜索出的物品数量。
     */
    private fun getItemCountForContainer(containerConfig: ContainerConfig): Int {
        val region = containerConfig.region
        val sizeX = (region.maxX - region.minX + 1).coerceAtLeast(1)
        val sizeY = (region.maxY - region.minY + 1).coerceAtLeast(1)
        val sizeZ = (region.maxZ - region.minZ + 1).coerceAtLeast(1)
        val volume = sizeX * sizeY * sizeZ

        // 从配置文件中读取物品数量范围
        val (minItems, maxItems) = configManager.getItemCountRangeForVolume(volume)

        return random.nextInt(maxItems - minItems + 1) + minItems
    }

    /**
     * 计算位置到区域的距离平方
     */
    private fun distanceSquaredToRegion(location: Location, cfg: ContainerConfig): Double {
        return ContainerSearchHelper.distanceSquaredToRegion(location, cfg.region)
    }

    /**
     * 构建容器轴对齐包围盒 12 条边上的采样点（用于粒子描边）。
     */
    private fun buildOutlinePoints(world: World, region: Region): List<Location> {
        return ContainerSearchHelper.buildOutlinePoints(world, region)
    }

    
}
