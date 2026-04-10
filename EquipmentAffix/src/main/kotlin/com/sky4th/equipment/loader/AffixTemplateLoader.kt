package com.sky4th.equipment.loader

import com.sky4th.equipment.EquipmentAffix
import com.sky4th.equipment.manager.RecipeManager
import com.sky4th.equipment.modifier.AffixConfigManager
import com.sky4th.equipment.attributes.EquipmentCategory
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import java.io.File
import java.io.InputStreamReader
import java.util.jar.JarFile

/**
 * 词条模板加载器
 * 从affix_template文件夹加载词条锻造模板的配置
 */
object AffixTemplateLoader {
    // 存储已加载的模板
    private val loadedTemplates = mutableMapOf<String, ItemStack>()

    // 存储模板的解锁阶段信息（按阶段分组存储）
    // Map<阶段名, 该阶段的模板ID列表>
    private val stageTemplates = mutableMapOf<String, MutableList<String>>()

    // 存储升级材料缓存
    // Map<词条ID, Pair<材料中文名称, 升级列表>>
    // 例如: {"fire_damage" -> Pair("羽毛", ["1", "2", "3", "5", "8"])}
    private val upgradeMaterialsCache = mutableMapOf<String, Pair<String, MutableList<String>>>()

    // 存储配方信息
    // Map<词条ID, List<SlotItem>>
    data class SlotItem(
        val row: Int,  // 行索引 (0-2)
        val col: Int,  // 列索引 (0-2)
        val material: String  // 材质名称
    )

    private val recipeCache = mutableMapOf<String, List<SlotItem>>()

