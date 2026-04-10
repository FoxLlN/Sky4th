package sky4th.dungeon.loadout.equipment

import sky4th.core.model.StorageEntry
import sky4th.core.model.StorageEntryType
import sky4th.core.api.LanguageAPI
import sky4th.dungeon.Dungeon
import sky4th.dungeon.config.ConfigManager
import sky4th.dungeon.loadout.shop.LoadoutShopAPI
import sky4th.dungeon.command.DungeonContext
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import org.bukkit.Registry
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionType
import org.bukkit.plugin.java.JavaPlugin

/**
 * 根据玩家选择的配装（商品简介 loadoutId）解析为实际发放的装备 ItemStack。
 * 若配置了 actual-equipment，则使用实际装备配置（可为一套多件）；否则回退为商店简介对应的展示物品。
 */
object ActualEquipmentResolver {

    /**
     * 将一条配装条目转为实际发放的 ItemStack 列表（单件为 1 个，套装为多件）。
     * 仅处理 type=LOADOUT；其他类型返回空列表。
     */
    @JvmStatic
    fun toActualItems(
        entry: StorageEntry,
        plugin: JavaPlugin,
        configManager: ConfigManager
    ): List<ItemStack> {
        if (entry.type != StorageEntryType.LOADOUT) return emptyList()
        val loadoutId = entry.loadoutId ?: return emptyList()

        val configList = configManager.getActualEquipmentConfigList(loadoutId)
        if (!configList.isNullOrEmpty()) {
            val list = configList.mapNotNull { buildFromActualConfig(it, loadoutId, plugin) }
            
            // 应用耐久度
            val finalList = if (entry.itemData != null && entry.itemData!!.isNotEmpty() && list.size > 1) {
                // 套装：根据itemData中的各部位耐久度信息设置，并过滤掉耐久度为0的部位
                plugin.logger.info("[ActualEquipmentResolver] 满足套装条件，调用applySetDurability")
                sky4th.dungeon.loadout.storage.DurabilityManager.applySetDurability(list, entry.itemData!!, plugin)
            } else {
                if (entry.durability != null) {
                    // 单件：应用保存的耐久度
                    plugin.logger.info("[ActualEquipmentResolver] 满足单件条件，应用耐久度: ${entry.durability}")
                    list.forEach { applyDurability(it, entry.durability) }
                } else {
                    plugin.logger.info("[ActualEquipmentResolver] 不满足任何耐久度条件")
                }
                list
            }

            if (finalList.size == 1 && configList.size == 1) {
                finalList[0].amount = entry.count.coerceIn(1, finalList[0].maxStackSize)
            }
            return finalList
        }
        // 未配置实际装备时，沿用商店简介物品
        val shopConfig = configManager.getLoadoutShopItemById(loadoutId) ?: return emptyList()
        val one = LoadoutShopAPI.createPurchasedItem(plugin, shopConfig)
        one.amount = entry.count.coerceIn(1, one.maxStackSize)
        applyDurability(one, entry.durability)
        return listOf(one)
    }

    private fun buildFromActualConfig(actual: ActualEquipmentConfig, loadoutId: String, plugin: JavaPlugin): ItemStack? {
        val material = Material.matchMaterial(actual.material.uppercase()) ?: return null
        val item = ItemStack(material)
        val setKey = NamespacedKey(plugin, "loadout_set")
        val shopIdKey = NamespacedKey(plugin, "loadout_shop_id")
        item.editMeta { meta ->
            val name = actual.displayName ?: "<white>${actual.material}"
            meta.displayName(MiniMessage.miniMessage().deserialize(name))
            // 地牢背包中的实际物品使用 normalLore 和 sellPrice
            val loreLines = actual.normalLore.toMutableList()
            if (actual.sellPrice != null) {
                loreLines.add("")
                loreLines.add(LanguageAPI.getText(plugin, "price-value", *arrayOf(Pair("value", actual.sellPrice))))
            }
            if (loreLines.isNotEmpty()) {
                meta.lore(loreLines.map { MiniMessage.miniMessage().deserialize(it) })
            }
            meta.persistentDataContainer.set(setKey, PersistentDataType.STRING, loadoutId)
            meta.persistentDataContainer.set(shopIdKey, PersistentDataType.STRING, loadoutId)
            // 存储价格以便地牢背包价值计算
            if (actual.sellPrice != null) {
                val priceKey = NamespacedKey(plugin, "loadout_price")
                meta.persistentDataContainer.set(priceKey, PersistentDataType.INTEGER, actual.sellPrice)
            }
            @Suppress("DEPRECATION")
            actual.enchants.forEach { (enchantKey, level) ->
                val key = NamespacedKey.minecraft(enchantKey.lowercase())
                Bukkit.getRegistry(Enchantment::class.java)?.get(key)?.let { enchant ->
                    meta.addEnchant(enchant, level.coerceIn(1, enchant.maxLevel), true)
                }
            }
        }
        if (item.type == Material.TIPPED_ARROW && actual.basePotionType != null) {
            item.editMeta { meta ->
                (meta as? PotionMeta)?.let { potionMeta ->
                    try {
                        val potionType = PotionType.valueOf(actual.basePotionType.uppercase())
                        potionMeta.setBasePotionType(potionType)
                    } catch (_: IllegalArgumentException) { }
                }
            }
        }
        return item
    }

    private fun applyDurability(item: ItemStack, durability: Int?) {
        if (durability == null || item.type.maxDurability <= 0) return
        val maxD = item.type.maxDurability.toInt()
        item.editMeta { meta ->
            (meta as? Damageable)?.setDamage((maxD - durability).coerceIn(0, maxD))
        }
    }
}
