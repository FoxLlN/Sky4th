package sky4th.bettervillage

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.NamespacedKey
import org.bukkit.plugin.java.JavaPlugin
import sky4th.bettervillage.command.VillageCommandHandler
import sky4th.bettervillage.command.VillageContext
import sky4th.bettervillage.listeners.RaidListener
import sky4th.bettervillage.listeners.VillagerCustomizer
import sky4th.bettervillage.listeners.VillagerProfessionLockListener
import sky4th.bettervillage.listeners.VillagerEventListener
import sky4th.bettervillage.listeners.VillageBlockProtectionListener
import sky4th.bettervillage.listeners.VillagerTransportListener
import sky4th.bettervillage.listeners.VillagerSummonIronGolemListener
import sky4th.bettervillage.manager.VillageManager
import sky4th.bettervillage.manager.VillagerBoundaryChecker
import sky4th.bettervillage.config.ConfigManager
import sky4th.bettervillage.config.TradeConfig
import sky4th.bettervillage.util.VillageStructureChecker
import sky4th.core.SkyCore

/**
 * BetterVillage插件主类
 *
 * 这是一个原版改革插件
 */
class BetterVillage : JavaPlugin() {

    companion object {
        lateinit var instance: BetterVillage
            private set

        /**
         * 创建命名空间键
         * @param key 键名
         * @return NamespacedKey对象
         */
        fun namespacedKey(key: String): NamespacedKey {
            return NamespacedKey(instance, key)
        }
    }

    // 缓存清理任务ID
    private var cacheCleanupTaskId: Int = -1

    // 村庄方块保护监听器引用
    private lateinit var villageBlockProtectionListener: VillageBlockProtectionListener

    // 村民铁傀儡监听器引用
    private lateinit var villagerSummonIronGolemListener: VillagerSummonIronGolemListener

    // 村民事件监听器引用
    private lateinit var villagerEventListener: VillagerEventListener

    override fun onEnable() {
        instance = this

        // 检查 SkyCore 是否可用
        if (!SkyCore.isInitialized()) {
            logger.severe("SkyCore 未初始化，BetterVillage 将无法正常工作！")
            server.pluginManager.disablePlugin(this)
            return
        }

        // 检查村庄服务是否可用
        if (!SkyCore.isVillageServiceAvailable()) {
            logger.warning("村庄服务不可用，部分功能可能无法正常工作")
        }

        // 初始化配置管理器
        ConfigManager.initialize()

        // 初始化交易配置
        TradeConfig.initialize()

        // 初始化村庄管理器
        VillageManager.initialize()

        // 注册劫掠监听器
        server.pluginManager.registerEvents(RaidListener(), this)

        // 注册村庄结构检测器
        server.pluginManager.registerEvents(VillageStructureChecker, this)

        // 注册村民自定义交易监听器
        server.pluginManager.registerEvents(VillagerCustomizer(), this)

        // 注册村民职业锁定监听器
        server.pluginManager.registerEvents(VillagerProfessionLockListener(), this)

        // 注册村民事件监听器
        villagerEventListener = VillagerEventListener()
        server.pluginManager.registerEvents(villagerEventListener, this)

        // 注册村庄方块保护监听器
        villageBlockProtectionListener = VillageBlockProtectionListener()
        server.pluginManager.registerEvents(villageBlockProtectionListener, this)

        // 注册村民搬运监听器
        server.pluginManager.registerEvents(VillagerTransportListener(), this)

        // 注册村民铁傀儡监听器
        villagerSummonIronGolemListener = VillagerSummonIronGolemListener()
        server.pluginManager.registerEvents(villagerSummonIronGolemListener, this)

        // 初始化命令上下文
        VillageContext.init(this)

        // 注册村庄命令
        getCommand("village")?.setExecutor(VillageCommandHandler)

        // 启动定时清理缓存任务（每10分钟执行一次）
        startCacheCleanupTask()

        // 启动村民边界检查任务
        if (ConfigManager.isVillagerMovementRestricted()) {
            VillagerBoundaryChecker.start()
        }

        logger.info("BetterVillage插件已启用")
    }

    /**
     * 启动定时清理缓存任务
     */
    private fun startCacheCleanupTask() {
        // 每10分钟（12000 ticks）清理一次缓存
        cacheCleanupTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(
            this,
            { VillageStructureChecker.cleanupCache() },
            6000L, // 延迟5分钟后开始
            12000L // 每10分钟执行一次
        )
    }

    override fun onDisable() {
        // 清理所有仇恨和临时标记
        if (::villageBlockProtectionListener.isInitialized) {
            villageBlockProtectionListener.cleanupAll()
        }

        // 清理村民铁傀儡监听器的仇恨
        if (::villagerSummonIronGolemListener.isInitialized) {
            villagerSummonIronGolemListener.cleanupAll()
        }

        // 清理村民事件监听器的缓存
        if (::villagerEventListener.isInitialized) {
            villagerEventListener.cleanupAll()
        }

        // 取消缓存清理任务
        if (cacheCleanupTaskId != -1) {
            Bukkit.getScheduler().cancelTask(cacheCleanupTaskId)
            cacheCleanupTaskId = -1
        }

        // 停止村民边界检查任务
        VillagerBoundaryChecker.stop()

        // 关闭村庄管理器,停止所有异步任务
        VillageManager.shutdown()
        logger.info("BetterVillage插件已禁用")
    }
}
