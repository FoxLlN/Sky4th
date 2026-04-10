package com.sky4th.equipment.manager

import com.sky4th.equipment.attributes.EquipmentCategory
import com.sky4th.equipment.attributes.EquipmentAttributes
import com.sky4th.equipment.modifier.ConfiguredModifier
import com.sky4th.equipment.manager.EquipmentManager
import com.sky4th.equipment.data.NBTEquipmentDataManager
import com.sky4th.equipment.util.DisplayUtil
import com.sky4th.equipment.util.LanguageUtil
import net.kyori.adventure.text.Component
import org.bukkit.inventory.ItemStack
import sky4th.core.api.LanguageAPI
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * Lore显示管理器
 * 负责根据显示模式生成物品描述
 * 数据从NBT标签中读取，Lore仅用于显示
 */
object LoreDisplayManager {

    /**
     * 修改物品描述
     * 优化版本：使用缓存机制和性能监控
     */
    fun modifyItemLore(item: ItemStack, detailed: Boolean): ItemStack {
        return com.sky4th.equipment.monitor.PerformanceMonitorHelper.monitor("lore_update") {
            val meta = item.itemMeta ?: return item
            if (!NBTEquipmentDataManager.isEquipment(item)) return item

            // 从缓存获取或生成 Lore
            val cachedLore = com.sky4th.equipment.cache.LoreDisplayCache.getLore(item, detailed)
            val lore = cachedLore ?: run {
                val attributes = EquipmentManager.getEquipmentAttributes(item)
                val newLore = if (detailed) generateDetailedLore(attributes, item) else generateSimpleLore(attributes, item)
                com.sky4th.equipment.cache.LoreDisplayCache.cacheLore(item, detailed, newLore)
                newLore
            }

            meta.lore(lore)
            item.itemMeta = meta
            item
        }
    }
    
