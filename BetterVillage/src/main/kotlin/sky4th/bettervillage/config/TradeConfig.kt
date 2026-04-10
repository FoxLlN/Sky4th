package sky4th.bettervillage.config

import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import sky4th.bettervillage.BetterVillage
import java.util.logging.Level

/**
 * 交易配置数据类
 */
data class TradeConfigData(
    val cost: Material,
    val costAmount: Int,
    val cost2: Material? = null,     // 第二个交易物品（可选）
    val cost2Amount: Int = 1,        // 第二个交易物品的数量
    val result: Material,
    val resultAmount: Int,
    val maxUses: Int,
    val experience: Int = 0,
    val extraType: String? = null,  // 额外类型：enchantment(附魔)、potion(药水效果)、suspicious_stew(秘制炖菜效果)等
    val extraData: List<String>? = null   // 额外数据列表：附魔类型和等级、药水效果类型等，格式如 ["sharpness:5", "protection:3"]
)

/**
 * 村民职业交易配置
 */
data class ProfessionTradeConfig(
    val profession: String,
    val emeraldTrades: Map<Int, List<TradeConfigData>> = emptyMap(),
    val otherTrades: Map<Int, List<TradeConfigData>> = emptyMap()
)

/**
 * 交易配置管理器
 * 负责管理所有村民的交易配置
 */
object TradeConfig {

    private val plugin get() = BetterVillage.instance
    private val config get() = ConfigManager.getConfig()

    // 缓存所有职业的交易配置
    private val professionTrades = mutableMapOf<String, ProfessionTradeConfig>()

    /**
     * 初始化交易配置
     */
    fun initialize() {
        // 加载所有职业的交易配置
        loadProfessionTrades()
    }

    /**
     * 加载所有职业的交易配置
     */
    private fun loadProfessionTrades() {
        professionTrades.clear()

        val tradesSection = config.getConfigurationSection("trades") ?: run {
            return
        }

        tradesSection.getKeys(false).forEach { professionName ->
            val professionConfig = loadProfessionConfig(professionName)
            if (professionConfig != null) {
                professionTrades[professionName] = professionConfig
            } else {
                plugin.logger.warning("职业 $professionName 加载失败")
            }
        }
    }

    /**
     * 加载单个职业的交易配置
     */
    private fun loadProfessionConfig(professionName: String): ProfessionTradeConfig? {
        val professionPath = "trades.$professionName"
        val professionSection = config.getConfigurationSection(professionPath) ?: run {
            plugin.logger.warning("未找到职业配置: $professionName")
            return null
        }

        val emeraldTrades = mutableMapOf<Int, List<TradeConfigData>>()
        val otherTrades = mutableMapOf<Int, List<TradeConfigData>>()

        professionSection.getKeys(false).forEach { levelKey ->
            if (!levelKey.startsWith("level-")) return@forEach

            val level = levelKey.substringAfter("level-").toIntOrNull() ?: run {
                return@forEach
            }
            // 加载绿宝石交易
            val emeraldLevelTrades = loadLevelTrades("$professionPath.$levelKey.emerald-trades")
            if (emeraldLevelTrades.isNotEmpty()) {
                emeraldTrades[level] = emeraldLevelTrades
            }
            // 加载非绿宝石交易
            val otherLevelTrades = loadLevelTrades("$professionPath.$levelKey.other-trades")
            if (otherLevelTrades.isNotEmpty()) {
                otherTrades[level] = otherLevelTrades
            }
        }

        return ProfessionTradeConfig(professionName, emeraldTrades, otherTrades)
    }


    /**
     * 加载指定等级的交易配置
     */
    private fun loadLevelTrades(levelPath: String): List<TradeConfigData> {
        // 尝试直接获取Map列表
        val tradeList = config.getMapList(levelPath)
        
        if (tradeList.isNotEmpty()) {
            return tradeList.mapNotNull { tradeMap ->
                loadTradeFromMap(tradeMap)
            }
        }

        // 如果不是列表格式，则使用键值对格式
        val levelSection = config.getConfigurationSection(levelPath) ?: run {
            return emptyList()
        }
        val tradeKeys = levelSection.getKeys(false)
        return tradeKeys.mapNotNull { tradeKey ->
            loadTradeConfig("$levelPath.$tradeKey")
        }
    }

