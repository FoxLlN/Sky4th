package sky4th.core.ui

import org.bukkit.plugin.Plugin
import org.bukkit.entity.Player
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.InputStreamReader
import java.util.jar.JarFile

/**
 * UI管理器
 * 负责加载和管理所有UI配置
 */
object UIManager {
    private val uiConfigs = mutableMapOf<String, UIConfig>()
    private val uiPageCache = mutableMapOf<String, MutableMap<String, UIPage>>() // 玩家ID -> UIID -> UIPage
    private val cacheTimestamps = mutableMapOf<String, MutableMap<String, Long>>() // 缓存时间戳
    private const val CACHE_EXPIRE_TIME = 5 * 60 * 1000L // 5分钟过期
    private var plugin: Plugin? = null
    private var uiFolder: File? = null
    private var listener: UIListener? = null

    /**
     * 初始化UI管理器
     */
    fun initialize(plugin: Plugin) {
        this.plugin = plugin
        this.uiFolder = File(plugin.dataFolder, "ui")

        if (!uiFolder!!.exists()) {
            uiFolder!!.mkdirs()
        }

        // 注册事件监听器
        listener = UIListener()
        plugin.server.pluginManager.registerEvents(listener!!, plugin)
    }

    /**
     * 从指定插件加载UI配置
     */
    fun loadPluginUIs(plugin: Plugin) {
        println("[UIManager] loadPluginUIs 被调用，插件: ${plugin.name}")

        // 从JAR文件内部读取UI配置
        try {
            val jarFile = File(plugin.javaClass.protectionDomain.codeSource.location.toURI())

            JarFile(jarFile).use { jar ->
                val entries = jar.entries()
                var loadedCount = 0
                var failCount = 0

                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.name.startsWith("ui/") && entry.name.endsWith(".yml")) {
                        try {
                            val input = jar.getInputStream(entry)
                            val reader = InputStreamReader(input)
                            val config = YamlConfiguration.loadConfiguration(reader)

                            val id = config.getString("id") ?: entry.name.substringAfterLast("/").removeSuffix(".yml")
                            val title = config.getString("title") ?: "未命名UI"
                            val shape = config.getStringList("shape")

                            // 加载模板
                            val templates = mutableMapOf<String, UITemplate>()
                            val templateSection = config.getConfigurationSection("template")
                            if (templateSection != null) {
                                templateSection.getKeys(false).forEach { key ->
                                    templates[key] = UIConfig.loadTemplate(templateSection, key)
                                }
                            }

                            val uiConfig = UIConfig(id, title, shape, templates, plugin.name)
                            uiConfigs[uiConfig.id] = uiConfig
                            loadedCount++
                        } catch (e: Exception) {
                            failCount++
                            println("✗ 加载UI配置失败 [${plugin.name}]: ${entry.name}")
                            println("  错误: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                }

                println("UI配置加载完成 [${plugin.name}]: 成功 ${loadedCount} 个, 失败 ${failCount} 个")
            }
        } catch (e: Exception) {
            println("✗ 加载UI配置时发生错误: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 加载所有UI配置
     */
    private fun loadAllUIs() {
        uiConfigs.clear()
        uiFolder?.listFiles { _, name -> name.endsWith(".yml") }?.forEach { file ->
            try {
                val config = UIConfig.load(file)
                uiConfigs[config.id] = config
                plugin?.logger?.info("已加载UI配置: ${config.id}")
            } catch (e: Exception) {
                plugin?.logger?.severe("加载UI配置失败: ${file.name} - ${e.message}")
            }
        }
    }

    /**
     * 获取UI配置
     */
    fun getUI(id: String): UIConfig? = uiConfigs[id]

    /**
     * 获取指定玩家的UIPage实例
     */
    fun getUIPage(player: Player, uiId: String): UIPage? {
        val playerCache = uiPageCache[player.uniqueId.toString()] ?: return null
        return playerCache[uiId]
    }

    /**
     * 获取所有UI配置的ID
     */
    fun getAllUIIds(): List<String> = uiConfigs.keys.toList()

    /**
     * 获取UI监听器实例
     */
    fun getListener(): UIListener? = listener

    /**
     * 打开UI
     */
    fun openUI(player: Player, uiId: String) {
        uiConfigs[uiId]?.let { config ->
            // 为每个玩家创建独立的UIPage实例
            val playerCache = uiPageCache.getOrPut(player.uniqueId.toString()) { mutableMapOf() }
            val timestampCache = cacheTimestamps.getOrPut(player.uniqueId.toString()) { mutableMapOf() }

            // 检查玩家是否已经打开了这个UI
            val currentPlayerUI = listener?.getPlayerUI(player)
            val isReopening = currentPlayerUI == uiId

            // 检查缓存是否过期
            val now = System.currentTimeMillis()
            val cachedUI = playerCache[uiId]
            val timestamp = timestampCache[uiId]

            // 判断是否是切换页面
            val isSwitching = currentPlayerUI != null && currentPlayerUI != uiId

            if (isReopening && cachedUI != null) {
                // 玩家正在重新打开当前UI，只更新内容不重新打开
                cachedUI.update()
            } else if (cachedUI != null && timestamp != null && (now - timestamp) < CACHE_EXPIRE_TIME) {
                // 使用缓存的UI
                listener?.recordPlayerUI(player, uiId, isSwitching)
                cachedUI.open(player)

                // 如果是切换页面，在打开后处理切换事件并更新UI
                if (isSwitching) {
                    sky4th.core.ui.feature.UIFeatureManager.handleUISwitch(config.pluginName, uiId, player)
                    cachedUI.update()
                }
            } else {
                // 创建新的UI
                val ui = UIPage(config, config.pluginName)
                playerCache[uiId] = ui
                timestampCache[uiId] = now
                listener?.recordPlayerUI(player, uiId, isSwitching)
                ui.open(player)

                // 如果是切换页面，在打开后处理切换事件并更新UI
                if (isSwitching) {
                    sky4th.core.ui.feature.UIFeatureManager.handleUISwitch(config.pluginName, uiId, player)
                    ui.update()
                }
            }
        }
    }

    /**
     * 更新当前打开的UI
     * 用于翻页等操作，不重新打开inventory，只更新内容
     */
    fun updateCurrentUI(player: Player) {
        val uiId = listener?.getPlayerUI(player) ?: return
        val playerCache = uiPageCache[player.uniqueId.toString()] ?: return
        val cachedUI = playerCache[uiId] ?: return
        // 只更新UI内容，不重新打开inventory
        cachedUI.update()
    }

    /**
     * 重载所有UI配置
     */
    fun reload() {
        // 清除UIPage缓存，确保使用新配置
        uiPageCache.clear()
        cacheTimestamps.clear()
        loadAllUIs()
    }

    /**
     * 清除UI缓存
     */
    fun clearCache() {
        uiPageCache.clear()
        cacheTimestamps.clear()
    }

    /**
     * 清除指定玩家的UI缓存
     */
    fun clearPlayerCache(player: Player) {
        uiPageCache.remove(player.uniqueId.toString())
        cacheTimestamps.remove(player.uniqueId.toString())
    }

    /**
     * 清理过期的UI缓存
     */
    fun clearExpiredCache() {
        val now = System.currentTimeMillis()
        uiPageCache.keys.forEach { playerId ->
            val timestampCache = cacheTimestamps[playerId] ?: return@forEach
            timestampCache.keys.forEach { uiId ->
                if (now - (timestampCache[uiId] ?: 0) > CACHE_EXPIRE_TIME) {
                    uiPageCache[playerId]?.remove(uiId)
                    timestampCache.remove(uiId)
                }
            }
            // 如果该玩家的所有UI缓存都已过期，清理玩家数据
            if (timestampCache.isEmpty()) {
                uiPageCache.remove(playerId)
                cacheTimestamps.remove(playerId)
            }
        }
    }

    /**
     * 清理UI管理器
     */
    fun cleanup() {
        uiPageCache.clear()
        cacheTimestamps.clear()
        uiConfigs.clear()
        plugin = null
        uiFolder = null
        listener = null
    }
}
