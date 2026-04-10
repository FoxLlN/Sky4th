package sky4th.dungeon

import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import sky4th.core.SkyCore
import sky4th.core.api.LanguageAPI
import sky4th.dungeon.command.*
import sky4th.dungeon.config.ConfigManager
import sky4th.dungeon.container.ContainerLootService
import sky4th.dungeon.container.ContainerSearchManager
import sky4th.dungeon.loadout.category.CategoryDetailListener
import sky4th.dungeon.loadout.equipment.*
import sky4th.dungeon.loadout.pick.*
import sky4th.dungeon.loadout.ranged.*
import sky4th.dungeon.loadout.screen.LoadoutScreenListener
import sky4th.dungeon.loadout.shop.LoadoutShopListener
import sky4th.dungeon.loadout.storage.StorageListener
import sky4th.dungeon.loadout.supply.SupplyItemUseListener
import sky4th.dungeon.loadout.repair.RepairItemUseListener
import sky4th.dungeon.loadout.weapon.*
import sky4th.dungeon.monster.core.*
import sky4th.dungeon.monster.spawn.*
import sky4th.dungeon.monster.head.*
import sky4th.dungeon.monster.event.*
import sky4th.dungeon.monster.instances.standard.*
import sky4th.dungeon.monster.instances.elite.*
import sky4th.dungeon.monster.instances.enhanced.*
import sky4th.dungeon.monster.instances.legendary.*
import sky4th.dungeon.player.*
import sky4th.dungeon.region.ExitRegionDetector
import sky4th.dungeon.region.RegionDetector
import sky4th.dungeon.team.TeamManager
import sky4th.dungeon.team.TeamConfig
import sky4th.dungeon.team.TeamListener
import sky4th.dungeon.search.SearchLootService
import sky4th.dungeon.shield.ShieldFixListener
import sky4th.dungeon.shield.InvisibilityEquipmentHiderReflection
import sky4th.dungeon.spawn.SpawnManager
import sky4th.dungeon.world.TemplateWorldManager
import java.io.File

class Dungeon : JavaPlugin() {
    companion object {
        @JvmStatic lateinit var instance: Dungeon private set
    }