    /**
     * 生成简单描述
     * 显示熟练度等级 + 词条/附魔名称和等级（不显示详细描述）
     */
    fun generateSimpleLore(attributes: EquipmentAttributes, item: ItemStack): List<Component> {
        val lore = mutableListOf<Component>()
        val plugin = com.sky4th.equipment.EquipmentAffix.instance

        // 获取装备类型信息 - 通过NBT中的装备类型ID获取
        val equipmentId = NBTEquipmentDataManager.getEquipmentId(item)
        val equipmentType = equipmentId?.let { com.sky4th.equipment.registry.EquipmentRegistry.getEquipmentType(it) }

        // 显示基础属性（武器显示攻击伤害，装备显示护甲）
        if (equipmentType != null) {
            val baseAttributes = when {
                // 三叉戟：显示特殊攻击伤害格式
                equipmentType.id == "trident" -> {
                    val damage = equipmentType.attackDamage
                    LanguageUtil.getComponentNoItalic(plugin, "lore.simple.base-trident",
                        "damage" to formatValue(damage))
                }
                // 重锤：显示特殊攻击伤害格式
                equipmentType.id == "mace" -> {
                    val damage = equipmentType.attackDamage
                    LanguageUtil.getComponentNoItalic(plugin, "lore.simple.base-mace",
                        "damage" to formatValue(damage))
                }
                // 弓：显示特殊攻击伤害格式
                equipmentType.id == "bow" -> {
                    LanguageUtil.getComponentNoItalic(plugin, "lore.simple.base-bow")
                }
                // 弩：显示特殊攻击伤害格式
                equipmentType.id == "crossbow" -> {
                    LanguageUtil.getComponentNoItalic(plugin, "lore.simple.base-crossbow")
                }
                EquipmentCategory.WEAPON in equipmentType.categories -> {
                    // 武器：显示攻击伤害
                    val damage = equipmentType.attackDamage
                    LanguageUtil.getComponentNoItalic(plugin, "lore.simple.base-weapon",
                        "damage" to formatValue(damage))
                }
                EquipmentCategory.LIGHT_ARMOR in equipmentType.categories ||
                EquipmentCategory.HEAVY_ARMOR in equipmentType.categories -> {
                    // 护甲：显示护甲值
                    LanguageUtil.getComponentNoItalic(plugin, "lore.simple.base-armor",
                        "armor" to formatValue(equipmentType.armor))
                }
                else -> null
            }
            if (baseAttributes != null) {
                lore.add(baseAttributes)
            }
        }

        // 显示熟练度等级
        val levelKey = DisplayUtil.getProficiencyLevelKey(attributes.proficiencyLevel)
        val levelName = LanguageAPI.getText(plugin, "proficiency.level.$levelKey")
        lore.add(LanguageUtil.getComponentNoItalic(plugin, "lore.simple.level", 
            "level" to levelName))

        // 显示材质效果（从ModifierManager中获取名称和描述）
        val materialEffect = attributes.materialEffect
        val modifier = com.sky4th.equipment.modifier.ModifierManager.instance.getModifier(materialEffect)
        val effectName = if (modifier is ConfiguredModifier) {
            modifier.getDisplayName()
        } else {
            materialEffect
        }
        lore.add(LanguageUtil.getComponentNoItalic(plugin, "lore.simple.material-effect",
            "effect_name" to effectName))

        // 显示词条名称和等级（使用罗马数字）
        // 排除材质效果词条（以material_开头的词条）
        val normalAffixes = attributes.affixes.filter { !it.key.startsWith("material_") }
        if (normalAffixes.isNotEmpty()) {
            normalAffixes.forEach { (affixId, level) ->
                val modifier = com.sky4th.equipment.modifier.ModifierManager.instance.getModifier(affixId)
                val affixName = if (modifier is ConfiguredModifier) {
                    modifier.getDisplayName()
                } else {
                    affixId
                }
                lore.add(LanguageUtil.getComponentNoItalic(plugin, "lore.simple.affix-entry",
                    "affix_name" to affixName,
                    "roman_level" to DisplayUtil.toRoman(level)))
            }
        }

        // 显示附魔名称和等级（使用罗马数字）
        if (attributes.enchantments.isNotEmpty()) {
            attributes.enchantments.forEach { (enchant, level) ->
                lore.add(LanguageUtil.getComponentNoItalic(plugin, "lore.simple.enchant-entry",
                    "enchant_name" to DisplayUtil.getEnchantmentName(enchant),
                    "roman_level" to DisplayUtil.toRoman(level)))
            }
        }
        
        // 显示纹质信息（如果装备有纹质）
        val trimInfo = getTrimInfo(item)
        if (trimInfo != null) {
            lore.add(trimInfo)
        }

        // 添加切换提示
        lore.add(Component.empty())
        lore.add(LanguageUtil.getComponentNoItalic(plugin, "lore.simple.toggle-hint"))

        return lore
    }


