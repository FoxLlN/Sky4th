package com.sky4th.equipment.ui.impl

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import sky4th.core.ui.UITemplate
import com.sky4th.equipment.util.LanguageUtil
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * 筛选名称处理器
 */
object FilterName {

    /**
     * 处理筛选按钮的物品创建
     */
    fun handleItemCreation(
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): ItemStack? {
        val resultItem = item.clone()
        val meta = resultItem.itemMeta ?: return null

        // 获取当前搜索关键词
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "affix_list")
        val searchQuery = state.searchQuery

        // 获取lore格式
        val loreFormat = template.features["lore"] as? List<*> ?: listOf(
            "&8| &7当前筛选的字符 ", 
            "&8| > {name}",
            "&7",
            "&8| &7左键点击 &a输入筛选字符",
            "&8| &7右键点击 &e重置当前筛选"
        )

        // 替换lore中的占位符
        val lore = loreFormat.map { line ->
            var result = line.toString()
            result = result.replace("{name}", if (searchQuery.isEmpty()) "&7-" else searchQuery)
            result
        }

        // 设置lore
        val convertedLore = lore.map {
            sky4th.core.util.ColorUtil.convertMiniMessageToLegacy(it)
        }
        meta.lore(convertedLore.map {
            val loreComponent = LegacyComponentSerializer.legacySection().deserialize(it)
            LanguageUtil.removeItalic(loreComponent)
        })

        resultItem.itemMeta = meta
        return resultItem
    }

    /**
     * 处理筛选名称按钮的右键点击
     * @return true表示已处理，false表示未处理
     */
    fun handleRightClick(
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): Boolean {
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "affix_list")
        if (state.searchQuery != "") {
            state.searchQuery = ""
            sky4th.core.ui.UIManager.updateCurrentUI(player)
            return true
        }
        return false
    }
}
