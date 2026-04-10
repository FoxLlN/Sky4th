package sky4th.dungeon.monster.event

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.block.Action
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.persistence.PersistentDataType
import sky4th.core.api.LanguageAPI
import sky4th.dungeon.util.LanguageUtil.sendLangSys
import sky4th.dungeon.Dungeon
import sky4th.dungeon.config.ConfigManager
import sky4th.dungeon.config.LootItemConfig
import sky4th.dungeon.loadout.equipment.LoadoutSetSkillState
import sky4th.dungeon.search.BaseSearchManager
import sky4th.dungeon.search.SearchLootService
import sky4th.dungeon.util.CreditsHeadUtil
import sky4th.dungeon.monster.core.MonsterRegistry
import sky4th.dungeon.monster.head.MonsterHeadFactory
import sky4th.dungeon.player.PlayerManager
import kotlin.random.Random

/**
 * 怪物头颅搜索管理器
 *
 * 功能：
 * - 玩家右键点击怪物头颅时打开搜索UI
 * - 搜索完成后物品直接显示在UI中
 * - 支持搜索进度保存，退出界面后可继续搜索
 * - 左上角第一个格子显示怪物头颅
 * - 标题为怪物实体名称
 * - 搜索逻辑根据怪物掉落物表的概率进行
 */
