package com.sky4th.equipment.ui.impl

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import sky4th.core.ui.UITemplate
import com.sky4th.equipment.util.LanguageUtil
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * 装备类型筛选处理器
 */
object FilterType {

    /**
     * 处理装备类型筛选按钮的物品创建
     */
    fun handleItemCreation(
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): ItemStack? {
        val resultItem = item.clone()
        val meta = resultItem.itemMeta ?: return null

        // 获取已选择的类型列表
        val selectedTypes = TypeList.getSelectedTypes(player)
        // 获取已排除的类型列表
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "type_filter")
        val excludedTypes = state.customFilters["excluded_types"] as? Set<String> ?: emptySet()

        // 确定筛选模式
        val mode = when {
            selectedTypes.isNotEmpty() -> "&a包含"
            excludedTypes.isNotEmpty() -> "&c排除"
            else -> "-"
        }

        // 确定要显示的类型列表
        val types = when {
            selectedTypes.isNotEmpty() -> selectedTypes
            excludedTypes.isNotEmpty() -> excludedTypes
            else -> emptySet()
        }

        // 获取lore格式
        val loreFormat = template.features["lore"] as? List<*> ?: listOf(
            "&8| &7当前筛选条件: {mode}",
            "&8| > {types}",
            "&7",
            "&8| &7左键点击 &a选择筛选条件",
            "&8| &7右键点击 &e重置当前筛选"
        )

        // 替换lore中的占位符
        val lore = mutableListOf<String>()
        loreFormat.forEach { line ->
            var result = line.toString()
            result = result.replace("{mode}", mode)

            // 处理types的显示，根据长度自动换行
            if (result.contains("{types}")) {
                // 将英文类别转换为中文显示名称
                val displayTypes = types.map { getTypeDisplayName(it) }
                val typesText = if (displayTypes.isEmpty()) "&7-" else displayTypes.joinToString(", ")
                if (typesText.length > 15) {
                    // 将类型列表按每行15个字符分割
                    val typesLines = mutableListOf<String>()
                    var currentLine = ""
                    typesText.split(", ").forEach { type ->
                        if (currentLine.isEmpty()) {
                            currentLine = type
                        } else if (currentLine.length + type.length + 2 <= 15) {
                            currentLine += ", $type"
                        } else {
                            typesLines.add(currentLine)
                            currentLine = type
                        }
                    }
                    if (currentLine.isNotEmpty()) {
                        typesLines.add(currentLine)
                    }

                    // 替换{types}为多行文本
                    val prefix = result.substringBefore("{types}")
                    lore.add(prefix + typesLines.first())
                    typesLines.drop(1).forEach { typeLine ->
                        lore.add("&8| &e  $typeLine")
                    }
                } else {
                    result = result.replace("{types}", typesText)
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
     * 处理装备类型筛选按钮的右键点击
     * @return true表示已处理，false表示未处理
     */
    fun handleRightClick(
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): Boolean {
        val selectedTypes = TypeList.getSelectedTypes(player)
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "type_filter")
        val excludedTypes = state.customFilters["excluded_types"] as? Set<String> ?: emptySet()

        if (selectedTypes.isNotEmpty() || excludedTypes.isNotEmpty()) {
            // 清除所有已选择的类型
            TypeList.clearSelectedTypes(player)
            // 清除所有排除的类型
            TypeList.clearExcludedTypes(player)
            // 更新UI
            sky4th.core.ui.UIManager.updateCurrentUI(player)
            return true
        }
        return false
    }
    
    /**
     * 类型名称映射
     * 将英文类别转换为中文显示名称
     */
    private fun getTypeDisplayName(typeKey: String): String {
        return when (typeKey) {
            "LIGHT_ARMOR_HEAD" -> "轻型头盔"
            "HEAVY_ARMOR_HEAD" -> "重型头盔"
            "LIGHT_ARMOR_CHEST" -> "轻型胸甲"
            "HEAVY_ARMOR_CHEST" -> "重型胸甲"
            "LIGHT_ARMOR_LEGS" -> "轻型护腿"
            "HEAVY_ARMOR_LEGS" -> "重型护腿"
            "LIGHT_ARMOR_FEET" -> "轻型靴子"
            "HEAVY_ARMOR_FEET" -> "重型靴子"
            "WEAPON_SWORD" -> "剑"
            "WEAPON_AXE" -> "斧"
            "TOOL_SHOVEL" -> "铲"
            "TOOL_PICKAXE" -> "镐"
            "TOOL_HOE" -> "锄"
            "WEAPON_MACE" -> "重锤"
            "WEAPON_TRIDENT" -> "三叉戟"
            "TOOL_FISHING_ROD" -> "钓鱼竿"
            "BOW_BOW" -> "弓"
            "BOW_CROSSBOW" -> "弩"
            "SHIELD_SHIELD" -> "盾牌"
            "ELYTRA_ELYTRA" -> "鞘翅"
            else -> typeKey
        }
    }
}
