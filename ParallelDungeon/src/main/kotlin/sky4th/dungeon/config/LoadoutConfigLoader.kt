package sky4th.dungeon.config

import sky4th.dungeon.loadout.LoadoutCategory
import sky4th.dungeon.loadout.LoadoutShopItemConfig
import sky4th.dungeon.loadout.equipment.ActualEquipmentConfig
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin

/**
 * 配装配置加载模块：从 config 读取 loadout-shop 与 actual-equipment。
 * 新增商品时只需在 config 对应分类下列表加一项；新增分类时在 [LoadoutCategory] 加枚举并在 config 加同名节点。
 */
object LoadoutConfigLoader {

    /**
     * 按分类获取配装商店商品列表。
     */
    fun getLoadoutShopItems(
        config: FileConfiguration?,
        category: LoadoutCategory,
        plugin: JavaPlugin
    ): List<LoadoutShopItemConfig> {
        val section = config?.getConfigurationSection(LoadoutConfigKeys.LOADOUT_SHOP) ?: return emptyList()
        val list = section.getList(category.configKey) ?: return emptyList()
        return list.mapNotNull { raw ->
            if (raw !is Map<*, *>) return@mapNotNull null
            parseOneShopItem(raw, category, plugin)
        }
    }

    /**
     * 所有分类下的商店商品（扁平列表）。
     */
    fun getAllLoadoutShopItems(config: FileConfiguration?, plugin: JavaPlugin): List<LoadoutShopItemConfig> =
        LoadoutCategory.entries.flatMap { getLoadoutShopItems(config, it, plugin) }

    /**
     * 按 id 查找商店商品。
     */
    fun getLoadoutShopItemById(
        config: FileConfiguration?,
        id: String,
        plugin: JavaPlugin
    ): LoadoutShopItemConfig? =
        getAllLoadoutShopItems(config, plugin).firstOrNull { it.id == id }

    /**
     * 有商品的分类（供 UI 只展示有内容的分类）。
     */
    fun getLoadoutCategoriesWithItems(config: FileConfiguration?, plugin: JavaPlugin): List<LoadoutCategory> =
        LoadoutCategory.entries.filter { getLoadoutShopItems(config, it, plugin).isNotEmpty() }

    /**
     * 按 loadoutId 获取实际装备配置列表。
     * 未配置或 id 不存在则返回 null（发放时沿用商店简介物品生成单件）。
     * 单件：actual-equipment.<id> 下 material/name/shop-lore/normal-lore/enchants/buy-price/sell-price；套装：items 列表，每项同上。
     */
    fun getActualEquipmentConfigList(
        config: FileConfiguration?,
        loadoutId: String,
        plugin: JavaPlugin
    ): List<ActualEquipmentConfig>? {
        val section = config?.getConfigurationSection(LoadoutConfigKeys.ACTUAL_EQUIPMENT) ?: return null
        val entry = section.getConfigurationSection(loadoutId) ?: return null
        val itemsList = entry.getList(LoadoutConfigKeys.ITEMS)
        if (itemsList != null && itemsList.isNotEmpty()) {
            return itemsList.mapNotNull { raw ->
                if (raw !is Map<*, *>) return@mapNotNull null
                parseOneActualItem(raw)
            }
        }
        val material = entry.getString(LoadoutConfigKeys.MATERIAL)?.trim().orEmpty()
        if (material.isEmpty()) return null
        val displayName = entry.getString(LoadoutConfigKeys.NAME)?.trim().takeIf { !it.isNullOrEmpty() }
        val shopLore = parseLore(entry.getList(LoadoutConfigKeys.SHOP_LORE))
        val normalLore = parseLore(entry.getList(LoadoutConfigKeys.NORMAL_LORE))
        val enchantsSection = entry.getConfigurationSection(LoadoutConfigKeys.ENCHANTS)
        val enchants = parseEnchants(enchantsSection)
        val buyPrice = entry.getInt(LoadoutConfigKeys.BUY_PRICE).takeIf { it > 0 }
        val sellPrice = entry.getInt(LoadoutConfigKeys.SELL_PRICE).takeIf { it > 0 }
        val basePotionType = entry.getString(LoadoutConfigKeys.BASE_POTION_TYPE)?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
        return listOf(ActualEquipmentConfig(material, displayName, shopLore, normalLore, enchants, buyPrice, sellPrice, basePotionType))
    }

