package sky4th.dungeon.world

import sky4th.core.api.LanguageAPI
import sky4th.dungeon.Dungeon
import sky4th.dungeon.config.ConfigManager
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.plugin.java.JavaPlugin
import java.io.IOException
import java.nio.file.*
import kotlin.io.path.isDirectory
import kotlin.io.path.name

class TemplateWorldManager(
    private val plugin: JavaPlugin,
    private val configManager: ConfigManager,
    // 系统初始化回调
    private val onWorldInit: ((World, String) -> Unit)? = null,
    // 系统清理回调
    private val onWorldCleanup: ((String) -> Unit)? = null
) {
    /**
     * 创建新的地牢实例世界（异步）
     * @param config 地牢配置
     * @param instanceId 实例ID
     * @param callback 完成回调，参数为创建的世界，失败返回null
     */
    fun createInstanceWorld(config: sky4th.dungeon.model.DungeonConfig, instanceId: String, callback: (World?) -> Unit) {
        val worldName = config.getInstanceWorldName(instanceId)
        val instanceFullId = "${config.id}_$instanceId"

        // 检查世界是否已存在
        Bukkit.getWorld(worldName)?.let { 
            // 如果世界已存在，仍然调用初始化回调
            onWorldInit?.invoke(it, instanceFullId)
            callback(it)
            return 
        }

        // 异步准备世界文件夹
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            try {
                val prepareSuccess = prepareInstanceWorldFolder(config, instanceId)

                if (!prepareSuccess) {
                    plugin.logger.severe("准备世界文件夹失败: $worldName")
                    Bukkit.getScheduler().runTask(plugin, Runnable { callback(null) })
                    return@Runnable
                }

                // 回到主线程加载世界
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    val world = loadWorldSync(worldName)
                    if (world != null) {
                        onWorldInit?.invoke(world, instanceFullId)
                    } else {
                        plugin.logger.severe("世界加载失败: $worldName")
                    }
                    callback(world)
                })
            } catch (e: Exception) {
                plugin.logger.severe("异步任务执行异常: ${e.message}")
                e.printStackTrace()
                Bukkit.getScheduler().runTask(plugin, Runnable { callback(null) })
            }
        })
    }

    /**
     * 重置地牢实例世界（完全异步）
     * @param instance 地牢实例
     * @return 是否成功重置
     */
    fun resetInstanceWorld(instance: sky4th.dungeon.model.DungeonInstance): Boolean {
        val worldName = instance.config.getInstanceWorldName(instance.instanceId)
        val instanceFullId = "${instance.config.id}_${instance.instanceId}"

        // 调用清理回调
        onWorldCleanup?.invoke(instanceFullId)

        // 先卸载世界
        val unloadSuccess = unloadWorldSync(worldName)
        if (!unloadSuccess) {
            plugin.logger.warning("卸载世界失败: $worldName")
            return false
        }

        // 异步重置世界文件夹
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val prepareSuccess = prepareInstanceWorldFolder(instance.config, instance.instanceId, reset = true)
            if (!prepareSuccess) {
                plugin.logger.warning("重置世界文件夹失败: $worldName")
                return@Runnable
            }

            // 在主线程重新加载世界
            Bukkit.getScheduler().runTask(plugin, Runnable {
                val world = loadWorldSync(worldName)
                if (world == null) {
                    plugin.logger.warning("加载世界失败: $worldName")
                } else {
                    plugin.logger.info("重置世界成功: $worldName")
                }
            })
        })

        return true
    }

    /**
     * 卸载地牢实例世界
     * @param instance 地牢实例
     * @return 是否成功卸载
     */
    fun unloadInstanceWorld(instance: sky4th.dungeon.model.DungeonInstance): Boolean {
        val worldName = instance.config.getInstanceWorldName(instance.instanceId)
        val instanceFullId = "${instance.config.id}_${instance.instanceId}"
        
        // 调用清理回调
        onWorldCleanup?.invoke(instanceFullId)
        
        return unloadWorldSync(worldName)
    }

    /**
     * 准备实例世界文件夹
     * @param config 地牢配置
     * @param instanceId 实例ID
     * @param reset 是否重置
     * @return 是否成功
     */
    private fun prepareInstanceWorldFolder(config: sky4th.dungeon.model.DungeonConfig, instanceId: String, reset: Boolean = false): Boolean {
        val worldName = config.getInstanceWorldName(instanceId)
        val worldFolder = plugin.server.worldContainer.toPath().resolve(worldName)
        val templatePath = resolveTemplatePath(config.templatePath)

        return try {
            if (reset && Files.exists(worldFolder)) {
                deleteRecursively(worldFolder)
            }
            if (!Files.exists(worldFolder)) {
                copyTemplateTo(templatePath, worldFolder, config.id)
            }
            true
        } catch (e: Exception) {
            plugin.logger.severe("准备世界文件夹失败: ${e.message ?: "unknown"}")
            false
        }
    }

    /**
     * 主线程：加载世界
     * @param worldName 世界名称
     * @return 加载的世界，失败返回null
     */
    private fun loadWorldSync(worldName: String): World? {
        Bukkit.getWorld(worldName)?.let { return it }

        val world = WorldCreator(worldName).createWorld()
        if (world == null) {
            plugin.logger.severe("加载世界失败: $worldName")
            return null
        }
        applyDungeonGameRules(world)
        return world
    }

    /**
     * 主线程：卸载世界
     * @param worldName 世界名称
     * @return 是否成功
     */
    private fun unloadWorldSync(worldName: String): Boolean {
        val world = Bukkit.getWorld(worldName) ?: return true
        return Bukkit.unloadWorld(world, false)
    }

    private fun applyDungeonGameRules(world: World) {
        world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false)
        world.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false)
        world.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, false)
        world.setGameRule(org.bukkit.GameRule.NATURAL_REGENERATION, false)
        world.setGameRule(org.bukkit.GameRule.ANNOUNCE_ADVANCEMENTS, false)
        world.difficulty = org.bukkit.Difficulty.HARD
        world.time = 6000
    }

    /**
     * 复制模板世界到目标文件夹
     * @param templatePath 模板世界路径
     * @param targetWorldFolder 目标世界文件夹
     * @param dungeonId 地牢ID
     */
    private fun copyTemplateTo(templatePath: Path, targetWorldFolder: Path, dungeonId: String) {
        if (!Files.exists(templatePath) || !Files.isDirectory(templatePath)) {
            throw IllegalArgumentException(
                LanguageAPI.getText(plugin, "log.template-world-missing", "path" to templatePath.toString())
            )
        }

        val ignore = configManager.templateIgnore
        // 这些是保证副本世界"完全从模板加载"所必须的核心文件/目录：
        // - level.dat      : 世界信息、种子等
        // - region / DIM-1 / DIM1 : 区块数据
        // 无论配置里怎么写 template-ignore，都不允许过滤掉它们，
        // 否则 Bukkit 会当成一个全新世界目录来重新生成随机地图。
        val coreFiles = setOf("level.dat")
        val coreTopDirs = setOf("region", "DIM-1", "DIM1")
        var coreFilterWarningLogged = false

        if (ignore.isNotEmpty()) {
            plugin.logger.info(
                LanguageAPI.getText(plugin, "log.template-filter", "filters" to ignore.joinToString(", "))
            )
        }
        Files.createDirectories(targetWorldFolder)

        Files.walk(templatePath).use { stream ->
            stream.forEach { src ->
                val rel = templatePath.relativize(src)
                val dest = targetWorldFolder.resolve(rel)

                // 统一过滤：按相对路径第一段（目录）+ 文件名
                val relString = rel.toString().replace("\\", "/").trimStart('/')
                val top = relString.substringBefore('/', relString)
                val fileName = src.name

                // 默认始终跳过 session.lock（防止锁冲突）
                if (fileName == "session.lock") return@forEach

                val isCoreFile = fileName in coreFiles
                val isCoreDir = top in coreTopDirs

                // 不允许通过过滤跳过核心世界数据，否则会导致 Bukkit 重新生成一个全新世界
                if (!(isCoreFile || isCoreDir)) {
                    // 非核心文件/目录才应用用户配置的忽略规则
                    if (ignore.contains(top) || ignore.contains(relString)) return@forEach
                } else {
                    if (!coreFilterWarningLogged &&
                        (ignore.contains(fileName) || ignore.contains(top) || ignore.contains(relString))
                    ) {
                        plugin.logger.warning("警告: 尝试过滤核心世界数据: $relString")
                        coreFilterWarningLogged = true
                    }
                }

                if (src.isDirectory()) {
                    Files.createDirectories(dest)
                } else {
                    Files.createDirectories(dest.parent)
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
                }
            }
        }
    }

    /**
     * 解析模板世界路径
     * @param templatePath 模板世界路径
     * @return 完整路径
     */
    private fun resolveTemplatePath(templatePath: String): Path {
        val raw = templatePath.trim()
        val p = Paths.get(raw)
        return if (p.isAbsolute) p else plugin.server.worldContainer.toPath().resolve(raw)
    }

    private fun deleteRecursively(path: Path) {
        // 先删文件再删目录
        Files.walk(path)
            .sorted(Comparator.reverseOrder())
            .forEach {
                try {
                    Files.deleteIfExists(it)
                } catch (e: IOException) {
                    throw IOException(
                        "删除文件失败: ${it.toString()}, 错误: ${e.message ?: "unknown"}",
                        e
                    )
                }
            }
    }

    /**
     * 异步确保副本世界就绪：
     * - 文件复制/删除：异步线程
     * - 卸载/加载世界：主线程
     */
    fun ensureWorldReadyAsync(worldName: String, templatePath: String, reset: Boolean, done: (World?) -> Unit) {
        // 主线程：如果已加载且不需要 reset，直接返回
        Bukkit.getScheduler().runTask(plugin, Runnable {
            val loaded = Bukkit.getWorld(worldName)
            if (loaded != null && !reset) {
                done(loaded)
                return@Runnable
            }

            if (reset) {
                // 先主线程卸载并把玩家送走，然后异步做文件操作，再主线程加载
                unloadAndTeleportPlayersSync(worldName) { unloadedOk ->
                    if (!unloadedOk) {
                        done(null)
                        return@unloadAndTeleportPlayersSync
                    }
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                        val fileOk = prepareWorldFolder(worldName, templatePath, reset = true)
                        Bukkit.getScheduler().runTask(plugin, Runnable {
                            done(if (fileOk) loadWorldSync(worldName) else null)
                        })
                    })
                }
            } else {
                // 不 reset：异步确保世界文件夹存在（不存在就复制），再主线程加载
                Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                    val fileOk = prepareWorldFolder(worldName, templatePath, reset = false)
                    Bukkit.getScheduler().runTask(plugin, Runnable {
                        done(if (fileOk) loadWorldSync(worldName) else null)
                    })
                })
            }
        })
    }

    /**
     * reset 命令使用：异步重置 + 主线程加载
     */
    fun resetAndLoadAsync(worldName: String, templatePath: String, done: (World?) -> Unit) {
        ensureWorldReadyAsync(worldName, templatePath, reset = true, done = done)
    }

    /**
     * 仅准备世界文件夹（可在异步线程调用）
     * - reset=false：如果不存在则从模板复制
     * - reset=true：删除旧世界文件夹后从模板复制
     */
    private fun prepareWorldFolder(worldName: String, templatePath: String, reset: Boolean): Boolean {
        val worldFolder = plugin.server.worldContainer.toPath().resolve(worldName)

        return try {
            if (reset && Files.exists(worldFolder)) {
                deleteRecursively(worldFolder)
            }
            if (!Files.exists(worldFolder)) {
                copyTemplateTo(resolveTemplatePath(templatePath), worldFolder, "")
            }
            true
        } catch (e: Exception) {
            plugin.logger.severe("准备世界文件夹失败: ${e.message ?: "unknown"}")
            false
        }
    }

    /**
     * 主线程：把玩家送走并卸载世界
     */
    private fun unloadAndTeleportPlayersSync(worldName: String, done: (Boolean) -> Unit) {
        val world = Bukkit.getWorld(worldName)
        val fallbackWorld = Bukkit.getWorlds().firstOrNull { it.name != worldName }
        if (world != null && fallbackWorld != null) {
            world.players.forEach { it.teleport(fallbackWorld.spawnLocation) }
        }

        if (world != null) {
            val ok = Bukkit.unloadWorld(world, false)
            if (!ok) {
                plugin.logger.warning(LanguageAPI.getText(plugin, "log.unload-world-failed", "world" to worldName))
                done(false)
                return
            }
        }
        done(true)
    }

    /**
     * 删除所有实例世界文件夹
     * 此方法应在服务器关闭时调用，确保所有数据已清理后执行
     */
    fun deleteAllInstanceWorldFolders() {
        val worldContainer = plugin.server.worldContainer.toPath()
        
        // 获取所有地牢配置
        val dungeonConfigs = configManager.loadDungeonConfigs()
        
        // 遍历每个地牢配置，删除其所有实例世界文件夹
        dungeonConfigs.values.forEach { config ->
            try {
                // 查找所有匹配该地牢ID的实例世界文件夹
                Files.list(worldContainer).use { stream ->
                    stream.filter { path ->
                        val fileName = path.fileName.toString()
                        // 匹配格式: {dungeonId}_{instanceId}_world
                        fileName.startsWith("${config.id}_") && fileName.endsWith("_world")
                    }.forEach { path ->
                        try {
                            plugin.logger.info("删除实例世界文件夹: ${path.fileName}")
                            deleteRecursively(path)
                        } catch (e: Exception) {
                            plugin.logger.severe("删除实例世界文件夹失败: ${path.fileName}, 错误: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                plugin.logger.severe("删除地牢 ${config.id} 的实例世界文件夹时出错: ${e.message}")
            }
        }
    }
}
