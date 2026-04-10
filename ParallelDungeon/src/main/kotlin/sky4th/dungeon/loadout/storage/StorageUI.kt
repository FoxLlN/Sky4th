package sky4th.dungeon.loadout.storage

import sky4th.dungeon.loadout.shop.LoadoutShopAPI
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.entity.Player
import sky4th.core.api.EconomyAPI
import sky4th.core.api.StorageAPI
import sky4th.core.api.LanguageAPI
import sky4th.core.model.StorageEntry
import sky4th.core.model.StorageEntryType
import sky4th.dungeon.loadout.storage.LootItemHelper
import sky4th.dungeon.command.DungeonContext
import sky4th.dungeon.config.ConfigManager
import org.bukkit.plugin.java.JavaPlugin
import sky4th.dungeon.loadout.equipment.ActualEquipmentResolver
import sky4th.dungeon.loadout.screen.LoadoutUI

/**
 * 仓库 UI：54 格箱子，左/右各一列为操作区，中间 42 格为存储；右下角绿宝石块打开商店。
 */
object StorageUI {

    /** 仓库右下角：绿宝石块，点击打开商店 */
    const val SHOP_BUTTON_SLOT = 53
    /** 配装按钮槽位：最后一列的倒数第二个（第44槽位），点击打开配装界面 TODO*/
    // const val LOADOUT_BUTTON_SLOT = 44
    /** 出售按钮槽位：左下角（第45槽位） */
    const val SELL_BUTTON_SLOT = 45
    /** 右上角信用点 */
    private const val SLOT_CREDITS = 8

    @JvmStatic
    fun open(player: Player) {
        val ctx = DungeonContext.get() ?: run {
            player.sendMessage("[ParallelDungeon] 上下文未初始化。")
            return
        }
        if (!StorageAPI.isAvailable()) {
            player.sendMessage("§c仓库服务不可用。")
            return
        }
        open(player, ctx.plugin, ctx.configManager)
    }

    @JvmStatic
    fun open(player: Player, plugin: JavaPlugin, configManager: ConfigManager) {
        val title = LanguageAPI.getText(plugin, "storage.title")
        val holder = StorageHolder(player.uniqueId)
        val inv = Bukkit.getServer().createInventory(holder, StorageAPI.CHEST_SIZE, LanguageAPI.getComponent(plugin, title))
        holder.backingInv = inv

        fillDecoration(inv, player, configManager)
        
        fillStorage(inv, player.uniqueId, plugin, configManager, player)

        player.openInventory(inv)
    }

