package sky4th.bettervillage.listeners

import org.bukkit.Material
import org.bukkit.entity.Villager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.VillagerCareerChangeEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.MerchantRecipe
import org.bukkit.persistence.PersistentDataType
import sky4th.bettervillage.BetterVillage
import sky4th.bettervillage.config.TradeConfig

class VillagerCustomizer : Listener {

    companion object {
        private val PROFESSION_LOCKED_KEY = BetterVillage.namespacedKey("profession_locked")
        private val REAL_LEVEL_KEY = BetterVillage.namespacedKey("real_level")

        fun getRealLevel(villager: Villager): Int {
            return villager.persistentDataContainer
                .get(REAL_LEVEL_KEY, PersistentDataType.INTEGER) ?: villager.villagerLevel
        }

        fun setRealLevel(villager: Villager, level: Int) {
            villager.persistentDataContainer.set(REAL_LEVEL_KEY, PersistentDataType.INTEGER, level)
        }

        fun initRealLevel(villager: Villager) {
            if (!villager.persistentDataContainer.has(REAL_LEVEL_KEY, PersistentDataType.INTEGER)) {
                setRealLevel(villager, villager.villagerLevel)
            }
        }
    }

    /**
     * 村民生成时初始化真实等级并应用初始交易
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onVillagerSpawn(event: CreatureSpawnEvent) {
        val villager = event.entity as? Villager ?: return
        initRealLevel(villager)
        applyCustomTrades(villager, true)
    }

    /**
     * 职业变更时锁定职业（防止原版转职）
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onVillagerCareerChange(event: VillagerCareerChangeEvent) {
        val villager = event.entity
        // 如果村民已锁定，则不允许任何职业变更（取消事件）
        if (isProfessionLocked(villager)) {
            event.isCancelled = true
            return
        }
        if (event.profession != Villager.Profession.NONE) {
            lockVillagerProfession(villager)
        }
    }

    // ========== 职业锁定 ==========
    private fun lockVillagerProfession(villager: Villager) {
        villager.persistentDataContainer.set(PROFESSION_LOCKED_KEY, PersistentDataType.BYTE, 1)
    }

    @Suppress("unused")
    private fun isProfessionLocked(villager: Villager): Boolean {
        return villager.persistentDataContainer.has(PROFESSION_LOCKED_KEY, PersistentDataType.BYTE)
    }

    // ========== 自定义交易 ==========
    
    /**
     * 根据村民当前显示等级应用自定义交易
     * 可被外部调用刷新交易
     */
    fun applyCustomTrades(villager: Villager, isInit: Boolean = false) {
        if (isInit) {
            villager.setRecipes(emptyList())
            val initRecipes = generateCustomRecipes(villager, true)
            villager.setRecipes(initRecipes)
        } else {
            val displayLevel = villager.villagerLevel
            val existingRecipes = villager.recipes
            val expectedCount = displayLevel * 4

            // 如果已有交易数量足够，不再添加（避免重复）
            if (existingRecipes.size >= expectedCount) return

            val newRecipes = generateCustomRecipes(villager)
            val combined = existingRecipes + newRecipes
            villager.setRecipes(combined)
        }
        
        // 确保职业锁定标记存在
        if (villager.profession != Villager.Profession.NONE && !isProfessionLocked(villager)) {
            lockVillagerProfession(villager)
        }
    }