    /**
     * 从Map加载交易配置
     */
    private fun loadTradeFromMap(tradeMap: Map<*, *>): TradeConfigData? {
        return try {
            val costMaterial = Material.matchMaterial(tradeMap["cost"]?.toString() ?: return null)
            val resultMaterial = Material.matchMaterial(tradeMap["result"]?.toString() ?: return null)

            if (costMaterial == null || resultMaterial == null) {
                plugin.logger.warning("无效的材料配置: $tradeMap")
                return null
            }

            // 读取第二个cost物品（可选）
            val cost2Material = tradeMap["cost-2"]?.toString()?.let { Material.matchMaterial(it) }
            val cost2Amount = (tradeMap["cost-2-amount"] as? Number)?.toInt() ?: 1

            // 处理extraData，支持列表格式和字符串格式
            val extraDataList = when (val extraDataRaw = tradeMap["extra-data"]) {
                is List<*> -> extraDataRaw.mapNotNull { it?.toString() }
                is String -> listOf(extraDataRaw)  // 兼容旧的单字符串格式
                else -> null
            }

            TradeConfigData(
                cost = costMaterial,
                costAmount = (tradeMap["cost-amount"] as? Number)?.toInt() ?: 1,
                cost2 = cost2Material,
                cost2Amount = cost2Amount,
                result = resultMaterial,
                resultAmount = (tradeMap["result-amount"] as? Number)?.toInt() ?: 1,
                maxUses = (tradeMap["max-uses"] as? Number)?.toInt() ?: 12,
                experience = (tradeMap["experience"] as? Number)?.toInt() ?: 0,
                extraType = tradeMap["extra-type"]?.toString(),
                extraData = extraDataList
            )
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "加载交易配置失败: $tradeMap", e)
            null
        }
    }

    /**
     * 加载单个交易配置
     */
    private fun loadTradeConfig(tradePath: String): TradeConfigData? {
        val section = config.getConfigurationSection(tradePath) ?: return null

        return try {
            val costMaterial = Material.matchMaterial(section.getString("cost") ?: return null)
            val resultMaterial = Material.matchMaterial(section.getString("result") ?: return null)

            if (costMaterial == null || resultMaterial == null) {
                plugin.logger.warning("无效的材料配置: $tradePath")
                return null
            }

            // 读取第二个cost物品（可选）
            val cost2Material = section.getString("cost-2")?.let { Material.matchMaterial(it) }
            val cost2Amount = section.getInt("cost-2-amount", 1)

            // 处理extraData，支持列表格式和字符串格式
            val extraDataList = section.getStringList("extra-data").takeIf { it.isNotEmpty() }
                ?: section.getString("extra-data")?.let { listOf(it) }

            TradeConfigData(
                cost = costMaterial,
                costAmount = section.getInt("cost-amount", 1),
                cost2 = cost2Material,
                cost2Amount = cost2Amount,
                result = resultMaterial,
                resultAmount = section.getInt("result-amount", 1),
                maxUses = section.getInt("max-uses", 12),
                experience = section.getInt("experience", 0),
                extraType = section.getString("extra-type"),
                extraData = extraDataList
            )
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "加载交易配置失败: $tradePath", e)
            null
        }
    }

    /**
     * 获取指定职业和等级的绿宝石交易配置
     */
    fun getEmeraldTrades(profession: String, level: Int): List<TradeConfigData> {
        val professionConfig = professionTrades[profession]
        if (professionConfig == null) {
            return emptyList()
        }
        val trades = professionConfig.emeraldTrades[level]
        if (trades == null) {
            return emptyList()
        }
        return trades
    }

    /**
     * 获取指定职业和等级的非绿宝石交易配置
     */
    fun getOtherTrades(profession: String, level: Int): List<TradeConfigData> {
        val professionConfig = professionTrades[profession]
        if (professionConfig == null) {
            return emptyList()
        }
        val trades = professionConfig.otherTrades[level]
        if (trades == null) {
            return emptyList()
        }
        return trades
    }

    /**
     * 获取所有职业的交易配置
     */
    fun getAllProfessionTrades(): Map<String, ProfessionTradeConfig> = professionTrades.toMap()

    /**
     * 重新加载交易配置
     */
    fun reload() {
        loadProfessionTrades()
        plugin.logger.info("交易配置已重新加载")
    }
}
