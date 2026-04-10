
package sky4th.dungeon.player

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import org.bukkit.event.EventHandler
import sky4th.dungeon.util.LanguageUtil.sendLangSys
import sky4th.core.api.LanguageAPI
import sky4th.dungeon.Dungeon
import sky4th.dungeon.config.ConfigManager
import sky4th.dungeon.search.SearchLootService
import sky4th.dungeon.head.PlayerCashHead
import java.util.UUID

/**
 * 玩家死亡UI
 * 用于显示玩家死亡时掉落的物品
 */
class PlayerDeathUI(
    private val plugin: JavaPlugin,
    private val configManager: ConfigManager,
    private val searchLootService: SearchLootService,
    private val baseSearchManager: sky4th.dungeon.search.BaseSearchManager,
    private val backpackManager: BackpackManager,
    private val deathListener: PlayerDeathListener?
) : org.bukkit.event.Listener {
    val deathInventoryKey = org.bukkit.NamespacedKey(plugin, "player_death_inventory")

    // 死亡玩家搜索状态
    private class PlayerDeathState(
        val playerUuid: UUID,
        val lootList: List<Pair<sky4th.dungeon.config.LootItemConfig, Int>>,
        val originalItems: List<ItemStack>, // 原始物品列表，保存完整信息
        val items: MutableList<ItemStack?> = MutableList(BACKPACK_SLOTS.size) { null },
        var currentItemIndex: Int = 0,
        var progressSlotIndex: Int = 0,
        var searchComplete: Boolean = false,
        var uiInitialized: Boolean = false
    )

    // ========== 复合键辅助方法 ==========
    /**
     * 创建复合键：instanceFullId_playerUuid
     */
    private fun makeFullKey(instanceFullId: String, playerUuid: UUID): String {
        return "${instanceFullId}_${playerUuid}"
    }
    
    /**
     * 从复合键中提取实例ID
     */
    private fun extractInstanceId(fullKey: String): String {
        return fullKey.substringBeforeLast("_")
    }
    
    /**
     * 从复合键中提取玩家UUID
     */
    private fun extractPlayerUuid(fullKey: String): UUID {
        return UUID.fromString(fullKey.substringAfterLast("_"))
    }

    // ========== 数据存储（使用复合键优化） ==========
    // 存储死亡玩家的搜索状态（复合键：instanceFullId_playerUuid -> state）
    private val deathPlayerStates: MutableMap<String, PlayerDeathState> = mutableMapOf()
    // 实例索引：用于快速批量操作（instanceFullId -> 玩家键集合）
    private val instancePlayerKeys: MutableMap<String, MutableSet<String>> = mutableMapOf()

    // 存储玩家的搜索上下文
    private data class SearchContext(
        val searchId: String, // 搜索对象的ID（复合键：instanceFullId_playerUuid）
        val lootList: List<Pair<sky4th.dungeon.config.LootItemConfig, Int>>, // 物品配置和搜索时间
        val items: MutableList<ItemStack?> = MutableList(BACKPACK_SLOTS.size) { null }, // 所有物品槽位的物品（null表示空）
        var currentItemIndex: Int = 0, // 当前正在搜索的物品索引
        var progressSlotIndex: Int = 0, // 当前进度条槽位索引
        var searchComplete: Boolean = false, // 搜索是否完成
        var searchTask: BukkitTask? = null, // 搜索任务
        var inventory: Inventory? = null // 搜索UI库存
    )

    private val playerSearches: MutableMap<UUID, SearchContext> = mutableMapOf()

    /**
     * 播放玩家尸体打开音效（铁装备穿戴声音）
     * @param player 玩家
     */
    private fun playPlayerCorpseOpenSound(player: Player) {
        player.playSound(player.location, org.bukkit.Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.0f)
    }

    /**
     * 播放玩家尸体关闭音效（铁装备卸下声音）
     * @param player 玩家
     */
    private fun playPlayerCorpseCloseSound(player: Player) {
        player.playSound(player.location, org.bukkit.Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.0f)
    }

    /**
     * 播放搜索过程中的锁链穿戴音效
     * @param player 玩家
     */
    private fun playChainEquipSound(player: Player) {
        player.playSound(player.location, org.bukkit.Sound.ITEM_ARMOR_EQUIP_CHAIN, 1.0f, 0.5f)
    }

    companion object {
        const val INVENTORY_SIZE = 54
        const val HEAD_SLOT = 0
        const val CASH_SLOT = 18 // 第三行第1格，用于显示死亡玩家的背包现金

        // 黑色玻璃板隔离槽位
        val GLASS_PANE_SLOTS = listOf(
            1,  // 第一行第2格
            2,  // 第一行第3格
            3,  // 第一行第4格
            4,  // 第一行第5格
            5,  // 第一行第6格
            6,  // 第一行第7格
            7,  // 第一行第8格
            8,  // 第一行第9格
            9,  // 第二行第1格
            10, // 第二行第2格
            16, // 第二行第8格
            17, // 第二行第9格
            19, // 第三行第2格
            25, // 第三行第8格
            26, // 第三行第9格
            27, // 第四行第1格
            28, // 第四行第2格
            34, // 第四行第8格
            35, // 第四行第9格
            36, // 第五行第1格
            37, // 第五行第2格
            43, // 第五行第8格
            44  // 第五行第9格
        )

        // 死亡玩家背包物品槽位（需要搜索读条）
        val BACKPACK_SLOTS = listOf(
            11, 12, 13, 14, 15, // 第二行（除了隔离）
            20, 21, 22, 23, 24, // 第三行（除了隔离）
            29, 30, 31, 32, 33, // 第四行（除了隔离）
            38, 39, 40, 41, 42  // 第五行（除了隔离）
        )

        // 搜索行槽位
        val SEARCH_BAR_SLOTS = listOf(45, 46, 47, 48, 49, 50, 51, 52, 53)
    }

    /**
     * 创建占位符物品
     */
    private fun createPlaceholderItem(): ItemStack {
        val item = ItemStack(Material.BLACK_STAINED_GLASS_PANE)
        item.editMeta { meta ->
            meta.displayName(LanguageAPI.getComponent(plugin, "search.ui.placeholder"))
        }
        return item
    }

    /**
     * 创建进度物品
     */
    private fun createProgressItem(completed: Boolean): ItemStack {
        val material = if (completed) Material.GREEN_STAINED_GLASS_PANE else Material.WHITE_STAINED_GLASS_PANE
        val item = ItemStack(material)
        item.editMeta { meta ->
            meta.displayName(LanguageAPI.getComponent(plugin, if (completed) "search.ui.progress.completed" else "search.ui.progress.in-progress"))
        }
        return item
    }

    /**
     * 创建进度物品（进行中）
     */
    private fun createProgressItemInProgress(): ItemStack {
        val item = ItemStack(Material.LIME_STAINED_GLASS_PANE)
        item.editMeta { meta ->
            meta.displayName(LanguageAPI.getComponent(plugin, "search.ui.progress.active"))
        }
        return item
    }

    /**
     * 创建玻璃板
     */
    private fun createGlassPane(): ItemStack {
        val item = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        item.editMeta { meta ->
            meta.displayName(LanguageAPI.getComponent(plugin, "search.ui.glass-pane"))
        }
        return item
    }



    /**
     * 打开玩家死亡搜索UI
     * @param player 玩家
     * @param deathState 死亡状态
     * @param instanceFullId 实例完整ID
     */
    private fun openSearchUI(player: Player, deathState: PlayerDeathState, instanceFullId: String) {
        val searchId = getSearchId(deathState, instanceFullId)

        // 检查是否可以获取搜索锁定
        if (!tryAcquireSearchLock(searchId, player.uniqueId)) {
            // 无法获取锁定，说明有其他玩家正在搜索
            player.sendLangSys(plugin, "search.already-in-use")
            return
        }

        val deathPlayer = Bukkit.getOfflinePlayer(deathState.playerUuid)
        val inventory = Bukkit.createInventory(
            null,
            INVENTORY_SIZE,
            LanguageAPI.getComponent(plugin, "death.inventory.title", "player" to (deathPlayer.name ?: "Unknown"))
        )

        // 设置玩家头颅
        inventory.setItem(HEAD_SLOT, PlayerDeathHead.create(deathPlayer.name ?: "Unknown", deathState.playerUuid))

        // 设置信用点头颅（显示死亡玩家的背包现金）
        val cashAmount = deathListener?.getDeathPlayerCash(deathState.playerUuid) ?: 0
        if (cashAmount > 0) {
            inventory.setItem(CASH_SLOT, PlayerCashHead.createFullWalletHead(configManager, cashAmount))
        } else {
            inventory.setItem(CASH_SLOT, PlayerCashHead.createEmptyWalletHead(configManager, ))
        }

        // 设置玻璃板
        val glassPane = createGlassPane()
        GLASS_PANE_SLOTS.forEach { inventory.setItem(it, glassPane) }

        // 检查是否是第一次打开UI
        if (!deathState.uiInitialized) {
            // 第一次打开，显示占位符
            for (i in deathState.lootList.indices) {
                val slot = BACKPACK_SLOTS[i]
                val placeholderItem = createPlaceholderItem()
                inventory.setItem(slot, placeholderItem)
            }
            // 标记UI已初始化
            deathState.uiInitialized = true
        } else {
            // 不是第一次打开，恢复所有物品
            for (i in BACKPACK_SLOTS.indices) {
                val slot = BACKPACK_SLOTS[i]
                if (deathState.items[i] != null) {
                    inventory.setItem(slot, deathState.items[i])
                }
            }
        }

        // 更新进度条
        updateProgress(inventory, deathState)
        // 取消旧的搜索任务（如果存在）
        playerSearches[player.uniqueId]?.searchTask?.cancel()

        // 创建临时的搜索上下文用于UI显示
        val context = SearchContext(
            searchId = searchId,
            lootList = deathState.lootList,
            items = MutableList(deathState.items.size) { deathState.items[it]?.clone() },
            currentItemIndex = deathState.currentItemIndex,
            progressSlotIndex = deathState.progressSlotIndex,
            searchComplete = deathState.searchComplete,
            inventory = inventory
        )

        playerSearches[player.uniqueId] = context
        player.openInventory(inventory)
        
        // 播放玩家尸体打开音效
        playPlayerCorpseOpenSound(player)

        // 如果搜索未完成，启动搜索任务
        if (!deathState.searchComplete) {
            createSearchTask(player, context, deathState)
        }
    }

    /**
     * 更新进度条（只在打开UI时调用）
     */
    private fun updateProgress(inventory: Inventory, deathState: PlayerDeathState) {

        if (deathState.searchComplete) {
            // 搜索完成，全部显示绿色
            SEARCH_BAR_SLOTS.forEach { inventory.setItem(it, createProgressItem(true)) }
        } else {
            // 更新进度条 - 只在打开UI时调用，恢复进度条状态
            for (i in SEARCH_BAR_SLOTS.indices) {
                if (i < deathState.progressSlotIndex) {
                    // 已完成的进度显示为黄绿色
                    inventory.setItem(SEARCH_BAR_SLOTS[i], createProgressItemInProgress())
                } else {
                    // 未开始的进度显示为白色
                    inventory.setItem(SEARCH_BAR_SLOTS[i], createProgressItem(false))
                }
            }
        }
    }

    /**
     * 获取搜索状态
     * @param fullKey 复合键（instanceFullId_playerUuid）
     * @return 死亡状态
     */
    private fun getSearchState(fullKey: String): PlayerDeathState? {
        return deathPlayerStates[fullKey]
    }

    /**
     * 获取搜索ID（复合键）
     * @param state 死亡状态
     * @param instanceFullId 实例完整ID
     * @return 复合键
     */
    private fun getSearchId(state: PlayerDeathState, instanceFullId: String): String {
        return makeFullKey(instanceFullId, state.playerUuid)
    }

    /**
     * 创建搜索任务
     */
    private fun createSearchTask(player: Player, context: SearchContext, state: PlayerDeathState) {
        val lootList = context.lootList
        val inventory = context.inventory ?: return

        // 检查 lootList 是否为空
        if (lootList.isEmpty()) {
            // 标记搜索完成
            context.searchComplete = true
            state.searchComplete = true
            return
        }

        val currentSearchTime = lootList[context.currentItemIndex].second
        val ticksPerSlot = (currentSearchTime * 20) / SEARCH_BAR_SLOTS.size

        context.searchTask = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            if (!player.isOnline) {
                cancelSearch(player.uniqueId)
                return@Runnable
            }

            if (context.searchComplete) {
                return@Runnable
            }

            if (context.progressSlotIndex < SEARCH_BAR_SLOTS.size) {
                // 更新进度条 - 与容器搜索一致，只更新当前槽位
                inventory.setItem(SEARCH_BAR_SLOTS[context.progressSlotIndex], createProgressItemInProgress())
                context.progressSlotIndex++
                state.progressSlotIndex = context.progressSlotIndex
                
                // 在进度条的2/4/6/8玻璃板后播放锁链穿戴音效
                if (context.progressSlotIndex == 2 || context.progressSlotIndex == 4 || 
                    context.progressSlotIndex == 6 || context.progressSlotIndex == 8) {
                    playChainEquipSound(player)
                }
            } else {
                // 进度条填满，显示物品
                if (context.currentItemIndex < lootList.size) {
                    // 使用原始物品，保留完整的物品信息
                    val item = state.originalItems[context.currentItemIndex].clone()

                    context.items[context.currentItemIndex] = item
                    state.items[context.currentItemIndex] = item

                    val slot = BACKPACK_SLOTS[context.currentItemIndex]
                    inventory.setItem(slot, item)
                    
                    // 播放物品发现音效
                    player.playSound(player.location, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 1.0f)

                    context.currentItemIndex++
                    state.currentItemIndex = context.currentItemIndex

                    if (context.currentItemIndex < lootList.size) {
                        // 重置进度条，开始下一个物品的搜索
                        SEARCH_BAR_SLOTS.forEach { inventory.setItem(it, createProgressItem(false)) }
                        context.progressSlotIndex = 0
                        state.progressSlotIndex = 0

                        val nextSearchTime = lootList[context.currentItemIndex].second
                        val nextTicksPerSlot = (nextSearchTime * 20) / SEARCH_BAR_SLOTS.size

                        context.searchTask?.cancel()
                        createSearchTask(player, context, state)
                        return@Runnable
                    } else {
                        // 所有物品搜索完成
                        context.searchComplete = true
                        state.searchComplete = true
                        SEARCH_BAR_SLOTS.forEach { inventory.setItem(it, createProgressItem(true)) }
                    }
                }
            }
        }, ticksPerSlot.toLong(), ticksPerSlot.toLong())
    }

    /**
     * 取消搜索
     */
    private fun cancelSearch(uuid: UUID) {
        val context = playerSearches.remove(uuid) ?: return
        context.searchTask?.cancel()
        context.searchTask = null
    }

    /**
     * 完成搜索
     */
    private fun finishSearch(uuid: UUID) {
        val context = playerSearches[uuid] ?: return
        context.searchTask?.cancel()
        context.searchTask = null
        // 不移除上下文，保留搜索状态
    }

    /**
     * 处理库存关闭事件
     */
    @EventHandler
    fun onInventoryClose(event: org.bukkit.event.inventory.InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        val uuid = player.uniqueId

        // 检查是否是搜索UI
        val context = playerSearches[uuid] ?: return
        if (event.inventory != context.inventory) return

        // 获取搜索状态
        val deathState = getSearchState(context.searchId) ?: return

        // 保存所有物品槽位的物品
        for (i in BACKPACK_SLOTS.indices) {
            val slot = BACKPACK_SLOTS[i]
            context.items[i] = event.inventory.getItem(slot)?.clone()
        }

        // 同步到全局状态
        for (i in BACKPACK_SLOTS.indices) {
            deathState.items[i] = context.items[i]?.clone()
        }

        // 停止搜索任务，但不移除上下文
        finishSearch(uuid)
        
        // 播放玩家尸体关闭音效
        playPlayerCorpseCloseSound(player)

        // 释放搜索锁定
        releaseSearchLock(context.searchId, uuid)
    }

    /**
     * 处理玩家退出事件
     */
    @EventHandler
    fun onPlayerQuit(event: org.bukkit.event.player.PlayerQuitEvent) {
        cancelSearch(event.player.uniqueId)
    }

    /**
     * 处理库存点击事件
     */
    @EventHandler
    fun onInventoryClick(event: org.bukkit.event.inventory.InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val inventory = event.clickedInventory ?: return
        
        // 检查是否是搜索UI
        val context = playerSearches[player.uniqueId] ?: return
        if (inventory != context.inventory) return

        // 获取搜索状态
        val deathState = getSearchState(context.searchId) ?: return

        val slot = event.slot

        // 玩家头颅槽位，禁止操作
        if (slot == HEAD_SLOT) {
            event.isCancelled = true
            return
        }
        // 信用点头颅槽位，处理现金转移
        if (slot == CASH_SLOT) {
            event.isCancelled = true
            val cashAmount = deathListener?.getDeathPlayerCash(deathState.playerUuid) ?: 0
            if (cashAmount > 0) {
                // 将现金转移到点击玩家的背包现金中
                backpackManager.addPlayerCash(player, cashAmount)
                // 设置死亡玩家的背包现金为0
                deathListener?.setDeathPlayerCash(deathState.playerUuid, 0)
                // 更新UI中的信用点头颅为空钱包
                inventory.setItem(CASH_SLOT, PlayerCashHead.createEmptyWalletHead(configManager))
                // 发送提示消息
                player.sendLangSys(plugin, "death.cash-collected", "amount" to cashAmount)
            }
            return
        }

        // 检查点击的是否是物品槽位
        val isItemSlot = BACKPACK_SLOTS.contains(slot)

        if (isItemSlot) {
            // 检查点击的物品是否是占位符
            val currentItem = inventory.getItem(slot)
            val isPlaceholder = currentItem != null &&
                currentItem.type == Material.BLACK_STAINED_GLASS_PANE &&
                currentItem.itemMeta?.hasDisplayName() == true &&
                currentItem.itemMeta?.displayName() == LanguageAPI.getComponent(plugin, "search.ui.placeholder")

            if (isPlaceholder) {
                // 占位符物品，禁止操作
                event.isCancelled = true
                return
            }

            // 非占位符物品，允许操作
            val cursor = event.cursor
            if (currentItem != null && cursor.type != Material.AIR) {
                // 交换物品
                event.isCancelled = false

                // 延迟更新上下文，确保物品交换完成
                plugin.server.scheduler.runTaskLater(plugin, Runnable {
                    val newItem = inventory.getItem(slot)
                    val itemIndex = BACKPACK_SLOTS.indexOf(slot)
                    if (itemIndex >= 0) {
                        context.items[itemIndex] = newItem?.clone()
                        deathState.items[itemIndex] = newItem?.clone()
                    }
                }, 1L)
            } else if (currentItem != null && cursor.type == Material.AIR) {
                // 拿出物品
                event.isCancelled = false

                // 延迟更新上下文，确保物品移除完成
                plugin.server.scheduler.runTaskLater(plugin, Runnable {
                    val itemIndex = BACKPACK_SLOTS.indexOf(slot)
                    if (itemIndex >= 0) {
                        context.items[itemIndex] = null
                        deathState.items[itemIndex] = null
                    }
                }, 1L)
            } else if (currentItem == null && cursor.type != Material.AIR) {
                // 放入物品
                event.isCancelled = false

                // 延迟更新上下文，确保物品放置完成
                plugin.server.scheduler.runTaskLater(plugin, Runnable {
                    val newItem = inventory.getItem(slot)
                    val itemIndex = BACKPACK_SLOTS.indexOf(slot)
                    if (itemIndex >= 0) {
                        context.items[itemIndex] = newItem?.clone()
                        deathState.items[itemIndex] = newItem?.clone()
                    }
                }, 1L)
            }
        } else {
            // 玻璃板槽位，禁止操作
            event.isCancelled = true
        }
    }

    /**
     * 打开玩家死亡搜索UI
     * @param player 玩家
     * @param deathPlayerUuid 死亡玩家UUID
     * @param deathListener 死亡监听器
     */
    fun openSearchUI(player: Player, deathPlayerUuid: UUID, deathListener: PlayerDeathListener) {
        // 获取玩家所在的实例
        val ctx = sky4th.dungeon.command.DungeonContext.get() ?: return
        val instanceFullId = ctx.playerManager.getCurrentInstanceFullId(player) ?: return
        
        // 检查尸体是否已有搜索状态
        val fullKey = makeFullKey(instanceFullId, deathPlayerUuid)
        val deathState = deathPlayerStates[fullKey]

        if (deathState != null) {
            // 死亡玩家已有状态，直接打开UI显示当前状态
            openSearchUI(player, deathState, instanceFullId)
            return
        }

        // 死亡玩家没有状态，开始新的搜索
        val deathItems = deathListener.getDeathPlayerItems(deathPlayerUuid) ?: emptyList()

        // 将物品和品级转换为战利品配置和搜索时间
        val lootList = mutableListOf<Pair<sky4th.dungeon.config.LootItemConfig, Int>>()
        val originalItems = mutableListOf<ItemStack>() // 保存原始物品
        val searchSecondsMap = configManager.lootTierSearchSeconds
        for ((item, tier) in deathItems) {
            // 保存原始物品
            originalItems.add(item.clone())

            // 创建战利品配置（仅用于确定搜索时间）
            @Suppress("DEPRECATION")
            val displayName = item.itemMeta?.displayName
            val itemName = if (displayName.isNullOrBlank()) item.type.name else displayName
            @Suppress("DEPRECATION")
            val lootConfig = sky4th.dungeon.config.LootItemConfig(
                id = "death_${deathPlayerUuid}_${item.hashCode()}",
                name = itemName,
                description = item.itemMeta?.lore?.joinToString("\n") ?: "",
                descriptionLore = item.itemMeta?.lore ?: emptyList(),
                tier = tier,
                price = 0,
                material = item.type.name,
                isshop = false,
                minAmount = item.amount,
                maxAmount = item.amount
            )
            val searchSeconds = searchSecondsMap[tier] ?: 5
            lootList.add(lootConfig to searchSeconds)
        }

        // 创建死亡玩家搜索状态
        val newState = PlayerDeathState(
            playerUuid = deathPlayerUuid,
            lootList = lootList,
            originalItems = originalItems, // 保存原始物品
            items = MutableList(BACKPACK_SLOTS.size) { null },
            currentItemIndex = 0,
            progressSlotIndex = 0,
            searchComplete = lootList.isEmpty(), 
            uiInitialized = false
        )
        deathPlayerStates[fullKey] = newState
        // 添加到实例索引
        instancePlayerKeys.getOrPut(instanceFullId) { mutableSetOf() }.add(fullKey)
        openSearchUI(player, newState, instanceFullId)
    }

    /**
     * 尝试获取搜索锁定
     * @param searchId 搜索对象的ID
     * @param playerUUID 玩家的UUID
     * @return 如果成功获取锁定返回true，否则返回false
     */
    private fun tryAcquireSearchLock(searchId: String, playerUUID: UUID): Boolean {
        val currentOwner = baseSearchManager.searchLocks[searchId]
        if (currentOwner == null) {
            // 没有锁定，获取锁定
            baseSearchManager.searchLocks[searchId] = playerUUID
            return true
        }
        // 已有锁定，检查是否是当前玩家
        return currentOwner == playerUUID
    }

    /**
     * 释放搜索锁定
     * @param searchId 搜索对象的ID
     * @param playerUUID 玩家的UUID
     */
    private fun releaseSearchLock(searchId: String, playerUUID: UUID) {
        val currentOwner = baseSearchManager.searchLocks[searchId]
        if (currentOwner == playerUUID) {
            baseSearchManager.searchLocks.remove(searchId)
        }
    }

    /**
     * 清空所有死亡玩家搜索数据
     * 用于地牢重置时清理内存中的搜索状态
     */
    fun clearAllDeathSearchData() {
        // 取消所有正在进行的搜索任务
        playerSearches.values.forEach { context ->
            context.searchTask?.cancel()
            context.searchTask = null
        }

        // 清空所有搜索状态
        deathPlayerStates.clear()
        instancePlayerKeys.clear()
        playerSearches.clear()
    }

    /**
     * 清理指定实例的所有死亡玩家数据
     * @param instanceFullId 实例完整ID
     */
    fun clearForInstance(instanceFullId: String) {
        val playerKeys = instancePlayerKeys.remove(instanceFullId) ?: return
        for (playerKey in playerKeys) {
            deathPlayerStates.remove(playerKey)
        }
    }
}
