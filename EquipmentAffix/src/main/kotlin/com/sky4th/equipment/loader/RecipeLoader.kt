package com.sky4th.equipment.loader

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.configuration.file.YamlConfiguration
import com.sky4th.equipment.manager.EquipmentManager
import com.sky4th.equipment.manager.RecipeManager
import com.sky4th.equipment.EquipmentAffix

/**
 * 配方加载器
 * 从装备配置文件中读取配方并注册到游戏中
 */
object RecipeLoader {

    /**
     * 从配置对象加载配方
     * 支持单个配方(recipe)或多个配方(recipe1, recipe2等)
     */
    fun loadRecipeFromConfig(config: YamlConfiguration, plugin: EquipmentAffix): Boolean {
        val id = config.getString("id") ?: return false

        // 检查是否有单个配方配置
        if (config.contains("recipe")) {
            val recipeSection = config.getConfigurationSection("recipe") ?: return false
            // 获取配方类型（默认为shaped）
            val recipeType = recipeSection.getString("type", "shaped")?.lowercase() ?: "shaped"

            return when (recipeType) {
                "shaped" -> loadShapedRecipe(config, plugin, "recipe")
                "shapeless" -> loadShapelessRecipe(config, plugin, "recipe")
                else -> {
                    plugin.logger.warning("未知的配方类型: $recipeType (装备: $id)")
                    false
                }
            }
        }

        // 检查是否有多个配方配置
        val recipeKeys = config.getKeys(true).filter { it.startsWith("recipe") && it.count { c -> c == '.' } == 0 }.sorted()
        if (recipeKeys.isEmpty()) {
            return false
        }

        var successCount = 0
        recipeKeys.forEach { recipeKey ->
            val recipeSection = config.getConfigurationSection(recipeKey) ?: return@forEach
            val recipeType = recipeSection.getString("type", "shaped")?.lowercase() ?: "shaped"

            val result = when (recipeType) {
                "shaped" -> loadShapedRecipe(config, plugin, recipeKey)
                "shapeless" -> loadShapelessRecipe(config, plugin, recipeKey)
                else -> {
                    plugin.logger.warning("未知的配方类型: $recipeType (装备: $id, 配方: $recipeKey)")
                    false
                }
            }

            if (result) {
                successCount++
            }
        }

        return successCount > 0
    }

    /**
     * 加载成型配方
     */
    private fun loadShapedRecipe(config: YamlConfiguration, plugin: EquipmentAffix, recipeSectionKey: String): Boolean {
        val id = config.getString("id") ?: return false
        val recipeSection = config.getConfigurationSection(recipeSectionKey) ?: return false

        // 获取配方形状
        val shape = recipeSection.getStringList("shape")
        if (shape.isEmpty()) {
            plugin.logger.warning("配方形状为空 (装备: $id, 配方: $recipeSectionKey)")
            return false
        }

        // 创建配方key，使用装备ID和配方部分key的组合
        val recipeKey = RecipeManager.createCustomKey("${id}_${recipeSectionKey}")

        // 创建结果物品
        val resultItem = EquipmentManager.createEquipment(id)
        if (resultItem == null) {
            plugin.logger.warning("无法创建装备物品 (装备: $id)")
            return false
        }

        // 创建成型配方
        val recipe = ShapedRecipe(recipeKey, resultItem)

        // 设置配方形状
        recipe.shape(*shape.toTypedArray())

        // 设置配方材料
        val ingredientsSection = recipeSection.getConfigurationSection("ingredients")
        
        if (ingredientsSection != null) {
            ingredientsSection.getKeys(false).forEach { key ->
                val value = ingredientsSection.get(key)
                if (value != null) {
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
                                    plugin.logger.warning("无效的材质: $materialName (装备: $id, 配方: $recipeSectionKey, 配方材料: $key)")
                                }
                            }
                        }
                        
                        // 使用RecipeChoice支持多个物品选择
                        if (materials.isNotEmpty()) {
                            val choice = org.bukkit.inventory.RecipeChoice.ExactChoice(*materials.map { ItemStack(it) }.toTypedArray())
                            recipe.setIngredient(key[0], choice)
                        }
                    } else {
                        // 原有逻辑：单个物品
                        val materialName = value.toString()
                        val material = Material.matchMaterial(materialName)
                        if (material != null) {
                            recipe.setIngredient(key[0], material)
                        } else {
                            plugin.logger.warning("无效的材质: $materialName (装备: $id, 配方: $recipeSectionKey, 配方材料: $key)")
                        }
                    }
                }
            }
        }

        // 添加配方
        if (RecipeManager.addCustomRecipe(recipe)) {
            return true
        }

        return false
    }

    /**
     * 加载无序配方
     */
    private fun loadShapelessRecipe(config: YamlConfiguration, plugin: EquipmentAffix, recipeSectionKey: String): Boolean {
        val id = config.getString("id") ?: return false
        val recipeSection = config.getConfigurationSection(recipeSectionKey) ?: return false

        // 获取配方材料列表
        val ingredientsList = recipeSection.getStringList("ingredients")
        if (ingredientsList.isEmpty()) {
            plugin.logger.warning("配方材料列表为空 (装备: $id, 配方: $recipeSectionKey)")
            return false
        }

        // 创建配方key，使用装备ID和配方部分key的组合
        val recipeKey = RecipeManager.createCustomKey("${id}_${recipeSectionKey}")

        // 创建结果物品
        val resultItem = EquipmentManager.createEquipment(id)
        if (resultItem == null) {
            plugin.logger.warning("无法创建装备物品 (装备: $id)")
            return false
        }

        // 创建无序配方
        val recipe = org.bukkit.inventory.ShapelessRecipe(recipeKey, resultItem)

        // 添加配方材料
        ingredientsList.forEach { materialName ->
            val material = org.bukkit.Material.matchMaterial(materialName)
            if (material != null) {
                recipe.addIngredient(material)
            } else {
                plugin.logger.warning("无效的材质: $materialName (装备: $id, 配方: $recipeSectionKey)")
            }
        }

        // 添加配方
        if (RecipeManager.addCustomRecipe(recipe)) {
            return true
        }

        return false
    }
}
