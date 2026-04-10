package sky4th.dungeon.loadout.supply

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
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import java.util.*

/**
 * 副本内补给品右键使用：绷带/急救包/解毒剂。
 * 右键开始读条，BossBar 与提示显示读条进度；读条期间受到非中毒伤害则打断；解毒剂读条时中毒伤害不打断。
 */
class SupplyItemUseListener(private val plugin: JavaPlugin) : Listener {

    private val shopIdKey: NamespacedKey by lazy { NamespacedKey(plugin, "loadout_shop_id") }
    private val lootIdKey: NamespacedKey by lazy { NamespacedKey(plugin, "dungeon_loot_id") }

    private val SUPPLY_IDS = setOf("bandage", "first_aid_kit", "antidote")

    /** 读条时长（tick）：绷带 2s=40, 急救包 4s=80, 解毒剂 2.5s=50, 粗糙绷带 2s=40 */
    private val DURATION_TICKS = mapOf(
        "bandage" to 40,
        "first_aid_kit" to 80,
        "antidote" to 50,
        "rough_bandage" to 40
    )

    /** 读条时长（秒，用于 BossBar 标题） */
    private val DURATION_SECONDS = mapOf(
        "bandage" to 2.0,
        "first_aid_kit" to 4.0,
        "antidote" to 2.5,
        "rough_bandage" to 2.0
    )

    private data class ChannelState(
        val loadoutId: String,
        val completionTask: BukkitTask,
        val bossBar: org.bukkit.boss.BossBar,
        val updateTask: BukkitTask,
        val startTimeMillis: Long,
        val durationTicks: Int
    )

    /** 当前读条中的玩家 */
    private val channeling = mutableMapOf<UUID, ChannelState>()

    private fun getLoadoutId(item: ItemStack?): String? {
        val meta = item?.itemMeta ?: return null
        val pdc = meta.persistentDataContainer

        // 优先检查配装补给品 ID
        val shopId = pdc.get(shopIdKey, PersistentDataType.STRING)
        if (shopId != null) return shopId

        // 检查掉落物 ID
        val lootId = pdc.get(lootIdKey, PersistentDataType.STRING)
        if (lootId != null) {
            // 将掉落物 ID 映射到对应的补给品 ID
            return when (lootId) {
                "rough_bandage" -> "rough_bandage"
                else -> null
            }
        }

        return null
    }

    private fun getSupplyDisplayName(loadoutId: String): String {
        val ctx = DungeonContext.get() ?: return loadoutId

        // 首先尝试从配装补给品中获取名称
        val shopConfig = ctx.configManager.getLoadoutShopItemById(loadoutId)
        if (shopConfig != null) {
            return shopConfig.name.replace("&", "§")
        }

        // 如果是掉落物，从掉落物配置中获取名称
        // 无法获取玩家当前所在的地牢ID，因为此方法没有player参数
        // 尝试从所有地牢配置中查找
        val dungeonConfigs = ctx.configManager.loadDungeonConfigs()
        for ((dungeonId, dungeonConfig) in dungeonConfigs) {
            val lootConfig = ctx.configManager.getLootItemById(dungeonId, loadoutId)
            if (lootConfig != null) {
                return lootConfig.name.replace("&", "§")
            }
        }

        return loadoutId
    }

    private fun cleanupChannel(player: Player, state: ChannelState?) {
        if (state == null) return
        state.completionTask.cancel()
        state.updateTask.cancel()
        state.bossBar.removeAll()
        channeling.remove(player.uniqueId)
    }

    /** 右键使用（对方块或空气） */
    @EventHandler(priority = EventPriority.LOW)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        // 防止双击触发：只处理主手点击
        if (event.hand != org.bukkit.inventory.EquipmentSlot.HAND) {
            return
        }
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return
        val player = event.player
        val ctx = DungeonContext.get() ?: return
        if (!ctx.playerManager.isPlayerInDungeon(player)) return

        val main = player.inventory.itemInMainHand
        val loadoutId = getLoadoutId(main) ?: return

        // 检查是否是可用的补给品（配装补给品或掉落物）
        val isUsable = loadoutId in SUPPLY_IDS || loadoutId == "rough_bandage"
        if (!isUsable) return

        // 检查玩家是否倒地
        val downedPlayerManager = ctx.downedPlayerManager
        if (downedPlayerManager != null && downedPlayerManager.isPlayerDowned(player.uniqueId)) {
            return
        }

