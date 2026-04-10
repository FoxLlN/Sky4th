package sky4th.economy

import org.bukkit.plugin.java.JavaPlugin

/**
 * CreditEconomy - Sky4th 经济系统
 * 
 * 负责处理游戏内的经济系统，包括货币、交易等功能
 */
class CreditEconomy : JavaPlugin() {
    
    override fun onSkyEnable() {
        logger.info("CreditEconomy 正在启动...")
        
        // TODO: 初始化经济系统
        // - 加载配置文件
        // - 初始化数据库/存储
        // - 注册命令
        // - 注册事件监听器
        
        logger.info("CreditEconomy 已成功启动！")
    }
    
    override fun onSkyDisable() {
        logger.info("CreditEconomy 正在关闭...")
        
        // TODO: 保存数据
        // - 保存玩家经济数据
        // - 关闭数据库连接
        
        logger.info("CreditEconomy 已关闭")
    }
}
