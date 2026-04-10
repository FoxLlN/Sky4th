
package sky4th.bettervillage.manager

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Villager
import org.bukkit.scheduler.BukkitTask
import sky4th.bettervillage.BetterVillage
import sky4th.bettervillage.config.ConfigManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 村民边界检查器
 * 负责定期检查村民位置，并将离开村庄的村民传送回村庄中心
 */
object VillagerBoundaryChecker {

    private var task: BukkitTask? = null
    private var currentBatchIndex = 0
    private val batchSize = 20

    /**
     * 启动边界检查任务
     */
    fun start() {
        if (task != null) return

        task = Bukkit.getScheduler().runTaskTimer(
            BetterVillage.instance,
            ::checkVillagers,
            0L,
            ConfigManager.getCheckInterval().toLong()
        )
    }

    /**
     * 停止边界检查任务
     */
    fun stop() {
        task?.cancel()
        task = null
    }

    /**
     * 检查所有村民
     */
    private fun checkVillagers() {
        // 获取所有村民
        val allVillagers = Bukkit.getWorlds()
            .flatMap { it.entities.filterIsInstance<Villager>() }

        if (allVillagers.isEmpty()) return

        // 分批处理
        val batches = allVillagers.chunked(batchSize)
        val currentBatch = batches[currentBatchIndex]

        // 检查当前批次的村民
        currentBatch.forEach { villager ->
            checkVillager(villager)
        }

        // 更新批次索引
        currentBatchIndex = (currentBatchIndex + 1) % batches.size
    }

    /**
     * 检查单个村民
     * @param villager 要检查的村民
     */
    private fun checkVillager(villager: Villager) {
        // 更新村民状态
        val statusChange = VillagerStateManager.updateVillagerStatus(villager) ?: return

        // 根据状态变化采取相应措施
        when (statusChange) {
            VillagerStateManager.VillagerStatus.OUTSIDE_VILLAGE -> {
                // 离开村庄，记录离开并传送回村庄
                val state = VillagerStateManager.getVillagerState(villager.uniqueId)
                if (state != null) {
                    VillagerLeaveManager.recordLeave(villager, state.villageId)
                }
            }
            VillagerStateManager.VillagerStatus.IN_VILLAGE -> {
                // 回到村庄内，清理离开记录
                VillagerLeaveManager.markReturned(villager.uniqueId)
            }
        }
    }

}
