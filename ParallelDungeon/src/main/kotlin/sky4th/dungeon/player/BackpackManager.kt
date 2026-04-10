package sky4th.dungeon.player

import sky4th.dungeon.Dungeon
import sky4th.dungeon.config.ConfigManager
import sky4th.dungeon.head.SafeSlotHead
import sky4th.dungeon.head.BackpackValueHead
import sky4th.dungeon.loadout.equipment.LoadoutSetSkillState
import sky4th.core.api.LanguageAPI
import net.kyori.adventure.text.Component
import org.bukkit.NamespacedKey
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import java.util.*


/**
 * 副本内玩家背包：总价值按玩家自身背包中带地牢标记的物品实时计算。
 */
class BackpackManager(
    private val plugin: JavaPlugin,
    private val configManager: ConfigManager
) {
    private val lootIdKey: NamespacedKey by lazy { NamespacedKey(plugin, "dungeon_loot_id") }
    private val loadoutPriceKey: NamespacedKey by lazy { NamespacedKey(plugin, "loadout_price") }
    private val loadoutSetKey: NamespacedKey by lazy { NamespacedKey(plugin, "loadout_set") }
    private val blockedSlotKey: NamespacedKey by lazy { NamespacedKey(plugin, "dungeon_blocked_slot") }
    private val cashIdKey: NamespacedKey by lazy { NamespacedKey(plugin, "dungeon_cash_id") }

    // 玩家背包中的现金总额
    private val playerCashAmounts: MutableMap<UUID, Int> = mutableMapOf()

    /** 背包可用槽位（9*3 中央 5 列，共 15 格；不影响物品栏 0-8 行） */
    private val allowedStorageSlots: Set<Int> = setOf(
        11, 12, 13, 14, 15,
        20, 21, 22, 23, 24,
        29, 30, 31, 32, 33
    )

    /** 地牢内被锁定的背包槽位（9*3 边框 12 格） */
    private val blockedStorageSlots: Set<Int> = (9..35).filter { it !in allowedStorageSlots }.toSet()

    // 智能更新：上一次的背包价值（用于检测变化）
    private val lastBackpackValue: MutableMap<UUID, Int> = mutableMapOf()

    // ========== 背包价值计算优化 ==========
    // 物品价值缓存（每个不同物品ID一个整数）
    private val lootPriceCache: MutableMap<String, Int> = mutableMapOf()
    // 配装物品价值缓存
    private val loadoutPriceCache: MutableMap<UUID, Int> = mutableMapOf()

    // 计分板管理器（使用DungeonContext中的共享实例）
    private val sidebarManager: SidebarManager
        get() = sky4th.dungeon.command.DungeonContext.get()?.sidebarManager
            ?: throw IllegalStateException("DungeonContext未初始化或sidebarManager为空")

    /**
     * 初始化物品价格缓存
     */
    fun initPriceCache() {
        lootPriceCache.clear()
        val dungeonConfigs = configManager.loadDungeonConfigs()
        dungeonConfigs.forEach { (dungeonId, _) ->
            val lootItems = configManager.getLootItems(dungeonId)
            lootItems.forEach { loot ->
                lootPriceCache[loot.id] = loot.price
            }
        }
        plugin.logger.info("物品价格缓存已加载，共 ${lootPriceCache.size} 种物品")
    }

    /**
     * 清理玩家缓存（玩家退出时调用）
     */
    fun clearPlayerCache(player: Player) {
        val uuid = player.uniqueId
        lastBackpackValue.remove(uuid)
        loadoutPriceCache.remove(uuid)
    }

    /**
     * 获取玩家背包中的现金总额
     */
    fun getPlayerCash(player: Player): Int {
        return playerCashAmounts[player.uniqueId] ?: 0
    }

    /**
     * 增加玩家背包中的现金
     */
    fun addPlayerCash(player: Player, amount: Int) {
        val current = playerCashAmounts[player.uniqueId] ?: 0
        playerCashAmounts[player.uniqueId] = current + amount
        refreshSidebarCash(player)
    }

    /**
     * 根据物品ID从缓存获取价格
     */
    private fun getLootItemPrice(lootId: String): Int? {
        return lootPriceCache[lootId]
    }

    /**
     * 计算单个物品的价值
     */
    private fun calculateItemValue(item: ItemStack?): Int {
        if (item == null || item.type.isAir) return 0
        val meta = item.itemMeta ?: return 0
        val pdc = meta.persistentDataContainer

        // 检查是否是地牢物品
        val lootId = pdc.get(lootIdKey, PersistentDataType.STRING)
        if (lootId != null) {
            val price = getLootItemPrice(lootId)
            if (price != null) {
                return price * item.amount
            }
            // 缓存未命中，尝试从配置加载
            val dungeonConfigs = configManager.loadDungeonConfigs()
            for ((dungeonId, dungeonConfig) in dungeonConfigs) {
                val loot = configManager.getLootItemById(dungeonId, lootId)
                if (loot != null) {
                    lootPriceCache[lootId] = loot.price
                    return loot.price * item.amount
                }
            }
            return 0
        }

        // 检查是否是配装物品
        val loadoutPrice = pdc.get(loadoutPriceKey, PersistentDataType.INTEGER)
        if (loadoutPrice != null) {
            return loadoutPrice * item.amount
        }

        return 0
    }

    /**
     * 计算背包总价值（内部方法，不使用缓存）
     */
    private fun calculateTotalValue(player: Player): Int {
        var total = 0
        for (item in player.inventory.storageContents) {
            total += calculateItemValue(item)
        }
        for (item in player.inventory.armorContents) {
            total += calculateItemValue(item)
        }
        total += calculateItemValue(player.inventory.itemInOffHand)
        return total
    }

    /**
     * 根据玩家背包+装备栏内带地牢标记的物品计算总价值（price * amount），每件物品只统计一次。
     * 使用 storageContents（36 格）避免与 armorContents 重复，因 contents 会包含护甲槽。
     * 优化版本：使用价格缓存机制，避免重复查询配置
     */
    fun getTotalValue(player: Player): Int {
        // 直接计算总价值（使用价格缓存）
        return calculateTotalValue(player)
    }

    fun countSetArmor(player: Player, setId: String): Int {
        fun isSetItem(item: ItemStack?): Boolean {
            if (item == null || item.type.isAir) return false
            return item.itemMeta?.persistentDataContainer?.get(loadoutSetKey, PersistentDataType.STRING) == setId
        }
        return player.inventory.armorContents.count { isSetItem(it) }
    }

    /** 判断某个玩家背包槽位在地牢中是否被锁定（仅 9–35 范围内的 9*3 背包区域） */
    fun isBlockedStorageSlot(slot: Int): Boolean = slot in blockedStorageSlots

    /** 判断物品是否为用于占位的"锁定格子"玻璃板 */
    fun isBlockedPlaceholder(item: ItemStack?): Boolean {
        if (item == null || item.type.isAir) return false
        val meta = item.itemMeta ?: return false
        return meta.persistentDataContainer.has(blockedSlotKey, PersistentDataType.BYTE)
    }

    /** 创建一个标记为"锁定格子"的灰色玻璃板，占用背包无效位置 */
    private fun createBlockedPlaceholderItem(): ItemStack {
        return ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
            editMeta { meta ->
                meta.displayName(LanguageAPI.getComponent(plugin, "backpack.placeholder.name"))
                meta.lore(
                    LanguageAPI.getComponentList(plugin, "backpack.placeholder.lore")
                )
                meta.persistentDataContainer.set(blockedSlotKey, PersistentDataType.BYTE, 1)
            }
        }
    }

    /**
     * 进入地牢时：将 9*3 背包外圈 12 个格子用占位物品填满，只保留中间 3*5（共 15 格）可用。
     * 第10个格子放置安全箱头颅，提示玩家最上面一行是安全箱。
     * 第16个格子放置价值头颅，提示玩家背包总价值
     * 若玩家原本在这些槽位中有物品，会尝试移动到可用槽位 / 物品栏，放不下的会掉落在脚下。
     */
    fun applyLimitedBackpackLayout(player: Player) {
        val inv = player.inventory
        val toReAdd = mutableListOf<ItemStack>()

        // 先收集需搬运的物品并清空这些槽位
        for (slot in blockedStorageSlots) {
            val current = inv.getItem(slot)
            if (current != null && !current.type.isAir && !isBlockedPlaceholder(current)) {
                toReAdd.add(current.clone())
            }
            inv.setItem(slot, null)
        }

        // 填充占位物品，确保后续 addItem 不会用到这些格子
        val filler = createBlockedPlaceholderItem()
        for (slot in blockedStorageSlots) {
            if (slot == 10) {
                // 第10个格子放置安全箱头颅
                inv.setItem(slot, SafeSlotHead.create(configManager))
            } else if (slot == 16) {
                // 第16个格子放置价值头颅
                inv.setItem(slot, BackpackValueHead.create(configManager, calculateTotalValue(player)))
            } else {
                inv.setItem(slot, filler.clone())
            }
        }

        // 将原本在锁定格子的物品重新尝试放入可用格子 / 物品栏
        for (item in toReAdd) {
            val leftovers = inv.addItem(item)
            if (leftovers.isNotEmpty()) {
                leftovers.values.forEach { leftover ->
                    player.world.dropItemNaturally(player.location, leftover)
                }
            }
        }
    }

    /**
     * 离开地牢时：移除 9*3 背包外圈 12 个格子中的占位物品，恢复为正常空格。
     * 仅删除带有 blockedSlotKey 标记的占位物，避免误删玩家真实物品。
     */
    fun clearLimitedBackpackLayout(player: Player) {
        val inv = player.inventory
        for (slot in blockedStorageSlots) {
            val current = inv.getItem(slot)
            if (isBlockedPlaceholder(current)) {
                inv.setItem(slot, null)
            }
        }
    }

    /**
     * 清空玩家背包（包括主背包、副手、护甲槽）
     * @param player 玩家
     */
    fun clearPlayerInventory(player: Player) {
        val inv = player.inventory
        // 清空主背包(0-35槽)
        for (slot in 0..35) {
            inv.setItem(slot, null)
        }
        // 清空副手
        inv.setItemInOffHand(null)
        // 清空护甲槽
        inv.helmet = null
        inv.chestplate = null
        inv.leggings = null
        inv.boots = null
    }

    /**
     * 玩家进入副本时调用：清空背包
     */
    fun onEnterDungeon(player: Player) {
        clearPlayerInventory(player)
        applyLimitedBackpackLayout(player)
        playerCashAmounts[player.uniqueId] = 0
    }

    /**
     * 玩家离开副本时调用：隐藏计分板
     */
    fun onLeaveDungeon(player: Player) {
        clearLimitedBackpackLayout(player)
        playerCashAmounts.remove(player.uniqueId)
        sidebarManager.hideSidebar(player)
        // 清理背包价值缓存
        lastBackpackValue.remove(player.uniqueId)
        // 清理玩家优化缓存
        clearPlayerCache(player)
    }

    /**
     * 刷新计分板（更新所有部分）
     */
    fun refreshSidebar(player: Player) {
        sidebarManager.updateAllSections(player)
    }
    
    /**
     * 刷新计分板背包现金部分
     */
    fun refreshSidebarCash(player: Player) {
        sidebarManager.updateSection(player, SidebarManager.Section.CASH)
    }
    
    /**
     * 刷新计分板技能部分
     */
    fun refreshSidebarSkill(player: Player) {
        sidebarManager.updateSection(player, SidebarManager.Section.SKILL)
    }

    /**
     * 更新背包价值头颅的描述文字
     */
    fun updateBackpackValueHead(player: Player) {
        val uuid = player.uniqueId
        // 直接计算总价值（不使用哈希缓存）
        val currentValue = calculateTotalValue(player)

        // 检查价值是否变化
        val lastValue = lastBackpackValue[uuid]
        if (lastValue != null && lastValue == currentValue) {
            // 价值没有变化，不更新
            return
        }

        // 更新缓存
        lastBackpackValue[uuid] = currentValue

        val inv = player.inventory
        val item = inv.getItem(16) ?: return

        // 检查是否是背包价值头颅
        if (item.type != Material.PLAYER_HEAD) return

        val meta = item.itemMeta as? org.bukkit.inventory.meta.SkullMeta ?: return

        // 只更新 Lore，不重新创建头颅
        val lore = LanguageAPI.getComponentList(plugin, "backpack.value-solt.lore", "value" to currentValue)
        meta.lore(lore)
        item.itemMeta = meta
    }

    /**
     * 清空所有玩家的背包现金
     * 用于地牢重置时清理内存中的现金数据
     */
    fun clearAllCash() {
        playerCashAmounts.clear()
    }

    /**
     * 清空所有缓存和任务
     * 用于服务器卸载时清理内存中的所有数据
     */
    fun clearAll() {
        // 清理所有现金数据
        playerCashAmounts.clear()

        // 清理计分板
        sidebarManager.clearAll()

        // 清理所有缓存
        lastBackpackValue.clear()

        // 清理优化缓存
        loadoutPriceCache.clear()
        // 物品价格缓存不清除，因为配置可能不会频繁变化
    }
}
