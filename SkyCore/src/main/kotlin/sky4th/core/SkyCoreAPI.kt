package sky4th.core

import org.bukkit.plugin.Plugin
import sky4th.core.api.EconomyAPI
import sky4th.core.api.MarkAPI
import sky4th.core.database.DatabaseManager
import sky4th.core.economy.CoreEconomyProvider
import sky4th.core.economy.EconomyService
import sky4th.core.head.HeadService
import sky4th.core.mark.MarkService
import sky4th.core.service.DungeonTechService
import sky4th.core.service.PlayerAttributesService
import sky4th.core.service.PlayerPermissionsService
import sky4th.core.service.PlayerService
import sky4th.core.service.StorageService
import sky4th.core.service.VillageService
import sky4th.core.ui.UIManager


/**
 * SkyCore - Sky4th 服务器核心 API
 *
 * 提供所有模块共享的基础接口和工具类。
 * 当前使用固定数据库参数，仅用于测试连接。
 */
object SkyCore {
    private var instance: Plugin? = null
    private var databaseManager: DatabaseManager? = null
    private var playerService: PlayerService? = null
    private var storageService: StorageService? = null
    private var dungeonTechService: DungeonTechService? = null
    private var playerPermissionsService: PlayerPermissionsService? = null
    private var villageService: VillageService? = null
    private var playerAttributesService: PlayerAttributesService? = null
    private var markService: MarkService? = null
    private var headService: HeadService? = null

    /**
     * 初始化 API（由第一个加载的插件调用）
     */
    fun init(plugin: Plugin) {
        if (instance != null) return
        instance = plugin
        plugin.logger.info("SkyCore 已初始化")

        try {
            initDatabase(plugin)
        } catch (e: Exception) {
            plugin.logger.severe("SkyCore 初始化失败: ${e.message}")
            e.printStackTrace()
        }
    }

    /** 经济系统初始化并自动注册默认提供者（Core 信用点），子插件直接使用 EconomyAPI */
    private fun initEconomy(playerService: PlayerService) {
        EconomyAPI.registerProvider(CoreEconomyProvider(playerService))
    }

    private fun initDatabase(plugin: Plugin) {
        try {
            plugin.logger.info("开始初始化数据库...")

            val dbManager = DatabaseManager(plugin)
            dbManager.initialize()
            dbManager.initializeTables()
            databaseManager = dbManager

            val service = PlayerService(dbManager)
            service.initialize()
            playerService = service

            storageService = StorageService(dbManager)
            dungeonTechService = DungeonTechService(dbManager)

            // 初始化村庄服务
            val villageService = VillageService(sky4th.core.database.VillageDataDAO(dbManager))
            villageService.initialize()
            this.villageService = villageService

            // 初始化权限服务
            val permissionsService = PlayerPermissionsService(dbManager)
            permissionsService.initialize()
            playerPermissionsService = permissionsService

            // 初始化玩家属性服务
            val attributesService = PlayerAttributesService(dbManager)
            attributesService.initialize()
            playerAttributesService = attributesService

            // 初始化标记服务
            val markService = MarkService
            markService.initialize()
            this.markService = markService

            // 初始化头颅服务
            val headService = HeadService
            headService.initialize()
            this.headService = headService

            // 初始化UI系统
            UIManager.initialize(plugin)

            // 启动定期清理过期UI缓存的定时任务（每20分钟清理一次）
            plugin.server.scheduler.runTaskTimerAsynchronously(plugin, Runnable {
                UIManager.clearExpiredCache()
            }, 20 * 60 * 20L, 20 * 60 * 20L)

            // 初始化工具API - 不再需要注册提供者

            initEconomy(service)
            
            plugin.logger.info("SkyCore 数据库、玩家服务、仓库服务、地牢科技树服务、村庄服务、权限服务、标记服务、头颅服务、UI系统、经济系统服务已初始化")
        } catch (e: Exception) {
            plugin.logger.severe("数据库初始化失败: ${e.javaClass.name} - ${e.message}")
            e.printStackTrace()
        }
    }

    fun getInstance(): Plugin? = instance
    fun isInitialized(): Boolean = instance != null
    fun getDatabaseManager(): DatabaseManager? = databaseManager
    fun getPlayerService(): PlayerService? = playerService
    fun getStorageService(): StorageService? = storageService
    fun getDungeonTechService(): DungeonTechService? = dungeonTechService
    fun getPlayerPermissionsService(): PlayerPermissionsService? = playerPermissionsService
    fun getVillageService(): VillageService? = villageService
    fun getPlayerAttributesService(): PlayerAttributesService? = playerAttributesService
    fun getHeadService(): HeadService? = headService

    fun isDatabaseAvailable(): Boolean = databaseManager?.isConnected() ?: false
    fun isPlayerServiceAvailable(): Boolean = playerService != null
    fun isEconomyAvailable(): Boolean = EconomyService.isRegistered()
    fun isPlayerPermissionsServiceAvailable(): Boolean = playerPermissionsService != null
    fun isVillageServiceAvailable(): Boolean = villageService != null
    fun isPlayerAttributesServiceAvailable(): Boolean = playerAttributesService != null
    fun isMarkServiceAvailable(): Boolean = markService != null
    fun isHeadServiceAvailable(): Boolean = headService != null

    /**
     * 关闭 API（清理资源）
     */
    fun shutdown() {
        playerService?.saveAll()
        playerPermissionsService?.clearAllCache()
        playerAttributesService?.saveAll()
        EconomyAPI.unregisterProvider()

        // 清理所有标记
        MarkAPI.clearAllMarks()

        databaseManager?.close()
        databaseManager = null
        playerService = null
        storageService = null
        dungeonTechService = null
        villageService = null
        playerPermissionsService = null
        playerAttributesService = null
        markService = null
        headService?.cleanup()
        headService = null

        // 清理UI系统
        UIManager.cleanup()

        instance = null
    }
}