    /**
     * 从资源文件夹加载所有词条模板配置
     */
    fun loadAll(plugin: EquipmentAffix) {
        val jarFile = File(plugin.javaClass.protectionDomain.codeSource.location.toURI())

        JarFile(jarFile).use { jar ->
            val entries = jar.entries()
            var loadedCount = 0
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.name.startsWith("affix_template/") && entry.name.endsWith(".yml")) {
                    try {
                        val input = jar.getInputStream(entry)
                        val config = YamlConfiguration.loadConfiguration(InputStreamReader(input))
                        loadTemplateFromConfig(config, plugin)
                        loadedCount++
                    } catch (e: Exception) {
                        plugin.logger.warning("加载词条模板配置失败: ${entry.name} - ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
            plugin.logger.info("已从JAR加载 $loadedCount 个词条模板配置")
        }
    }

    private fun loadTemplateFromConfig(config: YamlConfiguration, plugin: EquipmentAffix) {
        val affixId = config.getString("id") ?: return
        val displayName = "词条锻造模板"
        val maxLevel = config.getInt("max_level", 1)

        // 从词条配置中获取信息
        val affixConfig = AffixConfigManager.getAffixConfig(affixId)
        
        // 获取词条描述
        val affixDescription = if (affixConfig != null) {
            "<underlined>${affixConfig.displayName}</underlined><gray>: </gray>${affixConfig.description}"
        } else {
            config.getStringList("description").firstOrNull() ?: ""
        }

        // 获取解锁阶段
        val unlockStage = config.getString("unlock_stage") ?: ""

        // 存储阶段信息到内存（按阶段分组）
        if (unlockStage.isNotEmpty()) {
            stageTemplates.computeIfAbsent(unlockStage) { mutableListOf() }.add(affixId)
        }

        // 从词条配置中获取适用装备类型，如果不存在则从模板配置中获取
        val applicableToList = affixConfig?.applicableTo?.map { it.name } 
            ?: config.getStringList("applicable_to")

        // 从词条配置中获取装备槽位，如果不存在则从模板配置中获取
        val equipmentSlotList = affixConfig?.equipmentSlot 
            ?: config.getStringList("equipment_slot")
        val applicableToText = if (equipmentSlotList.isNotEmpty()) {
            // 如果有具体的装备槽位，显示具体的装备名称
            val slotTexts = equipmentSlotList.map { slot -> 
            when(slot.uppercase()) {
                    "HEAD" -> {
                        val armorTypes = mutableListOf<String>()
                        if (applicableToList.contains("LIGHT_ARMOR")) armorTypes.add("轻型头盔")
                        if (applicableToList.contains("HEAVY_ARMOR")) armorTypes.add("重型头盔")
                        armorTypes.joinToString(", ")
                    }
                    "CHEST" -> {
                        val armorTypes = mutableListOf<String>()
                        if (applicableToList.contains("LIGHT_ARMOR")) armorTypes.add("轻型胸甲")
                        if (applicableToList.contains("HEAVY_ARMOR")) armorTypes.add("重型胸甲")
                        armorTypes.joinToString(", ")
                    }
                    "LEGS" -> {
                        val armorTypes = mutableListOf<String>()
                        if (applicableToList.contains("LIGHT_ARMOR")) armorTypes.add("轻型护腿")
                        if (applicableToList.contains("HEAVY_ARMOR")) armorTypes.add("重型护腿")
                        armorTypes.joinToString(", ")
                    }
                    "FEET" -> {
                        val armorTypes = mutableListOf<String>()
                        if (applicableToList.contains("LIGHT_ARMOR")) armorTypes.add("轻型靴子")
                        if (applicableToList.contains("HEAVY_ARMOR")) armorTypes.add("重型靴子")
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
                "<#5555FF>${slotTexts.joinToString(", ")}</#5555FF>"
            } else {
                // 如果没有生成有效的槽位文本，回退到通用描述
                applicableToList.joinToString(", ") {
                    when(it.uppercase()) {
                        "LIGHT_ARMOR" -> "<#5555FF>轻型装备"
                        "HEAVY_ARMOR" -> "<#5555FF>重型装备"
                        "WEAPON" -> "<#5555FF>武器"
                        "BOW" -> "<#5555FF>弓, 弩"
                        "TOOL" -> "<#5555FF>工具"
                        "SHIELD" -> "<#5555FF>盾牌" 
                        else -> "<gray>$it"
                    }
                }
            }
        } else {
            // 没有具体槽位，显示通用装备类型
            applicableToList.joinToString(", ") {
                when(it.uppercase()) {
                    "LIGHT_ARMOR" -> "<#5555FF>轻型装备"
                    "HEAVY_ARMOR" -> "<#5555FF>重型装备"
                    "WEAPON" -> "<#5555FF>武器"
                    "BOW" -> "<#5555FF>弓, 弩"
                    "TOOL" -> "<#5555FF>工具"
                    "SHIELD" -> "<#5555FF>盾牌"
                    "ELYTRA" -> "<#5555FF>鞘翅"
                    else -> "<gray>$it"
                }
            }
        }

        // 生成描述信息
        val lore = mutableListOf<String>()

        // 添加词条描述
        if (affixDescription.isNotEmpty()) {
            lore.add(affixDescription)
        }

        // 添加"锻造不消耗模板"
        lore.add("<gray>锻造不消耗模板</gray>")
        lore.add("")

        // 添加"可应用于"
        lore.add("<gray>可应用于:</gray>")
        lore.add(applicableToText)

        // 添加"所需原材料"
        lore.add("<gray>所需原材料:</gray>")

        // 添加升级材料
        val upgradeMaterialsSection = config.getConfigurationSection("upgrade_materials")
        // 初始化该词条的材料缓存（只缓存第一种材料）
        var materialName = ""
        val materialCounts = mutableListOf<String>()
        if (upgradeMaterialsSection != null) {
            val romanNumerals = listOf("1级", "2级", "3级", "4级", "5级", "6级", "7级", "8级", "9级", "10级")
            for (i in 1..maxLevel) {
                val materials = upgradeMaterialsSection.getStringList("$i")
                if (materials.isNotEmpty()) {
                    val romanNumeral = if (i <= romanNumerals.size) romanNumerals[i - 1] else "$i"
                    val materialsText = materials.joinToString(" ") { material -> 
                        // 解析材料格式 "MATERIAL_NAME*count"
                        val parts = material.split("*")
                        val currentMaterialName = parts[0]
                        val count = if (parts.size > 1) parts[1] else "1"

                        // 只缓存第一种材料的中文名称
                        if (materialName.isEmpty()) {
                            materialName = getMaterialDisplayName(currentMaterialName)
                        }
                        materialCounts.add(count)

                        // 获取材料的中文名称
                        val materialDisplayName = getMaterialDisplayName(currentMaterialName)
                        "<#5555FF>$materialDisplayName × $count</#5555FF>"
                    }
                    lore.add("<#5555FF>$romanNumeral: $materialsText</#5555FF>")
                }
            }
        }

        // 缓存升级材料数据
        if (materialName.isNotEmpty() && materialCounts.isNotEmpty()) {
            upgradeMaterialsCache[affixId] = Pair(materialName, materialCounts)
        }

        // 创建模板物品
        val templateItem = ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE)
        val itemMeta = templateItem.itemMeta ?: return

        // 设置显示名称(无斜体)
        itemMeta.displayName(MiniMessage.miniMessage().deserialize(displayName).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false))

        // 隐藏原版描述信息
        itemMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP)

        // 设置描述(取消斜体)
        if (lore.isNotEmpty()) {
            itemMeta.lore(lore.map { MiniMessage.miniMessage().deserialize(it).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false) })
        }

        // 设置 CustomModelData（原版机制）
        val customModelData = config.getInt("custom_model_data", 0)
        if (customModelData > 0) {
            itemMeta.setCustomModelData(customModelData)
        }

        // 将模板信息存储到NBT中
        val container = itemMeta.persistentDataContainer

        // 存储affix_id
        container.set(
            NamespacedKey("sky_equipment", "affix_id"),
            org.bukkit.persistence.PersistentDataType.STRING,
            affixId
        )

        // 存储升级材料（每级用分号分隔，每级内用逗号分隔）
        val upgradeMaterialsText = mutableListOf<String>()
        if (upgradeMaterialsSection != null) {
            for (i in 1..maxLevel) {
                val materials = upgradeMaterialsSection.getStringList("$i")
                if (materials.isNotEmpty()) {
                    upgradeMaterialsText.add(materials.joinToString(","))
                }
            }
        }
        container.set(
            NamespacedKey("sky_equipment", "upgrade_materials"),
            org.bukkit.persistence.PersistentDataType.STRING,
            upgradeMaterialsText.joinToString(";")
        )

        // 将修改后的 meta 应用回物品
        templateItem.itemMeta = itemMeta

        // 存储模板
        loadedTemplates[affixId] = templateItem

        // 加载配方（不变）
        val recipeSection = config.getConfigurationSection("recipe")
        if (recipeSection != null) {
            val recipeType = recipeSection.getString("type", "shaped")?.lowercase() ?: "shaped"
            when (recipeType) {
                "shaped" -> loadShapedRecipe(config, plugin, templateItem)
                "shapeless" -> loadShapelessRecipe(config, plugin, templateItem)
                else -> plugin.logger.warning("未知的配方类型: $recipeType (模板: $affixId)")
            }
        }
    }

