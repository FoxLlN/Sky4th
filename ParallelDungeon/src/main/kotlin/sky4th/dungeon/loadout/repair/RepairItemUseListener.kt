
package sky4th.dungeon.loadout.repair

import sky4th.dungeon.util.LanguageUtil.sendLangSys
import sky4th.core.api.LanguageAPI
import sky4th.dungeon.command.DungeonContext
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.*

/**
 * 维修物品使用逻辑
 * 使用方法：将维修物品放到副手，主手拿着目标物品，按下F（交换副手）开始读条进行耐久维修
 */
class RepairItemUseListener(private val plugin: JavaPlugin) : Listener {

    private val shopIdKey: NamespacedKey by lazy { NamespacedKey(plugin, "loadout_shop_id") }

    /** 维修特效管理器 */
    private val effectManager = NewRepairEffectManager(plugin)

    /** 维修物品ID列表 */
    private val REPAIR_ITEMS = setOf("whetstone_flint")

    /** 读条时长（tick） */
    private val DURATION_TICKS = mapOf(
        "whetstone_flint" to 40  // 磨刀石读条2秒 
    )

    /** 读条时长（秒，用于BossBar标题） */
    private val DURATION_SECONDS = mapOf(
        "whetstone_flint" to 2.0
    )

    /** 维修物品恢复的耐久值 */
    private val REPAIR_AMOUNT = mapOf(
        "whetstone_flint" to 30  // 磨刀石恢复30点耐久
    )

    private data class ChannelState(
        val loadoutId: String,
        val completionTask: org.bukkit.scheduler.BukkitTask,
        val bossBar: org.bukkit.boss.BossBar,
        val updateTask: org.bukkit.scheduler.BukkitTask,
        val startTimeMillis: Long,
        val durationTicks: Int,
        val mainHandItem: ItemStack,
        val offHandItem: ItemStack
    )

    /** 当前读条中的玩家 */
    private val channeling = mutableMapOf<UUID, ChannelState>()

    private fun getLoadoutId(item: ItemStack?): String? {
        val meta = item?.itemMeta ?: return null
        val pdc = meta.persistentDataContainer
        return pdc.get(shopIdKey, PersistentDataType.STRING)
    }

    private fun getRepairItemDisplayName(loadoutId: String): String {
        val ctx = DungeonContext.get() ?: return loadoutId
        val shopConfig = ctx.configManager.getLoadoutShopItemById(loadoutId)
        return shopConfig?.name?.replace("&", "§") ?: loadoutId
    }

    private fun cleanupChannel(player: Player, state: ChannelState?) {
        if (state == null) return
        try {
            state.completionTask.cancel()
        } catch (e: Exception) {
            // 任务可能已经完成或取消
        }
        try {
            state.updateTask.cancel()
        } catch (e: Exception) {
            // 任务可能已经完成或取消
        }
        state.bossBar.removeAll()
        channeling.remove(player.uniqueId)

        // 停止维修特效
        effectManager.stopRepairEffect(player)
    }