    /**
     * 根据村民职业和显示等级生成新交易
     */
    private fun generateCustomRecipes(villager: Villager, isInit: Boolean = false): List<MerchantRecipe> {
        val profession = villager.profession
        val displayLevel = villager.villagerLevel

        val professionName = when (profession) {
            Villager.Profession.FARMER -> "farmer"
            Villager.Profession.LIBRARIAN -> "librarian"
            Villager.Profession.WEAPONSMITH -> "weaponsmith"
            Villager.Profession.ARMORER -> "armorer"
            Villager.Profession.TOOLSMITH -> "toolsmith"
            Villager.Profession.BUTCHER -> "butcher"
            Villager.Profession.LEATHERWORKER -> "leatherworker"
            Villager.Profession.CARTOGRAPHER -> "cartographer"
            Villager.Profession.SHEPHERD -> "shepherd"
            Villager.Profession.MASON -> "mason"
            Villager.Profession.FLETCHER -> "fletcher"
            Villager.Profession.CLERIC -> "cleric"
            Villager.Profession.FISHERMAN -> "fisherman"
            else -> "default"
        }
        var needCount = 4
        if (!isInit) {
            val existingCount = villager.recipes.size
            val expectedCount = displayLevel * 4
            if (existingCount >= expectedCount) return emptyList()

            needCount = expectedCount - existingCount
        }
        val emeraldConfigs = TradeConfig.getEmeraldTrades(professionName, displayLevel)
        val otherConfigs = TradeConfig.getOtherTrades(professionName, displayLevel)

        if (emeraldConfigs.isEmpty() && otherConfigs.isEmpty()) {
            return generateDefaultRecipes()
        }

        val recipes = mutableListOf<MerchantRecipe>()

        // 抽取绿宝石交易（约一半）
        val emeraldTake = needCount / 2
        val emeraldRecipes = emeraldConfigs
            .map { createRecipe(it.cost, it.costAmount, it.cost2, it.cost2Amount, it.result, it.resultAmount, it.maxUses, it.experience, it.extraType, it.extraData) }
            .shuffled()
            .take(minOf(emeraldTake, emeraldConfigs.size))
        recipes.addAll(emeraldRecipes)

        // 抽取非绿宝石交易
        val otherTake = needCount - emeraldRecipes.size
        val otherRecipes = otherConfigs
            .map { createRecipe(it.cost, it.costAmount, it.cost2, it.cost2Amount, it.result, it.resultAmount, it.maxUses, it.experience, it.extraType, it.extraData) }
            .shuffled()
            .take(minOf(otherTake, otherConfigs.size))
        recipes.addAll(otherRecipes)

        return recipes
    }

    private fun generateDefaultRecipes(): List<MerchantRecipe> = listOf(
        createRecipe(Material.EMERALD, 1, null, 1, Material.DIAMOND, 1, 10),
        createRecipe(Material.DIAMOND, 1, null, 1, Material.EMERALD, 3, 5)
    )

    private fun createRecipe(
        cost: Material,
        costAmount: Int,
        cost2: Material? = null,
        cost2Amount: Int = 1,
        result: Material,
        resultAmount: Int,
        maxUses: Int,
        experience: Int = 0,
        extraType: String? = null,
        extraData: List<String>? = null
    ): MerchantRecipe {
        val costItem = ItemStack(cost, costAmount)
        val resultItem = ItemStack(result, resultAmount)

        // 处理特殊效果
        if (extraType != null && extraData != null) {
            applySpecialEffect(resultItem, extraType, extraData)
        }

        val recipe = MerchantRecipe(resultItem, maxUses)
        recipe.addIngredient(costItem)
        
        // 如果存在第二个cost物品，也添加到交易中
        if (cost2 != null) {
            val cost2Item = ItemStack(cost2, cost2Amount)
            recipe.addIngredient(cost2Item)
        }
        
        recipe.villagerExperience = experience

        // 设置价格调整参数，使交易价格能够随使用次数变化
        // priceMultiplier: 每次使用后价格变化的幅度（默认0.05）
        // demand: 需求值，影响价格（默认0）
        recipe.priceMultiplier = 0.05f
        recipe.demand = 0

        return recipe
    }

    /**
     * 应用特殊效果到物品
     * @param item 要应用效果的物品
     * @param extraType 效果类型：enchantment(附魔)、potion(药水效果)、suspicious_stew(秘制炖菜效果)
     * @param extraData 效果数据列表，格式如 ["sharpness:5", "protection:3"] 或 ["speed:300:1"]
     */
    private fun applySpecialEffect(item: ItemStack, extraType: String, extraData: List<String>) {
        when (extraType.lowercase()) {
            "enchantment" -> extraData.forEach { applyEnchantment(item, it) }
            "potion" -> extraData.forEach { applyPotionEffect(item, it) }
            "suspicious_stew" -> extraData.forEach { applySuspiciousStewEffect(item, it) }
        }
    }

