package com.sky4th.equipment.ui.impl

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import sky4th.core.ui.UITemplate
import com.sky4th.equipment.util.LanguageUtil

/**
 * 排序按钮处理器
 */
object Sort {

    /**
     * 处理排序按钮的物品创建
     */
    fun handleItemCreation(
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): ItemStack? {
        val resultItem = item.clone()
        val meta = resultItem.itemMeta ?: return null

        // 获取当前排序模式
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "affix_list")
        val currentModeId = state.customFilters["sort_mode"] as? String

        // 定义排序模式数据
        val sortModes = listOf(
            Pair("name", "名字"),
            Pair("equipment_type", "装备类别"),
            Pair("material", "材料")
        )

        // 查找当前模式，默认为名字排序
        val currentMode = sortModes.find { it.first == currentModeId } ?: sortModes[0]

        // 获取lore格式
        val loreFormat = template.features["lore"] as? List<*> ?: listOf(
            "&8| &7当前排序的模式",
            "&8| > &e{rule}",
            "&7",
            "&8| &7左键点击 &a切换下一个排序模式",
            "&8| &7排序模式 &e<名字|装备类别|材料>"
        )

        // 替换lore中的占位符
        val lore = loreFormat.map { line ->
            var result = line.toString()
            result = result.replace("{rule}", currentMode.second)
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
     * 处理排序按钮的左键点击
     * @return true表示已处理，false表示未处理
     */
    fun handleLeftClick(
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): Boolean {
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "affix_list")
        val currentMode = state.customFilters["sort_mode"] as? String

        // 定义排序模式
        val sortModes = listOf("name", "equipment_type", "material")

        // 获取下一个排序模式
        val currentIndex = sortModes.indexOf(currentMode)
        val nextIndex = if (currentIndex < 0 || currentIndex >= sortModes.size - 1) 0 else currentIndex + 1
        val nextMode = sortModes[nextIndex]

        state.customFilters["sort_mode"] = nextMode
        // 重置到第一页
        state.currentPage = 0
        // 更新UI内容
        sky4th.core.ui.UIManager.updateCurrentUI(player)
        return true
    }
}