    /**
     * 获取材料的显示名称
     */
    private fun getMaterialDisplayName(materialName: String): String {
        return com.sky4th.equipment.util.MaterialNameUtil.getChineseName(materialName)
    }


    /**
     * 加载成型配方
     */
    private fun loadShapedRecipe(config: YamlConfiguration, plugin: EquipmentAffix, resultItem: ItemStack): Boolean {
        val affixId = config.getString("id") ?: return false
        val recipeSection = config.getConfigurationSection("recipe") ?: return false

        // 获取配方形状
        val shape = recipeSection.getStringList("shape")
        if (shape.isEmpty()) {
            plugin.logger.warning("配方形状为空 (模板: $affixId)")
            return false
        }

        // 创建配方key（将模板ID转换为小写，以符合NamespacedKey的要求）
        val recipeKey = RecipeManager.createCustomKey("affix_template_${affixId.lowercase()}")

        // 创建成型配方
        val recipe = ShapedRecipe(recipeKey, resultItem)

        // 设置配方形状
        recipe.shape(*shape.toTypedArray())

        // 创建符号到材质的映射
        val symbolToMaterials = mutableMapOf<Char, String>()

        // 设置配方材料
        val ingredientsSection = recipeSection.getConfigurationSection("ingredients")
        if (ingredientsSection != null) {
            ingredientsSection.getKeys(false).forEach { key ->
                val value = ingredientsSection.get(key)
                if (value != null && key.isNotEmpty()) {
                    // 使用key的第一个字符作为配方符号
                    val symbol = key[0]

                    // 检查是否为列表（支持多个物品混搭）
                    if (value is List<*>) {
                        // 收集所有有效的材质
                        val materials = mutableListOf<Material>()
                        value.forEach { materialName ->
                            if (materialName is String) {
                                val material = Material.matchMaterial(materialName)
                                if (material != null) {
                                    materials.add(material)
                                } else {
                                    plugin.logger.warning("无效的材质: $materialName (模板: $affixId, 配方材料: $key)")
                                }
                            }
                        }

                        // 使用RecipeChoice支持多个物品选择
                        if (materials.isNotEmpty()) {
                            val choice = org.bukkit.inventory.RecipeChoice.ExactChoice(*materials.map { ItemStack(it) }.toTypedArray())
                            recipe.setIngredient(symbol, choice)
                            // 存储符号到材质的映射（使用第一个材质）
                            symbolToMaterials[symbol] = value.firstOrNull()?.toString() ?: ""
                        }
                    } else {
                        // 原有逻辑：单个物品
                        val materialName = value.toString()
                        val material = Material.matchMaterial(materialName)
                        if (material != null) {
                            recipe.setIngredient(symbol, material)
                            // 存储符号到材质的映射
                            symbolToMaterials[symbol] = materialName
                        } else {
                            plugin.logger.warning("无效的材质: $materialName (模板: $affixId, 配方材料: $key)")
                        }
                    }
                }
            }
        }

        // 创建3x3格子中的物品列表
        val slots = mutableListOf<SlotItem>()
        for (row in shape.indices) {
            val line = shape[row]
            for (col in line.indices) {
                val symbol = line[col]
                if (symbol != ' ' && symbolToMaterials.containsKey(symbol)) {
                    val materialName = symbolToMaterials[symbol] ?: continue
                    slots.add(SlotItem(row, col, materialName))
                }
            }
        }

        // 存储配方信息
        recipeCache[affixId] = slots

        // 添加配方
        if (RecipeManager.addCustomRecipe(recipe)) {
            return true
        }

        return false
    }

