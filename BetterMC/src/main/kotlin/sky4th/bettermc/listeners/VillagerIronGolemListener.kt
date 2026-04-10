package sky4th.bettermc.listeners

import org.bukkit.entity.EntityType
import org.bukkit.entity.Villager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import sky4th.bettermc.command.FeatureManager

/**
 * 村民铁傀儡生成监听器
 *
 * 功能：
 * 禁止村民因为恐慌生成铁傀儡
 * 当村民受到惊吓时，不会生成铁傀儡来保护村庄
 */
class VillagerIronGolemListener : Listener {

    /**
     * 监听生物生成事件
     * 取消由村民恐慌生成的铁傀儡
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onCreatureSpawn(event: CreatureSpawnEvent) {
        // 检查功能是否启用
        if (!FeatureManager.isFeatureEnabled("villager-iron-golem")) return

        // 只处理铁傀儡的生成
        if (event.entityType != EntityType.IRON_GOLEM) return

        // 检查生成原因是否是村民恐慌
        if (event.spawnReason != CreatureSpawnEvent.SpawnReason.VILLAGE_DEFENSE) return

        // 取消生成事件
        event.isCancelled = true
    }
}
