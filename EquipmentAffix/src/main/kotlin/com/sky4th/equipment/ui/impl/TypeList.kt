package com.sky4th.equipment.ui.impl

import com.sky4th.equipment.loader.AffixTemplateLoader
import com.sky4th.equipment.modifier.AffixConfigManager
import com.sky4th.equipment.util.LanguageUtil
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import sky4th.core.ui.UITemplate
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * 装备类型列表处理器
 * 负责显示和管理装备类型列表
 */
object TypeList {

    // 装备类型定义
    // 每个类型包含: 显示名称, Material类型, 对应的EquipmentCategory和EquipmentSlot
    data class TypeItem(
        val displayName: String,
        val material: Material,
        val category: String,
        val slot: String
    )

    // 所有装备类型列表
    private val allTypes = listOf(
        // 轻型头盔
        TypeItem("轻型头盔", Material.LEATHER_HELMET, "LIGHT_ARMOR", "HEAD"),
        // 重型头盔
        TypeItem("重型头盔", Material.IRON_HELMET, "HEAVY_ARMOR", "HEAD"),
        // 轻型胸甲
        TypeItem("轻型胸甲", Material.LEATHER_CHESTPLATE, "LIGHT_ARMOR", "CHEST"),
        // 重型胸甲
        TypeItem("重型胸甲", Material.IRON_CHESTPLATE, "HEAVY_ARMOR", "CHEST"),
        // 轻型护腿
        TypeItem("轻型护腿", Material.LEATHER_LEGGINGS, "LIGHT_ARMOR", "LEGS"),
        // 重型护腿
        TypeItem("重型护腿", Material.IRON_LEGGINGS, "HEAVY_ARMOR", "LEGS"),
        // 轻型靴子
        TypeItem("轻型靴子", Material.LEATHER_BOOTS, "LIGHT_ARMOR", "FEET"),
        // 重型靴子
        TypeItem("重型靴子", Material.IRON_BOOTS, "HEAVY_ARMOR", "FEET"),
        // 剑
        TypeItem("剑", Material.IRON_SWORD, "WEAPON", "SWORD"),
        // 斧
        TypeItem("斧", Material.IRON_AXE, "WEAPON", "AXE"),
        // 铲
        TypeItem("铲", Material.IRON_SHOVEL, "TOOL", "SHOVEL"),
        // 镐
        TypeItem("镐", Material.IRON_PICKAXE, "TOOL", "PICKAXE"),
        // 锄
        TypeItem("锄", Material.IRON_HOE, "TOOL", "HOE"),
        // 重锤
        TypeItem("重锤", Material.MACE, "WEAPON", "MACE"),
        // 三叉戟
        TypeItem("三叉戟", Material.TRIDENT, "WEAPON", "TRIDENT"),
        // 钓鱼竿
        TypeItem("钓鱼竿", Material.FISHING_ROD, "TOOL", "FISHING_ROD"),
        // 弓
        TypeItem("弓", Material.BOW, "BOW", "BOW"),
        // 弩
        TypeItem("弩", Material.CROSSBOW, "BOW", "CROSSBOW"),
        // 盾牌
        TypeItem("盾牌", Material.SHIELD, "SHIELD", "SHIELD"),
        // 鞘翅
        TypeItem("鞘翅", Material.ELYTRA, "ELYTRA", "ELYTRA")
    )

    // 存储每个类型对应的词条数量
    private val typeCountMap = mutableMapOf<String, Int>()

    /**
     * 初始化类型列表
     * 统计每个类型适用的词条数量
     */
    fun initialize() {
        if (typeCountMap.isNotEmpty()) {
            return
        }
        typeCountMap.clear()

        val allAffixIds = AffixTemplateLoader.getAllTemplateIds()
        for (affixId in allAffixIds) {
            val affixConfig = AffixConfigManager.getAffixConfig(affixId) ?: continue

            // 检查每个装备类型
            for (type in allTypes) {
                // 检查词条是否适用于该类型
                val isApplicable = affixConfig.applicableTo.any { it.name == type.category } &&
                    (affixConfig.equipmentSlot.isEmpty() || affixConfig.equipmentSlot.contains(type.slot))

                if (isApplicable) {
                    val typeKey = "${type.category}_${type.slot}"
                    typeCountMap[typeKey] = (typeCountMap[typeKey] ?: 0) + 1
                }
            }
        }
    }

    /**
     * 获取所有装备类型
     */
    fun getAllTypes(): List<TypeItem> {
        return allTypes
    }

