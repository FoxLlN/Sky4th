
package sky4th.bettervillage.listeners

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.ItemStack
import sky4th.bettervillage.BetterVillage
import sky4th.bettervillage.manager.VillagerTransportManager
import java.util.UUID

/**
 * 村民搬运监听器
 * 负责处理玩家与村民的交互，实现搬运功能
 */
class VillagerTransportListener : Listener {

    // 存储正在搬运村民的玩家：Player UUID -> Villager UUID
    private val transportingPlayers = mutableMapOf<UUID, UUID>()

    /**
     * 玩家与村民交互事件
     * 当玩家使用特定物品与村民交互时，开始或结束搬运
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val player = event.player
        val entity = event.rightClicked

        // 检查是否是村民
        if (entity !is Villager) return

        // 检查玩家是否在潜行状态
        if (!player.isSneaking) return

        // 检查玩家手中是否持有特定物品（使用拴绳作为搬运工具）
        val item = player.inventory.itemInMainHand
        if (item.type != Material.LEAD) return

        // 检查玩家权限
        if (!VillagerTransportManager.hasTransportPermission(player)) {
            player.sendMessage("§c你没有权限搬运村民！")
            return
        }

        // 检查是否正在搬运
        if (transportingPlayers.containsKey(player.uniqueId)) {
            // 结束搬运
            val villagerId = transportingPlayers.remove(player.uniqueId)
            val villager = entity.world.entities.find { it.uniqueId == villagerId } as? Villager

            if (villager != null) {
                VillagerTransportManager.endTransport(player, villager)
            }
        } else {
            // 开始搬运
            val success = VillagerTransportManager.startTransport(player, entity)
            if (success) {
                transportingPlayers[player.uniqueId] = entity.uniqueId
            }
        }

        // 取消原事件，防止村民交易窗口打开
        event.isCancelled = true
    }

    /**
     * 清理玩家搬运状态
     * @param player 要清理的玩家
     */
    fun clearTransportState(player: Player) {
        val villagerId = transportingPlayers.remove(player.uniqueId) ?: return

        val villager = player.world.entities.find { it.uniqueId == villagerId } as? Villager
        if (villager != null) {
            VillagerTransportManager.endTransport(player, villager, success = false)
        }
    }
}
