package sky4th.dungeon.loadout.category

import sky4th.dungeon.loadout.screen.LoadoutScreenHolder
import sky4th.dungeon.loadout.screen.LoadoutUI
import sky4th.dungeon.loadout.pick.PickFromStorageUI
import sky4th.dungeon.loadout.storage.LootItemHelper
import sky4th.dungeon.loadout.shop.LoadoutShopAPI
import sky4th.dungeon.loadout.LoadoutCategory
import sky4th.core.api.EconomyAPI
import sky4th.core.api.StorageAPI
import sky4th.dungeon.util.LanguageUtil.sendLang
import sky4th.core.model.StorageEntry
import sky4th.core.model.StorageEntryType
import sky4th.dungeon.command.DungeonContext
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.meta.Damageable
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import sky4th.dungeon.config.ConfigManager

/**
 * 分类详情界面点击：首次点击选中某商品，再次点击根据仓库数量执行购买/直接选中/跳转仓库选择。
 */
class CategoryDetailListener : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder ?: return
        if (!CategoryDetailUI.isCategoryDetailGui(holder)) return

        event.isCancelled = true
        val who = event.whoClicked
        if (who !is Player) return

        val detailHolder = holder as CategoryDetailHolder
        val loadoutHolder = detailHolder.loadoutHolder
        val ctx = DungeonContext.get() ?: return
        val plugin = ctx.plugin
        val configManager = ctx.configManager
        val slot = event.rawSlot

        if (slot == CategoryDetailUI.SLOT_BACK) {
            who.closeInventory()
            reopenLoadout(who, loadoutHolder, plugin, configManager)
            return
        }

        // 左侧目录：点击切换分类，当前分类附魔高亮
        val clickedCategory = CategoryDetailUI.getCategorySlot(slot)
        if (clickedCategory != null) {
            if (clickedCategory != detailHolder.category) {
                detailHolder.category = clickedCategory
                detailHolder.categoryItems = when (clickedCategory) {
                    LoadoutCategory.SUPPLIES -> {
                        val ids = configManager.getEquippableSupplyIds().toSet()
                        val suppliesItems = configManager.getLoadoutShopItems(LoadoutCategory.SUPPLIES).filter { it.id in ids }
                        val repairItems = configManager.getLoadoutShopItems(LoadoutCategory.REPAIR)
                        suppliesItems + repairItems
                    }
                    else -> configManager.getLoadoutShopItems(clickedCategory)
                }
                detailHolder.selectedItemIndex = -1
                detailHolder.page = 0
                CategoryDetailUI.refreshAll(event.inventory, configManager, plugin)
            }
            return
        }

        // 翻页按钮
        if (CategoryDetailUI.isPrevPageSlot(slot) && detailHolder.page > 0) {
            detailHolder.page--
            detailHolder.selectedItemIndex = -1
            CategoryDetailUI.refreshAll(event.inventory, configManager, plugin)
            return
        }
        if (CategoryDetailUI.isNextPageSlot(slot) && (detailHolder.page + 1) * CategoryDetailUI.CONTENT_SLOTS < detailHolder.categoryItems.size) {
            detailHolder.page++
            detailHolder.selectedItemIndex = -1
            CategoryDetailUI.refreshAll(event.inventory, configManager, plugin)
            return
        }

        val contentIndex = CategoryDetailUI.getContentSlotIndex(slot) ?: return
        val pageItems = detailHolder.categoryItems.drop(detailHolder.page * CategoryDetailUI.CONTENT_SLOTS).take(CategoryDetailUI.CONTENT_SLOTS)
        if (contentIndex >= pageItems.size) return

        val config = pageItems[contentIndex]
        val count = LoadoutUI.getStorageTotalCountByLoadoutId(who.uniqueId, config.id)
        val suppliesLimit = configManager.suppliesCarryLimit
        val totalEquipped = loadoutHolder.selectedSupplies.sumOf { it.count }

        // 补给分类：左键装备一个 / 右键取消一个 / 无则双击购买
        if (detailHolder.category == LoadoutCategory.SUPPLIES) {
            if (event.isRightClick) {
                // 右键：取消装备一个（从已选补给中移除一个）
                val idx = loadoutHolder.selectedSupplies.indexOfLast { it.loadoutId == config.id }
                if (idx >= 0) {
                    val e = loadoutHolder.selectedSupplies[idx]
                    if (e.count <= 1) {
                        loadoutHolder.selectedSupplies.removeAt(idx)
                    } else {
                        loadoutHolder.selectedSupplies[idx] = e.copy(count = e.count - 1)
                    }
                    CategoryDetailUI.refreshContent(event.inventory, configManager, plugin)
                }
            } else {
                // 左键：首次点击选中，再次点击装备一个（有则从仓库取，无则购买后装备）
                if (detailHolder.selectedItemIndex != contentIndex) {
                    detailHolder.selectedItemIndex = contentIndex
                    CategoryDetailUI.refreshContent(event.inventory, configManager, plugin)
                    return
                }
                if (totalEquipped >= suppliesLimit) {
                    who.sendLang(plugin, "loadout.screen.supplies-limit-reached", "max" to suppliesLimit, "current" to totalEquipped)
                    detailHolder.selectedItemIndex = -1
                    CategoryDetailUI.refreshContent(event.inventory, configManager, plugin)
                    return
                }
                // 检查仓库中该物品的数量
                val storageCount = LoadoutUI.getStorageTotalCountByLoadoutId(who.uniqueId, config.id)
                // 计算已装备的数量
                val equippedCount = loadoutHolder.selectedSupplies.filter { it.loadoutId == config.id }.sumOf { it.count }
                // 可用数量 = 仓库数量 - 已装备数量
                val availableCount = storageCount - equippedCount

                if (availableCount > 0) {
                    // 仓库中有足够数量的物品，直接选择（不从仓库中扣除）
                    val storageEntries = LoadoutUI.getStorageEntriesByLoadoutId(who.uniqueId, config.id)
                    if (storageEntries.isEmpty()) {
                        who.sendLang(plugin, "loadout.screen.take-failed")
                        detailHolder.selectedItemIndex = -1
                        CategoryDetailUI.refreshContent(event.inventory, configManager, plugin)
                        return
                    }
                    // 使用仓库中的条目信息（保留durability和itemData）
                    val entry = storageEntries.first().second
                    loadoutHolder.selectedSupplies.add(entry.copy(count = 1))
                    who.sendLang(plugin, "loadout.screen.selected-confirm", "name" to config.name)
                } else {
                    // 仓库中数量不足，需要购买
                    if (!EconomyAPI.isAvailable()) {
                        who.sendLang(plugin, "shop.economy-unavailable")
                        return
                    }
                    if (!StorageAPI.isAvailable()) {
                        who.sendLang(plugin, "shop.storage-unavailable")
                        return
                    }
                    val price = config.buyPrice.toDouble()
                    if (!EconomyAPI.hasEnough(who, price)) {
                        who.sendLang(plugin, "shop.not-enough", "cost" to EconomyAPI.format(price))
                        detailHolder.selectedItemIndex = -1
                        CategoryDetailUI.refreshContent(event.inventory, configManager, plugin)
                        return
                    }

                    // 检查仓库是否有空位
                    val allStorageEntries = StorageAPI.getStorage(who.uniqueId, StorageAPI.STORAGE_SLOT_COUNT)
                    val emptySlot = allStorageEntries.indexOfFirst { it == null }
                    // 检查是否有可以堆叠的槽位
                    val stackableSlot = allStorageEntries.indexOfFirst { 
                        it != null && it.type == StorageEntryType.LOADOUT && it.loadoutId == config.id && it.count < 64
                    }

                    if (emptySlot < 0 && stackableSlot < 0) {
                        who.sendLang(plugin, "storage.full")
                        detailHolder.selectedItemIndex = -1
                        CategoryDetailUI.refreshContent(event.inventory, configManager, plugin)
                        return
                    }

                    if (!EconomyAPI.withdraw(who, price)) {
                        who.sendLang(plugin, "shop.purchase-failed")
                        detailHolder.selectedItemIndex = -1
                        CategoryDetailUI.refreshContent(event.inventory, configManager, plugin)
                        return
                    }

                    // 将物品添加到仓库（补给物品不需要耐久度信息）
                    LootItemHelper.addLoadoutItemToStorage(who.uniqueId, config.id, 1, false)

                    // 然后自动装备：在selectedSupplies中添加引用（不从仓库中扣除）
                    loadoutHolder.selectedSupplies.add(StorageEntry(type = StorageEntryType.LOADOUT, count = 1, loadoutId = config.id))
                    who.sendLang(plugin, "shop.purchased-to-storage", "item" to config.name, "cost" to EconomyAPI.format(price))
                    who.sendLang(plugin, "loadout.screen.selected-confirm", "name" to config.name)
                }
                detailHolder.selectedItemIndex = -1
                CategoryDetailUI.refreshContent(event.inventory, configManager, plugin)
            }
            return
        }

        // 装备/近战/远程：首次点击选中，再次点击购买/选择/跳转仓库
        if (detailHolder.selectedItemIndex == contentIndex) {
            when {
                count == 0 -> {
                    if (!EconomyAPI.isAvailable()) {
                        who.sendLang(plugin, "shop.economy-unavailable")
                        return
                    }
                    if (!StorageAPI.isAvailable()) {
                        who.sendLang(plugin, "shop.storage-unavailable")
                        return
                    }
                    val price = config.buyPrice.toDouble()
                    if (!EconomyAPI.hasEnough(who, price)) {
                        who.sendLang(plugin, "shop.not-enough", "cost" to EconomyAPI.format(price))
                        detailHolder.selectedItemIndex = -1
                        CategoryDetailUI.refreshContent(event.inventory, configManager, plugin)
                        return
                    }
                    val allStorageEntries = StorageAPI.getStorage(who.uniqueId, StorageAPI.STORAGE_SLOT_COUNT)
                    val emptySlot = allStorageEntries.indexOfFirst { it == null }
                    if (emptySlot < 0) {
                        who.sendLang(plugin, "storage.full")
                        detailHolder.selectedItemIndex = -1
                        CategoryDetailUI.refreshContent(event.inventory, configManager, plugin)
                        return
                    }
                    if (!EconomyAPI.withdraw(who, price)) {
                        who.sendLang(plugin, "shop.purchase-failed")
                        detailHolder.selectedItemIndex = -1
                        CategoryDetailUI.refreshContent(event.inventory, configManager, plugin)
                        return
                    }
                    // 检查是否是套装
                    val actualConfigList = configManager.getActualEquipmentConfigList(config.id)
                    val isSet = actualConfigList != null && actualConfigList.size > 1

                    val item = LoadoutShopAPI.createPurchasedItem(plugin, config)
                    val entryCount = when (config.id) {
                        "normal_arrow" -> 16
                        "baozha_jian", "guangling_jian", "huanman_jian", "zhongdu_jian", "zhiliao_jian", "xunjie_jian" -> 1
                        else -> item.amount
                    }

                    // 如果是套装，生成各部位耐久度信息
                    val itemData = if (isSet) {
                        val slotDurability = mutableMapOf<String, Pair<Int, Int>>()
                        actualConfigList.forEach { actualConfig ->
                            val material = org.bukkit.Material.matchMaterial(actualConfig.material.uppercase())
                            if (material != null && material.maxDurability > 0) {
                                val slotType = when (material) {
                                    org.bukkit.Material.LEATHER_HELMET, org.bukkit.Material.CHAINMAIL_HELMET, org.bukkit.Material.IRON_HELMET,
                                    org.bukkit.Material.GOLDEN_HELMET, org.bukkit.Material.DIAMOND_HELMET, org.bukkit.Material.NETHERITE_HELMET -> "头盔"
                                    org.bukkit.Material.LEATHER_CHESTPLATE, org.bukkit.Material.CHAINMAIL_CHESTPLATE, org.bukkit.Material.IRON_CHESTPLATE,
                                    org.bukkit.Material.GOLDEN_CHESTPLATE, org.bukkit.Material.DIAMOND_CHESTPLATE, org.bukkit.Material.NETHERITE_CHESTPLATE -> "胸甲"
                                    org.bukkit.Material.LEATHER_LEGGINGS, org.bukkit.Material.CHAINMAIL_LEGGINGS, org.bukkit.Material.IRON_LEGGINGS,
                                    org.bukkit.Material.GOLDEN_LEGGINGS, org.bukkit.Material.DIAMOND_LEGGINGS, org.bukkit.Material.NETHERITE_LEGGINGS -> "腿甲"
                                    org.bukkit.Material.LEATHER_BOOTS, org.bukkit.Material.CHAINMAIL_BOOTS, org.bukkit.Material.IRON_BOOTS,
                                    org.bukkit.Material.GOLDEN_BOOTS, org.bukkit.Material.DIAMOND_BOOTS, org.bukkit.Material.NETHERITE_BOOTS -> "靴子"
                                    else -> null
                                }
                                if (slotType != null) {
                                    val maxDur = material.maxDurability.toInt()
                                    // 新购买的套装，所有部位都是满耐久度
                                    slotDurability[slotType] = Pair(maxDur, maxDur)
                                }
                            }
                        }
                        // 构建耐久度信息字符串，格式：部位:当前/最大,部位:当前/最大
                        slotDurability.keys.sorted().joinToString(",") { slotType ->
                            val (current, max) = slotDurability[slotType] ?: Pair(0, 1)
                            "$slotType:$current/$max"
                        }
                    } else {
                        null
                    }

                    val maxD = item.type.maxDurability.toInt()
                    val durability = if (maxD > 0) {
                        val meta = item.itemMeta as? Damageable
                        if (meta != null) (maxD - meta.damage).coerceIn(0, maxD) else null
                    } else null

                    val entry = StorageEntry(
                        type = StorageEntryType.LOADOUT,
                        count = entryCount,
                        loadoutId = config.id,
                        durability = if (isSet) null else durability?.takeIf { it > 0 },
                        itemData = itemData
                    )
                    val isStackable = detailHolder.category == LoadoutCategory.SUPPLIES ||
                        config.id == "normal_arrow" || config.id == "baozha_jian" ||
                        config.id in LoadoutUI.SPECIAL_BOW_ARROW_IDS
                    if (isStackable) {
                        LootItemHelper.addLoadoutItemToStorage(who.uniqueId, config.id, entryCount)
                    } else {
                        StorageAPI.setSlot(who.uniqueId, emptySlot, entry)
                    }
                    if (config.id == "wind_crossbow" || config.id == "burst_crossbow") {
                        LootItemHelper.addLoadoutItemToStorage(who.uniqueId, "normal_arrow", 36)
                    }
                    if (config.id == "explosive_crossbow") {
                        LootItemHelper.addLoadoutItemToStorage(who.uniqueId, "baozha_jian", 12)
                    }
                    if (config.id == "special_bow") {
                        for (arrowId in LoadoutUI.SPECIAL_BOW_ARROW_IDS) {
                            LootItemHelper.addLoadoutItemToStorage(who.uniqueId, arrowId, 8)
                        }
                    }
                    who.sendLang(plugin, "shop.purchased-to-storage", "item" to config.name, "cost" to EconomyAPI.format(price))
                    setSelection(loadoutHolder, detailHolder.category, entry)
                    who.sendLang(plugin, "loadout.screen.selected-confirm", "name" to config.name)
                    who.closeInventory()
                    reopenLoadout(who, loadoutHolder, plugin, configManager)
                }
                count == 1 -> {
                    val list = LoadoutUI.getStorageEntriesByLoadoutId(who.uniqueId, config.id)
                    val (_, entry) = list.first()
                    // 确保套装的itemData字段被正确保留
                    val actualConfigList = configManager.getActualEquipmentConfigList(config.id)
                    val isSet = actualConfigList != null && actualConfigList.size > 1
                    val finalEntry = if (isSet) {
                        // 套装：确保itemData字段被正确保留
                        if (entry.itemData != null && entry.itemData!!.isNotEmpty()) {
                            entry
                        } else {
                            // 如果仓库中的物品没有itemData，需要重新获取完整的itemData
                            val fullEntry = list.firstOrNull { it.second.itemData != null && it.second.itemData!!.isNotEmpty() }
                            fullEntry?.second ?: entry
                        }
                    } else {
                        entry
                    }
                    setSelection(loadoutHolder, detailHolder.category, finalEntry)
                    who.sendLang(plugin, "loadout.screen.selected-confirm", "name" to config.name)
                    who.closeInventory()
                    reopenLoadout(who, loadoutHolder, plugin, configManager)
                }
                else -> {
                    val list = LoadoutUI.getStorageEntriesByLoadoutId(who.uniqueId, config.id)
                    who.closeInventory()
                    PickFromStorageUI.openWithEntries(who, loadoutHolder, detailHolder.category, list, plugin, configManager)
                }
            }
        } else {
            detailHolder.selectedItemIndex = contentIndex
            CategoryDetailUI.refreshContent(event.inventory, configManager, plugin)
        }
    }

    private fun setSelection(loadoutHolder: LoadoutScreenHolder, category: LoadoutCategory, entry: StorageEntry) {
        when (category) {
            LoadoutCategory.EQUIPMENT -> loadoutHolder.selectedEquipment = entry
            LoadoutCategory.MELEE -> loadoutHolder.selectedMelee = entry
            LoadoutCategory.RANGED -> {
                // 切换远程武器时，若已装配过弹药，则重置弹药选择（相当于“返还到仓库”）
                if (loadoutHolder.selectedRanged != null && loadoutHolder.selectedRanged != entry) {
                    loadoutHolder.selectedArrows.clear()
                }
                loadoutHolder.selectedRanged = entry
            }
            LoadoutCategory.SUPPLIES -> {
                val limit = DungeonContext.get()?.configManager?.suppliesCarryLimit ?: 6
                if (loadoutHolder.selectedSupplies.size < limit) {
                    loadoutHolder.selectedSupplies.add(entry)
                }
            }
            LoadoutCategory.REPAIR -> { }
        }
    }

    private fun reopenLoadout(
        player: Player,
        loadoutHolder: LoadoutScreenHolder,
        plugin: JavaPlugin,
        configManager: ConfigManager
    ) {
        object : BukkitRunnable() {
            override fun run() {
                LoadoutUI.openWithHolder(player, loadoutHolder, plugin, configManager)
            }
        }.runTaskLater(plugin, 1L)
    }
}
