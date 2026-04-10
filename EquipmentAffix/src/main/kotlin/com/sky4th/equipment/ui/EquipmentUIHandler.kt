package com.sky4th.equipment.ui

import com.sky4th.equipment.loader.AffixTemplateLoader
import com.sky4th.equipment.modifier.AffixConfigManager
import com.sky4th.equipment.modifier.ModifierManager
import com.sky4th.equipment.ui.template.TemplateListManager
import com.sky4th.equipment.ui.impl.*
import com.sky4th.equipment.ui.template.filter.*
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import sky4th.core.ui.UITemplate
import sky4th.core.ui.feature.UIFeatureHandler
import com.sky4th.equipment.util.LanguageUtil

/**
 * 装备系统UI处理器
 * 处理装备系统的所有UI逻辑
 */
class EquipmentUIHandler(private val modifierManager: ModifierManager) : UIFeatureHandler {

    // 筛选器注册标志
    private var filtersRegistered = false

    override fun handleItemCreation(
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): ItemStack? {
        // 检查functional特性
        val functional = template.features["functional"] as? String

        if (functional == "template_list") {
            // 确保筛选器已注册
            ensureFiltersRegistered()
            return TemplateList.handleItemCreation(template,item,player)
        }

        // 处理材料列表的物品创建
        if (functional == "material_list") {
            // 初始化材料列表
            MaterialList.initialize()
            return MaterialList.handleItemCreation(template, item, player)
        }

        // 处理类型列表的物品创建
        if (functional == "type_list") {
            // 初始化类型列表
            TypeList.initialize()
            return TypeList.handleItemCreation(template, item, player)
        }

        // 处理筛选按钮的物品创建
        if (functional == "filter_name") {
            return FilterName.handleItemCreation(template, item, player)
        }

        // 处理材料筛选按钮的物品创建
        if (functional == "filter_material") {
            return FilterMaterial.handleItemCreation(template, item, player)
        }

        // 处理装备类型筛选按钮的物品创建
        if (functional == "filter_type") {
            return FilterType.handleItemCreation(template, item, player)
        }

        // 处理排序按钮的物品创建
        if (functional == "sort") {
            return Sort.handleItemCreation(template, item, player)
        }

        // 处理词条信息页面的物品创建
        if (functional == "template_info") {
            return TemplateInfo.handleItemCreation(template, item, player)
        }

        // 处理词条升级材料的物品创建
        if (functional == "affix_update") {
            return AffixUpdate.handleItemCreation(template, item, player)
        }

        // 处理等级信息的物品创建
        if (functional == "level_info") {
            return LevelInfo.handleItemCreation(template, item, player)
        }

        // 处理活跃信息物品的创建
        if (functional == "affix_active") {
            return AffixActive.handleItemCreation(template, item, player)
        }

        // 返回null表示不处理，返回物品表示已处理
        return null
    }

    // 任意点击的方法 左右shift读取完后进行的
    override fun handleClick(
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): Boolean {
        // 检查functional特性
        val functional = template.features["functional"] as? String

        // 处理翻页按钮
        if (functional == "page_turn") {
            return PageTurn.handleClick(template, item, player)
        }

        // 返回false表示未处理，继续默认逻辑
        return false
    }

    override fun handleLeftClick(
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): Boolean {
        // 检查functional特性
        val functional = template.features["functional"] as? String

        // 处理名称筛选按钮的左键点击
        if (functional == "filter_name") {
            FilterNameListener.startWaiting(player)
            return true
        }

        // 处理材料筛选按钮的左键点击
        if (functional == "filter_material") {
            // 打开材料筛选界面
            sky4th.core.api.UIAPI.openUI(player, "affix_filter_materials")
            return true
        }

        // 处理装备类型筛选按钮的左键点击
        if (functional == "filter_type") {
            // 打开装备类型筛选界面
            sky4th.core.api.UIAPI.openUI(player, "affix_filter_types")
            return true
        }

        // 处理排序按钮的左键点击
        if (functional == "sort") {
            return Sort.handleLeftClick(template, item, player)
        }

        // 处理模板列表的左键点击
        if (functional == "template_list") {
            switchMode(player, "合成配方")
            return TemplateList.handleLeftClick(template, item, player)
        }

        // 处理等级上升按钮的左键点击
        if (functional == "level_up") {
            return handleLevelUp(player)
        }

        // 处理等级下降按钮的左键点击
        if (functional == "level_down") {
            return handleLevelDown(player)
        }

        // 处理等级信息的左键点击
        if (functional == "level_info") {
            return LevelInfo.handleLeftClick(template, item, player)
        }

        // 处理冲突词条的左键点击（切换到冲突词条模式）
        if (functional == "affix_conflict") {
            return switchMode(player, "冲突词条")
        }

        // 处理冲突附魔的左键点击（切换到冲突附魔式）
        if (functional == "affix_enchant_conflict") {
            return switchMode(player, "冲突附魔")
        }

        // 处理合成配方的左键点击（切换到合成配方模式）
        if (functional == "affix_recipe") {
            return switchMode(player, "合成配方")
        }

        // 处理活跃词条的左键点击（切换到活跃词条模式）
        if (functional == "affix_active") {
           return AffixActive.handleLeftClick(template, item, player)
        }

        // 处理材料列表的左键点击
        if (functional == "material_list") {
            return MaterialList.handleLeftClick(template, item, player)
        }

        // 处理类型列表的左键点击
        if (functional == "type_list") {
            return TypeList.handleLeftClick(template, item, player)
        }

        // 处理重置筛选按钮的左键点击
        if (functional == "reset_filter") {
            // 获取当前UI ID
            val currentUIId = sky4th.core.ui.UIManager.getListener()?.getPlayerUI(player) ?: ""

            // 根据当前页面重置对应的筛选
            when (currentUIId) {
                "affix_filter_materials" -> {
                    // 清除所有已选择的材料
                    MaterialList.clearSelectedMaterials(player)
                    // 清除所有排除的材料
                    MaterialList.clearExcludedMaterials(player)
                    // 重置材料筛选页面的页码
                    val materialState = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "material_filter")
                    materialState.currentPage = 0
                }
                "affix_filter_type" -> {
                    // 清除所有已选择的类型
                    TypeList.clearSelectedTypes(player)
                    // 清除所有排除的类型
                    TypeList.clearExcludedTypes(player)
                    // 重置类型筛选页面的页码
                    val typeState = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "type_filter")
                    typeState.currentPage = 0
                }
            }