    /**
     * 获取指定类型适用的词条数量
     */
    private fun getAffixCountByType(type: TypeItem): Int {
        val typeKey = "${type.category}_${type.slot}"
        return typeCountMap[typeKey] ?: 0
    }

    /**
     * 处理类型列表的物品创建
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

        // 计算当前页的类型数量
        val pageSize = countTemplateSlots(uiConfig.shape, template.key)

        // 获取当前template在shape中的所有槽位位置
        val allSlotIndices = getAllSlotIndices(uiConfig.shape, template.key)

        // 根据template的槽位位置确定当前是第几个槽位
        val slotIndex = allSlotIndices.indexOf(template.slot)

        // 如果槽位不在类型列表中，返回null
        if (slotIndex == -1) {
            return null
        }

        // 获取当前页码
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "type_filter")
        val currentPage = state.currentPage

        // 计算当前页的起始索引
        val startIndex = currentPage * pageSize

        // 计算当前槽位在整个列表中的实际索引
        val actualIndex = startIndex + slotIndex

        // 如果实际索引超出范围，返回null
        if (actualIndex < 0 || actualIndex >= allTypes.size) {
            return null
        }

        // 获取当前槽位对应的类型
        val currentType = allTypes[actualIndex]

        // 获取玩家已选择的类型列表
        val selectedTypes = getSelectedTypes(player)

        // 检查类型是否被排除
        val isExcluded = isTypeExcluded(player, currentType)

        // 创建类型物品
        val resultItem = createTypeItem(currentType, selectedTypes.contains("${currentType.category}_${currentType.slot}"), isExcluded, template.features, player)

        return resultItem
    }

    /**
     * 创建类型物品
     */
    private fun createTypeItem(
        type: TypeItem,
        isSelected: Boolean,
        isExcluded: Boolean,
        features: Map<String, Any>?,
        player: Player
    ): ItemStack {
        // 优先级：排除 > 选中 > 原始类型
        val material = when {
            isExcluded -> Material.RED_STAINED_GLASS_PANE
            isSelected -> Material.LIME_STAINED_GLASS_PANE
            else -> type.material
        }

        // 创建物品
        val resultItem = ItemStack(material)
        val meta = resultItem.itemMeta ?: return resultItem

        // 设置显示名称
        val displayName = when {
            isSelected -> "&a${type.displayName}"
            isExcluded -> "&c${type.displayName}"
            else -> "&7${type.displayName}"
        }
        // 使用 ColorUtil 转换所有格式为 § 格式
        val convertedName = sky4th.core.util.ColorUtil.convertMiniMessageToLegacy(displayName)
        // 使用 LegacyComponentSerializer 解析 § 格式
        val nameComponent = LegacyComponentSerializer.legacySection().deserialize(convertedName)

        meta.displayName(LanguageUtil.removeItalic(nameComponent))

        // 获取lore格式
        val loreFormat = features?.get("lore") as? List<*> ?: listOf(
            "&8| &7左键点击 &a查看此装备类型的词条",
            "&8| &7Shift + 点击 &e清除当前选择",
            "&8| &7右键点击 &c不看此装备类型的词条"
        )

        // 替换lore中的占位符
        val lore = loreFormat.map { line ->
            var result = line.toString()
            result = result.replace("{size}", getAffixCountByType(type).toString())
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

        val hideModifier = org.bukkit.attribute.AttributeModifier(
            org.bukkit.NamespacedKey("equipment_menu", "hide"),
            0.0,
            org.bukkit.attribute.AttributeModifier.Operation.ADD_SCALAR,
            org.bukkit.inventory.EquipmentSlotGroup.MAINHAND
        )
        meta.addAttributeModifier(org.bukkit.attribute.Attribute.GENERIC_ATTACK_SPEED, hideModifier)

        // 隐藏物品上的属性提示（确保每个物品都添加）
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES)
        
        resultItem.itemMeta = meta
        return resultItem
    }

