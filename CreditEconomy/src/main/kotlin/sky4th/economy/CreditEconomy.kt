package sky4th.economy

import org.bukkit.plugin.java.JavaPlugin
import sky4th.economy.listener.PlayTimeEconomyListener
import sky4th.economy.task.SurvivalTimeChargeTask

/**
 * CreditEconomy - Sky4th 经济系统
 * 
 * 负责处理游戏内的经济系统，包括货币、交易等功能
 */
class CreditEconomy : JavaPlugin() {

    companion object {
        lateinit var instance: CreditEconomy
            private set
    }
    
    override fun onEnable() {
        instance = this
        logger.info("CreditEconomy 正在启动...")

        // 保存默认配置文件
        saveDefaultConfig()
        
        // 注册事件监听器
        server.pluginManager.registerEvents(PlayTimeEconomyListener(), this)

        // 启动每分钟扣费任务
        SurvivalTimeChargeTask.start(this)
        
        logger.info("CreditEconomy 已成功启动！")
    }
    
    override fun onDisable() {
        logger.info("CreditEconomy 正在关闭...")
        
        logger.info("CreditEconomy 已关闭")
    }
}
