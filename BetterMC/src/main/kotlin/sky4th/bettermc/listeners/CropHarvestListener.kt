package sky4th.bettermc.listeners

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.Ageable
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import sky4th.bettermc.command.FeatureManager
import kotlin.random.Random

/**
 * 作物收割监听器
 * 
 * 用于处理玩家右键成熟作物直接收割的功能
 * 收割后作物会退回到初始生长阶段，并产生掉落物
 */
class CropHarvestListener : Listener {

    // 支持的作物类型
    private val supportedCrops = setOf(
        Material.WHEAT,           // 小麦
        Material.CARROTS,          // 胡萝卜
        Material.POTATOES,         // 土豆
        Material.BEETROOTS,        // 甜菜根
        Material.NETHER_WART,      // 地狱疣
        Material.COCOA,            // 可可豆
        Material.HAY_BLOCK,        // 干草块
        Material.MELON,            // 西瓜
        Material.PUMPKIN,          // 南瓜
    )

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        // 检查功能是否启用
        if (!FeatureManager.isFeatureEnabled("crop-harvest")) return

        // 只处理右键点击
        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        // 只处理主手触发的交互事件，避免副手重复触发
        if (event.hand != org.bukkit.inventory.EquipmentSlot.HAND) return

        val player = event.player
        val block = event.clickedBlock ?: return
        val blockData = block.blockData

        // 检查玩家主手中物品是否为可放置的方块
        val itemInHand = player.inventory.itemInMainHand
        if (itemInHand.type.isBlock && itemInHand.type.isSolid) {
            // 如果是可放置的方块，则不执行收割操作，让玩家正常放置方块
            return
        }
        val itemInOffHand = player.inventory.itemInOffHand
        if (itemInOffHand.type.isBlock && itemInOffHand.type.isSolid) {
            // 如果是可放置的方块，则不执行收割操作，让玩家正常放置方块
            return
        }

        // 检查是否是支持的作物
        if (!supportedCrops.contains(block.type)) return

        // 特殊处理干草块、西瓜和南瓜（这些不需要检查成熟度）
        if (block.type == Material.HAY_BLOCK || block.type == Material.MELON || block.type == Material.PUMPKIN) {
            harvestCrop(block, player)
            event.isCancelled = true
            return
        }

        // 检查作物是否成熟
        if (blockData !is Ageable) return
        if (blockData.age < blockData.maximumAge) return

        // 收割作物
        harvestCrop(block, player)

        // 取消事件，防止其他插件或原版行为干扰
        event.isCancelled = true
    }

    /**
     * 收割作物并重置生长阶段
     */
    private fun harvestCrop(block: Block, player: Player) {
        // 对于干草块、西瓜和南瓜，直接破坏方块
        if (block.type == Material.HAY_BLOCK || block.type == Material.MELON || block.type == Material.PUMPKIN) {
            // 根据作物类型生成掉落物
            val drops = getDrops(block.type)
            
            // 掉落物品到世界中
            drops.forEach { item ->
                block.world.dropItemNaturally(block.location, item)
            }
            
            // 破坏方块
            block.type = Material.AIR
            
            // 播放收割音效
            block.world.playEffect(block.location, org.bukkit.Effect.STEP_SOUND, block.type)
            return
        }

        val blockData = block.blockData as Ageable

        // 根据作物类型生成掉落物
        val drops = getDrops(block.type)

        // 掉落物品到世界中
        drops.forEach { item ->
            block.world.dropItemNaturally(block.location, item)
        }

        // 重置作物生长阶段
        blockData.age = 0
        block.blockData = blockData

        // 可选：播放收割音效
        block.world.playEffect(block.location, org.bukkit.Effect.STEP_SOUND, block.type)
    }

    /**
     * 根据作物类型获取掉落物
     */
    private fun getDrops(material: Material): List<ItemStack> {
        return when (material) {
            Material.WHEAT -> listOf(ItemStack(Material.WHEAT, 1), ItemStack(Material.WHEAT_SEEDS, Random.nextInt(1, 4)))
            Material.CARROTS -> listOf(ItemStack(Material.CARROT, Random.nextInt(1, 5)))
            Material.POTATOES -> {
                val drops = mutableListOf(ItemStack(Material.POTATO, Random.nextInt(1, 5)))
                // 2%概率掉落毒土豆
                if (Random.nextDouble() < 0.02) {
                    drops.add(ItemStack(Material.POISONOUS_POTATO, 1))
                }
                drops
            }
            Material.BEETROOTS -> listOf(ItemStack(Material.BEETROOT, 1), ItemStack(Material.BEETROOT_SEEDS, Random.nextInt(1, 3)))
            Material.NETHER_WART -> listOf(ItemStack(Material.NETHER_WART, Random.nextInt(2, 5)))
            Material.COCOA -> listOf(ItemStack(Material.COCOA_BEANS, Random.nextInt(2, 4)))
            Material.HAY_BLOCK -> listOf(ItemStack(Material.WHEAT, 9))
            Material.MELON -> listOf(ItemStack(Material.MELON_SLICE, Random.nextInt(3, 8)))
            Material.PUMPKIN -> listOf(ItemStack(Material.PUMPKIN, 1))
            else -> emptyList()
        }
    }
}