    /**
     * 应用附魔效果
     * @param item 要附魔的物品
     * @param data 附魔数据，格式如 "sharpness:5" 或 "sharpness:5:protection:3"
     */
    private fun applyEnchantment(item: ItemStack, data: String) {
        val itemMeta = item.itemMeta ?: return
        val parts = data.split(":")

        // 每两个部分组成一个附魔（名称:等级）
        for (i in parts.indices step 2) {
            if (i + 1 >= parts.size) break

            val enchantName = parts[i].lowercase()
            val level = parts[i + 1].toIntOrNull() ?: 1

            try {
                @Suppress("DEPRECATION")
                val enchantment = org.bukkit.Registry.ENCHANTMENT.get(
                    org.bukkit.NamespacedKey.minecraft(enchantName)
                )
                if (enchantment != null && enchantment.canEnchantItem(item)) {
                    itemMeta.addEnchant(enchantment, level, true)
                }
            } catch (e: Exception) {
                BetterVillage.instance.logger.warning("无法应用附魔: $enchantName, 原因: ${e.message}")
            }
        }

        item.itemMeta = itemMeta
    }

    /**
     * 应用药水效果
     * @param item 药水物品
     * @param data 药水效果数据，格式如 "speed:300:1" (效果类型:持续时间:等级)
     */
    private fun applyPotionEffect(item: ItemStack, data: String) {
        val itemMeta = item.itemMeta as? org.bukkit.inventory.meta.PotionMeta ?: return
        val parts = data.split(":")

        if (parts.size >= 3) {
            val effectName = parts[0].lowercase()
            val duration = parts[1].toIntOrNull() ?: 300
            val amplifier = parts[2].toIntOrNull() ?: 1

            try {
                val potionEffectType = org.bukkit.Registry.POTION_EFFECT_TYPE.get(org.bukkit.NamespacedKey.minecraft(effectName))
                if (potionEffectType != null) {
                    val effect = org.bukkit.potion.PotionEffect(potionEffectType, duration * 20, amplifier - 1)
                    @Suppress("DEPRECATION")
                    itemMeta.addCustomEffect(effect, true)
                }
            } catch (e: Exception) {
                BetterVillage.instance.logger.warning("无法应用药水效果: $effectName, 原因: ${e.message}")
            }
        }

        item.itemMeta = itemMeta
    }

    /**
     * 应用秘制炖菜效果
     * @param item 秘制炖菜物品
     * @param data 效果数据，格式如 "speed:300:1" (效果类型:持续时间:等级)
     */
    private fun applySuspiciousStewEffect(item: ItemStack, data: String) {
        if (item.type != org.bukkit.Material.SUSPICIOUS_STEW) {
            BetterVillage.instance.logger.warning("秘制炖菜效果只能应用到SUSPICIOUS_STEW物品上")
            return
        }

        val itemMeta = item.itemMeta as? org.bukkit.inventory.meta.SuspiciousStewMeta ?: return
        val parts = data.split(":")

        if (parts.size >= 3) {
            val effectName = parts[0].lowercase()
            val duration = parts[1].toIntOrNull() ?: 300
            val amplifier = parts[2].toIntOrNull() ?: 1

            try {
                val potionEffectType = org.bukkit.Registry.POTION_EFFECT_TYPE.get(org.bukkit.NamespacedKey.minecraft(effectName))
                if (potionEffectType != null) {
                    val effect = org.bukkit.potion.PotionEffect(potionEffectType, duration * 20, amplifier - 1)
                    @Suppress("DEPRECATION")
                    itemMeta.addCustomEffect(effect, true)
                }
            } catch (e: Exception) {
                BetterVillage.instance.logger.warning("无法应用秘制炖菜效果: $effectName, 原因: ${e.message}")
            }
        }

        item.itemMeta = itemMeta
    }
}