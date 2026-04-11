package sky4th.economy.config

import org.bukkit.configuration.ConfigurationSection
import sky4th.economy.CreditEconomy

/**
 * 经济系统配置管理
 * 
 * 负责读取和管理经济系统的配置数据
 */
object EconomyConfig {

    private val plugin: CreditEconomy by lazy { CreditEconomy.instance }
    private val config by lazy { plugin.config }

    /**
     * 重新加载配置文件
     */
    fun reload() {
        plugin.reloadConfig()
    }

    // ========== 存活时长扣费配置 ==========

    /**
     * 获取新人免费时长（小时）
     */
    fun getFreeHours(): Int {
        return config.getInt("survival.free-hours", 9)
    }

    /**
     * 扣费比例配置
     */
    data class ChargeRateConfig(
        val min: Int,
        val max: Int,  // -1 表示无上限
        val rate: Double
    )

    /**
     * 获取扣费比例配置列表
     */
    fun getChargeRates(): List<ChargeRateConfig> {
        val list = mutableListOf<ChargeRateConfig>()
        val ratesList = config.getMapList("survival.charge-rates")
        for (item in ratesList) {
            val min = (item["min"] as? Number)?.toInt() ?: continue
            val max = (item["max"] as? Number)?.toInt() ?: continue
            val rate = (item["rate"] as? Number)?.toDouble() ?: continue
            list.add(ChargeRateConfig(min, max, rate))
        }
        return list
    }

    /**
     * 根据存活小时数获取扣费比例
     */
    fun getChargeRate(hours: Int): Double {
        val rates = getChargeRates()
        for (rateConfig in rates) {
            if (hours >= rateConfig.min && (rateConfig.max == -1 || hours <= rateConfig.max)) {
                return rateConfig.rate
            }
        }
        return 0.0
    }

    /**
     * 每分钟费用配置
     */
    data class MinuteRateConfig(
        val min: Int,
        val max: Int,  // -1 表示无上限
        val rate: Double
    )

    /**
     * 获取每分钟费用配置列表
     */
    fun getMinuteRates(): List<MinuteRateConfig> {
        val list = mutableListOf<MinuteRateConfig>()
        val ratesList = config.getMapList("survival.minute-rates")
        for (item in ratesList) {
            val min = (item["min"] as? Number)?.toInt() ?: continue
            val max = (item["max"] as? Number)?.toInt() ?: continue
            val rate = (item["rate"] as? Number)?.toDouble() ?: continue
            list.add(MinuteRateConfig(min, max, rate))
        }
        return list
    }

    /**
     * 根据存活小时数获取每分钟费用
     */
    fun getMinuteRate(hours: Int): Double {
        val rates = getMinuteRates()
        for (rateConfig in rates) {
            if (hours >= rateConfig.min && (rateConfig.max == -1 || hours < rateConfig.max)) {
                return rateConfig.rate
            }
        }
        return 1.0  // 默认每分钟1信用点
    }

    /**
     * 检查是否应该扣费
     */
    fun shouldCharge(hours: Int): Boolean {
        return hours > getFreeHours()
    }

    // ========== 每日时长奖励配置 ==========

    /**
     * 每日奖励配置
     */
    data class DailyRewardConfig(
        val hour: Int,
        val amount: Double
    )

    /**
     * 获取每日奖励配置列表
     */
    fun getDailyRewards(): List<DailyRewardConfig> {
        val list = mutableListOf<DailyRewardConfig>()
        val ratesList = config.getMapList("daily-rewards.rewards")
        for (item in ratesList) {
            val hour = (item["hour"] as? Number)?.toInt() ?: continue
            val amount = (item["amount"] as? Number)?.toDouble() ?: continue
            list.add(DailyRewardConfig(hour, amount))
        }
        return list
    }
    

    /**
     * 根据小时数获取奖励金额
     */
    fun getDailyReward(hours: Int): Double {
        val rewards = getDailyRewards()
        for (reward in rewards) {
            if (reward.hour == hours) {
                return reward.amount
            }
        }
        return 0.0
    }

    /**
     * 检查是否有奖励
     */
    fun hasDailyReward(hours: Int): Boolean {
        return getDailyReward(hours) > 0
    }

}
