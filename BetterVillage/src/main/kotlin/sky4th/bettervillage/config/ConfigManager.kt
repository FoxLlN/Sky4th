package sky4th.bettervillage.config

import org.bukkit.configuration.file.FileConfiguration
import sky4th.bettervillage.BetterVillage
import java.util.logging.Level

/**
 * 配置管理器
 * 负责管理插件的配置文件
 */
object ConfigManager {

    private val plugin get() = BetterVillage.instance

    /**
     * 初始化配置管理器
     */
    fun initialize() {
        // 保存默认配置
        plugin.saveDefaultConfig()

        plugin.logger.info("配置管理器已初始化")
    }

    /**
     * 重新加载配置
     */
    fun reload() {
        plugin.reloadConfig()
        // 重新加载交易配置
        TradeConfig.reload()

        plugin.logger.info("配置已重新加载")
    }


    /**
     * 获取配置文件
     */
    fun getConfig(): FileConfiguration {
        return plugin.config
    }

    /**
     * 获取村庄基础人数
     * 即使面积很小也有的基础人数
     * @return 基础人数
     */
    fun getBasePopulation(): Int {
        return getConfig().getInt("villager.base_population", 5)
    }

    /**
     * 获取每单位面积增加的村民数量
     * 每200平方米增加1个村民（可配置）
     * @return 单位面积
     */
    fun getPopulationPerArea(): Int {
        return getConfig().getInt("villager.population_per_area", 200)
    }

    /**
     * 获取击杀村民后的交易禁令时长（毫秒）
     * @return 禁令时长（毫秒）
     */
    fun getVillagerKillTradeBanDuration(): Long {
        val minutes = getConfig().getInt("protection.trade-ban.villager-kill", 10)
        return minutes * 60 * 1000L
    }

    /**
     * 获取击杀铁傀儡后的交易禁令时长（毫秒）
     * @return 禁令时长（毫秒）
     */
    fun getIronGolemKillTradeBanDuration(): Long {
        val minutes = getConfig().getInt("protection.trade-ban.iron-golem-kill", 5)
        return minutes * 60 * 1000L
    }

    /**
     * 获取是否启用村民移动限制
     * @return 是否启用
     */
    fun isVillagerMovementRestricted(): Boolean {
        return getConfig().getBoolean("villager.restrict_movement", true)
    }

    /**
     * 获取村民检查间隔（tick）
     * @return 检查间隔
     */
    fun getCheckInterval(): Int {
        return getConfig().getInt("villager.check_interval", 20)
    }
}
