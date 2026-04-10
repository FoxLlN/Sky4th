package sky4th.dungeon.monster.head

import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.plugin.java.JavaPlugin

/**
 * 怪物头颅配置
 *
 * 这个文件负责从配置文件读取怪物头颅的配置信息，包括：
 * - 怪物ID
 * - 头颅类型（标准头颅或自定义玩家头颅）
 * - 自定义皮肤纹理（Base64编码）
 * - 显示名称
 *
 * 使用说明：
 * 1. 在 config.yml 的 dungeon.monster-heads 部分添加新的怪物头颅配置
 * 2. 对于标准头颅（如僵尸头、骷髅头），设置 head-type 为 standard 并指定 material
 * 3. 对于自定义头颅（如蜘蛛、卫道士），设置 head-type 为 custom 并提供 texture
 * 4. 纹理可以从 https://minecraft-heads.com/ 获取
 */
object MonsterHeadConfig {

    private lateinit var plugin: JavaPlugin
    private lateinit var configManager: sky4th.dungeon.config.ConfigManager

    /**
     * 初始化配置
     */
    fun init(plugin: JavaPlugin, configManager: sky4th.dungeon.config.ConfigManager) {
        this.plugin = plugin
        this.configManager = configManager
    }

    /**
     * 获取怪物的显示名称（去除颜色代码）
     * @param monsterId 怪物ID
     * @return 去除颜色代码后的显示名称，如果配置不存在则返回null
     */
    fun getPlainDisplayName(monsterId: String): String? {
        val config = findMonsterHeadConfig(monsterId) ?: return null
        val displayName = config.displayName
        // 去除Minecraft颜色代码（&开头后跟一个字符）
        return displayName.replace("&[0-9a-fk-or]".toRegex(RegexOption.IGNORE_CASE), "")
    }

    /**
     * 获取怪物的原始显示名称（包含颜色代码）
     * @param monsterId 怪物ID
     * @return 原始显示名称（包含颜色代码），如果配置不存在则返回null
     */
    fun getRawDisplayName(monsterId: String): String? {
        val config = findMonsterHeadConfig(monsterId) ?: return null
        return config.displayName
    }

    /**
     * 从所有地牢的配置中查找怪物头颅
     */
    private fun findMonsterHeadConfig(monsterId: String): sky4th.dungeon.config.MonsterHeadConfig? {
        // 从所有地牢的配置中查找
        val allDungeons = plugin.config.getConfigurationSection("dungeons") ?: return null
        for (dId in allDungeons.getKeys(false)) {
            val monsterHeads = configManager.getMonsterHeads(dId)
            val config = monsterHeads.find { it.monsterId.equals(monsterId, ignoreCase = true) }
            if (config != null) return config
        }
        return null
    }

    /**
     * 公开 findMonsterHeadConfig 方法，供 MonsterHeadFactory 使用
     */
    fun findMonsterHeadConfigPublic(monsterId: String): sky4th.dungeon.config.MonsterHeadConfig? {
        return findMonsterHeadConfig(monsterId)
    }
}
