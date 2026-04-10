
package com.sky4th.equipment.ui.template.sorter

import com.sky4th.equipment.loader.AffixTemplateLoader
import com.sky4th.equipment.modifier.AffixConfigManager
import com.sky4th.equipment.ui.template.TemplateListSorter
import com.sky4th.equipment.ui.template.TemplateListState
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

/**
 * 按名称排序
 * 根据词条的名称进行排序
 */
class NameSorter : TemplateListSorter {

    override fun sort(templates: List<String>, state: TemplateListState): List<String> {
        val ascending = state.customFilters["name_asc"] as? Boolean ?: true
        return if (ascending) {
            templates.sortedBy { templateId ->
                val template = AffixTemplateLoader.getAffixTemplate(templateId)
                getAffixDisplayName(template)
            }
        } else {
            templates.sortedByDescending { templateId ->
                val template = AffixTemplateLoader.getAffixTemplate(templateId)
                getAffixDisplayName(template)
            }
        }
    }

    /**
     * 获取词条的显示名称
     */
    private fun getAffixDisplayName(template: org.bukkit.inventory.ItemStack?): String {
        if (template == null) return ""

        // 从NBT中获取affix_id
        val affixId = template.itemMeta?.persistentDataContainer?.get(
            NamespacedKey("sky_equipment", "affix_id"),
            PersistentDataType.STRING
        )

        // 获取词条配置
        val affixConfig = affixId?.let { AffixConfigManager.getAffixConfig(it) }

        // 返回词条的显示名称
        if (affixConfig != null) {
            return affixConfig.displayName
        }

        // 如果没有词条配置，返回物品的显示名称
        val displayName = template.itemMeta?.displayName()
        return if (displayName != null) {
            PlainTextComponentSerializer.plainText().serialize(displayName)
        } else {
            ""
        }
    }

    override fun getSorterId() = "name"
    override fun getSorterName() = "名称"
}