    /**
     * 处理类型物品的左键点击
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

        // 获取当前槽位的类型
        val type = getTypeFromSlot(template, uiConfig, player)
        if (type == null) {
            println("未找到类型，当前模板: ${template}，槽位: ${template.slot}")
            return false
        }

        val typeKey = "${type.category}_${type.slot}"

        // 切换选择状态
        val selectedTypes = getSelectedTypes(player)
        if (typeKey in selectedTypes) {
            // 取消选中
            removeSelectedType(player, typeKey)
        } else {
            // 选中类型，并从排除列表中移除
            addSelectedType(player, typeKey)
            removeFromExcluded(player, typeKey)
        }

        // 更新UI
        sky4th.core.ui.UIManager.updateCurrentUI(player)
        return true
    }

    /**
     * 处理类型物品的右键点击
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

        // 获取当前槽位的类型
        val type = getTypeFromSlot(template, uiConfig, player) ?: return false
        val typeKey = "${type.category}_${type.slot}"

        // 切换排除状态
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "type_filter")
        val excludedTypes = (state.customFilters["excluded_types"] as? Set<String> ?: emptySet()).toMutableSet()

        if (typeKey in excludedTypes) {
            // 如果已经排除，则取消排除
            excludedTypes.remove(typeKey)
        } else {
            // 如果未排除，则添加到排除列表，并从选中列表中移除
            excludedTypes.add(typeKey)
            removeSelectedType(player, typeKey)
        }

        state.customFilters["excluded_types"] = excludedTypes

        // 更新UI
        sky4th.core.ui.UIManager.updateCurrentUI(player)
        return true
    }

    /**
     * 处理类型物品的Shift+点击
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

        // 获取当前槽位的类型
        val type = getTypeFromSlot(template, uiConfig, player) ?: return false
        val typeKey = "${type.category}_${type.slot}"

        // 从选中列表中移除
        removeSelectedType(player, typeKey)

        // 从排除列表中移除
        removeFromExcluded(player, typeKey)

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
     * 检查类型是否被排除
     */
    private fun isTypeExcluded(player: Player, type: TypeItem): Boolean {
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "type_filter")
        val excludedTypes = state.customFilters["excluded_types"] as? Set<String> ?: emptySet()
        val typeKey = "${type.category}_${type.slot}"
        return typeKey in excludedTypes
    }

    /**
     * 从排除列表中移除类型
     */
    private fun removeFromExcluded(player: Player, typeKey: String) {
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "type_filter")
        val excludedTypes = (state.customFilters["excluded_types"] as? Set<String> ?: emptySet()).toMutableSet()
        excludedTypes.remove(typeKey)
        state.customFilters["excluded_types"] = excludedTypes
    }

    /**
     * 清除所有排除的类型
     */
    fun clearExcludedTypes(player: Player) {
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "type_filter")
        state.customFilters["excluded_types"] = emptySet<String>()
    }

    /**
     * 获取玩家已选择的类型列表
     */
    fun getSelectedTypes(player: Player): Set<String> {
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "type_filter")
        return state.customFilters["selected_types"] as? Set<String> ?: emptySet()
    }

    /**
     * 设置玩家已选择的类型列表
     */
    fun setSelectedTypes(player: Player, types: Set<String>) {
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "type_filter")
        state.customFilters["selected_types"] = types
    }

    /**
     * 添加类型到已选择列表
     */
    fun addSelectedType(player: Player, typeKey: String) {
        val current = getSelectedTypes(player).toMutableSet()
        current.add(typeKey)
        setSelectedTypes(player, current)
    }

    /**
     * 从已选择列表中移除类型
     */
    fun removeSelectedType(player: Player, typeKey: String) {
        val current = getSelectedTypes(player).toMutableSet()
        current.remove(typeKey)
        setSelectedTypes(player, current)
    }

    /**
     * 清除所有已选择的类型
     */
    fun clearSelectedTypes(player: Player) {
        setSelectedTypes(player, emptySet())
    }

    /**
     * 从槽位获取类型
     */
    private fun getTypeFromSlot(
        template: UITemplate,
        uiConfig: sky4th.core.ui.UIConfig,
        player: Player
    ): TypeItem? {
        // 获取当前template在shape中的所有槽位位置
        val allSlotIndices = getAllSlotIndices(uiConfig.shape, template.key)

        // 根据template的槽位位置确定当前是第几个槽位
        val slotIndex = allSlotIndices.indexOf(template.slot)

        // 如果槽位不在类型列表中，返回null
        if (slotIndex == -1) {
            return null
        }

        // 获取当前页码
        val state = com.sky4th.equipment.ui.template.TemplateListManager.getPlayerState(player, "type_filter")
        val currentPage = state.currentPage

        // 计算当前页的起始索引
        val pageSize = countTemplateSlots(uiConfig.shape, template.key)
        val startIndex = currentPage * pageSize

        // 计算当前槽位在整个列表中的实际索引
        val actualIndex = startIndex + slotIndex

        // 如果实际索引超出范围，返回null
        if (actualIndex < 0 || actualIndex >= allTypes.size) {
            return null
        }

        return allTypes[actualIndex]
    }
}
