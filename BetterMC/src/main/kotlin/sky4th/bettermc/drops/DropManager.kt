package sky4th.bettermc.drops

import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.inventory.ItemStack
import sky4th.bettermc.config.ConfigManager

/**
 * 动物掉落物管理器
 * 
 * 负责管理动物掉落物的数量和种类
 */
object DropManager {

    // 额外掉落物映射：实体类型 -> 掉落物列表
    private val extraDrops = mutableMapOf<EntityType, List<ItemStack>>()

    /**
     * 从配置加载掉落物数据
     */
    fun loadFromConfig() {
        val config = ConfigManager.getConfig()

        // 加载额外掉落物
        extraDrops.clear()
        val dropsSection = config.getConfigurationSection("drops.extra-drops")
        if (dropsSection != null) {
            val entityTypes = dropsSection.getKeys(false)
            entityTypes.forEach { entityName ->
                try {
                    val entityType = EntityType.valueOf(entityName)
                    val dropsList = mutableListOf<ItemStack>()

                    // 获取该实体的掉落物列表
                    val dropsListSection = dropsSection.getMapList(entityName)
                    dropsListSection.forEach { raw ->
                        raw.forEach { (key, value) ->
                            val amount = (value as? Int) ?: 1
                            try {
                                val material = Material.valueOf(key.toString())
                                dropsList.add(ItemStack(material, amount))
                            } catch (e: IllegalArgumentException) {
                            }
                        }
                    }

                    if (dropsList.isNotEmpty()) {
                        extraDrops[entityType] = dropsList
                    }
                } catch (e: IllegalArgumentException) {
                    // 忽略无效的实体类型
                }
            }
        }
    }

    /**
     * 获取实体的额外掉落物
     */
    fun getExtraDrops(entityType: EntityType): List<ItemStack> {
        return extraDrops[entityType] ?: emptyList()
    }
}
