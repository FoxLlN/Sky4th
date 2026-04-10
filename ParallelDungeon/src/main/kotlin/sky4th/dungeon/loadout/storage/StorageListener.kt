package sky4th.dungeon.loadout.storage

import sky4th.dungeon.loadout.shop.LoadoutShopAPI
import sky4th.dungeon.loadout.screen.LoadoutUI
import sky4th.dungeon.command.DungeonContext
import sky4th.dungeon.command.CommandContext
import sky4th.core.api.StorageAPI
import sky4th.core.api.EconomyAPI
import sky4th.core.model.StorageEntry
import sky4th.core.model.StorageEntryType
import sky4th.dungeon.util.LanguageUtil.sendLang
import sky4th.dungeon.config.ConfigManager
import sky4th.dungeon.loadout.equipment.ActualEquipmentResolver
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import net.kyori.adventure.text.minimessage.MiniMessage

/**
 * 仓库界面：存储区 42 格支持箱子式操作（堆叠、挪位、交换）；禁止与背包交互及 shift 移出。
 * 关闭时把当前 42 格内容写回数据库。
 */
class StorageListener : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder ?: return
        if (!StorageUI.isStorageGui(holder)) return
        val storageHolder = holder as StorageHolder

        val who = event.whoClicked
        if (who !is Player) return

        val slot = event.rawSlot
        val clickedTop = (event.clickedInventory == event.inventory)
        val isStorageSlot = clickedTop && StorageAPI.uiSlotToStorageIndex(slot) != null
        val isOperationSlot = clickedTop && slot in StorageAPI.getOperationSlots()
        val ctx: CommandContext? = DungeonContext.get()

        // 右下角绿宝石块：打开商店
        if (isOperationSlot && slot == StorageUI.SHOP_BUTTON_SLOT) {
            event.isCancelled = true
            who.closeInventory()
            if (ctx == null) return
            LoadoutShopAPI.openShop(who, ctx.plugin, ctx.configManager)
            return
        }

        // 配装按钮：点击打开配装界面
        //if (isOperationSlot && slot == StorageUI.LOADOUT_BUTTON_SLOT) {
        //    event.isCancelled = true
        //    who.closeInventory()
        //    if (ctx == null) return
            // 获取玩家当前所在的地牢ID
        //    val dungeonId = ctx.playerManager.getCurrentDungeonId(who)
        //    if (dungeonId != null) {
        //        LoadoutUI.open(who, dungeonId)
        //    }
        //    return
        //}

        // 出售按钮：左键切换出售模式
        if (isOperationSlot && slot == StorageUI.SELL_BUTTON_SLOT) {
            event.isCancelled = true
            if (ctx == null) return

            if (event.isLeftClick) {
                //切换出售模式
                if (storageHolder.isSellingMode) {
                    // 取消出售模式
                    storageHolder.isSellingMode = false
                    storageHolder.selectedSlots.clear()
                    StorageUI.updateSellButton(event.inventory, storageHolder.isSellingMode, ctx.configManager)
                    StorageUI.updateStorageItems(event.inventory, storageHolder.selectedSlots, ctx.plugin)
                } else {
                    // 进入出售模式
                    storageHolder.isSellingMode = !storageHolder.isSellingMode
                    if (!storageHolder.isSellingMode) {
                        // 退出出售模式时清空选中
                        storageHolder.selectedSlots.clear()
                    }
                    StorageUI.updateSellButton(event.inventory, storageHolder.isSellingMode, ctx.configManager)
                    StorageUI.updateStorageItems(event.inventory, storageHolder.selectedSlots, ctx.plugin)
                } 
            }
            return
        }

        // 操作区（除商店按钮）：禁止点击
        if (isOperationSlot) {
            event.isCancelled = true
            return
        }

        // 点击了玩家背包：禁止（物品不能从仓库拖到背包或从背包拖到仓库）
        if (!clickedTop) {
            event.isCancelled = true
            return
        }

        // shift 点击存储区：禁止，避免整堆移到背包
        if (event.isShiftClick && isStorageSlot) {
            event.isCancelled = true
            return
        }

        // 存储区内：不取消，允许正常堆叠、挪位、交换
        // 但在出售模式下，点击物品进行选中/取消选中
        if (isStorageSlot && storageHolder.isSellingMode) {
            event.isCancelled = true
            val storageIndex = StorageAPI.uiSlotToStorageIndex(slot) ?: return
            val item = event.inventory.getItem(slot) ?: return
            if (ctx == null) return

            if (event.isLeftClick) {
                // 左键：选中/取消选中
                if (storageHolder.selectedSlots.contains(storageIndex)) {
                    // 已选中，执行出售
                    sellItem(who, item, storageHolder, event.inventory, ctx)
                } else {
                    // 未选中，添加到选中列表
                    storageHolder.selectedSlots.add(storageIndex)
                    StorageUI.updateStorageItems(event.inventory, storageHolder.selectedSlots, ctx.plugin)
                }
            } else if (event.isRightClick) {
                // 右键：取消选中
                storageHolder.selectedSlots.remove(storageIndex)
                StorageUI.updateStorageItems(event.inventory, storageHolder.selectedSlots, ctx.plugin)
            }
        }
    }

    /**
     * 出售物品
     * @param player 玩家
     * @param item 物品
     * @param holder 仓库Holder
     * @param inv 仓库界面
     * @param ctx 命令上下文
     */
    private fun sellItem(player: Player, item: org.bukkit.inventory.ItemStack, holder: StorageHolder, inv: org.bukkit.inventory.Inventory, ctx: CommandContext) {
        val configManager = ctx.configManager

        // 获取物品单价
        val unitPrice = getItemPrice(item, configManager, ctx.plugin)

        // 计算总价（单价 × 数量）
        val amount = item.amount
        val totalPrice = unitPrice * amount

        // 给玩家信用点
        if (EconomyAPI.isAvailable()) {
            EconomyAPI.deposit(player, totalPrice.toDouble())
            player.sendLang(ctx.plugin, "storage.sell-success", "totalPrice" to totalPrice, "unitPrice" to unitPrice, "amount" to amount)
        }

        // 从仓库中移除物品
        val slot = inv.contents.indexOfFirst { it == item }
        if (slot >= 0) {
            inv.clear(slot)
            val storageIndex = StorageAPI.uiSlotToStorageIndex(slot)
            if (storageIndex != null) {
                holder.selectedSlots.remove(storageIndex)
            }
        }

        // 更新界面
        StorageUI.updateStorageItems(inv, holder.selectedSlots, ctx.plugin)

        // 更新信用点显示
        val balance = if (EconomyAPI.isAvailable()) EconomyAPI.format(EconomyAPI.getBalance(player)) else "—"
        val creditHead = sky4th.dungeon.head.CreditsHead.createCreditsHead(balance, configManager)
        inv.setItem(8, creditHead)
    }

    /**
     * 获取物品价格
     * @param item 物品
     * @param configManager 配置管理器
     * @param plugin 插件实例
     * @return 价格
     */
    private fun getItemPrice(item: org.bukkit.inventory.ItemStack, configManager: ConfigManager, plugin: org.bukkit.plugin.java.JavaPlugin): Int {
        // 检查是否是 LOADOUT 类型的物品
        val loadoutId = item.itemMeta?.persistentDataContainer?.get(
            org.bukkit.NamespacedKey(plugin, "loadout_shop_id"),
            org.bukkit.persistence.PersistentDataType.STRING
        )
        if (!loadoutId.isNullOrBlank()) {
            val config = configManager.getLoadoutShopItemById(loadoutId)
            if (config != null) {
                val actualConfigList = configManager.getActualEquipmentConfigList(loadoutId)
                val isSet = actualConfigList != null && actualConfigList.size > 1

                val basePrice = if (isSet) {
                    config.sellPrice
                } else {
                    actualConfigList?.firstOrNull()?.sellPrice ?: config.sellPrice
                }

                // 检查是否有耐久度，如果有则折算价格
                val meta = item.itemMeta
                if (meta != null && item.type.maxDurability > 0) {
                    val damageable = meta as? org.bukkit.inventory.meta.Damageable
                    if (damageable != null) {
                        val maxDurability = item.type.maxDurability.toInt()
                        val currentDurability = maxDurability - damageable.damage
                        val durabilityRatio = currentDurability.toDouble() / maxDurability.toDouble()
                        return (basePrice * durabilityRatio).toInt().coerceAtLeast(1)
                    }
                }

                return basePrice
            }
        }

        // 检查是否是 LOOT 类型的物品
        val lootId = item.itemMeta?.persistentDataContainer?.get(
            org.bukkit.NamespacedKey(plugin, "dungeon_loot_id"),
            org.bukkit.persistence.PersistentDataType.STRING
        )
        if (!lootId.isNullOrBlank()) {
            // 从 lore 中获取价格信息
            val lore = item.itemMeta?.lore() ?: emptyList()
            for (line in lore) {
                val text = MiniMessage.miniMessage().serialize(line)
                val valueMatch = Regex("""价值:\s*<yellow>(\d+)""").find(text)
                if (valueMatch != null) {
                    return valueMatch.groupValues[1].toIntOrNull() ?: 0
                }
            }
        }

        // 检查 lore 中是否有价格信息（支持"价格"和"价值"两种格式）
        val lore = item.itemMeta?.lore() ?: emptyList()
        for (line in lore) {
            val text = MiniMessage.miniMessage().serialize(line)
            // 检查"价值"格式（优先级更高）
            val valueMatch = Regex("""价值:\s*<yellow>(\d+)""").find(text)
            if (valueMatch != null) {
                return valueMatch.groupValues[1].toIntOrNull() ?: 0
            }
        }

        return 0
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val holder = event.inventory.holder ?: return
        if (!StorageUI.isStorageGui(holder)) return
        val who = event.player
        if (who !is Player) return

        val ctx = DungeonContext.get() ?: return
        val inv = event.inventory

        // 获取原有的仓库数据，以便保留itemData和durability字段
        val existingEntries = StorageAPI.getStorage(who.uniqueId, StorageAPI.STORAGE_SLOT_COUNT)

        val entries = mutableListOf<StorageEntry?>()
        for (storageIndex in 0 until StorageAPI.STORAGE_SLOT_COUNT) {
            val uiSlot = StorageAPI.storageIndexToUiSlot(storageIndex)
            val item = inv.getItem(uiSlot)
            val entry = StorageUI.itemStackToEntry(item, ctx.plugin, who.uniqueId)

            // 检查是否是套装
            val isSet = if (entry != null && entry.type == StorageEntryType.LOADOUT) {
                val config = ctx.configManager.getLoadoutShopItemById(entry.loadoutId ?: "")
                val actualConfigList = ctx.configManager.getActualEquipmentConfigList(entry.loadoutId ?: "")
                actualConfigList != null && actualConfigList.size > 1
            } else {
                false
            }

            // 如果是套装，需要正确处理itemData和durability字段
            if (entry != null && entry.type == StorageEntryType.LOADOUT && isSet) {
                val existingEntry = existingEntries.getOrNull(storageIndex)

                if (existingEntry != null && existingEntry.loadoutId == entry.loadoutId) {
                    // 同一槽位同一套装：保留原有的itemData和durability字段
                    entries.add(entry.copy(
                        itemData = existingEntry.itemData,
                        durability = existingEntry.durability
                    ))
                } else {
                    // 物品被移动或更换：使用新entry的itemData和durability
                    entries.add(entry)
                }
            } else {
                entries.add(entry)
            }
        }
        StorageAPI.setStorage(who.uniqueId, entries)
    }
}
