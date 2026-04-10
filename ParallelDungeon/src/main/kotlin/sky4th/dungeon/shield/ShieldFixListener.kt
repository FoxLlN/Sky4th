package sky4th.dungeon.shield

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

/**
 * 修复盾牌遮挡右侧动画的问题
 * 
 * 当玩家右键进行某些操作（如拉弓、打开容器等）时，
 * 暂时禁用盾牌的举起效果，避免遮挡右侧动画
 */
class ShieldFixListener(private val plugin: JavaPlugin) : Listener {

    // 记录玩家是否应该禁用盾牌举起效果
    private val shieldDisabledPlayers = mutableSetOf<UUID>()

    // 记录玩家的恢复任务
    private val playerRestoreTasks = mutableMapOf<UUID, BukkitTask>()

    // 需要禁用盾牌的物品类型
    private val shieldBlockingItems = setOf(
        Material.BOW,
        Material.CROSSBOW,
        Material.TRIDENT,
        Material.FISHING_ROD,
        Material.GOAT_HORN
    )

    // 需要禁用盾牌的方块类型（右键交互）
    private val shieldBlockingBlocks = setOf(
        Material.CHEST,
        Material.TRAPPED_CHEST,
        Material.ENDER_CHEST,
        Material.BARREL,
        Material.SHULKER_BOX,
        Material.WHITE_SHULKER_BOX,
        Material.ORANGE_SHULKER_BOX,
        Material.MAGENTA_SHULKER_BOX,
        Material.LIGHT_BLUE_SHULKER_BOX,
        Material.YELLOW_SHULKER_BOX,
        Material.LIME_SHULKER_BOX,
        Material.PINK_SHULKER_BOX,
        Material.GRAY_SHULKER_BOX,
        Material.LIGHT_GRAY_SHULKER_BOX,
        Material.CYAN_SHULKER_BOX,
        Material.PURPLE_SHULKER_BOX,
        Material.BLUE_SHULKER_BOX,
        Material.BROWN_SHULKER_BOX,
        Material.GREEN_SHULKER_BOX,
        Material.RED_SHULKER_BOX,
        Material.BLACK_SHULKER_BOX,
        Material.CRAFTING_TABLE,
        Material.FURNACE,
        Material.BLAST_FURNACE,
        Material.SMOKER,
        Material.ANVIL,
        Material.CHIPPED_ANVIL,
        Material.DAMAGED_ANVIL,
        Material.SMITHING_TABLE,
        Material.FLETCHING_TABLE,
        Material.CARTOGRAPHY_TABLE,
        Material.LOOM,
        Material.GRINDSTONE,
        Material.STONECUTTER,
        Material.BREWING_STAND,
        Material.ENCHANTING_TABLE,
        Material.BEACON,
        Material.HOPPER,
        Material.DISPENSER,
        Material.DROPPER,
        Material.CAULDRON,
        Material.WATER_CAULDRON,
        Material.LAVA_CAULDRON,
        Material.POWDER_SNOW_CAULDRON
    )

    /**
     * 处理玩家右键交互事件
     */
    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val action = event.action
        val item = event.item

        // 防止双击触发：只处理主手点击
        if (event.hand != org.bukkit.inventory.EquipmentSlot.HAND) {
            return
        }

        // 只处理右键点击
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return

        // 检查玩家是否拿着盾牌
        val offHandItem = player.inventory.itemInOffHand
        val mainHandItem = player.inventory.itemInMainHand

        val hasShieldInOffHand = offHandItem.type == Material.SHIELD
        val hasShieldInMainHand = mainHandItem.type == Material.SHIELD

        if (!hasShieldInOffHand && !hasShieldInMainHand) return

        // 检查是否拿着需要禁用盾牌的物品
        if (item != null && shieldBlockingItems.contains(item.type)) {
            disableShieldTemporarily(player)
            return
        }

        // 检查是否右键点击需要禁用盾牌的方块
        if (action == Action.RIGHT_CLICK_BLOCK) {
            val block = event.clickedBlock
            if (block != null) {
                // 检查是否是原版容器
                if (shieldBlockingBlocks.contains(block.type)) {
                    disableShieldTemporarily(player)
                    return
                }
                
                // 检查是否是怪物头颅
                if (isMonsterHead(block)) {
                    disableShieldTemporarily(player)
                    return
                }
            }
        }

        // 检查是否拿着食物
        if (item != null && item.type.isEdible) {
            disableShieldTemporarily(player)
            return
        }