    /**
     * 处理玩家交换副手事件（F键）
     * 当玩家按下F键交换副手时，如果副手是维修物品，主手是可维修物品，则开始读条
     */
    @EventHandler(priority = EventPriority.LOW)
    fun onPlayerSwapHandItems(event: org.bukkit.event.player.PlayerSwapHandItemsEvent) {
        val player = event.player
        val ctx = DungeonContext.get() ?: return
        if (!ctx.playerManager.isPlayerInDungeon(player)) {
            player.sendLangSys(plugin, "repair-use.not-in-dungeon")
            event.isCancelled = true
            return
        }

        // 检查玩家是否倒地
        val downedPlayerManager = ctx.downedPlayerManager
        if (downedPlayerManager != null && downedPlayerManager.isPlayerDowned(player.uniqueId)) {
            return
        }

        val mainHandItem = player.inventory.itemInMainHand
        val offHandItem = player.inventory.itemInOffHand

        val offHandRepairId = getLoadoutId(offHandItem)
        if (offHandRepairId != null && offHandRepairId in REPAIR_ITEMS) {
            if (isRepairable(mainHandItem)) {
                event.isCancelled = true

                if (channeling.containsKey(player.uniqueId)) {
                    return
                }

                val durationTicks = DURATION_TICKS[offHandRepairId] ?: return
                val durationSec = DURATION_SECONDS[offHandRepairId] ?: 1.0

                val itemName = getRepairItemDisplayName(offHandRepairId)
                player.sendLangSys(plugin, "repair-use.start", "item" to itemName)

                // 开始显示维修特效，传入维修时长
                effectManager.startRepairEffect(player, mainHandItem, offHandItem, durationTicks)

                val title = LanguageAPI.getText(plugin, "repair-use.bossbar", "item" to itemName, "time" to durationSec.toString()).replace("&", "§")
                val bossBar = Bukkit.createBossBar(title, BarColor.BLUE, BarStyle.SOLID)
                bossBar.addPlayer(player)
                bossBar.progress = 1.0

                val startTime = System.currentTimeMillis()
                val durationMillis = durationTicks * 50L

                val completionTask = plugin.server.scheduler.runTaskLater(plugin, Runnable {
                    val state = channeling.remove(player.uniqueId) ?: return@Runnable
                    state.updateTask.cancel()
                    state.bossBar.removeAll()

                    // 停止维修特效
                    effectManager.stopRepairEffect(player)

                    if (!player.isOnline) return@Runnable
                    if (state.loadoutId != offHandRepairId) return@Runnable

                    // 检查玩家手中的物品是否还是原来的物品
                    val currentMain = player.inventory.itemInMainHand
                    val currentOff = player.inventory.itemInOffHand

                    // 检查副手是否还是维修物品
                    if (getLoadoutId(currentOff) != offHandRepairId || currentOff.amount <= 0) return@Runnable

                    // 检查主手物品是否还是原来的物品且可维修
                    if (!isSameItem(currentMain, state.mainHandItem) || !isRepairable(currentMain)) {
                        return@Runnable
                    }

                    // 执行维修（维修主手的武器）
                    repairItem(currentMain, offHandRepairId)

                    // 消耗维修物品（消耗副手的维修物品）
                    if (currentOff.amount <= 1) {
                        player.inventory.setItemInOffHand(ItemStack(Material.AIR))
                    } else {
                        currentOff.amount = currentOff.amount - 1
                        player.inventory.setItemInOffHand(currentOff)
                    }

                    player.sendLangSys(plugin, "repair-use.success", "item" to getRepairItemDisplayName(offHandRepairId))
                }, durationTicks.toLong())

                val updateTask = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
                    val state = channeling[player.uniqueId] ?: return@Runnable
                    val elapsed = System.currentTimeMillis() - state.startTimeMillis
                    val progress = (1.0 - elapsed.toDouble() / durationMillis).coerceIn(0.0, 1.0)
                    state.bossBar.progress = progress

                    // 检查玩家手中的物品是否发生变化
                    val currentMain = player.inventory.itemInMainHand
                    val currentOff = player.inventory.itemInOffHand

                    val currentOffRepairId = getLoadoutId(currentOff)
                    val isOffMatch = currentOffRepairId == offHandRepairId

                    if (!isOffMatch) {
                        cleanupChannel(player, state)
                        player.sendLangSys(plugin, "repair-use.interrupted")
                        return@Runnable
                    }
                }, 1L, 1L)

                channeling[player.uniqueId] = ChannelState(
                    loadoutId = offHandRepairId,
                    completionTask = completionTask,
                    bossBar = bossBar,
                    updateTask = updateTask,
                    startTimeMillis = startTime,
                    durationTicks = durationTicks,
                    mainHandItem = mainHandItem.clone(),
                    offHandItem = offHandItem.clone()
                )

                return
            }
        }
    }

    /** 禁止在读条过程中切换物品栏格子 */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerItemHeld(event: PlayerItemHeldEvent) {
        val player = event.player
        val state = channeling[player.uniqueId] ?: return
        event.isCancelled = true
        cleanupChannel(player, state)
        val ctx = DungeonContext.get() ?: return
        player.sendLangSys(plugin, "repair-use.interrupted")
    }

    /** 禁止在读条过程中打开背包 */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val player = event.player as? Player ?: return
        val state = channeling[player.uniqueId] ?: return
        event.isCancelled = true
        cleanupChannel(player, state)
        val ctx = DungeonContext.get() ?: return
        player.sendLangSys(plugin, "repair-use.interrupted")
    }

    /** 禁止在读条过程中点击背包 */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val state = channeling[player.uniqueId] ?: return
        event.isCancelled = true
        cleanupChannel(player, state)
        val ctx = DungeonContext.get() ?: return
        player.sendLangSys(plugin, "repair-use.interrupted")
    }

    /** 受到伤害打断读条 */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityDamage(event: EntityDamageEvent) {
        val entity = event.entity
        if (entity !is Player) return
        val ctx = DungeonContext.get() ?: return
        if (!ctx.playerManager.isPlayerInDungeon(entity)) return

        val state = channeling[entity.uniqueId] ?: return
        cleanupChannel(entity, state)
        entity.sendLangSys(plugin, "repair-use.interrupted")
    }

    /** 检查物品是否可维修 */
    private fun isRepairable(item: ItemStack?): Boolean {
        if (item == null || item.type == Material.AIR) return false
        val meta = item.itemMeta ?: return false

        // 检查是否有耐久属性
        val maxDurability = item.type.maxDurability
        if (maxDurability <= 0) return false

        // 检查当前耐久是否小于最大耐久
        val damageable = meta as? Damageable ?: return false
        val currentDurability = damageable.hasDamage()
        if (!currentDurability) return false

        // 检查是否已经满耐久
        val damage = damageable.damage
        return damage > 0
    }

    /** 比较两个物品是否相同（用于检查玩家是否切换了物品） */
    private fun isSameItem(item1: ItemStack?, item2: ItemStack?): Boolean {
        if (item1 == null || item2 == null) return false
        if (item1.type != item2.type) return false
        if (item1.amount != item2.amount) return false

        val meta1 = item1.itemMeta
        val meta2 = item2.itemMeta

        if (meta1 == null && meta2 == null) return true
        if (meta1 == null || meta2 == null) return false

        // 比较显示名称
        val hasName1 = meta1.hasDisplayName()
        val hasName2 = meta2.hasDisplayName()
        if (hasName1 != hasName2) return false
        if (hasName1 && meta1.displayName() != meta2.displayName()) return false

        // 比较附魔
        if (meta1.enchants != meta2.enchants) return false

        return true
    }

    /** 维修物品 */
    private fun repairItem(item: ItemStack, repairItemId: String) {
        val meta = item.itemMeta ?: return
        val repairAmount = REPAIR_AMOUNT[repairItemId] ?: return

        val damageable = meta as? Damageable ?: return
        val currentDamage = damageable.damage
        val newDamage = (currentDamage - repairAmount).coerceAtLeast(0)
        damageable.damage = newDamage

        item.itemMeta = meta
    }
}
