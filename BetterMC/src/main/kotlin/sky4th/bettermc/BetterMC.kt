package sky4th.bettermc

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import sky4th.bettermc.config.ConfigManager
import sky4th.bettermc.command.BetterMCCommandHandler
import sky4th.bettermc.command.BetterMCContext
import sky4th.bettermc.command.FeatureManager
import sky4th.bettermc.drops.DropListener
import sky4th.bettermc.listeners.VehicleListener
import sky4th.bettermc.listeners.PortalListener
import sky4th.bettermc.listeners.EndermanListener
import sky4th.bettermc.listeners.GrowthListener
import sky4th.bettermc.listeners.PlayerKillListener
import sky4th.bettermc.listeners.AnimalPanicListener
import sky4th.bettermc.listeners.IronGolemListener
import sky4th.bettermc.listeners.VillagerIronGolemListener
import sky4th.bettermc.listeners.FertilizeListener
import sky4th.bettermc.listeners.EnderPearlBlocker
import sky4th.bettermc.listeners.CropHarvestListener
import sky4th.bettermc.listeners.WaterflowImmunityListener
import sky4th.bettermc.listeners.PlayerHitboxListener
import sky4th.bettermc.listeners.GrassAttackListener
import sky4th.bettermc.time.TimeSkipListener
import sky4th.bettermc.time.TimeTickTask
import sky4th.core.api.LanguageAPI

/**
 * BetterMC插件主类
 * 
 * 这是一个原版改革插件
 */
class BetterMC : JavaPlugin() {

    companion object {
        lateinit var instance: BetterMC
            private set
    }

    override fun onEnable() {
        instance = this

        // 初始化命令上下文
        BetterMCContext.init(this)

        // 初始化语言管理器
        LanguageAPI.getLanguageManager(this)

        // 初始化配置
        ConfigManager.init(this)

        // 注册命令处理器
        getCommand("bettermc")?.setExecutor(BetterMCCommandHandler)
        getCommand("bettermc")?.setTabCompleter(BetterMCCommandHandler)

        // 注册交通工具监听器
        server.pluginManager.registerEvents(VehicleListener(), this)

        // 注册传送门监听器
        server.pluginManager.registerEvents(PortalListener(), this)

        // 注册末影人监听器
        server.pluginManager.registerEvents(EndermanListener(), this)

        // 注册成长监听器
        val growthListener = GrowthListener()
        growthListener.initCooldownKey(this)
        server.pluginManager.registerEvents(growthListener, this)

        // 注册掉落物监听器
        server.pluginManager.registerEvents(DropListener(), this)

        // 注册玩家击杀监听器
        server.pluginManager.registerEvents(PlayerKillListener(), this)

        // 注册动物惊慌监听器
        server.pluginManager.registerEvents(AnimalPanicListener(), this)

        // 注册铁傀儡改进监听器
        server.pluginManager.registerEvents(IronGolemListener(), this)

        // 注册村民铁傀儡生成监听器
        server.pluginManager.registerEvents(VillagerIronGolemListener(), this)

        // 注册夜间管理相关监听器
        server.pluginManager.registerEvents(TimeSkipListener(), this)

        // 注册骨粉催熟监听器
        server.pluginManager.registerEvents(FertilizeListener(), this)

        // 注册末影珍珠传送限制监听器
        server.pluginManager.registerEvents(EnderPearlBlocker(), this)

        // 注册作物收割监听器
        server.pluginManager.registerEvents(CropHarvestListener(), this)
        
        // 注册水流动免疫监听器
        server.pluginManager.registerEvents(WaterflowImmunityListener(), this)

        // 注册玩家部位受击监听器
        server.pluginManager.registerEvents(PlayerHitboxListener(), this)

        // 注册穿草攻击监听器
        server.pluginManager.registerEvents(GrassAttackListener(), this)

        // 启动时间循环任务
        TimeTickTask().runTaskTimer(this, 1L, 1L)

        logger.info(LanguageAPI.getText(this, "plugin.enabled"))
    }

    override fun onDisable() {
        logger.info(LanguageAPI.getText(this, "plugin.disabled"))
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        return BetterMCCommandHandler.onCommand(sender, command, label, args)
    }
}
