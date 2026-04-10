package com.sky4th.equipment

import org.bukkit.plugin.java.JavaPlugin
import sky4th.core.SkyCore
import sky4th.core.api.LanguageAPI

/**
 * EquipmentAffix - Sky4th 装备系统
 *
 * 负责处理游戏内的装备系统，包括装备属性、词条、权限检查等功能
 */
class EquipmentAffix : JavaPlugin() {

    private lateinit var equipmentProvider: EquipmentAffixProvider
    private lateinit var loreDisplayListener: com.sky4th.equipment.listener.LoreDisplayListener
    private lateinit var proficiencyListener: com.sky4th.equipment.listener.ProficiencyListener
    private lateinit var enchantmentDisplayListener: com.sky4th.equipment.listener.EnchantmentDisplayListener
    private lateinit var blockPlaceListener: com.sky4th.equipment.listener.BlockPlaceListener
    private lateinit var modifierManager: com.sky4th.equipment.modifier.ModifierManager
    private lateinit var equipmentAttributesManager: com.sky4th.equipment.manager.EquipmentAttributesManager
    private lateinit var equipmentAttributeListener: com.sky4th.equipment.listener.EquipmentAttributeListener
    private lateinit var playerAttributeListener: com.sky4th.equipment.listener.PlayerAttributeListener
    private lateinit var unifiedEquipmentChangeListener: com.sky4th.equipment.listener.EnhancedEquipmentChangeListener
    private lateinit var equipmentInstanceListener: com.sky4th.equipment.listener.EquipmentInstanceListener
    private lateinit var lootGenerateListener: com.sky4th.equipment.listener.LootGenerateListener
    private lateinit var uiHandler: com.sky4th.equipment.ui.EquipmentUIHandler


    companion object {
        lateinit var instance: EquipmentAffix
            private set
    }

