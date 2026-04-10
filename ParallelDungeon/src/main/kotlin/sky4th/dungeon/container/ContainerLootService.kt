
package sky4th.dungeon.container

import sky4th.dungeon.Dungeon
import sky4th.dungeon.config.ConfigManager
import sky4th.dungeon.config.LootItemConfig
import sky4th.dungeon.config.CashItemConfig
import sky4th.dungeon.head.CashHead
import sky4th.dungeon.loadout.storage.LootItemFactory
import sky4th.dungeon.player.BackpackManager
import sky4th.dungeon.player.PlayerManager
import sky4th.core.api.LanguageAPI
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import kotlin.random.Random

/**
 * 容器掉落物服务：按权重随机掉落、创建带品级/价值的物品、发放给玩家。
 */
class ContainerLootService(
    private val plugin: JavaPlugin,
    private val configManager: ConfigManager,
    private val backpackManager: BackpackManager,
    private val playerManager: PlayerManager
) {
    private val lootIdKey: NamespacedKey by lazy { NamespacedKey(plugin, "dungeon_loot_id") }
    private val dungeonIdKey: NamespacedKey by lazy { NamespacedKey(plugin, "dungeon_id") }  
    private val cashIdKey: NamespacedKey by lazy { NamespacedKey(plugin, "dungeon_cash_id") }
    private val cashValueKey: NamespacedKey by lazy { NamespacedKey(plugin, "dungeon_cash_value") }
    private val random = Random.Default


    /**
     * 随机掉落现金物品
     * @param dungeonId 地牢ID
     * @return 现金物品配置和随机生成的信用点数量
     */
    fun pickRandomCashItem(dungeonId: String): Pair<CashItemConfig, Int>? {
        val cashItems = configManager.getCashItems()
        if (cashItems.isEmpty()) return null

        val cashConfig = cashItems[random.nextInt(cashItems.size)]
        val credits = if (cashConfig.minCredits >= cashConfig.maxCredits) {
            cashConfig.minCredits
        } else {
            random.nextInt(cashConfig.minCredits, cashConfig.maxCredits + 1)
        }

        return cashConfig to credits
    }

    /**
     * 从配置中随机一个物品，并根据其品级获取对应的读条时间。
     * @param dungeonId 地牢ID
     * @param containerLevel 容器等级，用于按等级权重；null 或 0 时使用全局权重或完全随机
     * @param qualityBoost 斥候技能：是否提升高品质物品概率（降低 common/uncommon 权重，提高 rare+ 权重）
     * @param debugPlayer 非 null 时向该玩家发送当前各品级权重的调试信息
     */
    fun pickRandomLoot(dungeonId: String, containerLevel: Int? = null, qualityBoost: Boolean = false, debugPlayer: Player? = null): Pair<LootItemConfig, Int>? {
        val items = configManager.getLootItems(dungeonId)
        if (items.isEmpty()) return null

        val level = containerLevel ?: 0
        var tierWeightsByLevel = if (level > 0) {
            configManager.getLootTierWeightsForLevel(level)
        } else emptyMap()

        var tierWeights = if (tierWeightsByLevel.isNotEmpty()) {
            tierWeightsByLevel
        } else {
            configManager.getLootTierWeightsForLevel(0)
        }

        if (qualityBoost && tierWeights.isNotEmpty()) {
            val multipliers = configManager.scoutQualityBoostMultipliers
            if (multipliers.isNotEmpty()) {
                tierWeights = tierWeights.mapValues { (tier, w) ->
                    (w * (multipliers[tier.lowercase()] ?: 1.0)).toInt().coerceAtLeast(0)
                }.filterValues { it > 0 }
            }
        }

        if (tierWeights.isEmpty()) {
            val loot = items[random.nextInt(items.size)]
            val baseSeconds = configManager.lootTierSearchSeconds[loot.tier] ?: 5
            return loot to baseSeconds.coerceAtLeast(1)
        }

        val selectedTier = selectTierByWeight(tierWeights) ?: return null
        val itemsInTier = items.filter { it.tier == selectedTier }
        if (itemsInTier.isEmpty()) {
            val loot = items[random.nextInt(items.size)]
            val baseSeconds = configManager.lootTierSearchSeconds[loot.tier] ?: 5
            return loot to baseSeconds.coerceAtLeast(1)
        }

        val loot = itemsInTier[random.nextInt(itemsInTier.size)]
        val baseSeconds = configManager.lootTierSearchSeconds[loot.tier] ?: 5
        return loot to baseSeconds.coerceAtLeast(1)
    }

    /**
     * 按稀有度随机一个物品；tier 为 null 时按默认权重随机。
     * @param dungeonId 地牢ID
     */
    fun pickRandomLootByTier(dungeonId: String, tier: String?): LootItemConfig? {
        val items = configManager.getLootItems(dungeonId)
        if (items.isEmpty()) return null
        return if (!tier.isNullOrBlank()) {
            val tierLower = tier.trim().lowercase()
            val inTier = items.filter { it.tier.equals(tierLower, ignoreCase = true) }
            if (inTier.isEmpty()) null else inTier[random.nextInt(inTier.size)]
        } else {
            pickRandomLoot(dungeonId)?.first
        }
    }

    /**
     * 随机抽一个物品放入玩家背包（可选指定稀有度），副本内会刷新计分板。
     * @param dungeonId 地牢ID
     */
    fun giveRandomLootToPlayer(dungeonId: String, player: Player, tier: String? = null): Boolean {
        val loot = pickRandomLootByTier(dungeonId, tier) ?: return false
        val itemStack = createItemStack(dungeonId, loot)
        player.inventory.addItem(itemStack)
        return true
    }

    /**
     * 随机掉落现金物品到玩家位置
     * @param dungeonId 地牢ID
     */
    fun dropRandomCashItem(dungeonId: String, player: Player): Boolean {
        val (cashConfig, credits) = pickRandomCashItem(dungeonId) ?: return false
        val itemStack = createCashItemStack(cashConfig)
        player.world.dropItemNaturally(player.location, itemStack)
        return true
    }

    /**
     * 创建现金物品
     */
    fun createCashItemStack(cashConfig: CashItemConfig): ItemStack {
        // 随机生成信用点数量
        val credits = if (cashConfig.minCredits >= cashConfig.maxCredits) {
            cashConfig.minCredits
        } else {
            random.nextInt(cashConfig.minCredits, cashConfig.maxCredits + 1)
        }

        val head = sky4th.dungeon.head.CashHead.createCashHead(
            cashConfig.name,
            cashConfig.description,
            cashConfig.texture,
            cashConfig.minCredits,
            cashConfig.maxCredits
        )

        val meta = head.itemMeta ?: return head
        // 设置现金ID和实际价值
        meta.persistentDataContainer.set(cashIdKey, PersistentDataType.STRING, cashConfig.id)
        meta.persistentDataContainer.set(cashValueKey, PersistentDataType.INTEGER, credits)
        head.itemMeta = meta

        return head
    }

    /**
     * 根据配置创建带名字、描述、价值的物品栈。
     * @param dungeonId 地牢ID
     */
    fun createItemStack(dungeonId: String, loot: LootItemConfig): ItemStack {
        // 检查是否为现金物品
        if (loot.cashId.isNotBlank()) {
            val cashConfig = configManager.getCashItems().find { it.id == loot.cashId }
            if (cashConfig != null) {
                return createCashItemStack(cashConfig)
            }
        }

        // 根据minAmount和maxAmount随机生成物品数量
        val amount = if (loot.minAmount >= loot.maxAmount) {
            loot.minAmount
        } else {
            random.nextInt(loot.minAmount, loot.maxAmount + 1)
        }

        // 如果是商店物品，使用 LoadoutShopAPI 创建，确保与商店购买的物品一致
        if (loot.isshop) {
            val shopConfig = configManager.getLoadoutShopItemById(loot.id)
            if (shopConfig != null) {
                // 使用商店API创建物品，确保与商店购买的物品完全一致
                val item = sky4th.dungeon.loadout.shop.LoadoutShopAPI.createPurchasedItem(plugin, shopConfig)
                item.amount = amount.coerceIn(1, item.maxStackSize)
                return item
            }
            // 如果找不到商店配置，回退到原有逻辑
            val item = LootItemFactory.createItem(loot, plugin, configManager, amount)
            return item
        }

        // 否则使用原有逻辑创建物品
        val material = Material.matchMaterial(loot.material.uppercase()) ?: Material.PAPER
        val item = ItemStack(material, amount)
        val meta = item.itemMeta ?: return item

        val displayName = loot.name
        meta.displayName(LanguageAPI.toComponent(plugin, displayName))

        meta.persistentDataContainer.set(lootIdKey, PersistentDataType.STRING, loot.id)
        // 提取地牢名字（去掉实例ID，例如 "dungeon_name_1" -> "dungeon_name"）
        val dungeonName = dungeonId.substringBeforeLast("_")
        meta.persistentDataContainer.set(dungeonIdKey, PersistentDataType.STRING, dungeonName)

        // 构建lore列表，包含描述和价值
        val loreList = mutableListOf<String>()

        // 添加描述（如果有）
        if (loot.description.isNotBlank()) {
            loreList.add(loot.description)
        }
        loreList.add("")

        // 添加价值
        loreList.add(LanguageAPI.getText(plugin, "price-value", "value" to loot.price))

        meta.lore(loreList.map { LanguageAPI.toComponent(plugin, it) })

        item.itemMeta = meta
        return item
    }

    private fun selectTierByWeight(tierWeights: Map<String, Int>): String? {
        if (tierWeights.isEmpty()) return null
        val totalWeight = tierWeights.values.sum()
        if (totalWeight <= 0) return null
        var randomValue = random.nextInt(totalWeight)
        for ((tier, weight) in tierWeights) {
            randomValue -= weight
            if (randomValue < 0) return tier
        }
        return tierWeights.keys.firstOrNull()
    }
}
