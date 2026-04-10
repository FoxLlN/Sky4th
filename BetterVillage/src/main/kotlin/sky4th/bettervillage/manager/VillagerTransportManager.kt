
package sky4th.bettervillage.manager

import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import sky4th.bettervillage.BetterVillage

/**
 * 村民搬运管理器
 * 负责处理村民被玩家搬运的逻辑
 * 
 * 功能：
 * 1. 允许有权限的玩家将村民搬离村庄
 * 2. 管理村民搬运状态
 * 3. 防止村民在搬运过程中被传送回村庄
 */
object VillagerTransportManager {

    /**
     * 检查玩家是否有权限搬运村民
     * @param player 要检查的玩家
     * @return 是否有权限
     */
    fun hasTransportPermission(player: Player): Boolean {
        // TODO: 实现权限检查逻辑
        // 可以使用权限系统，如: player.hasPermission("bettervillage.transport")
        return player.hasPermission("bettervillage.transport")
    }

    /**
     * 开始搬运村民
     * @param player 搬运村民的玩家
     * @param villager 被搬运的村民
     * @return 是否成功开始搬运
     */
    fun startTransport(player: Player, villager: Villager): Boolean {
        // 检查玩家权限
        if (!hasTransportPermission(player)) {
            player.sendMessage("§c你没有权限搬运村民！")
            return false
        }

        // 标记村民正在被搬运
        VillagerLeaveManager.setTransporting(villager.uniqueId, true)

        // 发送提示消息
        player.sendMessage("§a你正在搬运村民，村民将不会被传送回村庄")

        BetterVillage.instance.logger.info("玩家 ${player.name} 开始搬运村民 ${villager.uniqueId}")

        return true
    }

    /**
     * 结束搬运村民
     * @param player 搬运村民的玩家
     * @param villager 被搬运的村民
     * @param success 是否成功将村民搬运到新位置
     */
    fun endTransport(player: Player, villager: Villager, success: Boolean = true) {
        // 取消搬运状态
        VillagerLeaveManager.setTransporting(villager.uniqueId, false)

        if (success) {
            // 更新村民所属村庄
            val newVillage = VillageManager.getVillageByLocation(villager.location)
            if (newVillage != null) {
                VillagerStateManager.updateVillagerStatus(villager)
                player.sendMessage("§a村民已成功搬运到新村庄")
            } else {
                player.sendMessage("§e村民已搬运到非村庄区域，村民将自由移动")
            }
        } else {
            player.sendMessage("§c搬运已取消")
        }

        BetterVillage.instance.logger.info("玩家 ${player.name} 结束搬运村民 ${villager.uniqueId}")
    }

    /**
     * 检查村民是否正在被搬运
     * @param villager 要检查的村民
     * @return 是否正在被搬运
     */
    fun isBeingTransported(villager: Villager): Boolean {
        return VillagerLeaveManager.isBeingTransported(villager.uniqueId)
    }
}
