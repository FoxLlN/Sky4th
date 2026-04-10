package com.sky4th.equipment.ui.impl

import com.sky4th.equipment.loader.AffixTemplateLoader
import com.sky4th.equipment.util.LanguageUtil
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import sky4th.core.ui.UITemplate

/**
 * 材料列表处理器
 * 负责显示和管理升级材料列表
 */
object MaterialList {

    // 存储所有唯一的升级材料名称和对应的词条数量
    // Map<材料名称, 词条数量>
    private val allMaterials = mutableMapOf<String, Int>()

    /**
     * 初始化材料列表
     * 从所有词条模板中提取升级材料并统计数量
     */
    fun initialize() {
        if (allMaterials.isNotEmpty()) {
            return
        }
        allMaterials.clear()
        val allAffixIds = AffixTemplateLoader.getAllTemplateIds()
        for (affixId in allAffixIds) {
            val materialCache = AffixTemplateLoader.getUpgradeMaterialsCache(affixId)
            if (materialCache != null) {
                val materialName = materialCache.first
                allMaterials[materialName] = (allMaterials[materialName] ?: 0) + 1
            }
        }
    }

    /**
     * 获取所有升级材料名称（排序后）
     */
    fun getAllMaterials(): List<String> {
        return allMaterials.keys.toList().sorted()
    }

    /**
     * 处理材料列表的物品创建
     */
    fun handleItemCreation(
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): ItemStack? {
        // 获取当前UI配置
        val uiConfig = sky4th.core.ui.UIManager.getUI(getCurrentUIId(player))
        if (uiConfig == null) {
            return null
        }

        // 计算当前页的材料数量
        val pageSize = countTemplateSlots(uiConfig.shape, template.key)

        // 获取当前template在shape中的所有槽位位置
        val allSlotIndices = getAllSlotIndices(uiConfig.shape, template.key)

        // 根据template的槽位位置确定当前是第几个槽位
        val slotIndex = allSlotIndices.indexOf(template.slot)

        // 如果槽位不在材料列表中，返回null
        if (slotIndex == -1) {
            return null
        }

        // 获取筛选后的材料列表
        val filteredMaterials = getFilteredMaterials(player)

        // 获取当前页码
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "material_filter")
        val currentPage = state.currentPage

        // 计算当前页的起始索引
        val startIndex = currentPage * pageSize

        // 计算当前槽位在整个列表中的实际索引
        val actualIndex = startIndex + slotIndex

        // 如果实际索引超出范围，返回null
        if (actualIndex < 0 || actualIndex >= filteredMaterials.size) {
            return null
        }

        // 获取当前槽位对应的材料名称
        val currentMaterial = filteredMaterials[actualIndex]

        // 获取玩家已选择的材料列表
        val selectedMaterials = getSelectedMaterials(player)

        // 检查材料是否被排除
        val isExcluded = isMaterialExcluded(player, currentMaterial)

        // 创建材料物品
        val resultItem = createMaterialItem(currentMaterial, selectedMaterials.contains(currentMaterial), isExcluded, template.features, player)

