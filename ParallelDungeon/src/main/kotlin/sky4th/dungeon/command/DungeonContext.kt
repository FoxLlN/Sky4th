package sky4th.dungeon.command

import sky4th.dungeon.config.ConfigManager
import sky4th.dungeon.container.ContainerSearchManager
import sky4th.dungeon.monster.event.MonsterSearchManager
import sky4th.dungeon.monster.spawn.DynamicMonsterManager
import sky4th.dungeon.player.PlayerDeathListener
import sky4th.dungeon.player.PlayerDeathUI
import sky4th.dungeon.player.PlayerManager
import sky4th.dungeon.player.DungeonDataStorage
import sky4th.dungeon.player.DownedPlayerManager
import sky4th.dungeon.world.TemplateWorldManager
import sky4th.dungeon.team.TeamManager
import sky4th.dungeon.util.LanguageUtil.sendLang
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import sky4th.core.api.LanguageAPI

/** 命令上下文：插件与各管理器，供 impl 子命令使用 */
data class CommandContext(
    val plugin: JavaPlugin,
    val configManager: ConfigManager,
    val playerManager: PlayerManager,
    val templateWorldManager: TemplateWorldManager,
    val containerSearchManager: ContainerSearchManager,
    val monsterSearchManager: MonsterSearchManager,
    val playerDeathListener: PlayerDeathListener,
    val playerDeathUI: PlayerDeathUI,
    val dynamicMonsterManager: DynamicMonsterManager,
    val dungeonInstanceManager: sky4th.dungeon.model.DungeonInstanceManager,
    val dataStorage: DungeonDataStorage,
    val teamManager: TeamManager?,
    val downedPlayerManager: DownedPlayerManager?,
    val sidebarManager: sky4th.dungeon.player.SidebarManager
)

object DungeonContext {
    @Volatile
    private var ctx: CommandContext? = null

    fun init(
        plugin: JavaPlugin,
        configManager: ConfigManager,
        playerManager: PlayerManager,
        templateWorldManager: TemplateWorldManager,
        containerSearchManager: ContainerSearchManager,
        monsterSearchManager: MonsterSearchManager,
        playerDeathListener: PlayerDeathListener,
        playerDeathUI: PlayerDeathUI,
        dynamicMonsterManager: DynamicMonsterManager,
        dungeonInstanceManager: sky4th.dungeon.model.DungeonInstanceManager,
        dataStorage: DungeonDataStorage,
        teamManager: TeamManager?,
        downedPlayerManager: DownedPlayerManager?,
        sidebarManager: sky4th.dungeon.player.SidebarManager
    ) {
        ctx = CommandContext(plugin, configManager, playerManager, templateWorldManager, containerSearchManager, monsterSearchManager, playerDeathListener, playerDeathUI, dynamicMonsterManager, dungeonInstanceManager, dataStorage, teamManager, downedPlayerManager, sidebarManager)
    }

    fun get(): CommandContext? = ctx

    fun getOrThrow(): CommandContext = ctx ?: error("DungeonContext 未初始化，请确保 ParallelDungeon 已加载")

    fun showHelp(sender: CommandSender, context: CommandContext) {
        @Suppress("DEPRECATION")
        val pluginName = context.plugin.description.name
        // 显示帮助时不需要特定地牢ID，因为帮助信息是通用的
        LanguageAPI.getTextList(context.plugin, "command.help", "plugin" to pluginName, "dungeonId" to "ALL")
            .forEach { sender.sendLang(ontext.plugin, it) }
    }

    /**
     * 重置地牢状态
     * 清空所有与地牢世界相关的数据，但不影响玩家仓库、信用点、装备等个人数据
     */
    fun resetDungeonState(context: CommandContext) {
        // 清空玩家管理器中的地牢相关数据
        context.playerManager.clearAllDeathData()

        // 清空死亡相关数据
        context.playerDeathListener.clearAllDeathData()
        context.playerDeathUI.clearAllDeathSearchData()

        // 清空搜索数据
        context.containerSearchManager.clearAllSearchData()
        context.monsterSearchManager.clearAllMonsterSearchData()
    }
}
