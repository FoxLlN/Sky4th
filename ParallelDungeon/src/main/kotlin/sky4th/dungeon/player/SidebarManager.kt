package sky4th.dungeon.player

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Scoreboard
import sky4th.core.api.LanguageAPI
import sky4th.dungeon.loadout.equipment.LoadoutSetSkillState
import sky4th.dungeon.command.DungeonContext
import java.util.*
import net.kyori.adventure.text.minimessage.MiniMessage

/**
 * 计分板管理器
 * 将计分板拆分成多个独立部分,每个部分可以独立更新和缓存
 */
class SidebarManager(
    private val plugin: JavaPlugin,
    private val backpackManager: BackpackManager
) {
    // 计分板部分枚举
    enum class Section {
        SKILL,         // 技能
        CASH,          // 现金
        DUNGEON_TIME   // 地牢倒计时
    }

    // 每个部分的行号
    private val sectionLines = mapOf(
        Section.SKILL to 0,          // 技能在第1行
        Section.CASH to 1,           // 现金在第2行
        Section.DUNGEON_TIME to 2    // 地牢倒计时在第3行
    )

    // 玩家计分板
    private val playerScoreboards: MutableMap<UUID, Scoreboard> = mutableMapOf()

    // 每个部分的缓存
    private val sectionCaches: MutableMap<UUID, MutableMap<Section, String>> = mutableMapOf()

    // 每个部分的entry缓存
    private val sectionEntries: MutableMap<UUID, MutableMap<Section, String>> = mutableMapOf()

    // 铁壁技能倒计时任务
    private val tiebiCountdownTasks: MutableMap<UUID, org.bukkit.scheduler.BukkitTask> = mutableMapOf()

    /**
     * 显示计分板
     */
    fun showSidebar(player: Player) {
        val scoreboard = player.server.scoreboardManager.newScoreboard
        val objectiveName = "dungeon_sidebar"
        var objective = scoreboard.getObjective(objectiveName)
        if (objective == null) {
            @Suppress("DEPRECATION")
            objective = scoreboard.registerNewObjective(
                objectiveName,
                Criteria.DUMMY,
                LanguageAPI.getText(plugin, "backpack.sidebar.title")
            )
        }
        objective.displaySlot = DisplaySlot.SIDEBAR
        playerScoreboards[player.uniqueId] = scoreboard
        player.scoreboard = scoreboard

        // 初始化缓存
        sectionCaches[player.uniqueId] = mutableMapOf()
        sectionEntries[player.uniqueId] = mutableMapOf()

        // 更新所有部分
        updateAllSections(player)
    }

    /**
     * 隐藏计分板
     */
    fun hideSidebar(player: Player) {
        val uuid = player.uniqueId
        playerScoreboards.remove(uuid)
        sectionCaches.remove(uuid)
        sectionEntries.remove(uuid)
        stopTiebiCountdownTask(uuid)
        player.scoreboard = player.server.scoreboardManager.mainScoreboard
    }

    /**
     * 更新所有部分
     */
    fun updateAllSections(player: Player) {
        updateSection(player, Section.SKILL)
        updateSection(player, Section.CASH)
        updateSection(player, Section.DUNGEON_TIME)
    }

    /**
     * 更新指定部分
     * 采用整体更新策略，避免部分内容被顶替
     */
    fun updateSection(player: Player, section: Section) {
        val uuid = player.uniqueId
        val scoreboard = playerScoreboards[uuid] ?: return
        val objective = scoreboard.getObjective("dungeon_sidebar") ?: return

        // 获取该部分的新内容
        val newContent = getSectionContent(player, section)

        // 从缓存中读取当前所有部分的内容
        val currentContents = mutableMapOf<Section, String>()
        Section.entries.forEach { s ->
            if (s == section) {
                // 使用新内容替换指定部分
                currentContents[s] = newContent
            } else {
                // 从缓存中读取其他部分的内容
                currentContents[s] = sectionCaches[uuid]?.get(s) ?: ""
            }
        } 
        
        // 清除所有旧的分数
        sectionEntries[uuid]?.values?.forEach { entry ->
            objective.scoreboard?.resetScores(entry)
        }

        // 重新设置所有部分的内容和分数
        Section.entries.forEach { s ->
            val content = currentContents[s] ?: ""
            val lineNum = sectionLines[s] ?: return@forEach
            val entry = "§$lineNum§$lineNum"
            val teamId = "sb_${s.name.lowercase()}_$lineNum"

            // 获取或创建团队
            var team = scoreboard.getTeam(teamId)
            if (team == null) {
                team = scoreboard.registerNewTeam(teamId)
                team.addEntry(entry)
            } else if (!team.hasEntry(entry)) {
                team.addEntry(entry)
            }

            // 设置内容
            team.prefix(MiniMessage.miniMessage().deserialize(content))
            team.suffix(Component.empty())
            
            // 设置分数
            objective.getScore(entry).score = 3 - lineNum  // 确保标题在顶部
            
            // 更新缓存
            sectionCaches[uuid]?.set(s, content)
            sectionEntries[uuid]?.set(s, entry)
        }

        // 如果更新的是技能部分,管理铁壁技能倒计时任务
        if (section == Section.SKILL) {
            manageTiebiCountdownTask(player)
        }
    }


    /**
     * 获取指定部分的内容
     */
    private fun getSectionContent(player: Player, section: Section): String {
        return when (section) {
            Section.SKILL -> getSkillContent(player)
            Section.CASH -> getCashContent(player)
            Section.DUNGEON_TIME -> getDungeonTimeContent(player)
        }
    }

    /**
     * 获取技能内容
     */
    private fun getSkillContent(player: Player): String {
        val skillParts = mutableListOf<String>()

        if (countSetArmor(player, "ironwall") >= 4) {
            val remainingSec = LoadoutSetSkillState.getTiebiSkillRemainingSeconds(player.uniqueId)
            val tiebiText = if (LoadoutSetSkillState.isTiebiSkillUsed(player.uniqueId) && remainingSec > 0) {
                "§f铁壁 §e${remainingSec}s"
            } else {
                "§f铁壁 §e${LoadoutSetSkillState.getTiebiSkillRemaining(player.uniqueId)}§7/§e2"
            }
            skillParts.add(tiebiText)
        }

        if (countSetArmor(player, "scout") >= 4) {
            skillParts.add(
                if (LoadoutSetSkillState.chikeQualityBoost.contains(player.uniqueId)) {
                    "§f斥候 §a激活中"
                } else {
                    "§f斥候 §e${LoadoutSetSkillState.getChikeSkillRemaining(player.uniqueId)}§7/§e2"
                }
            )
        }

        if (countSetArmor(player, "scholar") >= 4) {
            skillParts.add("§f学者 §e${LoadoutSetSkillState.getXuezheSkillRemaining(player.uniqueId)}§7/§e2")
        }

        if (countSetArmor(player, "ranger") >= 4) {
            skillParts.add(
                if (LoadoutSetSkillState.youxiaRangedBoost.contains(player.uniqueId)) {
                    "§f游侠 §a激活中"
                } else {
                    "§f游侠 §e${LoadoutSetSkillState.getYouxiaSkillRemaining(player.uniqueId)}§7/§e2"
                }
            )
        }

        if (countSetArmor(player, "shaman") >= 4) {
            skillParts.add("§f萨满 §e${LoadoutSetSkillState.getSamanSkillRemaining(player.uniqueId)}§7/§e2")
        }

        return if (skillParts.isEmpty()) {
            LanguageAPI.getText(plugin, "backpack.sidebar.no-skill")
        } else {
            LanguageAPI.getText(plugin, "backpack.sidebar.skill-prefix") + skillParts.joinToString(" ")
        }
    }

    /**
     * 获取现金内容
     */
    private fun getCashContent(player: Player): String {
        val cash = backpackManager.getPlayerCash(player)
        return LanguageAPI.getText(plugin, "backpack.sidebar.cash-amount", "cash" to cash)
    }

    /**
     * 获取地牢倒计时内容
     */
    private fun getDungeonTimeContent(player: Player): String {
        val playerManager = DungeonContext.get()?.playerManager
        if (playerManager == null) {
            return LanguageAPI.getText(plugin, "backpack.sidebar.dungeon-time-inactive")
        }

        val instance = playerManager.getPlayerInstance(player)
        if (instance == null) {
            return LanguageAPI.getText(plugin, "backpack.sidebar.dungeon-time-inactive")
        }

        val remainingSeconds = instance.getRemainingSeconds()
        if (remainingSeconds < 0) {
            return LanguageAPI.getText(plugin, "backpack.sidebar.dungeon-time-unlimited")
        }

        // 当时间到0秒时，触发重置并返回关闭状态
        if (remainingSeconds == 0) {
            return LanguageAPI.getText(plugin, "backpack.sidebar.dungeon-time-closed")
        }

        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        val timeString = String.format("%02d:%02d", minutes, seconds)

        return LanguageAPI.getText(plugin, "backpack.sidebar.dungeon-time", "time" to timeString)
    }

    /**
     * 计算玩家装备的套装数量
     */
    private fun countSetArmor(player: Player, setId: String): Int {
        return backpackManager.countSetArmor(player, setId)
    }

    /**
     * 管理铁壁技能倒计时任务
     */
    private fun manageTiebiCountdownTask(player: Player) {
        val uuid = player.uniqueId
        val isTiebiActive = LoadoutSetSkillState.isTiebiSkillActive(uuid)

        if (isTiebiActive) {
            if (tiebiCountdownTasks[uuid] == null) {
                tiebiCountdownTasks[uuid] = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
                    if (!player.isOnline) {
                        stopTiebiCountdownTask(uuid)
                        return@Runnable
                    }

                    if (!LoadoutSetSkillState.isTiebiSkillActive(uuid)) {
                        stopTiebiCountdownTask(uuid)
                        return@Runnable
                    }

                    // 只更新技能部分
                    updateSection(player, Section.SKILL)
                }, 20L, 20L)
            }
        } else {
            stopTiebiCountdownTask(uuid)
        }
    }

    /**
     * 停止铁壁技能倒计时任务
     */
    private fun stopTiebiCountdownTask(uuid: UUID) {
        tiebiCountdownTasks.remove(uuid)?.cancel()
    }

    /**
     * 清空所有缓存和任务
     */
    fun clearAll() {
        tiebiCountdownTasks.values.forEach { it.cancel() }
        tiebiCountdownTasks.clear()
        sectionCaches.clear()
        sectionEntries.clear()
        playerScoreboards.clear()
    }

    /**
     * 清理指定实例中的侧边栏数据
     * @param instanceFullId 实例完整ID
     */
    fun clearForInstance(instanceFullId: String) {
        // 需要通过DungeonContext获取playerManager来检查玩家所在的实例
        val context = sky4th.dungeon.command.DungeonContext.get() ?: return
        val playerManager = context.playerManager

        // 找出所有在该实例中的玩家
        val playersToRemove = playerScoreboards.filterKeys { playerUuid ->
            val player = Bukkit.getPlayer(playerUuid)
            if (player != null && player.isOnline) {
                val instance = playerManager.getPlayerInstance(player)
                instance?.getFullId() == instanceFullId
            } else {
                false
            }
        }.keys.toList()

        // 清理这些玩家的侧边栏数据
        playersToRemove.forEach { playerUuid ->
            val player = Bukkit.getPlayer(playerUuid)
            if (player != null && player.isOnline) {
                hideSidebar(player)
            } else {
                // 玩家离线，直接清理缓存
                playerScoreboards.remove(playerUuid)
                sectionCaches.remove(playerUuid)
                sectionEntries.remove(playerUuid)
                stopTiebiCountdownTask(playerUuid)
            }
        }
    }
}
