package sky4th.dungeon.loadout.shop

import sky4th.dungeon.loadout.storage.StorageUI
import sky4th.dungeon.loadout.storage.LootItemHelper
import sky4th.dungeon.loadout.LoadoutCategory
import sky4th.dungeon.loadout.screen.LoadoutUI
import sky4th.dungeon.util.LanguageUtil.sendLang
import sky4th.core.api.EconomyAPI
import sky4th.core.api.StorageAPI
import sky4th.core.model.StorageEntry
import sky4th.core.model.StorageEntryType
import sky4th.dungeon.command.DungeonContext
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.meta.Damageable
import org.bukkit.persistence.PersistentDataType

/**
 * 配装商店界面点击：切换分类；内容区第一次点击选中（附魔发光），第二次点击同一格确认购买。
 */
class LoadoutShopListener : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder ?: return
        if (!LoadoutShopAPI.isLoadoutShop(holder)) return

        event.isCancelled = true
        val who = event.whoClicked
        if (who !is Player) return

        val shopHolder = holder as LoadoutShopHolder
        val ctx = DungeonContext.get() ?: return
        val plugin = ctx.plugin
        val configManager = ctx.configManager
        val slot = event.rawSlot

        // 右下角箱子：左键打开仓库
        if (LoadoutShopAPI.isStorageButtonSlot(slot) && (event.click == ClickType.LEFT || event.click == ClickType.SHIFT_LEFT)) {
            who.closeInventory()
            StorageUI.open(who)
            return
        }

        // 配装按钮：点击打开配装界面
        if (LoadoutShopAPI.isLoadoutButtonSlot(slot)) {
            who.closeInventory()
            // 获取玩家当前所在的地牢ID
            val dungeonId = DungeonContext.get()?.playerManager?.getCurrentDungeonId(who)
            if (dungeonId != null) {
                LoadoutUI.open(who, dungeonId)
            }
            return
        }

        // 点击分类栏 (slot 2,3,4,5,6)：切换分类（新界面，重置页码与选中）
        if (LoadoutShopAPI.isCategorySlot(slot)) {
            val index = when (slot) {
                2 -> 0; 3 -> 1; 4 -> 2; 5 -> 3; 6 -> 4
                else -> return
            }
            val newCategory = LoadoutCategory.entries.getOrNull(index) ?: return
            LoadoutShopAPI.openShop(who, plugin, configManager, newCategory)
            return
        }

        // 点击内容区（9 格，按 config 顺序）：第一次点击选中（附魔发光），第二次点击同一格确认购买
        val contentIndex = LoadoutShopAPI.getContentIndex(slot)
        if (contentIndex != null) {
            val allItems = configManager.getLoadoutShopItems(shopHolder.category)
            val pageItems = allItems.drop(shopHolder.page * LoadoutShopAPI.CONTENT_SLOTS).take(LoadoutShopAPI.CONTENT_SLOTS)
            if (contentIndex >= pageItems.size) return
            val clicked = event.currentItem ?: return
            if (clicked.type == Material.AIR) return
            val config = pageItems.getOrNull(contentIndex) ?: return

            if (shopHolder.selectedContentIndex == contentIndex) {
                // 已选中该格，第二次点击：确认购买，直接加入仓库；失败则退出选中并刷新，需重新选择再两次点击
                fun failAndClearSelection(messageKey: String, vararg pairs: Pair<String, Any>) {
                    who.sendLang(plugin, messageKey, *pairs)
                    shopHolder.selectedContentIndex = -1
                    LoadoutShopAPI.refreshShopContent(event.inventory, configManager, plugin)
                }
                if (!EconomyAPI.isAvailable()) {
                    failAndClearSelection("shop.economy-unavailable")
                    return
                }
                if (!StorageAPI.isAvailable()) {
                    failAndClearSelection("shop.storage-unavailable")
                    return
                }
                val price = config.buyPrice.toDouble()
                if (!EconomyAPI.hasEnough(who, price)) {
                    failAndClearSelection("shop.not-enough", "cost" to EconomyAPI.format(price))
                    return
                }
                val itemToGive = LoadoutShopAPI.createPurchasedItem(plugin, config)
                val entryCount = when (config.id) {
                    "normal_arrow" -> 16
                    "baozha_jian", "guangling_jian", "huanman_jian", "zhongdu_jian", "zhiliao_jian", "xunjie_jian" -> 1
                    else -> itemToGive.amount
                }

                // 检查是否是套装
                val actualConfigList = configManager.getActualEquipmentConfigList(config.id)
                val isSet = actualConfigList != null && actualConfigList.size > 1

                val maxD = itemToGive.type.maxDurability.toInt()
                val durability = if (maxD > 0) {
                    val meta = itemToGive.itemMeta as? Damageable
                    if (meta != null) (maxD - meta.damage).coerceIn(0, maxD) else null
                } else null

                // 如果是套装，生成各部位耐久度信息
                val itemData = if (isSet) {
                    // 套装：生成各部位耐久度信息字符串
                    val slotDurability = mutableMapOf<String, Pair<Int, Int>>()
                    actualConfigList.forEach { actualConfig ->
                        val material = Material.matchMaterial(actualConfig.material.uppercase())
                        if (material != null && material.maxDurability > 0) {
                            val slotType = when (material) {
                                Material.LEATHER_HELMET, Material.CHAINMAIL_HELMET, Material.IRON_HELMET,
                                Material.GOLDEN_HELMET, Material.DIAMOND_HELMET, Material.NETHERITE_HELMET -> "头盔"
                                Material.LEATHER_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE, Material.IRON_CHESTPLATE,
                                Material.GOLDEN_CHESTPLATE, Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE -> "胸甲"
                                Material.LEATHER_LEGGINGS, Material.CHAINMAIL_LEGGINGS, Material.IRON_LEGGINGS,
                                Material.GOLDEN_LEGGINGS, Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS -> "腿甲"
                                Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS,
                                Material.GOLDEN_BOOTS, Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS -> "靴子"
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

                val entry = StorageEntry(
                    type = StorageEntryType.LOADOUT,
                    count = entryCount,
                    loadoutId = config.id,
                    durability = if (isSet) null else durability?.takeIf { it > 0 },
                    itemData = itemData
                )
                val entries = StorageAPI.getStorage(who.uniqueId, StorageAPI.STORAGE_SLOT_COUNT)
                val isStackable = config.category == LoadoutCategory.SUPPLIES ||
                    config.category == LoadoutCategory.REPAIR ||
                    config.id == "normal_arrow" || config.id == "baozha_jian" ||
                    config.id in sky4th.dungeon.loadout.screen.LoadoutUI.SPECIAL_BOW_ARROW_IDS
                val hasRoom = if (isStackable) {
                    entries.any { it == null } || entries.any { e ->
                        e?.type == StorageEntryType.LOADOUT && e.loadoutId == config.id && e.count < 64
                    }
                } else {
                    entries.any { it == null }
                }
                if (!hasRoom) {
                    failAndClearSelection("storage.full")
                    return
                }
                if (!EconomyAPI.withdraw(who, price)) {
                    failAndClearSelection("shop.purchase-failed")
                    return
                }
                if (isStackable) {
                    LootItemHelper.addLoadoutItemToStorage(who.uniqueId, config.id, entryCount)
                } else {
                    val emptySlot = entries.indexOfFirst { it == null }
                    StorageAPI.setSlot(who.uniqueId, emptySlot, entry)
                }
                // 购买风弹弩/爆发弩时赠送 36 支箭矢到仓库（优先堆叠到已有箭矢格）
                if (config.id == "wind_crossbow" || config.id == "burst_crossbow") {
                    LootItemHelper.addLoadoutItemToStorage(who.uniqueId, "normal_arrow", 36)
                }
                // 购买爆破弩时赠送 12 发爆炸箭到仓库
                if (config.id == "explosive_crossbow") {
                    LootItemHelper.addLoadoutItemToStorage(who.uniqueId, "baozha_jian", 12)
                }
                // 购买特种弓时赠送 5 种特殊箭各 8 支
                if (config.id == "special_bow") {
                    for (arrowId in sky4th.dungeon.loadout.screen.LoadoutUI.SPECIAL_BOW_ARROW_IDS) {
                        LootItemHelper.addLoadoutItemToStorage(who.uniqueId, arrowId, 8)
                    }
                }
                who.sendLang(plugin, "shop.purchased-to-storage", "item" to config.name.replace("&", "§"), "cost" to EconomyAPI.format(price))
                shopHolder.selectedContentIndex = -1
                // 更新信用点显示
                LoadoutShopAPI.updateCreditDisplay(event.inventory, who, configManager)
            } else {
                // 未选中或选中其它格：选中当前格
                shopHolder.selectedContentIndex = contentIndex
            }
            LoadoutShopAPI.refreshShopContent(event.inventory, configManager, plugin)
        }
    }
}
