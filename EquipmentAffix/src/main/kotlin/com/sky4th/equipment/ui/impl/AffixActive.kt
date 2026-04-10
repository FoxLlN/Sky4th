
package com.sky4th.equipment.ui.impl

import com.sky4th.equipment.modifier.AffixConfigManager
import com.sky4th.equipment.util.LanguageUtil
import com.sky4th.equipment.util.DisplayUtil
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import sky4th.core.ui.UITemplate
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * 活跃信息处理器
 * 根据当前模式（C/E/H）显示对应的内容
 */
object AffixActive {

    /**
     * 处理活跃信息物品的创建
     * 根据当前模式显示不同的内容
     */
    fun handleItemCreation(
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): ItemStack? {
        // 获取玩家状态中的当前模式
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "affix_info")
        val currentMode = state.customFilters["active_mode"] as? String ?: "合成配方"

        // 获取当前template在shape中的所有槽位位置
        val uiConfig = sky4th.core.ui.UIManager.getUI("affix_info") ?: return item
        val allSlotIndices = getAllSlotIndices(uiConfig.shape, template.key)
        val slotIndex = allSlotIndices.indexOf(template.slot)

        if (slotIndex == -1) {
            return item
        }

        // 获取当前词条ID
        val currentAffixId = state.customFilters["current_affix_id"] as? String ?: return item

        // 根据模式显示不同的内容
        return when (currentMode) {
            "冲突词条" -> {
                // 显示冲突词条
                val affixConfig = com.sky4th.equipment.modifier.AffixConfigManager.getAffixConfig(currentAffixId)
                val conflictAffixes = affixConfig?.conflictingAffixes ?: emptyList()

                if (slotIndex >= conflictAffixes.size) {
                    createEmptySlot()
                } else {
                    val conflictAffixId = conflictAffixes[slotIndex]
                    com.sky4th.equipment.ui.AffixItemBuilder.buildAffixItem(conflictAffixId, template.features) ?: createEmptySlot()
                }
            }
            "冲突附魔" -> {
                // 显示冲突附魔
                val affixConfig = com.sky4th.equipment.modifier.AffixConfigManager.getAffixConfig(currentAffixId)
                val conflictEnchants = affixConfig?.conflictingEnchantments ?: emptyList()

                if (slotIndex >= conflictEnchants.size) {
                    createEmptySlot()
                } else {
                    val conflictEnchant = conflictEnchants[slotIndex]
                    createEnchantBook(conflictEnchant, template.features) ?: createEmptySlot()
                }
            }
            "合成配方" -> {
                // 显示合成配方
                val slots = com.sky4th.equipment.loader.AffixTemplateLoader.getRecipeInfo(currentAffixId)
                if (slots == null || slots.isEmpty()) {
                    createEmptySlot()
                } else {
                    createRecipeItem(slots, slotIndex, template.features) ?: createEmptySlot()
                }
            }
            else -> item
        }
    }

    /**
     * 创建附魔书物品
     */
    private fun createEnchantBook(enchantment: org.bukkit.enchantments.Enchantment, features: Map<String, Any>?): ItemStack? {
        val book = ItemStack(org.bukkit.Material.ENCHANTED_BOOK)
        val meta = book.itemMeta ?: return null

        val enchantName = DisplayUtil.getEnchantmentName(enchantment)
        val displayName = "&a$enchantName"
        val nameComponent = LegacyComponentSerializer.legacySection().deserialize(displayName)
        meta.displayName(com.sky4th.equipment.util.LanguageUtil.removeItalic(nameComponent))

        val loreFormat = features?.get("lore") as? List<*> ?: listOf(
            "&8| &7冲突附魔 > &c{enchant}",
            "&7",
            "&8| &7此附魔与当前词条冲突"
        )

        val lore = loreFormat.map { line ->
            var result = line.toString()
            result = result.replace("{enchant}", enchantName)
            result
        }

        val convertedLore = lore.map {
            sky4th.core.util.ColorUtil.convertMiniMessageToLegacy(it)
        }
        meta.lore(convertedLore.map {
            val loreComponent = LegacyComponentSerializer.legacySection().deserialize(it)
            com.sky4th.equipment.util.LanguageUtil.removeItalic(loreComponent)
        })

        book.itemMeta = meta
        return book
    }

    /**
     * 创建配方物品
     */
    private fun createRecipeItem(slots: List<com.sky4th.equipment.loader.AffixTemplateLoader.SlotItem>, slotIndex: Int, features: Map<String, Any>?): ItemStack? {
        // 根据槽位索引获取对应的配方材料
        if (slotIndex >= slots.size) {
            return createEmptySlot()
        }

        val slotItem = slots[slotIndex]
        val materialName = slotItem.material
        val material = org.bukkit.Material.matchMaterial(materialName) ?: return createEmptySlot()

        // 直接创建简单的物品
        return ItemStack(material)
    }

    /**
     * 创建空槽位物品
     */
    private fun createEmptySlot(): ItemStack {
        return ItemStack(org.bukkit.Material.AIR)
    }

    /**
     * 获取指定字符在shape中的所有槽位位置
     */
    private fun getAllSlotIndices(shape: List<String>, slotChar: String): List<Int> {
        val indices = mutableListOf<Int>()
        var index = 0
        for (line in shape) {
            for (char in line) {
                if (char.toString() == slotChar) {
                    indices.add(index)
                }
                index++
            }
        }
        return indices
    }

    /**
     * 处理冲突词条的左键点击
     */
    fun handleLeftClick(
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): Boolean {
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "affix_info")
        val currentMode = state.customFilters["active_mode"] as? String ?: "合成配方"
        if (currentMode == "冲突词条") {
            // 获取当前槽位对应的词条ID
            val currentAffixId = state.customFilters["current_affix_id"] as? String ?: return false
            val affixConfig = com.sky4th.equipment.modifier.AffixConfigManager.getAffixConfig(currentAffixId) ?: return false
            val conflictAffixes = affixConfig.conflictingAffixes

            // 获取当前template在shape中的所有槽位位置
            val uiConfig = sky4th.core.ui.UIManager.getUI("affix_info") ?: return false
            val allSlotIndices = getAllSlotIndices(uiConfig.shape, template.key)
            val slotIndex = allSlotIndices.indexOf(template.slot)

            // 根据槽位索引获取对应的冲突词条ID
            val clickedAffixId = conflictAffixes.getOrNull(slotIndex) ?: return false

            // 获取词条配置
            val clickedaffixConfig = com.sky4th.equipment.modifier.AffixConfigManager.getAffixConfig(clickedAffixId)
            val maxLevel = clickedaffixConfig?.maxLevel ?: 1
            // 将词条ID和当前等级存储到affix_info的状态中
            val affixInfoState = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "affix_info")
            affixInfoState.customFilters["current_affix_id"] = clickedAffixId
            affixInfoState.customFilters["current_level"] = maxLevel  // 初始化为最大等级
            affixInfoState.customFilters["active_mode"] = "合成配方"
            // 打开词条信息页面
            sky4th.core.ui.UIManager.updateCurrentUI(player)
        }
        return false
    }
}
