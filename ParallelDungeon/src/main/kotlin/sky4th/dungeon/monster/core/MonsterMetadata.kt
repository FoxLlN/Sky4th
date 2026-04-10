package sky4th.dungeon.monster.core

import org.bukkit.entity.LivingEntity
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

/**
 * 统一管理怪物的持久化数据（ID、等级），方便后续职业/装备按怪物等级做处理。
 *
 * 所有通过 MonsterRegistry 生成的怪物，都会在 PDC 里打上：
 * - dungeon_monster_id
 * - dungeon_monster_level
 */
object MonsterMetadata {

    /**
     * 初始化怪物元数据
     * @param plugin 插件实例
     */
    fun init(plugin: JavaPlugin) {
        MonsterDataKeys.init(plugin)
    }

    /**
     * 标记怪物ID和等级
     * @param entity 实体
     * @param id 怪物ID
     * @param level 怪物等级
     */
    fun tagMonster(entity: LivingEntity, id: String, level: MonsterLevel) {
        val pdc = entity.persistentDataContainer
        pdc.set(MonsterDataKeys.MONSTER_ID_KEY, PersistentDataType.STRING, id)
        pdc.set(MonsterDataKeys.MONSTER_LEVEL_KEY, PersistentDataType.STRING, level.name)
    }

    /**
     * 获取怪物ID
     * @param entity 实体
     * @return 怪物ID，如果不是自定义怪物则返回null
     */
    fun getMonsterId(entity: LivingEntity): String? =
        entity.persistentDataContainer.get(MonsterDataKeys.MONSTER_ID_KEY, PersistentDataType.STRING)

    /**
     * 获取怪物等级
     * @param entity 实体
     * @return 怪物等级，如果未设置则返回null
     */
    fun getMonsterLevel(entity: LivingEntity): MonsterLevel? {
        val raw = entity.persistentDataContainer.get(MonsterDataKeys.MONSTER_LEVEL_KEY, PersistentDataType.STRING) ?: return null
        return runCatching { MonsterLevel.valueOf(raw) }.getOrNull()
    }
}
