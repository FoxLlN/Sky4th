package sky4th.infrastructure

import sky4th.core.api.SkyPlugin

/**
 * Infrastructure - Sky4th 基建系统
 * 
 * 负责处理建筑、基础设施、区域管理等功能
 */
class Infrastructure : SkyPlugin() {
    
    override fun onSkyEnable() {
        logger.info("Infrastructure 正在启动...")
        
        // TODO: 初始化基建系统
        // - 加载配置文件
        // - 初始化区域管理器
        // - 注册命令
        // - 注册事件监听器
        
        logger.info("Infrastructure 已成功启动！")
    }
    
    override fun onSkyDisable() {
        logger.info("Infrastructure 正在关闭...")
        
        // TODO: 保存数据
        // - 保存区域数据
        // - 保存建筑数据
        
        logger.info("Infrastructure 已关闭")
    }
}