    private fun fillDecoration(inv: Inventory, player: Player, configManager: ConfigManager) {
        val gray = ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
            editMeta { it.displayName(Component.empty()) }
        }
        val balance = if (EconomyAPI.isAvailable()) EconomyAPI.format(EconomyAPI.getBalance(player)) else "—"
        val creditHead = sky4th.dungeon.head.CreditsHead.createCreditsHead(
            balance,
            configManager
        )
        val operationSlots = StorageAPI.getOperationSlots()
        for (slot in operationSlots) {
            when {
                slot == SHOP_BUTTON_SLOT -> inv.setItem(slot, sky4th.dungeon.head.ShopHead.createOpenShopHead(configManager))
                slot == SELL_BUTTON_SLOT -> inv.setItem(slot, sky4th.dungeon.head.SellHead.createSellHead(configManager))
                slot == SLOT_CREDITS -> inv.setItem(slot, creditHead)
                else -> inv.setItem(slot, gray.clone())
            }
        }
    }

    private fun fillStorage(inv: Inventory, uuid: java.util.UUID, plugin: JavaPlugin, configManager: ConfigManager, player: org.bukkit.entity.Player) {
        val entries = StorageAPI.getStorage(uuid, StorageAPI.STORAGE_SLOT_COUNT)

        var itemCount = 0
        for (i in entries.indices) {
            val entry = entries[i] ?: continue

            val uiSlot = StorageAPI.storageIndexToUiSlot(i)
            val item = entryToItemStack(entry, plugin, configManager)

            if (item != null) {
                inv.setItem(uiSlot, item)
                itemCount++
            } 
        }
    }

    @JvmStatic
    internal fun entryToItemStack(entry: sky4th.core.model.StorageEntry, plugin: JavaPlugin, configManager: ConfigManager): ItemStack? {

        return when (entry.type) {
            StorageEntryType.LOADOUT -> {
                val loadoutId = entry.loadoutId ?: run {
                    return null
                }
                val config = configManager.getLoadoutShopItemById(loadoutId) ?: run {
                    return null
                }
                val actualConfigList = configManager.getActualEquipmentConfigList(loadoutId)
                
                // 对于套装（有多个 actual-equipment 配置），在仓库和配装中应该显示为整套，使用商店配置
                // 对于单件装备（只有1个 actual-equipment 配置），可以使用 actual-equipment 渲染
                val isSet = actualConfigList != null && actualConfigList.size > 1
                
                val item = if (isSet) {
                    // 套装：使用商店配置渲染（整套显示）
                    LoadoutShopAPI.createPurchasedItem(plugin, config)
                } else {
                    // 单件：优先使用 actual-equipment 渲染（如果有配置）
                    val actualItems = ActualEquipmentResolver.toActualItems(entry, plugin, configManager)
                    actualItems.firstOrNull() ?: LoadoutShopAPI.createPurchasedItem(plugin, config)
                }
                
                item.amount = entry.count.coerceIn(1, item.maxStackSize)

                val dur = entry.durability

                // 使用DurabilityManager统一处理耐久度
                if (isSet) {
                    // 套装：从itemData计算整体耐久度比例
                    val durabilityInfo = DurabilityManager.getSetDurabilityInfo(entry)
                    val overallRatio = durabilityInfo?.calculateOverallRatio() ?: 100
                    DurabilityManager.applyDurability(item, overallRatio, isSet = true)
                } else if (dur != null) {
                    // 单件：应用保存的耐久度
                    DurabilityManager.applyDurability(item, dur, isSet = false)
                }
                
                // 检查是否已经有"价值"行，避免重复添加
                val existingLore = item.itemMeta?.lore() ?: emptyList()
                val hasValueLine = existingLore.any { 
                    val text = MiniMessage.miniMessage().serialize(it)
                    text.contains("价值") || text.contains("价值")
                }
                
                // 计算基础价格
                val basePrice = if (isSet) {
                    // 套装使用商店配置的 sellPrice
                    config.sellPrice
                } else {
                    // 单件使用 actual-equipment 的 sellPrice（如果有）
                    actualConfigList?.firstOrNull()?.sellPrice ?: config.sellPrice
                }

                // 根据耐久度比例折算价值
                val sellPrice = if (isSet) {
                    // 套装：从itemData获取各部位耐久度信息，计算整体耐久度比例
                    val durabilityInfo = DurabilityManager.getSetDurabilityInfo(entry)
                    val overallRatio = durabilityInfo?.calculateOverallRatio() ?: 100
                    if (overallRatio < 100) (basePrice * overallRatio / 100).coerceAtLeast(1) else basePrice
                } else if (dur != null) {
                    // 单件：dur是实际耐久度值，需要计算百分比
                    val maxDur = item.type.maxDurability.toInt()
                    if (maxDur > 0) {
                        val durabilityRatio = (dur * 100 / maxDur).coerceIn(0, 100)
                        if (durabilityRatio < 100) (basePrice * durabilityRatio / 100).coerceAtLeast(1) else basePrice
                    } else {
                        basePrice
                    }
                } else {
                    basePrice
                }

                item.editMeta { meta ->
                    val lore = existingLore.toMutableList()

                    // 如果是套装且有各部位耐久度信息，添加耐久度描述
                    if (isSet) {
                        val durabilityInfo = DurabilityManager.getSetDurabilityInfo(entry)
                        if (durabilityInfo != null) {
                            // 检查是否有任何部位的耐久度损失
                            val hasDamage = durabilityInfo.slotDurabilities.any { it.value.first < it.value.second }
                            if (hasDamage) {
                                lore.add(LanguageAPI.getComponent(plugin, "storage.set-durability-title"))
                                durabilityInfo.slotDurabilities.forEach { entry ->
                                    val slot = entry.key
                                    val (current, max) = entry.value
                                    lore.add(LanguageAPI.getComponent(plugin, "storage.set-durability-line", "slot" to slot.displayName, "current" to current, "max" to max))
                                }
                            }
                        } 
                    } 

                    // 处理价值行
                    if (hasValueLine) {
                        // 判断是否需要更新价值行（有耐久度损失时需要更新）
                        val needsUpdate = if (isSet) {
                            // 套装：检查是否有任何部位的耐久度损失
                            val durabilityInfo = DurabilityManager.getSetDurabilityInfo(entry)
                            val hasDamage = durabilityInfo != null && durabilityInfo.slotDurabilities.any { it.value.first < it.value.second }
                            hasDamage
                        } else if (dur != null) {
                            // 单件：dur是实际耐久度值，需要计算百分比
                            val maxDur = item.type.maxDurability.toInt()
                            val hasDamage = maxDur > 0 && dur < maxDur
                            hasDamage
                        } else {
                            false
                        }
                        if (needsUpdate) {
                            val newLore = lore.map { loreLine ->
                                val text = MiniMessage.miniMessage().serialize(loreLine)
                                if (text.contains("价值")) {
                                    val newLine = LanguageAPI.getComponent(plugin, "price-value", "value" to sellPrice)
                                    newLine
                                } else {
                                    loreLine
                                }
                            }
                            meta.lore(newLore)
                        } else {
                            // 满耐久，保持原有价值行
                            meta.lore(lore)
                        }
                    } else {
                        // 没有价值行，添加折算后的价值
                        lore.add(LanguageAPI.getComponent(plugin, "price-value", "value" to sellPrice))
                        meta.lore(lore)
                    }
                }
                item
            }
            StorageEntryType.LOOT -> {
                // LOOT类型物品：从itemData中解析物品信息
                val itemData = entry.itemData ?: return null

                // 尝试使用物品ID创建物品
                val item = createLootItemFromData(itemData, entry.count)
                if (item != null) {
                    return item
                }

                return null
            }
        }
    }

    /**
     * 将界面中的 ItemStack 转回 StorageEntry（关闭仓库时写回 DB 用）。
     * 有 loadout_shop_id 的视为 LOADOUT，否则按 LOOT（材质名 + 数量）保存。
     */
    @JvmStatic
    internal fun itemStackToEntry(item: ItemStack?, plugin: JavaPlugin, uuid: java.util.UUID): StorageEntry? {
        if (item == null || item.type.isAir) return null
        val count = item.amount.coerceIn(1, item.maxStackSize)
        val loadoutId = item.itemMeta?.persistentDataContainer?.get(
            NamespacedKey(plugin, "loadout_shop_id"),
            PersistentDataType.STRING
        )
        if (!loadoutId.isNullOrBlank()) {
            // 获取装备配置以判断是否是套装
            val ctx = DungeonContext.get()
            val isSet = ctx?.configManager?.getActualEquipmentConfigList(loadoutId)?.size?.let { it > 1 } ?: false

            // 使用DurabilityManager获取耐久度
            val durability = DurabilityManager.getDurability(item, isSet)

            // 对于套装，需要从物品的lore中提取各部位耐久度信息
            val itemData = if (isSet) {
                // 从lore中提取耐久度信息
                val lore = item.itemMeta?.lore() ?: emptyList()
                val durabilityLines = lore.filter { loreLine ->
                    val text = MiniMessage.miniMessage().serialize(loreLine)
                    text.contains("耐久度") || text.contains("当前") || text.contains("/")
                }

                if (durabilityLines.isNotEmpty()) {
                    // 解析lore中的耐久度信息
                    val slotDurabilities = mutableMapOf<String, Pair<Int, Int>>()
                    durabilityLines.forEach { loreLine ->
                        val text = MiniMessage.miniMessage().serialize(loreLine)
                        try {
                            // 格式：§7部位: §e当前/最大
                            val parts = text.split(":")
                            if (parts.size >= 2) {
                                val slot = parts[0].replace("§7", "").trim()
                                val values = parts[1].replace("§e", "").trim()
                                val currentMax = values.split("/")
                                if (currentMax.size == 2) {
                                    val current = currentMax[0].toIntOrNull()
                                    val max = currentMax[1].toIntOrNull()
                                    if (current != null && max != null) {
                                        slotDurabilities[slot] = current to max
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // 忽略解析失败
                        }
                    }
                    // 构建itemData字符串
                    if (slotDurabilities.isNotEmpty()) {
                        slotDurabilities.entries.joinToString(",") { entry ->
                            val slot = entry.key
                            val (current, max) = entry.value
                            "$slot:$current/$max"
                        }
                    } else {
                        null
                    }
                } else {
                    null
                }
            } else {
                null
            }

            return StorageEntry(
                type = StorageEntryType.LOADOUT,
                count = count,
                loadoutId = loadoutId,
                durability = durability?.takeIf { it > 0 },
                itemData = itemData
            )
        }
        // 检查是否是地牢掉落物
        val lootId = item.itemMeta?.persistentDataContainer?.get(
            NamespacedKey(plugin, "dungeon_loot_id"),
            PersistentDataType.STRING
        )

        // 从物品上读取dungeonId
        val dungeonId = item.itemMeta?.persistentDataContainer?.get(
            NamespacedKey(plugin, "dungeon_id"),
            PersistentDataType.STRING
        )

        // 如果是地牢掉落物，保存lootId和dungeonId到itemData
        val itemData = if (!lootId.isNullOrBlank()) {
            if (dungeonId != null) {
                "lootId:$lootId;dungeonId:$dungeonId"
            } else {
                "lootId:$lootId"
            }
        } else {
            item.type.name
        }

        return StorageEntry(
            type = StorageEntryType.LOOT,
            count = count,
            itemData = itemData
        )
    }

    /**
     * 从键值对格式的itemData中反序列化物品
     * @param itemData 键值对格式的物品数据，格式：lootId:xxx;material:xxx;name:xxx;durability:xxx;maxDurability:xxx;description:xxx
     * @param count 物品数量
     * @param plugin 插件实例
     * @return 物品堆
     */
    private fun deserializeLootItemFromJson(itemData: String, count: Int, plugin: JavaPlugin): ItemStack? {
        try {
            // 解析键值对格式的物品数据
            val lootId = extractKeyValue(itemData, "lootId") ?: return null
            val materialName = extractKeyValue(itemData, "material") ?: return null
            val material = Material.matchMaterial(materialName) ?: Material.PAPER

            val item = ItemStack(material, count.coerceIn(1, material.maxStackSize))
            val meta = item.itemMeta ?: return null

            // 设置lootId标记
            val lootIdKey = NamespacedKey(plugin, "dungeon_loot_id")
            meta.persistentDataContainer.set(lootIdKey, PersistentDataType.STRING, lootId)

            // 设置名字
            val name = extractKeyValue(itemData, "name")
            if (name != null) {
                meta.displayName(MiniMessage.miniMessage().deserialize(name))
            }

            // 设置耐久度
            val durability = extractKeyValue(itemData, "durability")?.toIntOrNull()
            val maxDurability = extractKeyValue(itemData, "maxDurability")?.toIntOrNull()
            if (durability != null && maxDurability != null && material.maxDurability > 0) {
                val damageable = meta as? Damageable
                if (damageable != null) {
                    val damage = maxDurability - durability
                    damageable.damage = damage.coerceAtMost(maxDurability)
                }
            }

            // 设置描述
            val description = extractKeyValue(itemData, "description")
            if (description != null) {
                // 解析描述列表（用;分隔）
                val descriptions = description.split("\\;").map { it.replace("\\;", ";") }
                if (descriptions.isNotEmpty()) {
                    meta.lore(descriptions.map { MiniMessage.miniMessage().deserialize(it) })
                }
            }

            item.itemMeta = meta
            return item
        } catch (e: Exception) {
            plugin.logger.warning("Failed to deserialize loot item from key-value: $itemData, error: ${e.message}")
            return null
        }
    }

    /**
     * 从键值对字符串中提取指定字段的值
     * @param data 键值对字符串，格式：key1:value1;key2:value2;...
     * @param key 字段名
     * @return 字段值，如果不存在返回null
     */
    private fun extractKeyValue(data: String, key: String): String? {
        val pattern = Regex("$key:([^;]*)")
        val match = pattern.find(data) ?: return null
        return match.groupValues[1].replace("\\;", ";")
    }

    @JvmStatic
    fun isStorageGui(holder: Any?): Boolean = holder is StorageHolder

    /**
     * 更新出售按钮状态
     * @param inv 仓库界面
     * @param isSellingMode 是否处于出售模式
     */
    @JvmStatic
    fun updateSellButton(inv: Inventory, isSellingMode: Boolean, configManager: ConfigManager) {
        val sellButton = if (isSellingMode) {
            sky4th.dungeon.head.SellHead.createSellConfirmHead(configManager)
        } else {
            sky4th.dungeon.head.SellHead.createSellHead(configManager)
        }
        inv.setItem(SELL_BUTTON_SLOT, sellButton)
    }

    /**
     * 更新存储区物品显示（添加或移除附魔特效）
     * @param inv 仓库界面
     * @param selectedSlots 选中的槽位集合
     * @param plugin 插件实例
     */
    @JvmStatic
    fun updateStorageItems(inv: Inventory, selectedSlots: Set<Int>, plugin: JavaPlugin) {
        for (storageIndex in 0 until StorageAPI.STORAGE_SLOT_COUNT) {
            val uiSlot = StorageAPI.storageIndexToUiSlot(storageIndex)
            val item = inv.getItem(uiSlot) ?: continue
            val isSelected = selectedSlots.contains(storageIndex)

            item.editMeta { meta ->
                val hasGlow = meta.hasEnchants()
                if (isSelected && !hasGlow) {
                    // 添加附魔特效
                    sky4th.dungeon.loadout.shop.LoadoutShopAPI.addEnchantGlowPublic(meta)
                } else if (!isSelected && hasGlow) {
                    // 移除附魔特效
                    meta.enchants.forEach { (enchant, _) ->
                        meta.removeEnchant(enchant)
                    }
                }
            }
            inv.setItem(uiSlot, item)
        }
    }

    /**
     * 从 LOOT 条目的 itemData 中提取 lootId 并创建物品
     * @param itemData 物品数据
     * @param count 物品数量
     * @return 创建的物品堆，如果找不到配置则返回null
     */
    private fun createLootItemFromData(itemData: String, count: Int): ItemStack? {
        // 尝试从 itemData 中提取 lootId
        val lootId = extractKeyValue(itemData, "lootId")
        if (lootId == null) {
            return null
        }

        // 从itemData中提取dungeonId
        val actualDungeonId = extractKeyValue(itemData, "dungeonId")
        if (actualDungeonId == null) {
            return null
        }

        // 获取LootItemConfig
        val ctx = sky4th.dungeon.command.DungeonContext.get()
        val configManager = ctx?.configManager ?: return null
        val lootConfig = configManager.getLootItemById(actualDungeonId, lootId) ?: return null

        // 使用ContainerLootService创建物品
        val containerLootService = sky4th.dungeon.Dungeon.instance.containerLootService
        val item = containerLootService.createItemStack(actualDungeonId, lootConfig)
        item.amount = count.coerceIn(1, item.maxStackSize)

        return item
    }
}