    private lateinit var configManager: ConfigManager
    private lateinit var templateWorldManager: TemplateWorldManager
    private lateinit var spawnManager: SpawnManager
    private lateinit var monsterSpawnManager: MonsterSpawnManager
    private lateinit var dynamicMonsterManager: DynamicMonsterManager
    private lateinit var playerManager: PlayerManager
    private lateinit var backpackManager: BackpackManager
    lateinit var containerLootService: ContainerLootService private set
    private lateinit var searchLootService: SearchLootService
    private lateinit var containerSearchManager: ContainerSearchManager
    private lateinit var monsterSearchManager: MonsterSearchManager
    private lateinit var equipmentRefreshCoordinator: EquipmentRefreshCoordinator
    private lateinit var regionDetector: RegionDetector
    private lateinit var exitRegionDetector: ExitRegionDetector
    private lateinit var dungeonInstanceManager: sky4th.dungeon.model.DungeonInstanceManager
    private lateinit var teamManager: TeamManager
    private lateinit var downedPlayerManager: DownedPlayerManager
    private lateinit var sidebarManager: SidebarManager
    // 智能更新：已移除全局计分板更新任务，改为按需更新
    // private var sidebarUpdateTask: BukkitTask? = null
    // 监听器变量
    private lateinit var backpackInteractionListener: BackpackInteractionListener
    private lateinit var backpackChangeListener: BackpackChangeListener
    private lateinit var playerDeathUI: PlayerDeathUI
    private lateinit var playerDeathListener: PlayerDeathListener
    private lateinit var downedPlayerListener: DownedPlayerListener
    private lateinit var loadoutShopListener: LoadoutShopListener
    private lateinit var loadoutScreenListener: LoadoutScreenListener
    private lateinit var categoryDetailListener: CategoryDetailListener
    private lateinit var pickFromStorageListener: PickFromStorageListener
    private lateinit var pickArrowFromStorageListener: PickArrowFromStorageListener
    private lateinit var buyArrowListener: BuyArrowListener
    private lateinit var storageListener: StorageListener
    private lateinit var tiebiSetListener: TiebiSetListener
    private lateinit var chikeSetListener: ChikeSetListener
    private lateinit var xuezheSetListener: XuezheSetListener
    private lateinit var youxiaSetListener: YouxiaSetListener
    private lateinit var samanSetListener: SamanSetListener
    private lateinit var loadoutWeaponEffectListener: LoadoutWeaponEffectListener
    private lateinit var infiniteBowNoArrowListener: InfiniteBowNoArrowListener
    private lateinit var fengNuEffectListener: FengNuEffectListener
    private lateinit var explosiveCrossbowListener: ExplosiveCrossbowListener
    private lateinit var specialArrowNoDamageListener: SpecialArrowNoDamageListener
    private lateinit var supplyItemUseListener: SupplyItemUseListener
    private lateinit var repairItemUseListener: RepairItemUseListener
    private lateinit var monsterSpecialMechanicsListener: MonsterSpecialMechanicsListener
    private lateinit var shieldFixListener: ShieldFixListener
    private lateinit var invisibilityEquipmentHider: InvisibilityEquipmentHiderReflection
    private lateinit var achievementDisableListener: AchievementDisableListener
    override fun onEnable() {
        instance = this
        saveDefaultConfig()
        if (!File(dataFolder, "lang/zh_CN.yml").exists()) saveResource("lang/zh_CN.yml", false)
        configManager = ConfigManager(this).also { it.load() }
        
        // 初始化管理器
        templateWorldManager = TemplateWorldManager(
            this,
            configManager,
            onWorldInit = { world, instanceFullId ->
                // 世界初始化回调：初始化所有系统
                logger.info("初始化实例系统: $instanceFullId")
                val dungeonId = instanceFullId.substringBefore("_")
                containerSearchManager.initForWorld(world, instanceFullId, dungeonId)
                dynamicMonsterManager.initSpawnPoints(world, instanceFullId)
                dynamicMonsterManager.start(world, instanceFullId)
            },
            onWorldCleanup = { instanceFullId ->
                // 世界清理回调：清理所有系统数据
                logger.info("清理实例系统: $instanceFullId")
                containerSearchManager.clearForInstance(instanceFullId)
                monsterSearchManager.clearForInstance(instanceFullId)
                playerDeathUI.clearForInstance(instanceFullId)
                dynamicMonsterManager.stop(instanceFullId)
            }
        )
        spawnManager = SpawnManager(configManager)
        backpackManager = BackpackManager(this, configManager)
        // 初始化物品价格缓存
        backpackManager.initPriceCache()
        val techLevelBonusHandler = TechLevelBonusHandler(this, configManager)
        val dataStorage = DungeonDataStorage(this)

        // 先初始化 dungeonInstanceManager
        dungeonInstanceManager = sky4th.dungeon.model.DungeonInstanceManager(this, configManager, templateWorldManager, null)
        dungeonInstanceManager.initialize()

        // 初始化队伍管理器（需要先初始化，因为 playerManager 依赖它）
        teamManager = TeamManager(this, TeamConfig(), null)

        // 初始化倒地玩家管理器
        downedPlayerManager = DownedPlayerManager(this, teamManager)

        // 初始化侧边栏管理器
        sidebarManager = SidebarManager(this, backpackManager)

        // 初始化 playerManager，传入 dungeonInstanceManager 和 teamManager
        playerManager = PlayerManager(this, configManager, spawnManager, backpackManager, techLevelBonusHandler, dungeonInstanceManager, dataStorage, teamManager)

        // 将 playerManager 设置到 dungeonInstanceManager
        dungeonInstanceManager.setPlayerManager(playerManager)
        
        // 设置 teamManager 的 playerManager
        teamManager.setPlayerManager(playerManager)

        // 现在可以初始化需要 playerManager 的管理器
        monsterSpawnManager = MonsterSpawnManager(this, configManager, playerManager)
        dynamicMonsterManager = DynamicMonsterManager(this, configManager, playerManager, downedPlayerManager)
        backpackInteractionListener = BackpackInteractionListener(playerManager, backpackManager, this)
        containerLootService = ContainerLootService(this, configManager, backpackManager, playerManager)
        searchLootService = sky4th.dungeon.search.SearchLootService(this, configManager, containerLootService)
        containerSearchManager = ContainerSearchManager(this, configManager, playerManager, containerLootService, searchLootService)
        monsterSearchManager = MonsterSearchManager(this, configManager, playerManager, searchLootService)
        playerDeathListener = PlayerDeathListener(playerManager, backpackManager, configManager)
        playerDeathUI = PlayerDeathUI(this, configManager, searchLootService, containerSearchManager, backpackManager, playerDeathListener)
        playerDeathListener.setDeathUI(playerDeathUI)
        playerDeathListener.setDownedPlayerManager(downedPlayerManager)
        downedPlayerListener = DownedPlayerListener(downedPlayerManager)

        // 初始化怪物系统
        MonsterRegistry.init(this)
        MonsterHeadConfig.init(this, configManager)
        MonsterDeathListener.init(this, containerLootService, configManager)
        StandardZombie.register()
        StandardSkeleton.register()
        StandardSpider.register()
        EliteSkeleton.register()
        EliteZombie.register()
        EnhancedWitch.register()
        EnhancedShulker.register()
        LegendaryRaiderCaptain.register()
        EliteVindicator.register()
        EnhancedSkeleton.register()
        // 初始化装备套装监听器
        tiebiSetListener = TiebiSetListener(this)
        chikeSetListener = ChikeSetListener(this)
        xuezheSetListener = XuezheSetListener(this)
        youxiaSetListener = YouxiaSetListener(this)
        samanSetListener = SamanSetListener(this)
        equipmentRefreshCoordinator = EquipmentRefreshCoordinator(
            this, playerManager, tiebiSetListener, chikeSetListener, xuezheSetListener, youxiaSetListener, samanSetListener, backpackManager
        )

        // 初始化武器效果
        listOf(KuoJianEffectHandler(), LieYanEffectHandler(), CuiDuEffectHandler(), ExecutionEffectHandler(this), SuppressionAxeEffectHandler(this))
            .forEach { LoadoutWeaponEffectRegistry.register(it) }

        // 初始化其他监听器
        backpackChangeListener = BackpackChangeListener(this, backpackManager)
        loadoutShopListener = LoadoutShopListener()
        loadoutScreenListener = LoadoutScreenListener()
        categoryDetailListener = CategoryDetailListener()
        pickFromStorageListener = PickFromStorageListener()
        pickArrowFromStorageListener = PickArrowFromStorageListener()
        buyArrowListener = BuyArrowListener()
        storageListener = StorageListener()
        loadoutWeaponEffectListener = LoadoutWeaponEffectListener(this)
        infiniteBowNoArrowListener = InfiniteBowNoArrowListener(this)
        fengNuEffectListener = FengNuEffectListener(this)
        explosiveCrossbowListener = ExplosiveCrossbowListener(this)
        specialArrowNoDamageListener = SpecialArrowNoDamageListener(this)
        supplyItemUseListener = SupplyItemUseListener(this)
        repairItemUseListener = RepairItemUseListener(this)
        monsterSpecialMechanicsListener = MonsterSpecialMechanicsListener(this)
        shieldFixListener = ShieldFixListener(this)
        invisibilityEquipmentHider = InvisibilityEquipmentHiderReflection(this)
        achievementDisableListener = AchievementDisableListener(playerManager)

        // 创建区域检测器
        regionDetector = RegionDetector(this, configManager) { player ->
            // 玩家进入地牢区域，检查是否需要进入地牢
            val location = player.location
            val worldName = location.world?.name ?: return@RegionDetector

            // 检查世界名称是否符合地牢实例命名规范：dungeonId_instanceId
            val parts = worldName.split("_")
            if (parts.size >= 2) {
                val dungeonId = parts[0]
                val instanceId = parts[1]

                // 检查是否是有效的地牢配置
                if (configManager.getDungeonConfig(dungeonId) != null) {
                    // 如果玩家不在地牢中，尝试进入
                    if (!playerManager.isPlayerInDungeon(player)) {
                        playerManager.teleportToDungeon(player, dungeonId, instanceId)
                    }
                }
            }
        }
        exitRegionDetector = ExitRegionDetector(this, configManager, playerManager)
        
        // 注册事件监听器
        listOf(
            regionDetector, exitRegionDetector, PlayerListener(playerManager),
            backpackInteractionListener, backpackChangeListener, playerDeathListener, playerDeathUI, containerSearchManager, monsterSearchManager,
            equipmentRefreshCoordinator, loadoutShopListener, loadoutScreenListener,
            categoryDetailListener, pickFromStorageListener, pickArrowFromStorageListener,
            buyArrowListener, storageListener, tiebiSetListener, chikeSetListener,
            xuezheSetListener, youxiaSetListener, samanSetListener, loadoutWeaponEffectListener,
            infiniteBowNoArrowListener, fengNuEffectListener, explosiveCrossbowListener,
            specialArrowNoDamageListener, supplyItemUseListener, repairItemUseListener, monsterSpecialMechanicsListener,
            shieldFixListener, invisibilityEquipmentHider, TechLevelDamageListener(configManager, playerManager),
            TeamListener(teamManager, playerManager), downedPlayerListener, achievementDisableListener
        ).forEach { server.pluginManager.registerEvents(it, this) }

        DungeonContext.init(this, configManager, playerManager, templateWorldManager, containerSearchManager, monsterSearchManager, playerDeathListener, playerDeathUI, dynamicMonsterManager, dungeonInstanceManager, dataStorage, teamManager, downedPlayerManager, sidebarManager)
        getCommand("dungeon")?.apply {
            setExecutor(DungeonCommandHandler)
            tabCompleter = DungeonCommandHandler
        }

        // 智能更新：只在需要时刷新计分板
        // 移除了原来的每秒更新任务，改为以下策略：
        // 1. 铁壁技能激活时，每秒更新倒计时
        // 2. 其他信息只在变化时更新（技能使用、现金变化等）
        // 3. 进入地牢时初始化计分板
        // 
        // 原来的每秒更新任务（已禁用）：
        // sidebarUpdateTask = server.scheduler.runTaskTimer(this, Runnable {
        //     playerManager.getPlayersInDungeon().mapNotNull { server.getPlayer(it) }
        //         .filter { it.isOnline }.forEach { backpackManager.updateSidebar(it) }
        // }, 20L, 20L)
        
        // 为每个地牢配置初始化世界
        // 注意：系统初始化已经在 TemplateWorldManager 的回调中完成

        @Suppress("DEPRECATION")
        logger.info(LanguageAPI.getText(Dungeon.instance, "log.enabled", "plugin" to description.name, "version" to description.version))
        logger.info("经济 API 状态: ${SkyCore.isEconomyAvailable()}")
    }

