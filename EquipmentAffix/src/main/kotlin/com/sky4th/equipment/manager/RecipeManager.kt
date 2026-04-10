
package com.sky4th.equipment.manager

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import com.sky4th.equipment.EquipmentAffix

/**
 * 配方管理器
 * 负责移除原版合成配方，同时支持添加自定义配方
 */
object RecipeManager {

    // 需要移除配方的材质列表
    private val removedMaterials = mutableSetOf<Material>()

    // 自定义配方命名空间
    private const val CUSTOM_NAMESPACE = "sky_equipment"

    // 已移除的原版配方key（用于记录和可能的恢复）
    private val removedVanillaKeys = mutableSetOf<NamespacedKey>()

    /**
     * 初始化配方管理器
     */
    fun initialize(plugin: EquipmentAffix) {
        // 从配置加载需要移除配方的材质
        loadFromConfig()

        // 移除配方
        removeRecipes()

        plugin.logger.info("已移除 ${removedVanillaKeys.size} 个原版合成配方")

        // 注册自定义的下界合金升级配方
        registerNetheriteUpgradeRecipes(plugin)
        plugin.logger.info("已注册自定义下界合金升级配方")
    }

    /**
     * 从配置文件加载需要移除配方的材质
     */
    private fun loadFromConfig() {
        // 获取配置中的材质列表
        val materialsList = com.sky4th.equipment.config.ConfigManager.getConfig().getStringList("removed_recipes")

        // 转换为Material并添加到集合
        materialsList.forEach { materialName ->
            // 使用matchMaterial代替valueOf，更灵活且大小写不敏感
            val material = Material.matchMaterial(materialName)
            if (material != null) {
                removedMaterials.add(material)
            } else {
                // 使用插件实例记录日志
                com.sky4th.equipment.EquipmentAffix.instance.logger.warning("无效的材质名称: $materialName")
            }
        }
    }

    /**
     * 移除所有配置的材质的原版配方
     */
    private fun removeRecipes() {
        val recipeIterator = Bukkit.recipeIterator()
        val recipesToRemove = mutableListOf<Pair<NamespacedKey, Material>>()

        // 收集需要移除的原版配方
        while (recipeIterator.hasNext()) {
            val recipe = recipeIterator.next()
            val recipeKey = getRecipeKey(recipe)
            val resultMaterial = getRecipeResultMaterial(recipe)

            if (shouldRemoveRecipe(recipeKey, resultMaterial)) {
                recipesToRemove.add(Pair(recipeKey, resultMaterial))
            }
        }

        // 移除配方
        recipesToRemove.forEach { (key, _) ->
            Bukkit.removeRecipe(key)
            removedVanillaKeys.add(key)
        }
    }

    /**
     * 获取配方的NamespacedKey
     */
    private fun getRecipeKey(recipe: Recipe): NamespacedKey {
        return try {
            // 尝试通过反射获取key属性
            val method = recipe.javaClass.getMethod("getKey")
            method.invoke(recipe) as NamespacedKey
        } catch (e: Exception) {
            // 如果反射失败，返回默认的minecraft命名空间
            NamespacedKey.minecraft("unknown")
        }
    }

    /**
     * 获取配方结果物品的材质
     */
    private fun getRecipeResultMaterial(recipe: Recipe): Material {
        return try {
            // 尝试通过反射获取result属性
            val method = recipe.javaClass.getMethod("getResult")
            val result = method.invoke(recipe) as ItemStack
            result.type
        } catch (e: Exception) {
            // 如果反射失败，返回AIR
            Material.AIR
        }
    }

    /**
     * 判断是否应该移除某个配方
     * 只移除原版(minecraft命名空间)的配方
     */
    private fun shouldRemoveRecipe(recipeKey: NamespacedKey, resultMaterial: Material): Boolean {
        // 只移除原版配方
        if (recipeKey.namespace != NamespacedKey.MINECRAFT) {
            return false
        }

        // 检查结果物品是否在移除列表中
        if (removedMaterials.contains(resultMaterial)) {
            return true
        }

        return false
    }

    /**
     * 创建自定义配方使用的NamespacedKey
     * 确保使用自定义命名空间，避免与原版配方冲突
     */
    fun createCustomKey(recipeName: String): NamespacedKey {
        return NamespacedKey(CUSTOM_NAMESPACE, recipeName)
    }

    /**
     * 添加自定义配方
     * 自动使用自定义命名空间
     */
    fun addCustomRecipe(recipe: Recipe): Boolean {
        val recipeKey = getRecipeKey(recipe)

        // 确保配方使用自定义命名空间
        if (recipeKey.namespace != CUSTOM_NAMESPACE) {
            throw IllegalArgumentException("自定义配方必须使用 '$CUSTOM_NAMESPACE' 命名空间")
        }

        return Bukkit.addRecipe(recipe)
    }

    /**
     * 添加需要移除配方的材质
     */
    fun addRemovedMaterial(material: Material) {
        removedMaterials.add(material)
    }

    /**
     * 移除需要移除配方的材质
     */
    fun removeRemovedMaterial(material: Material) {
        removedMaterials.remove(material)
    }

    /**
     * 获取所有需要移除配方的材质
     */
    fun getRemovedMaterials(): Set<Material> {
        return removedMaterials.toSet()
    }

    /**
     * 获取已移除的原版配方key
     */
    fun getRemovedVanillaKeys(): Set<NamespacedKey> {
        return removedVanillaKeys.toSet()
    }