        // 检查加血物品：如果当前血量已经是最大值，则不允许使用
        if (loadoutId == "bandage" || loadoutId == "first_aid_kit" || loadoutId == "rough_bandage") {
            val maxHp = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH)?.value ?: 20.0
            if (player.health >= maxHp) {
                event.isCancelled = true
                player.sendLangSys(ctx.plugin, "supply-use.full-health")
                return
            }
        }

        if (channeling.containsKey(player.uniqueId)) {
            event.isCancelled = true
            return
        }

        val durationTicks = DURATION_TICKS[loadoutId] ?: return
        val durationSec = DURATION_SECONDS[loadoutId] ?: 1.0
        event.isCancelled = true

        val itemName = getSupplyDisplayName(loadoutId)
        player.sendLangSys(ctx.plugin, "supply-use.start", "item" to itemName)

        val title = LanguageAPI.getText(ctx.plugin, "supply-use.bossbar", "item" to itemName, "time" to durationSec.toString())
        val bossBar = Bukkit.createBossBar(title, BarColor.GREEN, BarStyle.SOLID)
        bossBar.addPlayer(player)
        bossBar.progress = 1.0

        val startTime = System.currentTimeMillis()
        val durationMillis = durationTicks * 50L
        val heldSlot = player.inventory.heldItemSlot

        val completionTask = plugin.server.scheduler.runTaskLater(plugin, Runnable {
            val state = channeling.remove(player.uniqueId) ?: return@Runnable
            state.updateTask.cancel()
            state.bossBar.removeAll()

            if (!player.isOnline) return@Runnable
            if (state.loadoutId != loadoutId) return@Runnable

            // 检查玩家是否还在原来的格子，且物品是否还在手中
            if (player.inventory.heldItemSlot != heldSlot) return@Runnable

            val hand = player.inventory.itemInMainHand
            if (getLoadoutId(hand) != loadoutId || hand.amount <= 0) return@Runnable

            applyEffect(player, loadoutId)
            if (hand.amount <= 1) {
                player.inventory.setItemInMainHand(ItemStack(Material.AIR))
            } else {
                hand.amount = hand.amount - 1
                player.inventory.setItemInMainHand(hand)
            }
            player.sendLangSys(ctx.plugin, "supply-use.success", "item" to getSupplyDisplayName(loadoutId))
        }, durationTicks.toLong())

        val updateTask = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            val state = channeling[player.uniqueId] ?: return@Runnable
            val elapsed = System.currentTimeMillis() - state.startTimeMillis
            val progress = (1.0 - elapsed.toDouble() / durationMillis).coerceIn(0.0, 1.0)
            state.bossBar.progress = progress

            // 检查玩家是否还在原来的格子，如果不在则打断读条
            if (player.inventory.heldItemSlot != heldSlot) {
                cleanupChannel(player, state)
                player.sendLangSys(ctx.plugin, "supply-use.interrupted")
                return@Runnable
            }
        }, 1L, 1L)

        channeling[player.uniqueId] = ChannelState(
            loadoutId = loadoutId,
            completionTask = completionTask,
            bossBar = bossBar,
            updateTask = updateTask,
            startTimeMillis = startTime,
            durationTicks = durationTicks
        )
    }

    /** 禁止在读条过程中切换物品栏格子 */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerItemHeld(event: PlayerItemHeldEvent) {
        val player = event.player
        val state = channeling[player.uniqueId] ?: return
        event.isCancelled = true
        cleanupChannel(player, state)
        val ctx = DungeonContext.get() ?: return
        player.sendLangSys(ctx.plugin, "supply-use.interrupted")
    }

    /** 禁止在读条过程中打开背包 */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val player = event.player as? Player ?: return
        val state = channeling[player.uniqueId] ?: return
        event.isCancelled = true
        cleanupChannel(player, state)
        val ctx = DungeonContext.get() ?: return
        player.sendLangSys(ctx.plugin, "supply-use.interrupted")
    }

    /** 禁止在读条过程中点击背包 */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val state = channeling[player.uniqueId] ?: return
        event.isCancelled = true
        cleanupChannel(player, state)
        val ctx = DungeonContext.get() ?: return
        player.sendLangSys(ctx.plugin, "supply-use.interrupted")
    }

    /** 受到伤害打断读条（解毒剂读条时仅非中毒伤害打断） */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityDamage(event: EntityDamageEvent) {
        val entity = event.entity
        if (entity !is Player) return
        val ctx = DungeonContext.get() ?: return
        if (!ctx.playerManager.isPlayerInDungeon(entity)) return

        val state = channeling[entity.uniqueId] ?: return
        if (state.loadoutId == "antidote" && event.cause == EntityDamageEvent.DamageCause.POISON) {
            return
        }
        cleanupChannel(entity, state)
        entity.sendLangSys(ctx.plugin, "supply-use.interrupted")
    }

    private fun applyEffect(player: Player, loadoutId: String) {
        when (loadoutId) {
            "bandage" -> {
                val maxHp = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH)?.value ?: 20.0
                val health = (player.health + 4.0).coerceIn(0.0, maxHp)
                player.health = health
            }
            "rough_bandage" -> {
                val maxHp = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH)?.value ?: 20.0
                val health = (player.health + 2.0).coerceIn(0.0, maxHp)
                player.health = health
            }
            "first_aid_kit" -> {
                val maxHp = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH)?.value ?: 20.0
                val health = (player.health + 10.0).coerceIn(0.0, maxHp)
                player.health = health
            }
            "antidote" -> {
                player.removePotionEffect(PotionEffectType.POISON)
            }
        }
    }
}
