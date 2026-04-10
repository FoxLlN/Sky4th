package sky4th.dungeon.search

import sky4th.core.api.LanguageAPI
import sky4th.dungeon.Dungeon
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import org.bukkit.persistence.PersistentDataType
import sky4th.dungeon.config.LootItemConfig
import java.util.UUID

/**
 * 搜索管理器基类
 * 
 * 提供通用的搜索UI和进度管理功能，供容器搜索和怪物搜索共同使用
 */
abstract class BaseSearchManager(
    protected val plugin: JavaPlugin,
    protected val searchLootService: SearchLootService
) : Listener {

    companion object {
        const val INVENTORY_SIZE = 27 // 3x9
        const val PROGRESS_SLOTS_COUNT = 9 // 进度条槽位数量

        // 物品槽位（6个槽位：第一行3个+第二行3个）
        @JvmStatic
        val ITEM_SLOTS = listOf(3, 4, 5, 12, 13, 14)

        // 进度条槽位（第三行）
        @JvmStatic
        val PROGRESS_SLOTS = listOf(18, 19, 20, 21, 22, 23, 24, 25, 26)

        // 玻璃板槽位（除了物品槽位和进度条槽位外的所有槽位）
        @JvmStatic
        val GLASS_PANE_SLOTS = (0 until INVENTORY_SIZE).filter {
            !PROGRESS_SLOTS.contains(it) && !ITEM_SLOTS.contains(it)
        }
    }

    /**
     * 搜索状态接口，用于统一处理不同类型的搜索状态
     */
    interface SearchState {
        val lootList: List<Pair<LootItemConfig, Int>>
        val items: MutableList<ItemStack?>
        var currentItemIndex: Int
        var progressSlotIndex: Int
        var searchComplete: Boolean
        var uiInitialized: Boolean
    }

    /**
     * 搜索上下文数据
     */
    protected data class SearchContext(
        val searchId: String, // 搜索对象的ID（容器ID或头颅ID）
        val lootList: List<Pair<LootItemConfig, Int>>, // 物品配置和搜索时间
        val items: MutableList<ItemStack?> = MutableList(ITEM_SLOTS.size) { null }, // 所有物品槽位的物品（null表示空）
        var currentItemIndex: Int = 0, // 当前正在搜索的物品索引
        var progressSlotIndex: Int = 0, // 当前进度条槽位索引
        var searchComplete: Boolean = false, // 搜索是否完成
        var searchTask: BukkitTask? = null, // 搜索任务
        var inventory: org.bukkit.inventory.Inventory? = null // 搜索UI库存
    )

    protected val playerSearches: MutableMap<UUID, SearchContext> = mutableMapOf()
    protected val loadoutSetKey: NamespacedKey by lazy { NamespacedKey(plugin, "loadout_set") }

    // 全局搜索锁定：记录每个搜索对象当前被哪个玩家搜索
    val searchLocks: MutableMap<String, UUID> = mutableMapOf()

    /**
     * 获取搜索状态，由子类实现
     */
    protected abstract fun getSearchState(searchId: String): SearchState?

    /**
     * 创建搜索UI的标题，由子类实现
     */
    protected abstract fun createInventoryTitle(searchId: String): String

    /**
     * 获取头颅/容器物品，由子类实现
     */
    protected abstract fun getHeadItem(searchId: String): ItemStack?

    /**
     * 创建搜索任务，由子类实现
     */
    protected abstract fun createSearchTask(player: Player, context: SearchContext, state: SearchState)

    /**
     * 打开搜索UI
     */
    protected fun openSearchUI(player: Player, state: SearchState) {
        val searchId = getSearchId(state)
        val itemCount = state.lootList.size

        // 创建库存标题
        val title = createInventoryTitle(searchId)

        // 创建库存界面
        val inventory = Bukkit.createInventory(null, INVENTORY_SIZE, net.kyori.adventure.text.Component.text(title))

        // 清空所有槽位
        for (i in 0 until INVENTORY_SIZE) {
            inventory.setItem(i, null)
        }

        // 在左上角第一个格子（槽位0）显示头颅/容器
        val headItem = getHeadItem(searchId)
        if (headItem != null) {
            inventory.setItem(0, headItem)
        }

        // 设置玻璃板（排除槽位0）
        val glassPane = createGlassPane()
        GLASS_PANE_SLOTS.filter { it != 0 }.forEach { inventory.setItem(it, glassPane) }

        // 检查是否是第一次打开UI
        if (!state.uiInitialized) {
            // 第一次打开，显示占位符
            val placeholderItem = createPlaceholderItem()
            for (i in 0 until itemCount) {
                val slot = ITEM_SLOTS[i]
                inventory.setItem(slot, placeholderItem)
            }
            // 标记UI已初始化
            state.uiInitialized = true
        } else {
            // 不是第一次打开，恢复所有物品
            for (i in ITEM_SLOTS.indices) {
                val slot = ITEM_SLOTS[i]
                if (state.items[i] != null) {
                    inventory.setItem(slot, state.items[i])
                }
            }
        }

        // 恢复进度条
        updateProgress(inventory, state)

        // 创建临时的搜索上下文用于UI显示
        val context = SearchContext(
            searchId = searchId,
            lootList = state.lootList,
            items = MutableList(state.items.size) { state.items[it]?.clone() },
            currentItemIndex = state.currentItemIndex,
            progressSlotIndex = state.progressSlotIndex,
            searchComplete = state.searchComplete,
            inventory = inventory
        )

        playerSearches[player.uniqueId] = context
        player.openInventory(inventory)

        // 如果搜索未完成，启动搜索任务
        if (!state.searchComplete) {
            createSearchTask(player, context, state)
        }
    }

    /**
     * 创建玻璃板
     */
    protected fun createGlassPane(): ItemStack {
        val item = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        val meta = item.itemMeta
        meta?.displayName(LanguageAPI.getComponent(plugin, "search.ui.glass-pane"))
        item.itemMeta = meta
        return item
    }

    /**
     * 创建占位符物品
     */
    protected fun createPlaceholderItem(): ItemStack {
        val item = ItemStack(Material.BLACK_STAINED_GLASS_PANE)
        val meta = item.itemMeta
        meta?.displayName(LanguageAPI.getComponent(plugin, "search.ui.placeholder"))
        item.itemMeta = meta
        return item
    }

    /**
     * 创建进度物品
     * @param completed 是否已完成（绿色）
     */
    protected fun createProgressItem(completed: Boolean): ItemStack {
        val material = if (completed) Material.GREEN_STAINED_GLASS_PANE else Material.WHITE_STAINED_GLASS_PANE
        val item = ItemStack(material)
        val meta = item.itemMeta
        meta?.displayName(LanguageAPI.getComponent(plugin, if (completed) "search.ui.progress.completed" else "search.ui.progress.in-progress"))
        item.itemMeta = meta
        return item
    }

    /**
     * 创建进度物品（进行中）
     */
    protected fun createProgressItemInProgress(): ItemStack {
        val item = ItemStack(Material.LIME_STAINED_GLASS_PANE)
        val meta = item.itemMeta
        meta?.displayName(LanguageAPI.getComponent(plugin, "search.ui.progress.active"))
        item.itemMeta = meta
        return item
    }

    /**
     * 播放容器打开音效
     * @param player 玩家
     * @param level 容器等级
     */
    protected fun playContainerOpenSound(player: Player, level: Int) {
        when (level) {
            1 -> {
                // 1级容器 → block.barrel.open
                player.playSound(player.location, org.bukkit.Sound.BLOCK_BARREL_OPEN, 1.0f, 1.0f)
            }
            2 -> {
                // 2级容器 → block.chest.open
                player.playSound(player.location, org.bukkit.Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f)
            }
            3 -> {
                // 3级容器 → block.ender_chest.open
                player.playSound(player.location, org.bukkit.Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 1.0f)
            }
        }
    }

    /**
     * 播放容器关闭音效
     * @param player 玩家
     * @param level 容器等级
     */
    protected fun playContainerCloseSound(player: Player, level: Int) {
        when (level) {
            1 -> {
                // 1级容器 → block.barrel.close
                player.playSound(player.location, org.bukkit.Sound.BLOCK_BARREL_CLOSE, 1.0f, 1.0f)
            }
            2 -> {
                // 2级容器 → block.chest.close
                player.playSound(player.location, org.bukkit.Sound.BLOCK_CHEST_CLOSE, 1.0f, 1.0f)
            }
            3 -> {
                // 3级容器 → block.ender_chest.close
                player.playSound(player.location, org.bukkit.Sound.BLOCK_ENDER_CHEST_CLOSE, 1.0f, 1.0f)
            }
        }
    }

    /**
     * 播放搜索过程中的翻书音效
     * @param player 玩家
     */
    protected fun playPageTurnSound(player: Player) {
        player.playSound(player.location, org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f)
    }

    /**
     * 播放物品发现音效
     * @param player 玩家
     */
    protected fun playItemFoundSound(player: Player) {
        player.playSound(player.location, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 1.0f)
    }

    /**
     * 播放怪物尸体打开音效
     * @param player 玩家
     */
    protected fun playMonsterBodyOpenSound(player: Player) {
        player.playSound(player.location, org.bukkit.Sound.ENTITY_SLIME_SQUISH, 1.0f, 1.0f)
    }

    /**
     * 播放怪物尸体关闭音效
     * @param player 玩家
     */
    protected fun playMonsterBodyCloseSound(player: Player) {
        player.playSound(player.location, org.bukkit.Sound.ENTITY_SLIME_SQUISH, 1.0f, 1.0f)
    }

    /**
     * 更新进度条
     */
    protected fun updateProgress(inventory: org.bukkit.inventory.Inventory, state: SearchState) {
        if (state.searchComplete) {
            // 搜索完成，全部显示绿色
            PROGRESS_SLOTS.forEach { inventory.setItem(it, createProgressItem(true)) }
        } else {
            // 更新进度条
            for (i in 0 until PROGRESS_SLOTS.size) {
                if (i < state.progressSlotIndex) {
                    inventory.setItem(PROGRESS_SLOTS[i], createProgressItemInProgress())
                } else {
                    inventory.setItem(PROGRESS_SLOTS[i], createProgressItem(false))
                }
            }
        }
    }

    /**
     * 取消搜索
     */
    protected fun cancelSearch(uuid: UUID) {
        val context = playerSearches.remove(uuid) ?: return
        context.searchTask?.cancel()
        context.searchTask = null
    }

    /**
     * 完成搜索
     */
    protected fun finishSearch(uuid: UUID) {
        val context = playerSearches[uuid] ?: return
        context.searchTask?.cancel()
        context.searchTask = null
        // 不移除上下文，保留搜索状态
    }

    /**
     * 获取搜索ID，由子类实现
     */
    protected abstract fun getSearchId(state: SearchState): String

    /**
     * 处理库存点击事件
     */
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val inventory = event.clickedInventory ?: return

        // 检查是否是搜索UI
        val context = playerSearches[player.uniqueId] ?: return
        if (inventory != context.inventory) return

        // 获取搜索状态
        val state = getSearchState(context.searchId) ?: return

        val slot = event.slot
        val lootList = context.lootList
        val itemCount = lootList.size

        // 检查点击的是否是物品槽位
        val isItemSlot = ITEM_SLOTS.contains(slot)

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
                    val itemIndex = ITEM_SLOTS.indexOf(slot)
                    if (itemIndex >= 0) {
                        context.items[itemIndex] = newItem?.clone()
                        state.items[itemIndex] = newItem?.clone()
                    }
                }, 1L)
            } else if (currentItem != null && cursor.type == Material.AIR) {
                // 拿出物品
                event.isCancelled = false

                // 延迟更新上下文，确保物品移除完成
                plugin.server.scheduler.runTaskLater(plugin, Runnable {
                    val itemIndex = ITEM_SLOTS.indexOf(slot)
                    if (itemIndex >= 0) {
                        context.items[itemIndex] = null
                        state.items[itemIndex] = null
                    }
                }, 1L)
            } else if (currentItem == null && cursor.type != Material.AIR) {
                // 放入物品
                event.isCancelled = false

                // 延迟更新上下文，确保物品放置完成
                plugin.server.scheduler.runTaskLater(plugin, Runnable {
                    val newItem = inventory.getItem(slot)
                    val itemIndex = ITEM_SLOTS.indexOf(slot)
                    if (itemIndex >= 0) {
                        context.items[itemIndex] = newItem?.clone()
                        state.items[itemIndex] = newItem?.clone()
                    }
                }, 1L)
            }
        } else {
            // 玻璃板槽位，禁止操作
            event.isCancelled = true
        }
    }

    /**
     * 处理库存关闭事件
     */
    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        val uuid = player.uniqueId

        // 检查是否是搜索UI
        val context = playerSearches[uuid] ?: return
        if (event.inventory != context.inventory) return

        // 获取搜索状态并更新
        val state = getSearchState(context.searchId)
        if (state != null) {
            val inventory = context.inventory
            if (inventory != null) {
                // 直接从inventory读取所有物品槽位的物品
                for (i in ITEM_SLOTS.indices) {
                    val slot = ITEM_SLOTS[i]
                    state.items[i] = inventory.getItem(slot)?.clone()
                }
            }
        }

        // 停止搜索任务并移除上下文
        finishSearch(uuid)
        playerSearches.remove(uuid)

        // 释放搜索锁定
        releaseSearchLock(context.searchId, uuid)
    }

    /**
     * 处理玩家退出事件
     */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        cancelSearch(event.player.uniqueId)
    }

    /**
     * 检查玩家是否有学者2件套
     */
    protected fun hasXuezheTwoPiece(player: Player): Boolean {
        var count = 0
        for (item in player.inventory.armorContents) {
            if (item == null || item.type.isEmpty) continue
            if (item.itemMeta?.persistentDataContainer?.get(loadoutSetKey, PersistentDataType.STRING) == "scholar") count++
        }
        return count >= 2
    }

    /**
     * 尝试获取搜索锁定
     * @param searchId 搜索对象的ID
     * @param playerUUID 玩家的UUID
     * @return 如果成功获取锁定返回true，否则返回false
     */
    protected fun tryAcquireSearchLock(searchId: String, playerUUID: UUID): Boolean {
        val currentOwner = searchLocks[searchId]
        if (currentOwner == null) {
            // 没有锁定，获取锁定
            searchLocks[searchId] = playerUUID
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
    protected fun releaseSearchLock(searchId: String, playerUUID: UUID) {
        val currentOwner = searchLocks[searchId]
        if (currentOwner == playerUUID) {
            searchLocks.remove(searchId)
        }
    }

    /**
     * 清空所有搜索数据
     * 用于地牢重置时清理内存中的搜索状态
     */
    fun clearAllSearchData() {
        playerSearches.clear()
        searchLocks.clear()
    }
}
