package com.sky4th.equipment.ui.impl

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import sky4th.core.ui.UITemplate
import com.sky4th.equipment.util.LanguageUtil
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * 材料筛选处理器
 */
object FilterMaterial {

    /**
     * 处理材料筛选按钮的物品创建
     */
    fun handleItemCreation(
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): ItemStack? {
        val resultItem = item.clone()
        val meta = resultItem.itemMeta ?: return null

        // 获取已选择的材料列表
        val selectedMaterials = MaterialList.getSelectedMaterials(player)
        // 获取已排除的材料列表
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "material_filter")
        val excludedMaterials = state.customFilters["excluded_materials"] as? Set<String> ?: emptySet()

        // 确定筛选模式
        val mode = when {
            selectedMaterials.isNotEmpty() -> "&a包含"
            excludedMaterials.isNotEmpty() -> "&c排除"
            else -> "-"
        }

        // 确定要显示的材料列表
        val materials = when {
            selectedMaterials.isNotEmpty() -> selectedMaterials
            excludedMaterials.isNotEmpty() -> excludedMaterials
            else -> emptySet()
        }

        // 获取lore格式
        val loreFormat = template.features["lore"] as? List<*> ?: listOf(
            "&8| &7当前筛选条件: {mode}",
            "&8| > {materials}",
            "&7",
            "&8| &7左键点击 &a选择筛选条件",
            "&8| &7右键点击 &e重置当前筛选"
        )

        // 替换lore中的占位符
        val lore = mutableListOf<String>()
        loreFormat.forEach { line ->
            var result = line.toString()
            result = result.replace("{mode}", mode)

            // 处理materials的显示，根据长度自动换行
            if (result.contains("{materials}")) {
                val materialsText = if (materials.isEmpty()) "&7-" else materials.joinToString(", ")
                if (materialsText.length > 15) {
                    // 将材料列表按每行15个字符分割
                    val materialsLines = mutableListOf<String>()
                    var currentLine = ""
                    materialsText.split(", ").forEach { material ->
                        if (currentLine.isEmpty()) {
                            currentLine = material
                        } else if (currentLine.length + material.length + 2 <= 15) {
                            currentLine += ", $material"
                        } else {
                            materialsLines.add(currentLine)
                            currentLine = material
                        }
                    }
                    if (currentLine.isNotEmpty()) {
                        materialsLines.add(currentLine)
                    }

                    // 替换{materials}为多行文本
                    val prefix = result.substringBefore("{materials}")
                    lore.add(prefix + materialsLines.first())
                    materialsLines.drop(1).forEach { materialLine ->
                        lore.add("&8| &e  $materialLine")
                    }
                } else {
                    result = result.replace("{materials}", materialsText)
                    lore.add(result)
                }
            } else {
                lore.add(result)
            }
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
     * 处理材料筛选按钮的右键点击
     * @return true表示已处理，false表示未处理
     */
    fun handleRightClick(
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): Boolean {
        val selectedMaterials = MaterialList.getSelectedMaterials(player)
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "material_filter")
        val excludedMaterials = state.customFilters["excluded_materials"] as? Set<String> ?: emptySet()

        if (selectedMaterials.isNotEmpty() || excludedMaterials.isNotEmpty()) {
            // 清除所有已选择的材料
            MaterialList.clearSelectedMaterials(player)
            // 清除所有排除的材料
            MaterialList.clearExcludedMaterials(player)
            // 更新UI
            sky4th.core.ui.UIManager.updateCurrentUI(player)
            return true
        }
        return false
    }
}
