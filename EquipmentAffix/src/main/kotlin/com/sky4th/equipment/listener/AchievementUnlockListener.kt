package com.sky4th.equipment.listener

import com.sky4th.equipment.EquipmentAffix
import com.sky4th.equipment.loader.AffixTemplateLoader
import com.sky4th.equipment.manager.RecipeManager
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import java.util.*

/**
 * 成就解锁监听器
 * 监听玩家获得成就后解锁对应阶段的模板配方
 */
object AchievementUnlockListener : Listener {

    // 进度与装备阶段的映射关系（硬编码）
    // 使用原版Minecraft进度名称直接对应装备阶段
    private val achievementStageMapping = mapOf(
        "minecraft:story/mine_stone" to "主世界",        // 石器时代：获得圆石
        "minecraft:story/mine_diamond" to "深层",        // 钻石！：获得钻石
        "minecraft:story/enter_the_nether" to "地狱",    // 勇往直下：通过传送门进入下界
        "minecraft:story/enter_the_end" to "末地"        // 结束了？：进入末地
    )

    /**
     * 初始化监听器
     */
    fun initialize(plugin: EquipmentAffix) {
        // 注册监听器
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    /**
     * 处理玩家获得成就事件
     * 注意：这里使用PlayerAdvancementDoneEvent，需要确保使用的是支持该事件的Bukkit版本
     */
    @EventHandler
    fun onPlayerAchievement(event: org.bukkit.event.player.PlayerAdvancementDoneEvent) {
        val player = event.player
        val advancement = event.advancement

        // 获取进度key（用于匹配映射关系）
        val advancementKey = advancement.key
        val advancementId = "${advancementKey.namespace}:${advancementKey.key}"

        // 检查该成就是否映射到某个阶段
        val stage = achievementStageMapping[advancementId]
        if (stage != null) {
            unlockStageForPlayer(player, stage)
        }
    }

    /**
     * 为玩家解锁指定阶段的所有模板
     * @param player 玩家
     * @param stage 阶段名称
     */
    fun unlockStageForPlayer(player: Player, stage: String) {
        val uuid = player.uniqueId

        // 获取该阶段的所有模板ID
        val templateIds = AffixTemplateLoader.getTemplatesByStage(stage)

        if (templateIds.isEmpty()) {
            return
        }

        // 为玩家解锁所有模板的配方
        var unlockedCount = 0
        templateIds.forEach { templateId ->
            val recipeKey = NamespacedKey("sky_equipment", "affix_template_${templateId.lowercase()}")

            // 发现配方（使玩家可以看到并使用配方）
            player.discoverRecipe(recipeKey)
            unlockedCount++
        }
    }

    /**
     * 手动为玩家解锁阶段（用于命令或其他触发方式）
     * @param player 玩家
     * @param stage 阶段名称
     */
    fun forceUnlockStage(player: Player, stage: String) {
        unlockStageForPlayer(player, stage)
    }
}