    /**
     * 加载无序配方
     */
    private fun loadShapelessRecipe(config: YamlConfiguration, plugin: EquipmentAffix, resultItem: ItemStack): Boolean {
        val affixId = config.getString("id") ?: return false
        val recipeSection = config.getConfigurationSection("recipe") ?: return false

        // 获取配方材料列表
        val ingredientsList = recipeSection.getStringList("ingredients")
        if (ingredientsList.isEmpty()) {
            plugin.logger.warning("配方材料列表为空 (模板: $affixId)")
            return false
        }

        // 创建配方key（将模板ID转换为小写，以符合NamespacedKey的要求）
        val recipeKey = RecipeManager.createCustomKey("affix_template_${affixId.lowercase()}")

        // 创建无序配方
        val recipe = ShapelessRecipe(recipeKey, resultItem)

        // 创建3x3格子中的物品列表
        val slots = mutableListOf<SlotItem>()
        var slotIndex = 0

        // 添加配方材料
        ingredientsList.forEach { materialName ->
            val material = Material.matchMaterial(materialName)
            if (material != null) {
                recipe.addIngredient(material)
                // 按顺序填充到3x3格子中
                val row = slotIndex / 3
                val col = slotIndex % 3
                slots.add(SlotItem(row, col, materialName))
                slotIndex++
            } else {
                plugin.logger.warning("无效的材质: $materialName (模板: $affixId)")
            }
        }

        // 存储配方信息
        recipeCache[affixId] = slots

        // 添加配方
        if (RecipeManager.addCustomRecipe(recipe)) {
            return true
        }

        return false
    }

