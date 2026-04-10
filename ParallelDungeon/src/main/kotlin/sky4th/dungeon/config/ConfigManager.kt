package sky4th.dungeon.config

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.plugin.java.JavaPlugin
import sky4th.dungeon.loadout.LoadoutCategory
import sky4th.dungeon.loadout.LoadoutShopItemConfig
import sky4th.dungeon.loadout.equipment.ActualEquipmentConfig
import sky4th.dungeon.model.DungeonConfig
import sky4th.dungeon.monster.core.MonsterLevel
import sky4th.dungeon.monster.core.MonsterRegistry

/**
 * 配置管理器
 * 负责加载和管理所有配置信息
 */
class ConfigManager(
    private val plugin: JavaPlugin
) {
    private var config: FileConfiguration? = null


    /**
     * 加载配置文件
     */
    fun load() {
        plugin.saveDefaultConfig()
        config = plugin.config
    }

    /**
     * 重新加载配置文件
     */
    fun reload() {
        plugin.reloadConfig()
        config = plugin.config
    }

    // ==============================
    // 通用配置（从common节点）
    // ==============================

    // ==============================
    // UI头颅纹理配置
    // ==============================

    fun getCreditsHeadTexture(): String =
        config?.getString("common.ui-heads.credits") ?: ""

    fun getFullWalletHeadTexture(): String =
        config?.getString("common.ui-heads.full-wallet") ?: ""

    fun getEmptyWalletHeadTexture(): String =
        config?.getString("common.ui-heads.empty-wallet") ?: ""

    fun getEquipmentBackHeadTexture(): String =
        config?.getString("common.ui-heads.equipment-back") ?: ""

    fun getShopOpenHeadTexture(): String =
        config?.getString("common.ui-heads.shop-open") ?: ""

    fun getSellDefaultHeadTexture(): String =
        config?.getString("common.ui-heads.sell-default") ?: ""

    fun getSellConfirmHeadTexture(): String =
        config?.getString("common.ui-heads.sell-confirm") ?: ""

    fun getSafeSlotHeadTexture(): String =
        config?.getString("common.ui-heads.safe-slot") ?: ""
    
    fun getBackpackValueHeadTexture(): String =
        config?.getString("common.ui-heads.backpack-value") ?: ""


    /** 补给品携带上限 */
    val suppliesCarryLimit: Int
        get() {
            val raw = config?.getInt("common.supplies-carry-limit") ?: 0
            if (raw <= 0) return Int.MAX_VALUE
            return raw.coerceIn(1, 64)
        }

    /** 装配页可装备的补给品 id 列表 */
    fun getEquippableSupplyIds(): List<String> =
        (config?.getStringList("common.equippable-supplies") ?: emptyList()).filter { it.isNotBlank() }

    /** 模板忽略规则 */
    val templateIgnore: Set<String>
        get() {
            val list = config?.getStringList("common.template-ignore") ?: emptyList()
            return list.map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { it.replace("\\", "/").trim('/') }
                .toSet()
        }

    /** 容器物品数量范围配置列表 */
    val containerItemCounts: List<ContainerItemCountConfig>
        get() {
            val list = config?.getList("common.container-item-counts") ?: return emptyList()
            return list.mapNotNull { raw ->
                if (raw !is Map<*, *>) return@mapNotNull null
                val volumeRange = raw["volume-range"]?.toString()?.trim().orEmpty()
                val minItems = (raw["min-items"] as? Number)?.toInt()
                val maxItems = (raw["max-items"] as? Number)?.toInt()

                if (volumeRange.isEmpty() || minItems == null || maxItems == null) {
                    plugin.logger.warning("Invalid container item count config entry: $raw")
                    null
                } else {
                    ContainerItemCountConfig(volumeRange, minItems, maxItems)
                }
            }
        }

    /** 根据容器体积获取物品数量范围 */
    fun getItemCountRangeForVolume(volume: Int): Pair<Int, Int> {
        val config = containerItemCounts.firstOrNull { it.matches(volume) }
        return if (config != null) {
            config.minItems to config.maxItems
        } else {
            1 to 3
        }
    }

    /** 不同品级的基础搜索时间（秒） */
    val lootTierSearchSeconds: Map<String, Int>
        get() {
            val section = config?.getConfigurationSection("common.loot-tiers") ?: return emptyMap()
            return section.getKeys(false).associateWith { key ->
                section.getInt("$key.search-time", 5).coerceAtLeast(1)
            }
        }

    /** 按"容器等级"读取当前等级下各品级的掉落权重 */
    fun getLootTierWeightsForLevel(level: Int): Map<String, Int> {
        val section = config?.getConfigurationSection("common.loot-tiers") ?: return emptyMap()
        if (level <= 0) return emptyMap()

        val pathSuffix = "weight-level-$level"
        val result = mutableMapOf<String, Int>()

        for (tier in section.getKeys(false)) {
            val weight = section.getInt("$tier.$pathSuffix", 0)
            if (weight > 0) {
                result[tier] = weight
            }
        }

        return result
    }

    /** 斥候技能加成倍数 */
    val scoutQualityBoostMultipliers: Map<String, Double>
        get() {
            val section = config?.getConfigurationSection("common.scout-quality-boost") ?: return defaultScoutQualityBoostMultipliers
            val map = section.getKeys(false).associateWith { tier ->
                section.getDouble(tier, 1.0).coerceIn(0.0, 100.0)
            }
            return if (map.isEmpty()) defaultScoutQualityBoostMultipliers else map
        }

    private val defaultScoutQualityBoostMultipliers: Map<String, Double> = mapOf(
        "common" to 0.2,
        "uncommon" to 0.4,
        "rare" to 2.0,
        "epic" to 3.0,
        "legendary" to 4.0,
        "mythic" to 5.0
    )

    // ==============================
    // 配装商店 / 实际装备
    // ==============================

    fun getLoadoutShopItems(category: LoadoutCategory): List<LoadoutShopItemConfig> =
        LoadoutConfigLoader.getLoadoutShopItems(config, category, plugin)

    val allLoadoutShopItems: List<LoadoutShopItemConfig>
        get() = LoadoutConfigLoader.getAllLoadoutShopItems(config, plugin)

    fun getLoadoutShopItemById(id: String): LoadoutShopItemConfig? =
        LoadoutConfigLoader.getLoadoutShopItemById(config, id, plugin)

    fun getLoadoutCategoriesWithItems(): List<LoadoutCategory> =
        LoadoutConfigLoader.getLoadoutCategoriesWithItems(config, plugin)

    fun getActualEquipmentConfigList(loadoutId: String): List<ActualEquipmentConfig>? =
        LoadoutConfigLoader.getActualEquipmentConfigList(config, loadoutId, plugin)

    fun getArrowDisplayValue(loadoutId: String = "normal_arrow"): Int =
        getActualEquipmentConfigList(loadoutId)?.firstOrNull()?.sellPrice ?: 2


    // ==============================
    // 地牢配置（从dungeons节点）
    // ==============================

    /** 获取指定地牢的配置 */
    fun getDungeonConfig(dungeonId: String): DungeonConfig? =
        loadDungeonConfigs()[dungeonId]

    /** 加载所有地牢配置 */
    fun loadDungeonConfigs(): Map<String, DungeonConfig> {
        val result = mutableMapOf<String, DungeonConfig>()
        val dungeonsSection = config?.getConfigurationSection("dungeons") ?: return result


        for (dungeonId in dungeonsSection.getKeys(false)) {
            val dungeonConfig = loadSingleDungeonConfig(dungeonId)
            if (dungeonConfig != null) {
                result[dungeonId] = dungeonConfig
            } 
        }

        return result
    }

    /** 加载单个地牢配置 */
    private fun loadSingleDungeonConfig(dungeonId: String): DungeonConfig? {
        val section = config?.getConfigurationSection("dungeons.$dungeonId") ?: return null
        val dungeonConfig = config ?: return null

        return DungeonConfig(
            id = dungeonId,
            displayName = section.getString("world-name", dungeonId) ?: dungeonId,
            templatePath = section.getString("template-world-path", "${dungeonId}_template") ?: "${dungeonId}_template",
            cost = section.getDouble("cost", 0.0),
            maxInstances = section.getInt("max-instances", 1),
            maxPlayersPerInstance = section.getInt("max-players-per-instance", 10),
            durationMinutes = section.getInt("duration-minutes", 0),
            spawnPoints = loadSpawnPoints(section),
            exitRegions = loadExitRegions(section),
            containers = loadContainers(section),
            monsterSpawns = loadMonsterSpawns(section),
            techLevelBonuses = loadTechLevelBonuses(section)
        )
    }

    /** 加载出生点列表 */
    private fun loadSpawnPoints(section: ConfigurationSection): List<SpawnPoint> {
        val points = section.getMapList("spawn-points")
        return points.mapNotNull { point ->
            SpawnPoint(
                (point["x"] as? Number)?.toDouble() ?: 0.0,
                (point["y"] as? Number)?.toDouble() ?: 100.0,
                (point["z"] as? Number)?.toDouble() ?: 0.0,
                point["name"]?.toString()?.trim().orEmpty()
            )
        }
    }

    /** 加载撤离区域列表 */
    private fun loadExitRegions(section: ConfigurationSection): List<Region> {
        val list = section.getMapList("exit-regions")
        val worldName = section.getString("id", "") ?: ""
        return list.mapNotNull { raw ->
            val minX = (raw["min-x"] as? Number)?.toInt()
            val minY = (raw["min-y"] as? Number)?.toInt()
            val minZ = (raw["min-z"] as? Number)?.toInt()
            val maxX = (raw["max-x"] as? Number)?.toInt()
            val maxY = (raw["max-y"] as? Number)?.toInt()
            val maxZ = (raw["max-z"] as? Number)?.toInt()
            // 不硬编码世界名称，只存储地牢ID，实际世界名称在实例创建时动态匹配
            val regionWorld = raw["world"]?.toString()?.trim() ?: worldName
            val name = raw["name"]?.toString()?.trim().orEmpty()

            if (minX == null || minY == null || minZ == null || maxX == null || maxY == null || maxZ == null) {
                plugin.logger.warning("Invalid exit-region entry: $raw")
                null
            } else {
                Region(minX, minY, minZ, maxX, maxY, maxZ, regionWorld, name)
            }
        }
    }

    /** 加载容器配置列表 */
    private fun loadContainers(section: ConfigurationSection): List<ContainerConfig> {
        val list = section.getMapList("containers") 
        val worldName = section.getString("id", "") ?: ""
        return list.mapNotNull { raw ->
            val id = raw["id"]?.toString()?.trim().orEmpty()
            val minX = (raw["min-x"] as? Number)?.toInt()
            val minY = (raw["min-y"] as? Number)?.toInt()
            val minZ = (raw["min-z"] as? Number)?.toInt()
            val maxX = (raw["max-x"] as? Number)?.toInt()
            val maxY = (raw["max-y"] as? Number)?.toInt()
            val maxZ = (raw["max-z"] as? Number)?.toInt()
            val level = (raw["level"] as? Number)?.toInt() ?: 1

            if (id.isEmpty() || minX == null || minY == null || minZ == null || maxX == null || maxY == null || maxZ == null) {
                plugin.logger.warning("Invalid container config entry: $raw")
                null
            } else {
                // 不硬编码世界名称，只存储地牢ID，实际世界名称在实例创建时动态匹配
                val region = Region(minX, minY, minZ, maxX, maxY, maxZ, worldName)
                val name = raw["name"]?.toString() ?: ""
                val texture = raw["texture"]?.toString() ?: ""
                ContainerConfig(id, region, level, name, texture)
            }
        }
    }

    /** 加载怪物生成点配置列表 */
    private fun loadMonsterSpawns(section: ConfigurationSection): List<MonsterSpawnConfig> {
        val list = section.getMapList("monster-spawns")
        return list.mapNotNull { raw ->
            val id = raw["id"]?.toString()?.trim().orEmpty()
            val monsterId = raw["monster-id"]?.toString()?.trim().orEmpty()
            val x = (raw["x"] as? Number)?.toDouble()
            val y = (raw["y"] as? Number)?.toDouble()
            val z = (raw["z"] as? Number)?.toDouble()
            val count = (raw["count"] as? Number)?.toInt()?.coerceAtLeast(1) ?: 1
            val name = raw["name"]?.toString()?.trim() ?: ""

            if (id.isEmpty() || monsterId.isEmpty() || x == null || y == null || z == null) {
                plugin.logger.warning("Invalid monster spawn config entry: $raw")
                null
            } else {
                MonsterSpawnConfig(id, monsterId, x, y, z, count, name)
            }
        }
    }

    /** 加载科技树等级加成 */
    private fun loadTechLevelBonuses(section: ConfigurationSection): Map<Int, TechLevelBonus> {
        val result = mutableMapOf<Int, TechLevelBonus>()
        val bonusesSection = section.getConfigurationSection("tech-level-bonuses") ?: return result

        for (levelKey in bonusesSection.getKeys(false)) {
            val level = levelKey.toIntOrNull() ?: continue
            val bonusSection = bonusesSection.getConfigurationSection(levelKey) ?: continue
            val health = bonusSection.getInt("health", 0)
            val damageReduction = bonusSection.getDouble("player-damage-reduction", 0.0)
            result[level] = TechLevelBonus(health, damageReduction)
        }
        return result
    }

    /** 获取地牢的撤离等待秒数 */
    fun getExitWaitSeconds(dungeonId: String): Int {
        val section = config?.getConfigurationSection("dungeons.$dungeonId") ?: return 10
        return section.getInt("exit-wait-seconds", 10)
    }

    // ==============================
    // 怪物掉落物表
    // ==============================

    /** 获取所有怪物掉落物表 */
    fun getMonsterLootTables(dungeonId: String): Map<MonsterLevel, List<MonsterLootItem>> {
        val section = config?.getConfigurationSection("dungeons.$dungeonId.monster-loot-tables") ?: return emptyMap()
        val result = mutableMapOf<MonsterLevel, List<MonsterLootItem>>()

        for (levelKey in section.getKeys(false)) {
            val level = try {
                MonsterLevel.valueOf(levelKey.uppercase())
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("Invalid monster level in config: $levelKey")
                continue
            }
            val dropsSection = section.getConfigurationSection("$levelKey.drops") ?: continue

            val drops: List<MonsterLootItem> = dropsSection.getKeys(false).mapNotNull { dropKey ->
                val dropSection = dropsSection.getConfigurationSection(dropKey) ?: return@mapNotNull null
                val itemId = dropSection.getString("item-id") ?: return@mapNotNull null
                val chance = dropSection.getDouble("chance", 0.0)

                MonsterLootItem(itemId, chance)
            }

            result[level] = drops
        }

        return result
    }

    /** 根据怪物等级获取掉落物表 */
    fun getMonsterLootTable(dungeonId: String, level: MonsterLevel): List<MonsterLootItem> {
        return getMonsterLootTables(dungeonId)[level] ?: emptyList()
    }

    /** 根据怪物ID获取怪物等级 */
    fun getMonsterLevel(dungeonId: String, monsterId: String): MonsterLevel? {
        val dungeonConfig = config ?: return null
        val spawns = loadMonsterSpawns(dungeonConfig)
        val spawnConfig = spawns.firstOrNull { it.monsterId == monsterId }
        if (spawnConfig != null) {
            val monsterDefinition = MonsterRegistry.getDefinition(spawnConfig.monsterId)
            return monsterDefinition?.level
        }
        return null
    }

    // ==============================
    // 现金物品配置
    // ==============================

    /** 获取地牢的现金物品配置 */
    fun getCashItems(): List<CashItemConfig> {
        val list = config?.getMapList("common.cash-items") ?: return emptyList()
        return list.mapNotNull { raw ->
            val id = raw["id"]?.toString()?.trim().orEmpty()
            val name = raw["name"]?.toString()?.trim().orEmpty()
            val description = raw["description"]?.toString()?.trim().orEmpty()
            val texture = raw["texture"]?.toString()?.trim().orEmpty()
            val minCredits = (raw["min-credits"] as? Number)?.toInt() ?: 0
            val maxCredits = (raw["max-credits"] as? Number)?.toInt() ?: 0

            if (id.isEmpty() || name.isEmpty() || texture.isEmpty()) {
                plugin.logger.warning("Invalid cash item config entry: $raw")
                null
            } else {
                CashItemConfig(id, name, description, texture, minCredits, maxCredits)
            }
        }
    }

    /** 根据物品ID获取现金物品配置 */
    fun getCashItemById(dungeonId: String, id: String): CashItemConfig? =
        getCashItems().firstOrNull { it.id == id }

    // ==============================
    // 怪物头颅配置
    // ==============================

    /** 获取地牢的怪物头颅配置 */
    fun getMonsterHeads(dungeonId: String): List<MonsterHeadConfig> {
        val list = config?.getList("dungeons.$dungeonId.monster-heads") ?: return emptyList()
        return list.mapNotNull { raw ->
            if (raw !is Map<*, *>) return@mapNotNull null
            val monsterId = raw["monster-id"]?.toString()?.trim().orEmpty()
            val headType = raw["head-type"]?.toString()?.trim().orEmpty()
            val material = raw["material"]?.toString()?.trim().orEmpty()
            val texture = raw["texture"]?.toString()?.trim().orEmpty()
            val displayName = raw["display-name"]?.toString()?.trim().orEmpty()

            if (monsterId.isEmpty() || displayName.isEmpty()) {
                plugin.logger.warning("Invalid monster head config entry: $raw")
                null
            } else {
                MonsterHeadConfig(monsterId, headType, material, texture, displayName)
            }
        }
    }

    /** 可随机掉落的物品列表（按品级分组读取） */
    fun getLootItems(dungeonId: String): List<LootItemConfig> {
        val section = config?.getConfigurationSection("dungeons.$dungeonId.loot-items") ?: return emptyList()
        val items = mutableListOf<LootItemConfig>()

        for (tier in section.getKeys(false)) {
            val tierList = section.getList(tier) ?: continue

            for (raw in tierList) {
                if (raw !is Map<*, *>) continue
                val id = raw["id"]?.toString()?.trim().orEmpty()
                val name = raw["name"]?.toString()?.trim().orEmpty()
                val description = raw["description"]?.toString()?.trim().orEmpty()
                val descriptionLore = (raw["description-lore"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                val price = (raw["price"] as? Number)?.toInt() ?: 0
                val material = raw["material"]?.toString()?.trim().orEmpty()
                val isshop = raw["isshop"] as? Boolean ?: false
                val minAmount = (raw["min-amount"] as? Number)?.toInt() ?: 1
                val maxAmount = (raw["max-amount"] as? Number)?.toInt() ?: 1
                val cashId = raw["cash-id"]?.toString()?.trim().orEmpty()

                if (cashId.isEmpty()){
                    if (id.isEmpty() || name.isEmpty() || material.isEmpty()) {
                        plugin.logger.warning("Invalid loot item config entry in tier '$tier': $raw")
                        continue
                    }
                }

                items.add(LootItemConfig(id, name, description, descriptionLore, tier, price, material, isshop, minAmount, maxAmount, cashId))
            }
        }

        return items
    }

    /** 根据物品 ID 获取掉落物配置 */
    fun getLootItemById(dungeonId: String, id: String): LootItemConfig? =
        getLootItems(dungeonId).firstOrNull { it.id == id }

}