    override fun onEnable() {
        instance = this
        logger.info("EquipmentAffix 插件正在启用...")

        // 加载UI配置
        logger.info("开始加载UI配置...")
        sky4th.core.api.UIAPI.loadPluginUIs(this)
        logger.info("UI配置加载完成")

        // 初始化配置管理器
        com.sky4th.equipment.config.ConfigManager.init(this)

        // 初始化词条系统
        modifierManager = com.sky4th.equipment.modifier.ModifierManager(this)

        // 初始化UI处理器
        uiHandler = com.sky4th.equipment.ui.EquipmentUIHandler(modifierManager)
        sky4th.core.api.UIAPI.registerUIHandler(this, uiHandler)

        // 初始化发光效果工具类
        com.sky4th.equipment.util.GlowingEntityUtil.initialize(this)

        // 注册事件分发器
        server.pluginManager.registerEvents(
            com.sky4th.equipment.modifier.ModifierEventDispatcher(),
            this
        )
        // 注册所有词条监听器
        com.sky4th.equipment.modifier.listener.ModifierListenerInitializer.registerAll(this)

        // 加载默认词条（从JAR内读取配置）
        com.sky4th.equipment.loader.AffixLoader.loadAll(this, modifierManager)

        // 加载默认装备
        com.sky4th.equipment.loader.EquipmentLoader.loadAll(this)

        // 初始化装备提供者
        equipmentProvider = EquipmentAffixProvider(this)
        // 注册到SkyCore
        sky4th.core.api.EquipmentAPI.registerProvider(equipmentProvider)

        // 材质效果现在通过YAML配置文件定义，由AffixLoader统一加载

        // 初始化装备属性管理器
        equipmentAttributesManager = com.sky4th.equipment.manager.EquipmentAttributesManager(this)

        // 初始化配方管理器，移除原版合成配方
        com.sky4th.equipment.manager.RecipeManager.initialize(this)

        // 加载词条模板配置（必须在配方管理器初始化之后）
        com.sky4th.equipment.loader.AffixTemplateLoader.loadAll(this)

        // 注册命令
        val command = getCommand("equipment")
        val commandHandler = com.sky4th.equipment.command.EquipmentCommandHandler(this)
        command?.setExecutor(commandHandler)
        command?.setTabCompleter(commandHandler)


        // 注册熟练度监听器
        proficiencyListener = com.sky4th.equipment.listener.ProficiencyListener(this)
        server.pluginManager.registerEvents(proficiencyListener, this)

        // 注册增强版装备变更监听器
        unifiedEquipmentChangeListener = com.sky4th.equipment.listener.EnhancedEquipmentChangeListener(
            this,
            equipmentAttributesManager,
            modifierManager
        )
        server.pluginManager.registerEvents(unifiedEquipmentChangeListener, this)

        // 注册装备实例监听器
        equipmentInstanceListener = com.sky4th.equipment.listener.EquipmentInstanceListener()
        server.pluginManager.registerEvents(equipmentInstanceListener, this)
        
        // 注册装备属性监听器（仅处理饥饿度变化）
        equipmentAttributeListener = com.sky4th.equipment.listener.EquipmentAttributeListener(this, equipmentAttributesManager)
        server.pluginManager.registerEvents(equipmentAttributeListener, this)

        // 注册玩家属性监听器
        playerAttributeListener = com.sky4th.equipment.listener.PlayerAttributeListener(this)
        server.pluginManager.registerEvents(playerAttributeListener, this)

        // 注册Lore显示监听器（监听双击右键）
        loreDisplayListener = com.sky4th.equipment.listener.LoreDisplayListener(this)
        server.pluginManager.registerEvents(loreDisplayListener, this)

        // 注册配方修复监听器（阻止合成台修复装备）
        server.pluginManager.registerEvents(com.sky4th.equipment.listener.RecipeRepairListener(), this)

        // 注册锻造台管理器（统一处理所有锻造台相关逻辑）
        server.pluginManager.registerEvents(com.sky4th.equipment.listener.smithing.SmithingManager(), this)
        // 注册锻造台点击处理器（处理词条锻造和工具升级的点击事件）
        server.pluginManager.registerEvents(com.sky4th.equipment.listener.smithing.SmithingClickHandler, this)

        // 注册附魔显示监听器（自动转换附魔描述）
        enchantmentDisplayListener = com.sky4th.equipment.listener.EnchantmentDisplayListener(this)
        server.pluginManager.registerEvents(enchantmentDisplayListener, this)

        // 注册方块放置监听器（标记玩家放置的方块）
        blockPlaceListener = com.sky4th.equipment.listener.BlockPlaceListener()
        server.pluginManager.registerEvents(blockPlaceListener, this)

        // 注册战利品生成监听器（将原版装备替换为自定义版本）
        lootGenerateListener = com.sky4th.equipment.listener.LootGenerateListener()
        server.pluginManager.registerEvents(lootGenerateListener, this)

        // 注册资源充能监听器（Shift+F充能）
        server.pluginManager.registerEvents(com.sky4th.equipment.listener.ResourceChargeListener(), this)

        // 注册筛选名称监听器
        server.pluginManager.registerEvents(com.sky4th.equipment.ui.FilterNameListener, this)

        // 初始化成就解锁监听器
        com.sky4th.equipment.listener.AchievementUnlockListener.initialize(this)

        // 初始化统一词条管理器
        com.sky4th.equipment.modifier.manager.UnifiedModifierManager.initialize(this)

        // 初始化冰冻管理器
        com.sky4th.equipment.modifier.manager.FreezeManager.initialize(this)

        // 初始化投掷物路径管理器
        com.sky4th.equipment.util.projectile.ProjectilePathManager.initialize()

        logger.info("EquipmentAffix插件已启用")
    }

    override fun onDisable() {
        // 批量保存所有待更新的玩家属性
        try {
            sky4th.core.api.PlayerAttributesAPI.flushPendingUpdates()
            logger.info("已批量保存所有玩家属性")
        } catch (e: Exception) {
            logger.warning("批量保存玩家属性失败: ${e.message}")
            e.printStackTrace()
        }

        // 清理发光效果工具类
        com.sky4th.equipment.util.GlowingEntityUtil.disable()

        // 关闭词条管理器，清理所有实体上的修饰符
        modifierManager.shutdown()

        // 关闭冰冻管理器
        com.sky4th.equipment.modifier.manager.FreezeManager.shutdown()

        // 关闭投掷物路径管理器
        com.sky4th.equipment.util.projectile.ProjectilePathManager.shutdown()

        // 关闭统一词条管理器
        com.sky4th.equipment.modifier.manager.UnifiedModifierManager.shutdown()
        
        // 注销UI处理器
        sky4th.core.api.UIAPI.unregisterUIHandler(this)

        logger.info("EquipmentAffix插件已禁用")
    }

}