    /**
     * 获取已加载的词条模板
     * @param affixId 词条ID
     * @return 模板物品，如果不存在则返回null
     */
    fun getAffixTemplate(affixId: String): ItemStack? {
        return loadedTemplates[affixId]
    }

    /**
     * 获取所有已加载的词条模板ID
     * @return 模板ID列表
     */
    fun getAllTemplateIds(): List<String> {
        return loadedTemplates.keys.toList()
    }

    /**
     * 获取指定阶段的所有模板ID
     * @param stage 阶段名称
     * @return 该阶段的模板ID列表，如果阶段不存在则返回空列表
     */
    fun getTemplatesByStage(stage: String): List<String> {
        return stageTemplates[stage]?.toList() ?: emptyList()
    }

    /**
     * 获取模板所属的阶段
     * @param affixId 词条ID
     * @return 阶段名称，如果未设置则返回null
     */
    fun getTemplateStage(affixId: String): String? {
        return stageTemplates.entries.find { (_, templates) -> affixId in templates }?.key
    }

    /**
     * 获取所有阶段及其对应的模板
     * @return 阶段名称到模板ID列表的映射
     */
    fun getAllStageTemplates(): Map<String, List<String>> {
        return stageTemplates.mapValues { (_, templates) -> templates.toList() }
    }

    /**
     * 获取所有已定义的阶段名称
     * @return 阶段名称列表
     */
    fun getAllStages(): List<String> {
        return stageTemplates.keys.toList()
    }

    /**
     * 获取所有词条ID
     * @return 词条ID列表
     */
    fun getAllAffixIds(): List<String> {
        return loadedTemplates.keys.toList()
    }

    /**
     * 获取指定模板的升级材料文本
     * @param affixId 词条ID
     * @return 升级材料文本列表，每级一个字符串，格式为 "等级: 材料×数量"
     */
    fun getUpgradeMaterialsText(affixId: String): List<String> {
        val (materialName, counts) = upgradeMaterialsCache[affixId] ?: return emptyList()
        val romanNumerals = listOf("1级", "2级", "3级", "4级", "5级", "6级", "7级", "8级", "9级", "10级")

        val result = mutableListOf<String>()
        for (i in counts.indices) {
            val romanNumeral = if (i < romanNumerals.size) romanNumerals[i] else "${i + 1}级"
            val count = counts[i]
            result.add("<#5555FF>$romanNumeral: $materialName × $count</#5555FF>")
        }

        return result
    }

    /**
     * 获取指定模板的升级材料缓存
     * @param affixId 词条ID
     * @return 升级材料缓存，Pair<材料名称, 升级列表>
     */
    fun getUpgradeMaterialsCache(affixId: String): Pair<String, List<String>>? {
        return upgradeMaterialsCache[affixId]
    }

    /**
     * 获取词条的配方信息
     * @param affixId 词条ID
     * @return 配方信息列表，如果不存在则返回null
     */
    fun getRecipeInfo(affixId: String): List<SlotItem>? {
        return recipeCache[affixId]
    }
}
