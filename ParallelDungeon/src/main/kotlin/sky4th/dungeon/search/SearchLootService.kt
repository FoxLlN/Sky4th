package sky4th.dungeon.search

import sky4th.dungeon.config.ConfigManager
import sky4th.dungeon.config.LootItemConfig
import sky4th.dungeon.config.MonsterLootItem
import sky4th.dungeon.monster.core.MonsterLevel
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import kotlin.random.Random

/**
 * 通用搜索掉落物服务：按权重随机掉落、创建带品级/价值的物品、发放给玩家。
 *
 * 这个服务使用ContainerLootService的物品创建逻辑，确保容器和怪物头颅生成的物品完全一致。
 */
class SearchLootService(
    private val plugin: JavaPlugin,
    private val configManager: ConfigManager,
    private val containerLootService: sky4th.dungeon.container.ContainerLootService
) {
    private val random = Random.Default

    /**
     * 从配置中随机一个物品，并根据其品级获取对应的读条时间。
     * 直接使用ContainerLootService的方法，确保逻辑一致。
     * @param dungeonId 地牢ID
     * @param containerLevel 容器等级，用于按等级权重；null 或 0 时使用全局权重或完全随机
     * @param qualityBoost 斥候技能：是否提升高品质物品概率（降低 common/uncommon 权重，提高 rare+ 权重）
     * @param debugPlayer 非 null 时向该玩家发送当前各品级权重的调试信息
     */
    fun pickRandomLoot(dungeonId: String, containerLevel: Int? = null, qualityBoost: Boolean = false, debugPlayer: Player? = null): Pair<LootItemConfig, Int>? {
        return containerLootService.pickRandomLoot(dungeonId, containerLevel, qualityBoost, debugPlayer)
    }

    /**
     * 从怪物掉落物表中随机选择物品
     * @param dungeonId 地牢ID
     * @param monsterLevel 怪物等级
     * @param debugPlayer 非 null 时向该玩家发送调试信息
     */
    fun pickRandomMonsterLoot(dungeonId: String, monsterLevel: MonsterLevel, debugPlayer: Player? = null): LootItemConfig? {
        val lootTable = configManager.getMonsterLootTable(dungeonId, monsterLevel)
        if (lootTable.isEmpty()) return null

        // 根据概率随机选择物品
        val selectedLoot = selectLootByChance(lootTable) ?: return null

        // 根据物品ID获取物品配置
        val lootItem = configManager.getLootItemById(dungeonId, selectedLoot.itemId)

        return lootItem
    }

    /**
     * 按稀有度随机一个物品；tier 为 null 时按默认权重随机。
     * 直接使用ContainerLootService的方法，确保逻辑一致。
     * @param dungeonId 地牢ID
     */
    fun pickRandomLootByTier(dungeonId: String, tier: String?): LootItemConfig? {
        return containerLootService.pickRandomLootByTier(dungeonId, tier)
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
     * 根据配置创建带描述、价值的物品栈。
     * @param dungeonId 地牢ID
     * 直接使用ContainerLootService的方法，确保物品生成逻辑一致。
     */
    fun createItemStack(dungeonId: String, loot: LootItemConfig): ItemStack {
        return containerLootService.createItemStack(dungeonId, loot)
    }

    /**
     * 根据概率从怪物掉落物表中选择物品
     */
    private fun selectLootByChance(lootTable: List<MonsterLootItem>): MonsterLootItem? {
        // 过滤掉概率为0的物品
        val validItems = lootTable.filter { it.chance > 0 }
        if (validItems.isEmpty()) return null

        // 按概率随机选择
        val randomValue = random.nextDouble()
        var cumulativeChance = 0.0

        for (loot in validItems) {
            cumulativeChance += loot.chance
            if (randomValue <= cumulativeChance) {
                return loot
            }
        }

        // 如果没有选中任何物品，返回第一个有效物品
        return validItems.firstOrNull()
    }
}