    /**
     * 生成详细描述
     * 显示熟练度等级 + 熟练度进度 + 词条详情
     */
    fun generateDetailedLore(attributes: EquipmentAttributes, item: ItemStack): List<Component> {
        val lore = mutableListOf<Component>()
        val plugin = com.sky4th.equipment.EquipmentAffix.instance

        // 获取装备类型信息 - 通过NBT中的装备类型ID获取
        val equipmentId = NBTEquipmentDataManager.getEquipmentId(item)
        val equipmentType = equipmentId?.let { com.sky4th.equipment.registry.EquipmentRegistry.getEquipmentType(it) }

        // 显示基础属性（武器显示攻击伤害，装备显示护甲）
        if (equipmentType != null) {
            val baseAttributes = when {
                // 三叉戟：显示特殊攻击伤害格式
                equipmentType.id == "trident" -> {
                    val damage = equipmentType.attackDamage
                    LanguageUtil.getComponentNoItalic(plugin, "lore.detailed.base-trident",
                        "damage" to formatValue(damage))
                }
                // 重锤：显示特殊攻击伤害格式
                equipmentType.id == "mace" -> {
                    val damage = equipmentType.attackDamage
                    LanguageUtil.getComponentNoItalic(plugin, "lore.detailed.base-mace",
                        "damage" to formatValue(damage))
                }
                // 弓：显示特殊攻击伤害格式
                equipmentType.id == "bow" -> {
                    LanguageUtil.getComponentNoItalic(plugin, "lore.simple.base-bow")
                }
                // 弩：显示特殊攻击伤害格式
                equipmentType.id == "crossbow" -> {
                    LanguageUtil.getComponentNoItalic(plugin, "lore.simple.base-crossbow")
                }
                EquipmentCategory.WEAPON in equipmentType.categories -> {
                    // 武器：显示攻击伤害
                    val damage = equipmentType.attackDamage
                    LanguageUtil.getComponentNoItalic(plugin, "lore.detailed.base-weapon",
                        "damage" to formatValue(damage))
                }
                EquipmentCategory.LIGHT_ARMOR in equipmentType.categories ||
                EquipmentCategory.HEAVY_ARMOR in equipmentType.categories -> {
                    // 护甲：显示护甲值
                    LanguageUtil.getComponentNoItalic(plugin, "lore.detailed.base-armor",
                        "armor" to formatValue(equipmentType.armor))
                }
                else -> null
            }
            if (baseAttributes != null) {
                lore.add(baseAttributes)
            }
        }

        // 显示特殊类型机制（轻型/重型装备）
        if (equipmentType != null) {
            when {
                EquipmentCategory.LIGHT_ARMOR in equipmentType.categories -> {
                    // 轻型装备：显示闪避率
                    if (attributes.dodgeChance > 0) {
                        lore.add(LanguageUtil.getComponentNoItalic(plugin, "lore.detailed.light-armor",
                            "dodge" to formatValue(attributes.dodgeChance * 100)))
                    }
                }
                EquipmentCategory.HEAVY_ARMOR in equipmentType.categories -> {
                    // 重型装备：显示抗击退、速度惩罚、饥饿惩罚
                    val hasKnockback = attributes.knockbackResistance > 0
                    val hasMovementPenalty = attributes.movementPenalty > 0
                    val hasHungerPenalty = attributes.hungerPenalty > 0
                    
                    if (hasKnockback) {
                        lore.add(LanguageUtil.getComponentNoItalic(plugin, "lore.detailed.heavy-armor",
                            "knockback" to formatValue(attributes.knockbackResistance * 100)))
                    }
                    if (hasMovementPenalty || hasHungerPenalty) {
                        val speedValue = if (hasMovementPenalty) formatValue(attributes.movementPenalty * 100) else "0"
                        val hungerValue = if (hasHungerPenalty) formatValue(attributes.hungerPenalty * 100) else "0"
                        lore.add(LanguageUtil.getComponentNoItalic(plugin, "lore.detailed.heavy-armor-punishment",
                            "speed" to speedValue,
                            "hunger" to hungerValue))
                    }
                }
                else -> {}
            }
        }

        // 熟练度等级和进度
        val levelKey = DisplayUtil.getProficiencyLevelKey(attributes.proficiencyLevel)
        val levelName = LanguageAPI.getText(plugin, "proficiency.level.$levelKey")
        val usage = attributes.proficiency.toString()
        val levelRequirements = com.sky4th.equipment.config.ConfigManager.levelRequirements
        val maxLevel = levelRequirements.size
        val nextThreshold = if (attributes.proficiencyLevel < maxLevel) {
            levelRequirements[attributes.proficiencyLevel].toString()
        } else {
            attributes.proficiency.toString()
        }
        lore.add(LanguageUtil.getComponentNoItalic(plugin, "lore.detailed.level",
            "level" to levelName,
            "usage" to usage,
            "nextThreshold" to nextThreshold))

        // 显示材质效果（从ModifierManager中获取名称和描述）
        val materialEffect = attributes.materialEffect
        val modifier = com.sky4th.equipment.modifier.ModifierManager.instance.getModifier(materialEffect)
        val effectName = if (modifier is ConfiguredModifier) {
            modifier.getDisplayName()
        } else {
            materialEffect
        }
        val effectDesc = if (modifier is ConfiguredModifier) {
            modifier.getDescription()
        } else {
            ""
        }
        lore.add(LanguageUtil.getComponentNoItalic(plugin, "lore.detailed.material-effect-detail",
            "effect_name" to effectName,
            "description" to effectDesc))

        // 词条详细信息
        // 排除材质效果词条（以material_开头的词条）
        val normalAffixes = attributes.affixes.filter { !it.key.startsWith("material_") }
        if (normalAffixes.isNotEmpty()) {
            normalAffixes.forEach { (affixId, level) ->
                val modifier = com.sky4th.equipment.modifier.ModifierManager.instance.getModifier(affixId)
                val affixName = if (modifier is ConfiguredModifier) {
                    modifier.getDisplayName()
                } else {
                    affixId
                }
                val affixDesc = if (modifier is ConfiguredModifier) {
                    modifier.getDescription()
                } else {
                    ""
                }
                
                // 检查词条是否支持充能，如果支持则使用带存储的格式
                val affixConfig = com.sky4th.equipment.modifier.AffixConfigManager.getAffixConfig(affixId)
                if (affixConfig != null && affixConfig.isChargeable) {
                    val currentAmount = NBTEquipmentDataManager.getAffixResource(item, affixId)
                    val maxAmount = affixConfig.chargeMaxStorage[level] ?: 0

                    // 获取资源名称
                    val resourceName = affixConfig.chargeResourceName ?: affixConfig.chargeResource ?: affixId

                    lore.add(LanguageUtil.getComponentNoItalic(plugin, "lore.detailed.affix-entry-store",
                        "affix_name" to affixName,
                        "roman_level" to DisplayUtil.toRoman(level),
                        "description" to affixDesc,
                        "currentAmount" to currentAmount.toString(),
                        "maxAmount" to maxAmount.toString(),
                        "resource_name" to resourceName))
                } else {
                    lore.add(LanguageUtil.getComponentNoItalic(plugin, "lore.detailed.affix-entry",
                        "affix_name" to affixName,
                        "roman_level" to DisplayUtil.toRoman(level),
                        "description" to affixDesc))
                }
            }
        }

        // 剩余词条槽位（只有当剩余槽位大于0时才显示）
        // 材质效果词条不计入槽位
        val maxAffixSlots = NBTEquipmentDataManager.getMaxAffixSlots(item)
        if (maxAffixSlots > 0) {
            val usedAffixSlots = normalAffixes.size
            val remainingAffixSlots = maxAffixSlots - usedAffixSlots
            if (remainingAffixSlots > 0) {
                lore.add(LanguageUtil.getComponentNoItalic(plugin, "lore.detailed.slots-remaining",
                    "remaining" to remainingAffixSlots.toString()))
            }
        }

        // 附魔详细信息
        if (attributes.enchantments.isNotEmpty()) {
            attributes.enchantments.forEach { (enchant, level) ->
                val enchantName = DisplayUtil.getEnchantmentName(enchant)
                val enchantDesc = DisplayUtil.getEnchantmentDescription(enchant, level)
                lore.add(LanguageUtil.getComponentNoItalic(plugin, "lore.detailed.enchant-entry",
                    "enchant_name" to enchantName,
                    "roman_level" to DisplayUtil.toRoman(level),
                    "description" to enchantDesc))
            }
        }

        // 剩余附魔槽位（只有当剩余槽位大于0时才显示）
        val maxEnchantSlots = NBTEquipmentDataManager.getMaxEnchantmentSlots(item)
        if (maxEnchantSlots > 0) {
            val usedEnchantSlots = attributes.enchantments.size
            val remainingEnchantSlots = maxEnchantSlots - usedEnchantSlots
            if (remainingEnchantSlots > 0) {
                lore.add(LanguageUtil.getComponentNoItalic(plugin, "lore.detailed.enchant-slots-remaining",
                    "remaining" to remainingEnchantSlots.toString()))
            }
        }

        // 显示纹质信息（如果装备有纹质）
        val trimInfo = getTrimInfo(item)
        if (trimInfo != null) {
            lore.add(trimInfo)
        }

        // 添加切换提示
        lore.add(Component.empty())
        lore.add(LanguageUtil.getComponentNoItalic(plugin, "lore.detailed.toggle-hint"))

        return lore
    }
    
