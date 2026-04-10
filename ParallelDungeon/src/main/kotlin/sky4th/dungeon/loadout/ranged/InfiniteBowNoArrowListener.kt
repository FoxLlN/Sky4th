package sky4th.dungeon.loadout.ranged

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

/**
 * 无限弓（infinite_bow）：不占箭矢格子。
 * - 拉弓时若背包无箭，临时注入 1 支箭供射出；满背包时临时「借」一格（存起该格物品，射完/取消后还原）。
 * - 射出后或切走武器后立即移除该箭并还原借格，保持 0 箭不占格。
 */
class InfiniteBowNoArrowListener(private val plugin: JavaPlugin) : Listener {

    private val shopIdKey: NamespacedKey by lazy { NamespacedKey(plugin, "loadout_shop_id") }

    /** 本局由我们临时注入箭的玩家（射出或切武器后移除箭并移出此集合） */
    private val addedArrowFor = mutableSetOf<UUID>()

    /** 满背包时借用的格子：玩家 -> (格子下标, 当时存起的物品)，射完/取消后还原 */
    private val borrowedSlot = mutableMapOf<UUID, Pair<Int, ItemStack>>()

    private fun isWuxianBow(item: ItemStack?): Boolean =
        item?.type == Material.BOW && item.itemMeta?.persistentDataContainer?.get(shopIdKey, PersistentDataType.STRING) == WUXIAN_GONG_ID

    /** 检查玩家在背包（9-35）中是否有箭矢 */
    private fun hasNoArrowsInBackpack(player: Player): Boolean {
        return !(9..35).any { i ->
            val item = player.inventory.getItem(i)
            item != null && item.type == Material.ARROW && item.amount > 0
        }
    }

    /** 拉弓（右键持弓）：无箭时临时给 1 支；只在背包（9-35）中替换箭矢，不影响装备栏（0-8） */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onInteract(event: PlayerInteractEvent) {
        // 防止双击触发：只处理主手点击
        if (event.hand != org.bukkit.inventory.EquipmentSlot.HAND) {
            return
        }
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return
        val player = event.player
        val main = player.inventory.itemInMainHand
        if (!isWuxianBow(main)) return
        
        // 只检查背包（9-35）中是否有箭，不影响装备栏（0-8）
        if (!hasNoArrowsInBackpack(player)) return
        
        val uuid = player.uniqueId
        val arrow = ItemStack(Material.ARROW, 1)
        
        // 只在背包（9-35）中添加箭
        // 尝试在背包（9-35）中找到空位或可堆叠的箭
        for (i in 9..35) {
            val item = player.inventory.getItem(i)
            if (item == null || item.type.isAir) {
                player.inventory.setItem(i, arrow)
                addedArrowFor.add(uuid)
                return
            }
            if (item.type == Material.ARROW && item.amount < item.maxStackSize) {
                item.amount++
                addedArrowFor.add(uuid)
                return
            }
        }
        // 背包满了，借用背包格子
        for (i in 9..35) {
            val stack = player.inventory.getItem(i) ?: continue
            if (stack.type.isAir) continue
            val stored = stack.clone()
            player.inventory.setItem(i, arrow)
            borrowedSlot[uuid] = i to stored
            addedArrowFor.add(uuid)
            return
        }
    }

    /** 射出后：移除临时箭并还原借格 */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onShootBow(event: EntityShootBowEvent) {
        if (event.entity !is Player) return
        val player = event.entity as Player
        if (!isWuxianBow(event.bow)) return
        if (!addedArrowFor.remove(player.uniqueId)) return
        plugin.server.scheduler.runTaskLater(plugin, Runnable { removeArrowAndRestore(player) }, 1L)
    }

    /** 切走武器（未射出）：移除临时箭并还原借格 */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onItemHeld(event: PlayerItemHeldEvent) {
        val player = event.player
        if (!addedArrowFor.remove(player.uniqueId)) return
        removeArrowAndRestore(player)
    }

    private fun removeArrowAndRestore(player: Player) {
        removeOneArrow(player)
        val uuid = player.uniqueId
        val borrowed = borrowedSlot.remove(uuid) ?: return
        val (slot, stored) = borrowed
        player.inventory.setItem(slot, stored)
    }

    /** 移除一支箭：只在背包（9-35）中移除，不影响装备栏（0-8） */
    private fun removeOneArrow(player: Player) {
        for (i in 9..35) {
            val stack = player.inventory.getItem(i) ?: continue
            if (stack.type != Material.ARROW) continue
            if (stack.amount <= 1) {
                player.inventory.setItem(i, ItemStack(Material.AIR))
            } else {
                stack.amount = stack.amount - 1
            }
            return
        }
    }

    companion object {
        const val WUXIAN_GONG_ID = "infinite_bow"
    }
}