        return resultItem
    }

    /**
     * 创建材料物品
     */
    private fun createMaterialItem(
        materialName: String,
        isSelected: Boolean,
        isExcluded: Boolean,
        features: Map<String, Any>?,
        player: Player
    ): ItemStack {
        // 使用MaterialNameUtil获取材料对应的Material类型
        // 优先级：排除 > 选中 > 原始材料
        val material = when {
            isExcluded -> Material.RED_STAINED_GLASS_PANE
            isSelected -> Material.LIME_STAINED_GLASS_PANE
            else -> com.sky4th.equipment.util.MaterialNameUtil.getMaterialFromChinese(materialName) ?: Material.BARRIER
        }

        // 创建物品
        val resultItem = ItemStack(material)
        val meta = resultItem.itemMeta ?: return resultItem

        // 设置显示名称
        val displayName = when {
            isExcluded -> "<red>$materialName</red>"
            isSelected -> "<green>$materialName</green>"
            else -> "<gray>$materialName</gray>"
        }
        val nameComponent = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(displayName)
        meta.displayName(LanguageUtil.removeItalic(nameComponent))

        // 获取lore格式
        val loreFormat = features?.get("lore") as? List<*> ?: listOf(
            "&8| &7本升级材料适用词条数量: &e{size}",
            "&7",
            "&8| &7左键点击 &a查看此升级材料的词条",
            "&8| &7Shift + 点击 &e清除当前选择",
            "&8| &7右键点击 &c不看此升级材料的词条"
        )

        // 替换lore中的占位符
        val lore = loreFormat.map { line ->
            var result = line.toString()
            result = result.replace("{material}", materialName)
            result = result.replace("{size}", getAffixCountByMaterial(materialName).toString())
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
     * 处理材料物品的左键点击
     */
    fun handleLeftClick(
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): Boolean {
        // 获取当前UI配置
        val uiConfig = sky4th.core.ui.UIManager.getUI(getCurrentUIId(player))
        if (uiConfig == null) {
            return false
        }

        // 获取当前槽位的材料名称
        val materialName = getMaterialNameFromSlot(template, uiConfig, player)
        if (materialName == null) {
            println("未找到材料名称，当前模板: ${template}，槽位: ${template.slot}, 模板: ${template.key}")
            return false
        }

        // 切换选择状态
        val selectedMaterials = getSelectedMaterials(player)
        if (materialName in selectedMaterials) {
            // 取消选中
            removeSelectedMaterial(player, materialName)
        } else {
            // 选中材料，并从排除列表中移除
            addSelectedMaterial(player, materialName)
            removeFromExcluded(player, materialName)
        }

        // 更新UI
        sky4th.core.ui.UIManager.updateCurrentUI(player)
        return true
    }

    /**
     * 处理材料物品的右键点击
     */
    fun handleRightClick(
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): Boolean {
        // 获取当前UI配置
        val uiConfig = sky4th.core.ui.UIManager.getUI(getCurrentUIId(player))
        if (uiConfig == null) {
            return false
        }

        // 获取当前槽位的材料名称
        val materialName = getMaterialNameFromSlot(template, uiConfig, player) ?: return false

        // 切换排除状态
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "material_filter")
        val excludedMaterials = (state.customFilters["excluded_materials"] as? Set<String> ?: emptySet()).toMutableSet()

        if (materialName in excludedMaterials) {
            // 如果已经排除，则取消排除
            excludedMaterials.remove(materialName)
        } else {
            // 如果未排除，则添加到排除列表，并从选中列表中移除
            excludedMaterials.add(materialName)
            removeSelectedMaterial(player, materialName)
        }

        state.customFilters["excluded_materials"] = excludedMaterials

        // 更新UI
        sky4th.core.ui.UIManager.updateCurrentUI(player)
        return true
    }

    /**
     * 处理材料物品的Shift+点击
     */
    fun handleShiftClick(
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): Boolean {
        // 获取当前UI配置
        val uiConfig = sky4th.core.ui.UIManager.getUI(getCurrentUIId(player))
        if (uiConfig == null) {
            return false
        }

        // 获取当前槽位的材料名称
        val materialName = getMaterialNameFromSlot(template, uiConfig, player) ?: return false

        // 从选中列表中移除
        removeSelectedMaterial(player, materialName)

        // 从排除列表中移除
        removeFromExcluded(player, materialName)

        // 更新UI
        sky4th.core.ui.UIManager.updateCurrentUI(player)
        return true
    }


    /**
     * 获取玩家当前打开的UI ID
     */
    private fun getCurrentUIId(player: Player): String {
        return sky4th.core.ui.UIManager.getListener()?.getPlayerUI(player) ?: ""
    }

    /**
     * 计算UI中指定字符的槽位数量
     */
    private fun countTemplateSlots(shape: List<String>, slotChar: String): Int {
        return shape.sumOf { line ->
            line.count { it.toString() == slotChar }
        }
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
     * 获取排序后的材料列表
     */
    private fun getFilteredMaterials(player: Player): List<String> {
        return getAllMaterials()
    }

    /**
     * 检查材料是否被排除
     */
    private fun isMaterialExcluded(player: Player, materialName: String): Boolean {
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "material_filter")
        val excludedMaterials = state.customFilters["excluded_materials"] as? Set<String> ?: emptySet()
        return materialName in excludedMaterials
    }

    /**
     * 从排除列表中移除材料
     */
    private fun removeFromExcluded(player: Player, material: String) {
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "material_filter")
        val excludedMaterials = (state.customFilters["excluded_materials"] as? Set<String> ?: emptySet()).toMutableSet()
        excludedMaterials.remove(material)
        state.customFilters["excluded_materials"] = excludedMaterials
    }

    /**
     * 清除所有排除的材料
     */
    fun clearExcludedMaterials(player: Player) {
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "material_filter")
        state.customFilters["excluded_materials"] = emptySet<String>()
    }

    /**
     * 获取玩家已选择的材料列表
     */
    fun getSelectedMaterials(player: Player): Set<String> {
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "material_filter")
        return state.customFilters["selected_materials"] as? Set<String> ?: emptySet()
    }

    /**
     * 设置玩家已选择的材料列表
     */
    fun setSelectedMaterials(player: Player, materials: Set<String>) {
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "material_filter")
        state.customFilters["selected_materials"] = materials
    }

    /**
     * 添加材料到已选择列表
     */
    fun addSelectedMaterial(player: Player, material: String) {
        val current = getSelectedMaterials(player).toMutableSet()
        current.add(material)
        setSelectedMaterials(player, current)
    }

    /**
     * 从已选择列表中移除材料
     */
    fun removeSelectedMaterial(player: Player, material: String) {
        val current = getSelectedMaterials(player).toMutableSet()
        current.remove(material)
        setSelectedMaterials(player, current)
    }

    /**
     * 清除所有已选择的材料
     */
    fun clearSelectedMaterials(player: Player) {
        setSelectedMaterials(player, emptySet())
    }

    /**
     * 获取使用指定材料的词条数量
     * @param materialName 材料名称
     * @return 使用该材料的词条数量
     */
    private fun getAffixCountByMaterial(materialName: String): Int {
        return allMaterials[materialName] ?: 0
    }
    /**
     * 从槽位获取材料名称
     */
    private fun getMaterialNameFromSlot(
        template: UITemplate,
        uiConfig: sky4th.core.ui.UIConfig,
        player: Player
    ): String? {
        // 获取当前template在shape中的所有槽位位置
        val allSlotIndices = getAllSlotIndices(uiConfig.shape, template.key)

        // 根据template的槽位位置确定当前是第几个槽位
        val slotIndex = allSlotIndices.indexOf(template.slot)

        // 如果槽位不在材料列表中，返回null
        if (slotIndex == -1) {
            return null
        }

        // 获取筛选后的材料列表
        val filteredMaterials = getFilteredMaterials(player)

        // 获取当前页码
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "material_filter")
        val currentPage = state.currentPage

        // 计算当前页的起始索引
        val pageSize = countTemplateSlots(uiConfig.shape, template.key)
        val startIndex = currentPage * pageSize

        // 计算当前槽位在整个列表中的实际索引
        val actualIndex = startIndex + slotIndex

        // 如果实际索引超出范围，返回null
        if (actualIndex < 0 || actualIndex >= filteredMaterials.size) {
            return null
        }

        return filteredMaterials[actualIndex]
    }
}
