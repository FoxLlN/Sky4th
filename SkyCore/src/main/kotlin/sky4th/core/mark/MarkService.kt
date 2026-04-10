package sky4th.core.mark

import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack

/**
 * 标记服务
 * 提供标记系统的服务层抽象
 */
object MarkService {

    private var initialized = false

    /**
     * 初始化标记服务
     */
    fun initialize() {
        if (initialized) return
        initialized = true
    }

    /**
     * 检查服务是否已初始化
     */
    fun isInitialized(): Boolean = initialized

    /**
     * 创建标记
     * @param victim 被标记的实体
     * @param markId 标记的唯一标识符
     * @param itemStack 显示的物品
     * @param showToAllPlayers 是否对所有玩家显示
     * @param duration 持续时间（秒），0或负数表示永不过期
     * @return Display 实体的 ID
     */
    fun createMark(victim: LivingEntity, markId: String, itemStack: ItemStack, showToAllPlayers: Boolean = false, duration: Long = 0): Int {
        return MarkManager.createMarkDisplay(victim, markId, itemStack, showToAllPlayers, duration)
    }

    /**
     * 移除标记
     * @param victim 被标记的实体
     */
    fun removeMark(victim: LivingEntity) {
        MarkManager.removeMarkDisplay(victim)
    }

    /**
     * 移除特定的标记
     * @param markId 标记的唯一标识符
     */
    fun removeMark(markId: String) {
        MarkManager.removeMarkDisplay(markId)
    }

    /**
     * 移除指定实体的特定标记
     * @param victim 被标记的实体
     * @param markId 标记的唯一标识符
     */
    fun removeMark(victim: LivingEntity, markId: String) {
        MarkManager.removeMarkDisplay(victim, markId)
    }

    /**
     * 检查实体是否有标记
     * @param entity 要检查的实体
     * @return 是否有标记
     */
    fun hasMark(entity: org.bukkit.entity.Entity): Boolean {
        return MarkManager.hasMark(entity)
    }

    /**
     * 检查实体是否有特定的标记
     * @param markId 标记的唯一标识符
     * @return 是否有该标记
     */
    fun hasMark(markId: String): Boolean {
        return MarkManager.hasMark(markId)
    }

    /**
     * 检查实体是否有特定的标记
     * @param entity 要检查的实体
     * @param markId 标记的唯一标识符
     * @return 是否有该标记
     */
    fun hasMark(entity: org.bukkit.entity.Entity, markId: String): Boolean {
        return MarkManager.hasMark(entity, markId)
    }

    /**
     * 更新标记位置
     * @param entity 被标记的实体
     */
    fun updateMarkPosition(entity: org.bukkit.entity.Entity) {
        MarkManager.updateMarkPosition(entity)
    }

    /**
     * 获取实体的所有 Display 实体
     * @param entity 被标记的实体
     * @return Display 实体列表
     */
    fun getMarkDisplays(entity: org.bukkit.entity.Entity): List<ItemDisplay> {
        return MarkManager.getMarkDisplays(entity)
    }

    /**
     * 获取特定标记的 Display 实体
     * @param markId 标记的唯一标识符
     * @return Display 实体
     */
    fun getMarkDisplay(markId: String): ItemDisplay? {
        return MarkManager.getMarkDisplay(markId)
    }

    /**
     * 清理所有标记
     */
    fun clearAllMarks() {
        MarkManager.clearAllMarks()
    }
}
