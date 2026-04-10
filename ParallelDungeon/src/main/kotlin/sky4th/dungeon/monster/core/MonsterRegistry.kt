package sky4th.dungeon.monster.core

import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import org.bukkit.plugin.java.JavaPlugin
import sky4th.dungeon.monster.core.MonsterMetadata

/**
 * 全局怪物注册表。
 *
 * 用法：
 * - 在插件启动时调用 MonsterRegistry.init(plugin)
 * - 在具体怪物文件中调用 MonsterRegistry.register(definition)
 * - 在任意代码中：MonsterRegistry.spawn("standard_zombie", location)
 */
object MonsterRegistry {

    private val definitions: MutableMap<String, MonsterDefinition> = mutableMapOf()

    fun init(plugin: JavaPlugin) {
        MonsterMetadata.init(plugin)
    }

    fun register(definition: MonsterDefinition) {
        definitions[definition.id.lowercase()] = definition
    }

    fun get(id: String): MonsterDefinition? =
        definitions[id.lowercase()]

    /**
     * 获取怪物定义
     */
    fun getDefinition(id: String): MonsterDefinition? =
        definitions[id.lowercase()]

    /**
     * 在指定位置生成给定 ID 的怪物实例。
     */
    fun spawn(id: String, location: Location): LivingEntity? {
        val def = get(id) ?: return null
        return def.spawnAt(location)
    }

    /**
     * 方便后续调试/命令查看当前有哪些已注册怪物。
     */
    fun listIds(): Set<String> = definitions.keys
}
