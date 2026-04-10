
package com.sky4th.equipment.ui.impl

import com.sky4th.equipment.modifier.AffixConfigManager
import com.sky4th.equipment.util.LanguageUtil
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import sky4th.core.ui.UITemplate

/**
 * 等级信息处理器
 * 显示当前词条的等级信息
 */
object LevelInfo {

    /**
     * 处理等级信息物品的创建
     */
    fun handleItemCreation(
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): ItemStack? {
        val resultItem = item.clone()
        val meta = resultItem.itemMeta ?: return null

        // 获取玩家状态中的当前词条ID和等级
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "affix_info")
        val currentAffixId = state.customFilters["current_affix_id"] as? String ?: return null
        val currentLevel = state.customFilters["current_level"] as? Int ?: 1

        // 获取词条配置
        val affixConfig = AffixConfigManager.getAffixConfig(currentAffixId) ?: return null
        val maxLevel = affixConfig.maxLevel

        // 从template的feature中读取lore格式
        val loreFormat = template.features["lore"] as? List<*> ?: listOf(
            "&8| &7当前等级 > &e{level}",
            "&8| &7最大等级 > &c{max_level}",
            " ",
            "&8| &7左键点击 &e重置到最大等级"
        )

        // 替换lore中的占位符
        val lore = loreFormat.map { line ->
            var result = line.toString()
            result = result.replace("{level}", currentLevel.toString())
            result = result.replace("{max_level}", maxLevel.toString())
            result
        }

        // 设置lore
        val convertedLore = lore.map {
            sky4th.core.util.ColorUtil.convertLegacyToMiniMessage(it)
        }
        meta.lore(convertedLore.map {
            val loreComponent = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(it)
            LanguageUtil.removeItalic(loreComponent)
        })

        resultItem.itemMeta = meta
        return resultItem
    }

    /**
     * 处理等级信息物品的左键点击
     * 重置等级到最大等级
     */
    fun handleLeftClick(
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): Boolean {
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "affix_info")
        val currentAffixId = state.customFilters["current_affix_id"] as? String ?: return false

        // 获取词条配置
        val affixConfig = AffixConfigManager.getAffixConfig(currentAffixId) ?: return false
        val maxLevel = affixConfig.maxLevel

        // 重置等级到最大等级
        state.customFilters["current_level"] = maxLevel

        // 更新UI
        sky4th.core.ui.UIManager.updateCurrentUI(player)
        return true
    }
}
