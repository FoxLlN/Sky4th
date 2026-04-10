package com.sky4th.equipment.ui

import com.sky4th.equipment.loader.AffixTemplateLoader
import com.sky4th.equipment.modifier.AffixConfigManager
import com.sky4th.equipment.util.DescriptionEvaluator
import com.sky4th.equipment.util.DisplayUtil
import com.sky4th.equipment.util.LanguageUtil
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * 词条物品构建器
 * 用于根据词条ID生成词条模板UI物品
 */
object AffixItemBuilder {

    /**
     * 根据词条ID生成词条模板UI物品
     * 
     * @param affixId 词条ID
     * @param templateFeatures UI模板特性，包含lore格式等信息
     * @param currentLevel 当前显示的等级（可选，默认为最大等级）
     * @return 生成的词条物品，如果失败返回null
     */
    fun buildAffixItem(affixId: String, templateFeatures: Map<String, Any>? = null, currentLevel: Int? = null): ItemStack? {
        // 获取词条模板物品
        val affixItem = AffixTemplateLoader.getAffixTemplate(affixId) ?: return null

        // 获取词条配置
        val affixConfig = AffixConfigManager.getAffixConfig(affixId) ?: return null

        // 确定当前显示的等级
        val displayLevel = currentLevel ?: affixConfig.maxLevel

        // 创建新的物品副本
        val resultItem = affixItem.clone()
        val meta = resultItem.itemMeta ?: return null

        // 从template的feature中读取lore格式，如果没有则使用默认格式
        val loreFormat = templateFeatures?.get("lore") as? List<*> ?: getDefaultLoreFormat()

        // 获取材料名称
        val materialsCache = AffixTemplateLoader.getUpgradeMaterialsCache(affixId)

        // 提取displayName的颜色代码（MiniMessage格式）
        val displayNameColor = if (affixConfig.displayName.length >= 9 && affixConfig.displayName.startsWith("<#")) {
            affixConfig.displayName.substring(0, 9)
        } else {
            ""
        }

        val materialsNames = materialsCache?.let { (materialName, _) ->
            "$displayNameColor$materialName"
        } ?: ""

        // 获取装备类型文本
        val equipmentText = getEquipmentText(affixConfig)

        // 替换lore中的占位符
        val lore = loreFormat.map { line ->
            var result = line.toString()
            result = result.replace("{affix_name}", affixConfig.displayName)
            result = result.replace("{max_level}", affixConfig.maxLevel.toString())
            result = result.replace("{level}", displayLevel.toString())  // 使用当前显示的等级
            result = result.replace("{materials}", materialsNames)
            // 使用描述评估工具类处理描述，使用当前显示的等级
            val description = DescriptionEvaluator.getEvaluatedDescription(affixConfig, displayLevel)
            result = result.replace("{description}", description)
            result = result.replace("{equipment}", equipmentText)  // 支持equipment占位符
            result
        }

        val number = DisplayUtil.toRoman(displayLevel)

        // 设置物品名称为 "词条名字 最大等级"
        val displayName = "${affixConfig.displayName} ${number}"
        val convertedName = sky4th.core.util.ColorUtil.convertLegacyToMiniMessage(displayName)
        val nameComponent = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(convertedName)
        meta.displayName(LanguageUtil.removeItalic(nameComponent))

        // 设置物品描述
        val convertedLore = lore.map {
            sky4th.core.util.ColorUtil.convertLegacyToMiniMessage(it)
        }
        meta.lore(convertedLore.map { 
            val loreComponent = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(it)
            LanguageUtil.removeItalic(loreComponent)
        })

        resultItem.itemMeta = meta

        // 返回词条物品
        return resultItem
    }

    /**
     * 获取默认的lore格式
     */
    private fun getDefaultLoreFormat(): List<String> = listOf(
        "&8| &7锻造词条 > {affix_name}",
        "&8| &7最大等级 > {max_level}",
        "&8| &7升级材料 > {materials}",
        "&8| &7简介 > {description}",
        "&7",
        "&8| &7点击查看详细信息"
    )

