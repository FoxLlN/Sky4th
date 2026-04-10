
package sky4th.bettervillage.listeners

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.VillagerCareerChangeEvent
import org.bukkit.event.entity.VillagerReplenishTradeEvent
import org.bukkit.persistence.PersistentDataType
import sky4th.bettervillage.BetterVillage

/**
 * 村民职业锁定监听器
 *
 * 功能：
 * 防止已锁定职业的村民因工作方块被拆除而重置职业
 */
class VillagerProfessionLockListener : Listener {

    companion object {
        // 用于标记村民职业已锁定的NBT键
        private val PROFESSION_LOCKED_KEY = BetterVillage.namespacedKey("profession_locked")
    }

    /**
     * 村民职业变更事件处理
     * 防止已锁定职业的村民因工作方块被拆除而重置职业
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onVillagerCareerChange(event: VillagerCareerChangeEvent) {
        val villager = event.entity

        // 检查村民职业是否已锁定
        if (isProfessionLocked(villager)) {
            // 如果职业从有职业变为无职业（工作方块被拆除），取消事件
            if (event.profession == org.bukkit.entity.Villager.Profession.NONE) {
                event.isCancelled = true
            }
        }
    }

    /**
     * 检查村民职业是否已锁定
     */
    private fun isProfessionLocked(villager: org.bukkit.entity.Villager): Boolean {
        val container = villager.persistentDataContainer
        return container.has(PROFESSION_LOCKED_KEY, PersistentDataType.BYTE)
    }
}