    /**
     * 重新加载配方
     * 注意：无法恢复之前被移除的原版配方
     */
    fun reload() {
        // 清空当前移除列表
        removedMaterials.clear()

        // 重新加载配置
        loadFromConfig()

        // 重新移除配方
        removeRecipes()
        
        // 注册自定义的下界合金升级配方
        registerNetheriteUpgradeRecipes(com.sky4th.equipment.EquipmentAffix.instance)
        com.sky4th.equipment.EquipmentAffix.instance.logger.info("已注册自定义下界合金升级配方")

        com.sky4th.equipment.EquipmentAffix.instance.logger.info("已重新加载配方配置，移除了 ${removedVanillaKeys.size} 个原版合成配方")
    }

    /**
     * 注册自定义的下界合金升级配方
     * 使锻造台的下界合金升级模板可以接收所有工具/装备
     */
    private fun registerNetheriteUpgradeRecipes(plugin: EquipmentAffix) {
        val smithingMaterials = getSmithingMaterials()

        smithingMaterials.forEach { material ->
            try {
                val recipeKey = RecipeManager.createCustomKey("equipment_fix_${material.name.lowercase()}")
                val result = ItemStack(material)

                // 创建锻造配方，将 Material 转换为 RecipeChoice
                val recipe = org.bukkit.inventory.SmithingTransformRecipe(
                    recipeKey,
                    result,
                    org.bukkit.inventory.RecipeChoice.MaterialChoice(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                    org.bukkit.inventory.RecipeChoice.MaterialChoice(material),
                    org.bukkit.inventory.RecipeChoice.MaterialChoice(Material.SPAWNER)
                )

                RecipeManager.addCustomRecipe(recipe)
            } catch (e: Exception) {
                plugin.logger.warning("注册下界合金升级配方失败: ${material.name}, 原因: ${e.message}")
            }
        }

        val smithingMineral = getSmithingMineral()

        smithingMineral.forEach { mineral ->
            try{
                val key = RecipeManager.createCustomKey("equipment_fix_${mineral.name.lowercase()}")

                // 创建锻造配方，将 Material 转换为 RecipeChoice
                val recipes = org.bukkit.inventory.SmithingTransformRecipe(
                    key,
                    ItemStack(mineral),
                    org.bukkit.inventory.RecipeChoice.MaterialChoice(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                    org.bukkit.inventory.RecipeChoice.MaterialChoice(Material.SPAWNER),
                    org.bukkit.inventory.RecipeChoice.MaterialChoice(mineral)
                )

                RecipeManager.addCustomRecipe(recipes)
            } catch (e: Exception) {
                plugin.logger.warning("注册下界合金升级配方失败: ${mineral.name}, 原因: ${e.message}")
            }
        }

    }

    /**
     * 获取所有可锻造的装备和工具材质
     */
    private fun getSmithingMaterials(): List<Material> {
        return listOf(
            Material.WOODEN_SWORD,
            Material.WOODEN_PICKAXE,
            Material.WOODEN_AXE,
            Material.WOODEN_SHOVEL,
            Material.WOODEN_HOE,
            Material.STONE_SWORD,
            Material.STONE_PICKAXE,
            Material.STONE_AXE,
            Material.STONE_SHOVEL,
            Material.STONE_HOE,
            Material.IRON_SWORD,
            Material.IRON_PICKAXE,
            Material.IRON_AXE,
            Material.IRON_SHOVEL,
            Material.IRON_HOE,
            Material.GOLDEN_SWORD,
            Material.GOLDEN_PICKAXE,
            Material.GOLDEN_AXE,
            Material.GOLDEN_SHOVEL,
            Material.GOLDEN_HOE,
            Material.NETHERITE_SWORD,
            Material.NETHERITE_PICKAXE,
            Material.NETHERITE_AXE,
            Material.NETHERITE_SHOVEL,
            Material.NETHERITE_HOE,
            Material.TRIDENT,
            Material.MACE,
            Material.SHIELD,
            Material.BOW,
            Material.CROSSBOW,
            Material.FISHING_ROD,
            Material.TURTLE_HELMET,
            Material.ELYTRA
        )
    }

    /**
     * 获取额外材料
     */
    private fun getSmithingMineral(): List<Material> {
        return listOf(
            Material.IRON_BLOCK,         // 铁块
            Material.COBBLESTONE,        // 圆石
            Material.COBBLED_DEEPSLATE,  // 深板岩圆石
            Material.BLACKSTONE,         // 黑石
            Material.OBSIDIAN,           // 黑曜石
            Material.ROOTED_DIRT,        // 缠根泥土
            Material.CACTUS,             // 仙人掌
            Material.STRING,             // 线
            Material.ARROW,              // 箭
            Material.FLINT,              // 燧石
            Material.CHARCOAL,           // 木炭
            Material.PHANTOM_MEMBRANE,   // 幻翼膜
            Material.NAUTILUS_SHELL,     // 鹦鹉螺壳
            Material.WIND_CHARGE,        // 风弹
            Material.ROTTEN_FLESH,       // 腐肉
            Material.SUGAR,              // 糖
            Material.SPIDER_EYE,         // 蜘蛛眼
            Material.GUNPOWDER,          // 火药
            Material.BONE,               // 骨头 
            Material.ICE,                // 冰
            Material.FEATHER,            // 羽毛
            Material.RABBIT_FOOT,        // 兔子脚
            Material.VINE,               // 藤蔓
            Material.SNOWBALL,           // 雪球 
            Material.PRISMARINE_SHARD,   // 海晶碎片
            Material.SLIME_BALL,         // 粘液球 
            Material.BREEZE_ROD,         // 旋风棒
            Material.GLOWSTONE_DUST,     // 萤石粉
            Material.BLAZE_POWDER,       // 烈焰粉
            Material.ENDER_PEARL,        // 末影珍珠
            Material.ENDER_EYE,          // 末影之眼
            Material.SHULKER_SHELL       // 潜影壳
        )
    }
}
