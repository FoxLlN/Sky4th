package sky4th.core.api

import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack
import sky4th.core.mark.MarkService

/**
 * 标记系统对外 API（唯一入口）
 *
 * 提供在实体头顶显示标记的功能，使用 ItemDisplay 实体实现。
 * 所有调用统一委托给 MarkService，未初始化时操作无效。
 */
object MarkAPI {

    /**
     * 检查标记系统是否可用
     */
    fun isAvailable(): Boolean = MarkService.isInitialized()

    /**
     * 创建标记
     * @param victim 被标记的实体
     * @param markId 标记的唯一标识符（用于区分不同类型的标记）
     * @param itemStack 显示的物品
     * @param showToAllPlayers 是否对所有玩家显示（默认为 false）
     * @param duration 持续时间（秒），0或负数表示永不过期（默认为 0）
     * @return Display 实体的 ID，如果创建失败返回 -1
     */
    fun createMark(victim: LivingEntity, markId: String, itemStack: ItemStack, showToAllPlayers: Boolean = false, duration: Long = 0): Int {
        if (!isAvailable()) return -1
        return MarkService.createMark(victim, markId, itemStack, showToAllPlayers, duration)
    }

    /**
     * 移除标记
     * @param victim 被标记的实体
     */
    fun removeMark(victim: LivingEntity) {
        if (!isAvailable()) return
        MarkService.removeMark(victim)
    }

    /**
     * 移除特定的标记
     * @param markId 标记的唯一标识符
     */
    fun removeMark(markId: String) {
        if (!isAvailable()) return
        MarkService.removeMark(markId)
    }

    /**
     * 移除指定实体的特定标记
     * @param victim 被标记的实体
     * @param markId 标记的唯一标识符
     */
    fun removeMark(victim: LivingEntity, markId: String) {
        if (!isAvailable()) return
        MarkService.removeMark(victim, markId)
    }

    /**
     * 检查实体是否有标记
     * @param entity 要检查的实体
     * @return 是否有标记
     */
    fun hasMark(entity: org.bukkit.entity.Entity): Boolean {
        if (!isAvailable()) return false
        return MarkService.hasMark(entity)
    }

    /**
     * 检查实体是否有特定的标记
     * @param markId 标记的唯一标识符
     * @return 是否有该标记
     */
    fun hasMark(markId: String): Boolean {
        if (!isAvailable()) return false
        return MarkService.hasMark(markId)
    }

    /**
     * 检查实体是否有特定的标记
     * @param entity 要检查的实体
     * @param markId 标记的唯一标识符
     * @return 是否有该标记
     */
    fun hasMark(entity: org.bukkit.entity.Entity, markId: String): Boolean {
        if (!isAvailable()) return false
        return MarkService.hasMark(entity, markId)
    }

    /**
     * 更新标记位置
     * @param entity 被标记的实体
     */
    fun updateMarkPosition(entity: org.bukkit.entity.Entity) {
        if (!isAvailable()) return
        MarkService.updateMarkPosition(entity)
    }

    /**
     * 获取实体的所有 Display 实体
     * @param entity 被标记的实体
     * @return Display 实体列表，如果不存在或系统不可用则返回空列表
     */
    fun getMarkDisplays(entity: org.bukkit.entity.Entity): List<ItemDisplay> {
        if (!isAvailable()) return emptyList()
        return MarkService.getMarkDisplays(entity)
    }

    /**
     * 获取特定标记的 Display 实体
     * @param markId 标记的唯一标识符
     * @return Display 实体，如果不存在或系统不可用则返回 null
     */
    fun getMarkDisplay(markId: String): ItemDisplay? {
        if (!isAvailable()) return null
        return MarkService.getMarkDisplay(markId)
    }

    /**
     * 清理所有标记
     */
    fun clearAllMarks() {
        if (!isAvailable()) return
        MarkService.clearAllMarks()
    }
}
