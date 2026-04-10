package sky4th.dungeon.loadout.equipment

import sky4th.dungeon.Dungeon
import sky4th.dungeon.command.CommandContext
import sky4th.dungeon.command.DungeonContext
import sky4th.dungeon.util.LanguageUtil.sendLangSys
import org.bukkit.NamespacedKey
import java.util.UUID
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

/**
 * 套装监听器基类：统一「套装 id、装备计数、4 件套判定、蹲下主动技能调度、离开副本清理」。
 * 子类只需实现 [refreshPassive]、[onLeaveDungeon]，以及可选的 [onSneakActiveSkill]（蹲满时间后执行）。
 */
abstract class BaseLoadoutSetListener(
    protected val plugin: JavaPlugin,
    protected val setId: String
) : Listener {

    protected val loadoutSetKey: NamespacedKey by lazy { NamespacedKey(plugin, "loadout_set") }

    /** 记录玩家正在蓄力技能的结束时间 */
    private val playerSkillCharging: MutableMap<UUID, Long> = mutableMapOf()

    /** 玩家身上该套装件数 */
    protected fun countArmor(player: Player): Int =
        player.inventory.armorContents.count { isSetItem(it) }

    /** 是否至少 4 件（满足主动技能条件） */
    protected fun hasFourPiece(player: Player): Boolean = countArmor(player) >= 4

    /** 是否至少 2 件 */
    protected fun hasTwoPiece(player: Player): Boolean = countArmor(player) >= 2

    /** 是否为该套装部件 */
    protected fun isSetItem(item: ItemStack?): Boolean {
        if (item == null || item.type.isEmpty) return false
        return item.itemMeta?.persistentDataContainer?.get(loadoutSetKey, PersistentDataType.STRING) == setId
    }

    /**
     * 调度「蹲下后」执行主动技能。
     * @param player 蹲下的玩家
     * @param ctx 副本上下文（调用前需已校验在副本内）
     * @param recheck 蹲下时间结束后再次判定，返回 false 则取消执行（如仍蹲着、仍 4 件、技能仍可用等）
     * @param onTrigger 通过 recheck 后执行的具体技能逻辑（发消息、刷新计分板由子类在 onTrigger 内完成）
     */
    protected fun scheduleSneakSkill(
        player: Player,
        ctx: CommandContext,
        recheck: () -> Boolean,
        onTrigger: () -> Unit
    ) {
        val playerId = player.uniqueId
        val currentTime = System.currentTimeMillis()

        // 检查玩家是否倒地
        val downedPlayerManager = ctx.downedPlayerManager
        if (downedPlayerManager != null && downedPlayerManager.isPlayerDowned(player.uniqueId)) {
            return
        }

        // 检查是否有套装技能效果正在生效
        if (LoadoutSetSkillState.isAnySetSkillActive(playerId)) {
            player.sendLangSys(plugin, "loadout.common.skill-active")
            return
        }

        // 记录蓄力结束时间（3秒后）
        playerSkillCharging[playerId] = currentTime + 3000
        
        object : BukkitRunnable() {
            override fun run() {
                // 清除蓄力记录
                playerSkillCharging.remove(playerId)

                if (!player.isOnline) return
                if (!ctx.playerManager.isPlayerInDungeon(player)) return
                if (!recheck()) return
                if (!player.isSneaking) {
                    return
                }
                if (!hasFourPiece(player)) {
                    return
                }
                onTrigger()
            }
        }.runTaskLater(plugin, 3 * 20L)
    }

    /**
     * 子类可重写：在蹲下开始时注册主动技能。
     * 默认不处理；子类做前置校验后调用 [scheduleSneakSkill]。
     */
    open fun onSneakActiveSkill(event: PlayerToggleSneakEvent) {}

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerToggleSneak(event: PlayerToggleSneakEvent) {
        onSneakActiveSkill(event)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerChangedWorld(event: PlayerChangedWorldEvent) {
        val playerManager = DungeonContext.get()?.playerManager ?: return
        // 检查玩家是否从地牢世界离开
        if (playerManager.isPlayerInDungeon(event.player)) {
            onLeaveDungeon(event.player)
            plugin.server.scheduler.runTask(plugin, Runnable { refreshPassive(event.player) })
        }
    }

    /** 玩家离开副本时清理本套装状态（如已用次数、激活标记等） */
    protected abstract fun onLeaveDungeon(player: Player)

    /** 装备变更或离开副本后刷新被动效果；由 EquipmentRefreshCoordinator 统一调用 */
    abstract fun refreshPassive(player: Player)
}
