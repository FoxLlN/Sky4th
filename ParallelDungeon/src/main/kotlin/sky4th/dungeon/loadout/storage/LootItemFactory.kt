package sky4th.dungeon.loadout.storage

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import sky4th.dungeon.config.ConfigManager
import sky4th.dungeon.config.LootItemConfig
import sky4th.dungeon.command.DungeonContext
import sky4th.core.api.LanguageAPI
import net.kyori.adventure.text.minimessage.MiniMessage

/**
 * 掉落物物品工厂类
 * 用于根据 LootItemConfig 生成物品，特别是处理 isshop 为 true 的情况
 */
object LootItemFactory {

    /**
     * 根据 LootItemConfig 创建物品
     *
     * @param config 物品配置
     * @param plugin 插件实例
     * @param configManager 配置管理器
     * @param count 物品数量（默认为1）
     * @return 生成的物品堆
     */
    fun createItem(
        config: LootItemConfig,
        plugin: JavaPlugin,
        configManager: ConfigManager,
        count: Int = 1
    ): ItemStack {
        val ctx = DungeonContext.get() ?: return ItemStack(Material.AIR)

        // 如果是商店物品，根据ID使用特定方式生成
        if (config.isshop) {
            return createShopItem(config, plugin, configManager, count)
        }

        // 否则使用普通方式生成物品
        return createBasicItem(plugin, config, count)
    }

    /**
     * 创建商店物品（根据ID使用特定方式生成）
     *
     * @param config 物品配置
     * @param plugin 插件实例
     * @param configManager 配置管理器
     * @param count 物品数量
     * @return 生成的物品堆
     */
    private fun createShopItem(
        config: LootItemConfig,
        plugin: JavaPlugin,
        configManager: ConfigManager,
        count: Int
    ): ItemStack {
        // 根据 config.id 查找对应的 LoadoutShopItemConfig
        val shopConfig = configManager.getLoadoutShopItemById(config.id)
        if (shopConfig != null) {
            // 使用商店的创建方式，确保与商店购买的物品完全一致
            val item = sky4th.dungeon.loadout.shop.LoadoutShopAPI.createPurchasedItem(plugin, shopConfig)
            item.amount = count.coerceIn(1, item.maxStackSize)
            return item
        }
        // 如果找不到对应的商店配置，回退到基本创建方式
        return createBasicItem(plugin, config, count)
    }

    /**
     * 创建基本物品（不使用特殊逻辑）
     *
     * @param config 物品配置
     * @param count 物品数量
     * @return 生成的物品堆
     */
    private fun createBasicItem(
        plugin: JavaPlugin,
        config: LootItemConfig,
        count: Int
    ): ItemStack {
        // 如果 cashId 不为空，说明这是一个现金物品，不需要 material
        if (config.cashId.isNotEmpty()) {
            // 返回空气物品，因为现金物品应该由 cashId 处理
            return ItemStack(Material.AIR, 0)
        }

        val material = Material.entries.find { it.name.equals(config.material, ignoreCase = true) } ?: Material.ARROW
        val item = ItemStack(material, count.coerceIn(1, material.maxStackSize))

        item.editMeta { meta ->
            // 设置显示名称
            if (config.name.isNotEmpty()) {
                meta.displayName(MiniMessage.miniMessage().deserialize(config.name))
            }

            // 设置描述
            val lore = mutableListOf<net.kyori.adventure.text.Component>()

            // 添加描述文本
            if (config.description.isNotEmpty()) {
                lore.add(MiniMessage.miniMessage().deserialize(config.description))
            }

            // 添加描述列表
            config.descriptionLore.forEach { loreLine ->
                lore.add(MiniMessage.miniMessage().deserialize(loreLine))
            }

            // 添加价值信息
            if (config.price > 0) {
                lore.add(MiniMessage.miniMessage().deserialize(""))
                lore.add(LanguageAPI.getComponent(plugin, "price-value", "value" to config.price))
            }

            if (lore.isNotEmpty()) {
                meta.lore(lore)
            }
        }

        return item
    }
}