        // 检查是否拿着可投掷物品（如雪球、蛋等）
        if (item != null && isThrowableItem(item.type)) {
            disableShieldTemporarily(player)
            return
        }
    }

    /**
     * 处理玩家打开库存事件
     */
    @EventHandler
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val player = event.player as? Player ?: return

        // 检查玩家是否拿着盾牌
        val offHandItem = player.inventory.itemInOffHand
        val mainHandItem = player.inventory.itemInMainHand

        val hasShieldInOffHand = offHandItem.type == Material.SHIELD
        val hasShieldInMainHand = mainHandItem.type == Material.SHIELD

        if (!hasShieldInOffHand && !hasShieldInMainHand) return

        // 打开库存时禁用盾牌
        disableShieldTemporarily(player)
    }

    /**
     * 处理玩家发射投射物事件（如箭）
     */
    @EventHandler
    fun onProjectileLaunch(event: ProjectileLaunchEvent) {
        val player = event.entity.shooter as? Player ?: return

        // 检查玩家是否拿着盾牌
        val offHandItem = player.inventory.itemInOffHand
        val mainHandItem = player.inventory.itemInMainHand

        val hasShieldInOffHand = offHandItem.type == Material.SHIELD
        val hasShieldInMainHand = mainHandItem.type == Material.SHIELD

        if (!hasShieldInOffHand && !hasShieldInMainHand) return

        // 发射投射物时禁用盾牌
        disableShieldTemporarily(player)
    }

    /**
     * 暂时禁用玩家的盾牌举起效果
     */
    private fun disableShieldTemporarily(player: Player) {
        val uuid = player.uniqueId

        // 取消之前的恢复任务
        playerRestoreTasks[uuid]?.cancel()

        // 标记玩家禁用盾牌
        shieldDisabledPlayers.add(uuid)

        // 如果玩家拿着盾牌，暂时将其从副手移除
        val offHandItem = player.inventory.itemInOffHand
        if (offHandItem.type == Material.SHIELD) {
            // 暂时将盾牌从副手移除
            player.inventory.setItemInOffHand(null)
            
            // 延迟恢复盾牌
            val restoreTask = Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                // 只有当玩家当前副手没有物品时才恢复盾牌
                if (player.inventory.itemInOffHand.type == Material.AIR) {
                    player.inventory.setItemInOffHand(offHandItem)
                }
                shieldDisabledPlayers.remove(uuid)
                playerRestoreTasks.remove(uuid)
            }, 1L) 
            
            playerRestoreTasks[uuid] = restoreTask
        } else {
            // 如果玩家没有拿着盾牌，直接标记为禁用
            val restoreTask = Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                shieldDisabledPlayers.remove(uuid)
                playerRestoreTasks.remove(uuid)
            }, 1L) // 1tick秒后恢复
            
            playerRestoreTasks[uuid] = restoreTask
        }
    }

    /**
     * 检查物品是否为可投掷物品
     */
    private fun isThrowableItem(material: Material): Boolean {
        return when (material) {
            Material.SNOWBALL,
            Material.EGG,
            Material.ENDER_PEARL,
            Material.ENDER_EYE,
            Material.POTION,
            Material.SPLASH_POTION,
            Material.LINGERING_POTION,
            Material.EXPERIENCE_BOTTLE,
            Material.FIREWORK_ROCKET -> true
            else -> false
        }
    }
    
    /**
     * 检查方块是否是怪物头颅
     */
    private fun isMonsterHead(block: org.bukkit.block.Block): Boolean {
        // 检查方块是否是头颅类型（支持所有类型的头颅）
        val isHeadBlock = when (block.type) {
            Material.PLAYER_HEAD, Material.PLAYER_WALL_HEAD,
            Material.ZOMBIE_HEAD, Material.ZOMBIE_WALL_HEAD,
            Material.SKELETON_SKULL, Material.SKELETON_WALL_SKULL,
            Material.CREEPER_HEAD, Material.CREEPER_WALL_HEAD,
            Material.DRAGON_HEAD, Material.DRAGON_WALL_HEAD,
            Material.PIGLIN_HEAD, Material.PIGLIN_WALL_HEAD -> true
            else -> false
        }
        
        if (!isHeadBlock) {
            return false
        }
        
        // 检查方块状态是否有怪物ID标记
        val state = block.state as? org.bukkit.block.Skull ?: return false
        
        // 尝试从方块状态获取怪物ID
        val monsterKey = NamespacedKey(plugin, "monster_id")
        val container = state.persistentDataContainer
        val result = container.get(monsterKey, PersistentDataType.STRING)
        return result != null
    }
}
