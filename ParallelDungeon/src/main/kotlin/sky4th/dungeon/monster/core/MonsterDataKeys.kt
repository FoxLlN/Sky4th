
package sky4th.dungeon.monster.core

import org.bukkit.NamespacedKey
import org.bukkit.plugin.java.JavaPlugin

/**
 * 怪物数据键统一管理
 * 集中管理所有怪物相关的持久化数据键，避免重复定义和不一致
 */
object MonsterDataKeys {

    // 怪物ID键
    lateinit var MONSTER_ID_KEY: NamespacedKey
        private set

    // 怪物等级键
    lateinit var MONSTER_LEVEL_KEY: NamespacedKey
        private set

    // 实例ID键
    lateinit var INSTANCE_ID_KEY: NamespacedKey
        private set

    // 生成点键
    lateinit var SPAWN_POINT_KEY: NamespacedKey
        private set

    // 仇恨状态键
    lateinit var HAS_AGGRO_KEY: NamespacedKey
        private set

    /**
     * 初始化所有键
     * @param plugin 插件实例
     */
    fun init(plugin: JavaPlugin) {
        MONSTER_ID_KEY = NamespacedKey(plugin, "dungeon_monster_id")
        MONSTER_LEVEL_KEY = NamespacedKey(plugin, "dungeon_monster_level")
        INSTANCE_ID_KEY = NamespacedKey(plugin, "instance_id")
        SPAWN_POINT_KEY = NamespacedKey(plugin, "spawn_point")
        HAS_AGGRO_KEY = NamespacedKey(plugin, "has_aggro")
    }
}