            // 更新UI
            sky4th.core.ui.UIManager.updateCurrentUI(player)
            return true
        }

        // 返回false表示未处理，继续默认逻辑
        return false
    }

    override fun handleRightClick(
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): Boolean {
        // 检查functional特性
        val functional = template.features["functional"] as? String

        // 处理名称筛选按钮的右键点击
        if (functional == "filter_name") {
            return FilterName.handleRightClick(template, item, player)
        }

        // 处理材料筛选按钮的右键点击
        if (functional == "filter_material") {
            return FilterMaterial.handleRightClick(template, item, player)
        }

        // 处理装备类型筛选按钮的右键点击
        if (functional == "filter_type") {
            return FilterType.handleRightClick(template, item, player)
        }

        // 处理材料列表的右键点击
        if (functional == "material_list") {
            return MaterialList.handleRightClick(template, item, player)
        }

        // 处理类型列表的右键点击
        if (functional == "type_list") {
            return TypeList.handleRightClick(template, item, player)
        }

        // 返回false表示未处理，继续默认逻辑
        return false
    }

    override fun handleShiftClick(
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): Boolean {
        // 检查functional特性
        val functional = template.features["functional"] as? String

        // 处理材料列表的Shift+点击
        if (functional == "material_list") {
            return MaterialList.handleShiftClick(template, item, player)
        }

        // 处理类型列表的Shift+点击
        if (functional == "type_list") {
            return TypeList.handleShiftClick(template, item, player)
        }

        // 返回false表示未处理，继续默认逻辑
        return false
    }

    override fun handleUIClose(
        uiId: String,
        player: Player
    ) {
        // 关闭词条信息页面时清除存储的词条ID
        if (uiId == "affix_info") {
            val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "affix_info")
            state.customFilters.remove("current_affix_id")
        }

        // 只在关闭词条列表UI时重置筛选
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "affix_list")
        state.searchQuery = ""
        state.currentPage = 0
        state.customFilters["sort_mode"] = "name"
        // 清除所有已选择的材料
        MaterialList.clearSelectedMaterials(player)
        // 清除所有排除的材料
        MaterialList.clearExcludedMaterials(player)
        // 清除所有已选择的类型
        TypeList.clearSelectedTypes(player)
        // 清除所有排除的类型
        TypeList.clearExcludedTypes(player)
    }

    override fun handleUISwitch(
        uiId: String,
        player: Player
    ) {
        // 切换到词条列表UI时重置页面 不重置筛选
        if (uiId == "affix_list") {
            val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "affix_list")
            state.currentPage = 0
            state.customFilters["sort_mode"] = "name"
        }

        // 切换到材质选择UI时重置页面
        if (uiId == "affix_filter_materials") {
            val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "material_filter")
            state.currentPage = 0
        }

        // 切换到类型选择UI时重置页面
        if (uiId == "affix_filter_type") {
            val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "type_filter")
            state.currentPage = 0
        }
    }


    /**
     * 确保筛选器已注册
     */
    private fun ensureFiltersRegistered() {
        if (!filtersRegistered) {
            // 注册多模式排序器
            val multiModeSorter = com.sky4th.equipment.ui.template.sorter.MultiModeSorter()
            com.sky4th.equipment.ui.template.TemplateListManager.registerListConfig(
                com.sky4th.equipment.ui.template.TemplateListConfig(
                    id = "affix_list",
                    name = "词条列表",
                    description = "所有可锻造的词条",
                    filters = listOf(
                        NameFilter(), 
                        MaterialFilter(),
                        TypeFilter()
                    ),
                    sorter = multiModeSorter
                )
            )
            filtersRegistered = true
        }
    }

    /**
     * 处理等级上升
     */
    private fun handleLevelUp(player: Player): Boolean {
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "affix_info")
        val currentAffixId = state.customFilters["current_affix_id"] as? String ?: return false
        val affixConfig = com.sky4th.equipment.modifier.AffixConfigManager.getAffixConfig(currentAffixId) ?: return false

        val currentLevel = state.customFilters["current_level"] as? Int ?: 1
        val newLevel = currentLevel + 1

        if (newLevel <= affixConfig.maxLevel) {
            state.customFilters["current_level"] = newLevel
            sky4th.core.ui.UIManager.updateCurrentUI(player)
            return true
        }
        return false
    }

    /**
     * 处理等级下降
     */
    private fun handleLevelDown(player: Player): Boolean {
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "affix_info")
        val currentLevel = state.customFilters["current_level"] as? Int ?: 1
        val newLevel = currentLevel - 1

        if (newLevel >= 1) {
            state.customFilters["current_level"] = newLevel
            sky4th.core.ui.UIManager.updateCurrentUI(player)
            return true
        }
        return false
    }

    /**
     * 切换活跃模式
     */
    private fun switchMode(player: Player, mode: String): Boolean {
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "affix_info")
        state.customFilters["active_mode"] = mode
        sky4th.core.ui.UIManager.updateCurrentUI(player)
        return true
    }
}