    override fun onDisable() {
        val plugin = Dungeon.instance
        // 智能更新：已移除全局计分板更新任务
        // sidebarUpdateTask?.cancel()
        // sidebarUpdateTask = null
        // 清理背包管理器的所有缓存和任务
        backpackManager.clearAll()
        // 停止所有实例的动态怪物
        // 清理所有系统数据
        // 先清理倒地玩家状态
        downedPlayerManager.clearAllData()
        // 再传送玩家出地牢
        playerManager.clearAllPlayers()
        // 清理队伍数据
        teamManager.clearAllData()
        // 最后关闭地牢实例
        dungeonInstanceManager.shutdown()
        
        // 删除所有实例世界文件夹
        try {
            templateWorldManager.deleteAllInstanceWorldFolders()
        } catch (e: Exception) {
            logger.severe("删除实例世界文件夹时出错: ${e.message}")
            e.printStackTrace()
        }
        
        @Suppress("DEPRECATION")
        logger.info(LanguageAPI.getText(plugin, "log.disabled", "plugin" to description.name))
    }
    
    fun reloadPluginConfig() {
        configManager.reload()
        logger.info(LanguageAPI.getText(Dungeon.instance, "log.config-reloaded"))
    }

    fun refreshSidebar(player: Player) {
        backpackManager.refreshSidebar(player)
    }
}