class MonsterSearchManager(
    plugin: JavaPlugin,
    private val configManager: ConfigManager,
    private val playerManager: PlayerManager,
    searchLootService: SearchLootService
) : BaseSearchManager(plugin, searchLootService), Listener {

    /**
     * 怪物头颅全局状态数据
     * 记录怪物头颅的搜索状态，包括已找到的物品和搜索进度
     */
    private data class MonsterHeadState(
        val headId: String,
        val monsterId: String,
        override val lootList: List<Pair<LootItemConfig, Int>>, // 物品配置和搜索时间
        override val items: MutableList<ItemStack?> = MutableList(BaseSearchManager.ITEM_SLOTS.size) { null }, // 所有物品槽位的物品（null表示空）
        override var currentItemIndex: Int = 0, // 当前正在搜索的物品索引
        override var progressSlotIndex: Int = 0, // 当前进度条槽位索引
        override var searchComplete: Boolean = false, // 搜索是否完成
        override var uiInitialized: Boolean = false // UI是否已初始化（是否已显示过占位符）
    ) : SearchState

    // ========== 复合键辅助方法 ==========
    /**
     * 创建复合键：instanceFullId_worldX_worldY_worldZ
     */
    private fun makeFullKey(instanceFullId: String, worldName: String, x: Int, y: Int, z: Int): String {
        return "${instanceFullId}_${worldName}_${x}_${y}_${z}"
    }
    
    /**
     * 从复合键中提取实例ID
     */
    private fun extractInstanceId(fullKey: String): String {
        return fullKey.substringBeforeLast("_").substringBeforeLast("_").substringBeforeLast("_").substringBeforeLast("_")
    }

    /**
     * 从完整实例ID中提取地牢ID
     * 完整实例ID格式：{dungeonId}_{instanceId}
     */
    private fun extractDungeonId(instanceFullId: String): String? {
        return instanceFullId.substringBeforeLast('_')
    }

    // ========== 数据存储（使用复合键优化） ==========
    // 怪物头颅状态（复合键：instanceFullId_worldX_worldY_worldZ -> state）
    private val monsterHeadStates: MutableMap<String, MonsterHeadState> = mutableMapOf()
    // 实例索引：用于快速批量操作（instanceFullId -> 头颅键集合）
    private val instanceHeadKeys: MutableMap<String, MutableSet<String>> = mutableMapOf()

    private val random: Random = Random.Default

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.hand != org.bukkit.inventory.EquipmentSlot.HAND || event.action != Action.RIGHT_CLICK_BLOCK) return
        val clickedBlock = event.clickedBlock ?: return
        val player = event.player
        
        // 获取玩家所在的实例
        val instanceFullId = playerManager.getCurrentInstanceFullId(player) ?: return
        
        // 检查点击的方块是否是怪物头颅
        val monsterId = getMonsterIdFromBlock(clickedBlock)
        if (monsterId == null) return


        // 取消事件，防止打开默认界面
        event.isCancelled = true


        // 处理怪物头颅搜索
        handleMonsterHeadSearch(player, clickedBlock, monsterId, instanceFullId)
    }

    /**
     * 从方块获取怪物ID
     */
    private fun getMonsterIdFromBlock(block: org.bukkit.block.Block): String? {
        // 检查方块是否是头颅类型（支持所有类型的头颅）
        val isHeadBlock = when (block.type) {
            Material.PLAYER_HEAD, Material.PLAYER_WALL_HEAD,
            Material.ZOMBIE_HEAD, Material.ZOMBIE_WALL_HEAD,
            Material.SKELETON_SKULL, Material.SKELETON_WALL_SKULL,
            Material.CREEPER_HEAD, Material.CREEPER_WALL_HEAD,
            Material.DRAGON_HEAD, Material.DRAGON_WALL_HEAD,
            Material.PIGLIN_HEAD, Material.PIGLIN_WALL_HEAD -> true
            else -> false
        }

        if (!isHeadBlock) {
            return null
        }

        // 检查方块状态是否有怪物ID标记
        val state = block.state as? org.bukkit.block.Skull ?: return null

        // 尝试从方块状态获取怪物ID
        val monsterKey = NamespacedKey(plugin, "monster_id")
        val container = state.persistentDataContainer
        val result = container.get(monsterKey, PersistentDataType.STRING)
        return result
    }

    /**
     * 处理怪物头颅搜索
     * @param player 玩家
     * @param block 点击的方块
     * @param monsterId 怪物ID
     * @param instanceFullId 实例完整ID
     */
    private fun handleMonsterHeadSearch(player: Player, block: org.bukkit.block.Block, monsterId: String, instanceFullId: String) {
        val plugin = Dungeon.instance
        // 生成唯一的头颅ID（使用复合键）
        val headId = makeFullKey(instanceFullId, block.location.world?.name ?: "", block.location.blockX, block.location.blockY, block.location.blockZ)

        // 检查怪物头颅是否有全局状态
        val headState = monsterHeadStates[headId]
        if (headState != null) {
            // 怪物头颅已有状态，检查是否可以获取搜索锁定
            if (!tryAcquireSearchLock(headId, player.uniqueId)) {
                // 无法获取锁定，说明有其他玩家正在搜索
                player.sendLangSys(plugin, "search.already-in-use")
                return
            }
            // 怪物头颅已有状态，直接打开UI显示当前状态
            openSearchUI(player, headState)
            // 播放怪物尸体打开音效
            playMonsterBodyOpenSound(player)
            return
        }

        // 尝试获取搜索锁定
        if (!tryAcquireSearchLock(headId, player.uniqueId)) {
            // 无法获取锁定，说明有其他玩家正在搜索
            player.sendLangSys(plugin, "search.already-in-use")
            return
        }

        // 检查是否已有搜索上下文
        val existingContext = playerSearches[player.uniqueId]
        if (existingContext != null && existingContext.searchId == headId) {
            // 继续之前的搜索，获取头颅状态
            val headState = monsterHeadStates[headId]
            if (headState != null) {
                openSearchUI(player, headState)
            } else {
                // 如果没有头颅状态，直接打开现有上下文的UI
                player.openInventory(existingContext.inventory ?: return)
            }
            return
        }

        // 获取怪物定义
        val monsterDefinition = MonsterRegistry.getDefinition(monsterId)
        if (monsterDefinition == null) {
            player.sendLangSys(plugin, "search.invalid-monster")
            return
        }

        // 开始新的搜索
        val qualityBoost = LoadoutSetSkillState.chikeQualityBoost.remove(player.uniqueId)

        // 使用怪物掉落物表生成物品
        val lootList = mutableListOf<Pair<LootItemConfig, Int>>()

        // 获取玩家当前所在的地牢ID
        val dungeonId = playerManager.getCurrentInstanceFullId(player)?.substringBefore("_") ?: return

        // 根据怪物等级获取掉落物
        val loot = searchLootService.pickRandomMonsterLoot(dungeonId, monsterDefinition.level, player)
        if (loot != null) {
            // 获取物品的搜索时间（根据品级）
            val baseSeconds = configManager.lootTierSearchSeconds[loot.tier] ?: 5
            val searchSeconds = if (hasXuezheTwoPiece(player)) (baseSeconds - 2).coerceAtLeast(1) else baseSeconds
            lootList.add(loot to searchSeconds)
        }

        // 创建怪物头颅全局状态
        val newState = MonsterHeadState(
            headId = headId,
            monsterId = monsterId,
            lootList = lootList,
            items = MutableList(BaseSearchManager.ITEM_SLOTS.size) { null },
            currentItemIndex = 0,
            progressSlotIndex = 0,
            searchComplete = lootList.isEmpty(), // 如果没有掉落物，直接标记为完成
            uiInitialized = false
        )
        monsterHeadStates[headId] = newState
        // 添加到实例索引
        instanceHeadKeys.getOrPut(instanceFullId) { mutableSetOf() }.add(headId)

        openSearchUI(player, newState)
        // 播放怪物尸体打开音效
        playMonsterBodyOpenSound(player)
    }



    override fun getSearchState(searchId: String): SearchState? {
        return monsterHeadStates[searchId]
    }

    override fun createInventoryTitle(searchId: String): String {
        return LanguageAPI.getText(Dungeon.instance, "search.monster-body-title")
    }

    override fun getHeadItem(searchId: String): ItemStack? {
        val headState = monsterHeadStates[searchId] ?: return null
        // 从搜索ID中提取地牢ID（格式：{instanceFullId}_{worldX}_{worldY}_{worldZ}）
        val instanceFullId = extractInstanceId(searchId)
        val dungeonId = extractDungeonId(instanceFullId)
        return MonsterHeadFactory.getMonsterHead(headState.monsterId, dungeonId)
    }

    override fun getSearchId(state: SearchState): String {
        return (state as MonsterHeadState).headId
    }

    override fun createSearchTask(player: Player, context: SearchContext, state: SearchState) {
        val headState = state as MonsterHeadState
        val lootList = context.lootList
        val inventory = context.inventory ?: return

        // 检查 lootList 是否为空
        if (lootList.isEmpty()) {
            // 标记搜索完成
            context.searchComplete = true
            headState.searchComplete = true
            return
        }

        // 获取当前物品的搜索时间
        val currentSearchTime = lootList[context.currentItemIndex].second

        // 计算每个进度槽位的tick数
        val ticksPerSlot = (currentSearchTime * 20) / BaseSearchManager.PROGRESS_SLOTS_COUNT

        context.searchTask = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            // 检查玩家是否在线
            if (!player.isOnline) {
                cancelSearch(player.uniqueId)
                return@Runnable
            }

            // 如果搜索已完成，直接返回
            if (context.searchComplete) {
                return@Runnable
            }

            // 更新进度条
            if (context.progressSlotIndex < BaseSearchManager.PROGRESS_SLOTS.size) {
                // 将当前槽位设置为进行中（黄绿色）
                inventory.setItem(BaseSearchManager.PROGRESS_SLOTS[context.progressSlotIndex], createProgressItemInProgress())
                context.progressSlotIndex++
                // 同步更新怪物头颅状态
                headState.progressSlotIndex = context.progressSlotIndex
                
                // 在进度条的2/4/6/8玻璃板后播放翻书音效
                if (context.progressSlotIndex == 2 || context.progressSlotIndex == 4 || 
                    context.progressSlotIndex == 6 || context.progressSlotIndex == 8) {
                    playPageTurnSound(player)
                }
            } else {
                // 进度条填满，当前物品搜索完成
                if (context.currentItemIndex < lootList.size) {
                    // 显示实际物品
                    val loot = lootList[context.currentItemIndex].first
                    // 从实例完整ID中提取dungeonId
                    val dungeonId = extractInstanceId(context.searchId)
                    val item = searchLootService.createItemStack(dungeonId, loot)

                    // 保存已找到的物品
                    context.items[context.currentItemIndex] = item
                    headState.items[context.currentItemIndex] = item

                    // 更新UI
                    val slot = BaseSearchManager.ITEM_SLOTS[context.currentItemIndex]
                    inventory.setItem(slot, item)
                    
                    // 播放物品发现音效
                    playItemFoundSound(player)

                    context.currentItemIndex++
                    headState.currentItemIndex = context.currentItemIndex

                    // 检查是否还有更多物品
                    if (context.currentItemIndex < lootList.size) {
                        // 重置进度条，继续下一个物品
                        BaseSearchManager.PROGRESS_SLOTS.forEach { inventory.setItem(it, createProgressItem(false)) }
                        context.progressSlotIndex = 0
                        headState.progressSlotIndex = 0

                        // 重新计算下一个物品的搜索时间
                        val nextSearchTime = lootList[context.currentItemIndex].second
                        val nextTicksPerSlot = (nextSearchTime * 20) / BaseSearchManager.PROGRESS_SLOTS_COUNT

                        // 取消当前任务并创建新任务（使用新的搜索时间）
                        context.searchTask?.cancel()
                        createSearchTask(player, context, headState)
                    } else {
                        // 所有物品搜索完成
                        context.searchComplete = true
                        headState.searchComplete = true
                        // 将进度条全部设为绿色
                        BaseSearchManager.PROGRESS_SLOTS.forEach { inventory.setItem(it, createProgressItem(true)) }

                        // 怪物头颅状态已更新，可以重复打开
                    }
                }
            }
        }, ticksPerSlot.toLong(), ticksPerSlot.toLong())
    }

    /**
     * 清空所有怪物头颅搜索数据
     * 用于地牢重置时清理内存中的搜索状态
     */
    fun clearAllMonsterSearchData() {
        // 取消所有正在进行的搜索任务
        playerSearches.values.forEach { context ->
            context.searchTask?.cancel()
            context.searchTask = null
        }

        // 清空所有怪物头颅状态
        monsterHeadStates.clear()
        instanceHeadKeys.clear()
    }

    /**
     * 清理指定实例的所有怪物头颅数据
     * @param instanceFullId 实例完整ID
     */
    fun clearForInstance(instanceFullId: String) {
        val headKeys = instanceHeadKeys.remove(instanceFullId) ?: return
        for (headKey in headKeys) {
            monsterHeadStates.remove(headKey)
        }
    }
}
