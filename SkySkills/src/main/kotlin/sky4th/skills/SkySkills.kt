package sky4th.skills

import sky4th.core.api.SkyPlugin

/**
 * SkySkills - Sky4th 技能盘系统
 * 
 * 负责处理玩家技能、天赋、技能树等功能
 */
class SkySkills : SkyPlugin() {
    
    override fun onSkyEnable() {
        logger.info("SkySkills 正在启动...")
        
        // TODO: 初始化技能系统
        // - 加载配置文件
        // - 初始化技能管理器
        // - 注册命令
        // - 注册事件监听器
        
        logger.info("SkySkills 已成功启动！")
    }
    
    override fun onSkyDisable() {
        logger.info("SkySkills 正在关闭...")
        
        // TODO: 保存数据
        // - 保存玩家技能数据
        // - 保存技能配置
        
        logger.info("SkySkills 已关闭")
    }
}