    private fun parseOneShopItem(
        raw: Map<*, *>,
        category: LoadoutCategory,
        plugin: JavaPlugin
    ): LoadoutShopItemConfig? {
        val id = raw[LoadoutConfigKeys.ID]?.toString()?.trim().orEmpty()
        val name = raw[LoadoutConfigKeys.NAME]?.toString()?.trim().orEmpty()
        val material = raw[LoadoutConfigKeys.MATERIAL]?.toString()?.trim().orEmpty()
        if (id.isEmpty() || name.isEmpty() || material.isEmpty()) {
            plugin.logger.warning("Invalid loadout-shop item in '${category.configKey}': $raw")
            return null
        }
        
        // 解析价格
        val buyPrice = (raw[LoadoutConfigKeys.BUY_PRICE] as? Number)?.toInt() ?: 0
        val sellPrice = (raw[LoadoutConfigKeys.SELL_PRICE] as? Number)?.toInt() ?: 0
        
        // 解析描述
        val shopLore = parseLore(raw[LoadoutConfigKeys.SHOP_LORE])
        val normalLore = parseLore(raw[LoadoutConfigKeys.NORMAL_LORE])
        
        // 解析品级
        val tier = raw[LoadoutConfigKeys.TIER]?.toString()?.trim()?.takeIf { it.isNotEmpty() } ?: "epic"

        return LoadoutShopItemConfig(
            id = id,
            name = name,
            material = material,
            buyPrice = buyPrice.coerceAtLeast(0),
            sellPrice = sellPrice.coerceAtLeast(0),
            category = category,
            shopLore = shopLore,
            normalLore = normalLore,
            tier = tier
        )
    }

    private fun parseOneActualItem(raw: Any): ActualEquipmentConfig? {
        val map = when (raw) {
            is Map<*, *> -> raw
            else -> return null
        }
        val material = map[LoadoutConfigKeys.MATERIAL]?.toString()?.trim().orEmpty()
        if (material.isEmpty()) return null
        val displayName = map[LoadoutConfigKeys.NAME]?.toString()?.trim().takeIf { !it.isNullOrEmpty() }
        
        // 解析描述
        val shopLore = parseLore(map[LoadoutConfigKeys.SHOP_LORE])
        val normalLore = parseLore(map[LoadoutConfigKeys.NORMAL_LORE])
        
        val enchants = when (val e = map[LoadoutConfigKeys.ENCHANTS]) {
            is Map<*, *> -> e.mapNotNull { (k, v) ->
                val key = k?.toString()?.trim()?.uppercase() ?: return@mapNotNull null
                val level = (v as? Number)?.toInt()?.coerceIn(1, 255) ?: return@mapNotNull null
                key to level
            }.toMap()
            else -> emptyMap()
        }
        
        // 解析价格
        val buyPrice = (map[LoadoutConfigKeys.BUY_PRICE] as? Number)?.toInt()?.takeIf { it > 0 }
        val sellPrice = (map[LoadoutConfigKeys.SELL_PRICE] as? Number)?.toInt()?.takeIf { it > 0 }
        
        val basePotionType = map[LoadoutConfigKeys.BASE_POTION_TYPE]?.toString()?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
        return ActualEquipmentConfig(material, displayName, shopLore, normalLore, enchants, buyPrice, sellPrice, basePotionType)
    }

    private fun parseLore(raw: Any?): List<String> = when (raw) {
        is List<*> -> raw.mapNotNull { (it as? String)?.trim() }.filter { it.isNotEmpty() }
        is String -> if (raw.isNotBlank()) listOf(raw.trim()) else emptyList()
        else -> emptyList()
    }

    private fun parseEnchants(section: ConfigurationSection?): Map<String, Int> =
        if (section != null) {
            section.getKeys(false).mapNotNull { key ->
                val level = section.getInt(key, 1).coerceIn(1, 255)
                key.trim().uppercase() to level
            }.toMap()
        } else emptyMap()
}