    /**
     * 获取装备类型文本
     */
    private fun getEquipmentText(affixConfig: com.sky4th.equipment.modifier.AffixConfig): String {
        val applicableToList = affixConfig.applicableTo
        val equipmentSlotList = affixConfig.equipmentSlot

        return if (equipmentSlotList.isNotEmpty()) {
            // 如果有具体的装备槽位，显示具体的装备名称
            val slotTexts = equipmentSlotList.map { slot ->
                when(slot.uppercase()) {
                    "HEAD" -> {
                        val armorTypes = mutableListOf<String>()
                        if (applicableToList.contains(com.sky4th.equipment.attributes.EquipmentCategory.LIGHT_ARMOR)) armorTypes.add("轻型头盔")
                        if (applicableToList.contains(com.sky4th.equipment.attributes.EquipmentCategory.HEAVY_ARMOR)) armorTypes.add("重型头盔")
                        armorTypes.joinToString(", ")
                    }
                    "CHEST" -> {
                        val armorTypes = mutableListOf<String>()
                        if (applicableToList.contains(com.sky4th.equipment.attributes.EquipmentCategory.LIGHT_ARMOR)) armorTypes.add("轻型胸甲")
                        if (applicableToList.contains(com.sky4th.equipment.attributes.EquipmentCategory.HEAVY_ARMOR)) armorTypes.add("重型胸甲")
                        armorTypes.joinToString(", ")
                    }
                    "LEGS" -> {
                        val armorTypes = mutableListOf<String>()
                        if (applicableToList.contains(com.sky4th.equipment.attributes.EquipmentCategory.LIGHT_ARMOR)) armorTypes.add("轻型护腿")
                        if (applicableToList.contains(com.sky4th.equipment.attributes.EquipmentCategory.HEAVY_ARMOR)) armorTypes.add("重型护腿")
                        armorTypes.joinToString(", ")
                    }
                    "FEET" -> {
                        val armorTypes = mutableListOf<String>()
                        if (applicableToList.contains(com.sky4th.equipment.attributes.EquipmentCategory.LIGHT_ARMOR)) armorTypes.add("轻型靴子")
                        if (applicableToList.contains(com.sky4th.equipment.attributes.EquipmentCategory.HEAVY_ARMOR)) armorTypes.add("重型靴子")
                        armorTypes.joinToString(", ")
                    }
                    "SWORD" -> "剑"
                    "AXE" -> "斧"
                    "PICKAXE" -> "镐"
                    "SHOVEL" -> "铲"
                    "HOE" -> "锄"
                    "BOW" -> "弓"
                    "CROSSBOW" -> "弩"
                    "TRIDENT" -> "三叉戟"
                    "MACE" -> "重锤"
                    "FISHING_ROD" -> "钓鱼竿"
                    "ELYTRA" -> "鞘翅"
                    else -> slot
                }
            }.filter { it.isNotEmpty() }
            if (slotTexts.isNotEmpty()) {
                slotTexts.joinToString(", ")
            } else {
                // 如果没有生成有效的槽位文本，回退到通用描述
                applicableToList.joinToString(", ") {
                    when(it) {
                        com.sky4th.equipment.attributes.EquipmentCategory.LIGHT_ARMOR -> "轻型装备"
                        com.sky4th.equipment.attributes.EquipmentCategory.HEAVY_ARMOR -> "重型装备"
                        com.sky4th.equipment.attributes.EquipmentCategory.WEAPON -> "武器"
                        com.sky4th.equipment.attributes.EquipmentCategory.BOW -> "弓, 弩"
                        com.sky4th.equipment.attributes.EquipmentCategory.TOOL -> "工具"
                        com.sky4th.equipment.attributes.EquipmentCategory.SHIELD -> "盾牌"
                        else -> it.name
                    }
                }
            }
        } else {
            // 没有具体槽位，显示通用装备类型
            applicableToList.joinToString(", ") {
                when(it) {
                    com.sky4th.equipment.attributes.EquipmentCategory.LIGHT_ARMOR -> "轻型装备"
                    com.sky4th.equipment.attributes.EquipmentCategory.HEAVY_ARMOR -> "重型装备"
                    com.sky4th.equipment.attributes.EquipmentCategory.WEAPON -> "武器"
                    com.sky4th.equipment.attributes.EquipmentCategory.BOW -> "弓, 弩"
                    com.sky4th.equipment.attributes.EquipmentCategory.TOOL -> "工具"
                    com.sky4th.equipment.attributes.EquipmentCategory.SHIELD -> "盾牌"
                    com.sky4th.equipment.attributes.EquipmentCategory.ELYTRA -> "鞘翅"
                    else -> it.name
                }
            }
        }
    }
}