    /**
     * 格式化数值显示
     * 如果是整数（如1.0）则显示为整数（1），否则保留一位小数
     */
    private fun formatValue(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format("%.1f", value)
        }
    }

    /**
     * 获取纹质信息
     * @param item 装备物品
     * @return 纹质信息的Component，如果没有纹质则返回null
     */
    private fun getTrimInfo(item: ItemStack): Component? {
        val meta = item.itemMeta ?: return null

        // 检查是否有纹质
        val hasTrim = try {
            val trimField = meta.javaClass.getDeclaredField("trim")
            trimField.isAccessible = true
            trimField.get(meta) != null
        } catch (e: Exception) {
            false
        }

        if (!hasTrim) return null

        // 获取纹饰信息
        val trim = try {
            val trimField = meta.javaClass.getDeclaredField("trim")
            trimField.isAccessible = true
            trimField.get(meta)
        } catch (e: Exception) {
            return null
        }

        // 获取材质和模板
        val material = try {
            val materialField = trim.javaClass.getDeclaredField("material")
            materialField.isAccessible = true
            val materialObj = materialField.get(trim)
            // 获取内部枚举值
            val keyField = materialObj.javaClass.getDeclaredField("key")
            keyField.isAccessible = true
            keyField.get(materialObj).toString()
        } catch (e: Exception) {
            return null
        }

        val pattern = try {
            val patternField = trim.javaClass.getDeclaredField("pattern")
            patternField.isAccessible = true
            val patternObj = patternField.get(trim)
            // 获取内部枚举值
            val keyField = patternObj.javaClass.getDeclaredField("key")
            keyField.isAccessible = true
            keyField.get(patternObj).toString()
        } catch (e: Exception) {
            return null
        }

        // 获取带颜色的材质名称
        val coloredMaterial = getColoredMaterial(material)

        // 获取模板名称
        val patternName = getPatternName(pattern)

        // 让纹饰也显示相同的颜色
        val patternLore = sky4th.core.util.ColorUtil.convertMiniMessageToLegacy("$coloredMaterial ${patternName}纹饰")
        //val patternComponent = LegacyComponentSerializer.legacySection().deserialize(patternLore)
        // 使用语言文件中的格式
        val plugin = com.sky4th.equipment.EquipmentAffix.instance
        val trimText = LanguageUtil.getComponentNoItalic(plugin, "lore.detailed.trim-info",
            "pattern" to patternLore)

        return trimText
    }

    /**
     * 获取带颜色的材质名称
     */
    private fun getColoredMaterial(material: Any): String {
        val materialName = material.toString()
        val (name, color) = when (materialName) {
            "minecraft:amethyst" -> "紫水晶质" to "#9A5CC6"
            "minecraft:copper" -> "铜质" to "#B4684D"
            "minecraft:diamond" -> "钻石质" to "#6EECD2"
            "minecraft:emerald" -> "绿宝石质" to "#0EC754"
            "minecraft:gold" -> "金质" to "#DEB12D"
            "minecraft:iron" -> "铁质" to "#A2B0B3"
            "minecraft:lapis" -> "青金石质" to "#345EC3"
            "minecraft:netherite" -> "下界合金质" to "#625859"
            "minecraft:quartz" -> "下界石英质" to "#E3D4C4"
            "minecraft:redstone" -> "红石质" to "#BD2008"
            else -> materialName to "#FFFFFF"
        }
        // <#颜色>文本
        return  "<$color>$name"
    }

    /**
     * 获取模板名称
     */
    private fun getPatternName(pattern: Any): String {
        val patternName = pattern.toString()
        return when (patternName) {
            "minecraft:sentry" -> "哨兵"
            "minecraft:vex" -> "恼鬼"
            "minecraft:wild" -> "荒野"
            "minecraft:coast" -> "海岸"
            "minecraft:dune" -> "沙丘"
            "minecraft:wayfinder" -> "向导"
            "minecraft:raiser" -> "牧民"
            "minecraft:shaper" -> "塑造"
            "minecraft:host" -> "雇主"
            "minecraft:ward" -> "监守"
            "minecraft:silence" -> "幽静"
            "minecraft:tide" -> "潮汐"
            "minecraft:snout" -> "猪鼻"
            "minecraft:rib" -> "肋骨"
            "minecraft:eye" -> "眼眸"
            "minecraft:spire" -> "尖塔"
            "minecraft:flow" -> "涡轮"
            "minecraft:bolt" -> "镶铆"
            else -> patternName
        }
    }
}
