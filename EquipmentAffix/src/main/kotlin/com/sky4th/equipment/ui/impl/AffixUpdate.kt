
package com.sky4th.equipment.ui.impl

import com.sky4th.equipment.loader.AffixTemplateLoader
import com.sky4th.equipment.util.LanguageUtil
import com.sky4th.equipment.util.MaterialNameUtil
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import sky4th.core.ui.UITemplate
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer


/**
 * 词条升级材料处理器
 * 显示所有等级的升级材料
 */
object AffixUpdate {

    /**
     * 处理升级材料槽位的物品创建
     */
    fun handleItemCreation(
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): ItemStack? {
        // 获取当前UI配置
        val uiConfig = sky4th.core.ui.UIManager.getUI("affix_info") ?: return null

        // 获取当前template在shape中的所有槽位位置
        val allSlotIndices = getSlotIndicesByColumn(uiConfig.shape, template.key)

        // 根据template的槽位位置确定当前是第几个U槽位
        val slotIndex = allSlotIndices.indexOf(template.slot)

        if (slotIndex == -1) {
            return null
        }

        // 获取玩家状态中的当前词条ID
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "affix_info")
        val currentAffixId = state.customFilters["current_affix_id"] as? String ?: return null

        // 获取升级材料缓存
        val materialsCache = AffixTemplateLoader.getUpgradeMaterialsCache(currentAffixId) ?: return null
        val materialName = materialsCache.first
        val upgradeList = materialsCache.second

        // 检查是否超出升级列表范围
        if (slotIndex >= upgradeList.size) {
            // 超出范围，返回空槽位
            return item
        }

        // 获取当前等级区间需要的材料数量
        val materialCount = upgradeList[slotIndex].toIntOrNull() ?: 0

        // 创建升级材料物品
        val material = MaterialNameUtil.getMaterialFromChinese(materialName) ?: Material.BARRIER
        val resultItem = ItemStack(material, materialCount.coerceAtMost(64))
        val meta = resultItem.itemMeta ?: return resultItem

        // 设置显示名称
        val displayName = "&e$materialName"
        val newName = sky4th.core.util.ColorUtil.convertMiniMessageToLegacy(displayName)
        val nameComponent = LegacyComponentSerializer.legacySection().deserialize(newName)
        meta.displayName(LanguageUtil.removeItalic(nameComponent))

        // 从template的feature中读取lore格式
        val loreFormat = template.features["lore"] as? List<*> ?: listOf(
            "&8| &e{level}&7级 -> &a{next_level}&7级",
            "&8| &7需求 > &e{needs}个"
        )

        // 替换lore中的占位符
        val lore = loreFormat.map { line ->
            var result = line.toString()
            result = result.replace("{level}", slotIndex.toString())
            result = result.replace("{next_level}", (slotIndex + 1).toString())
            result = result.replace("{needs}", "$materialCount")
            result
        }

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
     * 获取指定字符在shape中的所有槽位位置（按列从上到下排序）
     * 例如：UU
     *      UU
     *      UU
     * 返回顺序：0, 2, 4, 1, 3, 5（第一列从上到下，然后第二列从上到下）
     */
    private fun getSlotIndicesByColumn(shape: List<String>, slotChar: String): List<Int> {
        val rows = shape.size
        val cols = shape[0].length

        // 按列收集槽位
        val columns = mutableListOf<MutableList<Int>>()
        for (col in 0 until cols) {
            val columnIndices = mutableListOf<Int>()
            for (row in 0 until rows) {
                val index = row * cols + col
                if (index < shape.joinToString("").length && shape[row][col].toString() == slotChar) {
                    columnIndices.add(index)
                }
            }
            if (columnIndices.isNotEmpty()) {
                columns.add(columnIndices)
            }
        }

        // 将所有列的索引合并成一个列表
        return columns.flatten()
    }
}
