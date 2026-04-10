
package com.sky4th.equipment.listener

import com.sky4th.equipment.manager.EquipmentManager
import com.sky4th.equipment.util.EnchantmentUtil
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.LootGenerateEvent
import org.bukkit.event.entity.ItemSpawnEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable

/**
 * 战利品生成监听器
 * 将原版装备武器替换为自定义版本
 */
class LootGenerateListener : Listener {

    @EventHandler
    fun onItemSpawn(event: ItemSpawnEvent) {
        val item = event.entity.itemStack
        if (isVanillaWeaponOrArmor(item)) {
            val customItem = generateCustomEquipment(item, event.entity.world.environment)
            if (customItem != null) {
                event.entity.itemStack = customItem
            }
        }
    }

    @EventHandler
    fun onLootGenerate(event: LootGenerateEvent) {
        val loot = event.loot // MutableList<ItemStack>
        val iterator = loot.iterator()
        val newLoot = mutableListOf<ItemStack>()

        // 直接通过 event.world 获取世界环境
        val world = event.world
        val environment = world.environment

        while (iterator.hasNext()) {
            val item = iterator.next()
            if (isVanillaWeaponOrArmor(item)) {
                iterator.remove()
                val customItem = generateCustomEquipment(item, environment)
                if (customItem != null) {
                    newLoot.add(customItem)
                }
            }
        }
        loot.addAll(newLoot)
    }

    /**
     * 判断是否为原版武器或护甲
     */
    private fun isVanillaWeaponOrArmor(item: ItemStack): Boolean {
        // 首先检查是否已经是自定义装备，如果是则跳过
        if (com.sky4th.equipment.data.NBTEquipmentDataManager.isEquipment(item)) {
            return false
        }

        val material = item.type
        val name = material.name

        return when {
            // 武器
            name.endsWith("_SWORD") ||
            name.endsWith("_AXE") ||
            name.endsWith("_PICKAXE") ||
            name.endsWith("_SHOVEL") ||
            name.endsWith("_HOE") ||
            material == Material.BOW ||
            material == Material.CROSSBOW ||
            material == Material.TRIDENT ||
            material == Material.MACE ||
            material == Material.SHEARS ||
            material == Material.FISHING_ROD ||
            material == Material.SHIELD -> true

            // 护甲
            name.endsWith("_HELMET") ||
            name.endsWith("_CHESTPLATE") ||
            name.endsWith("_LEGGINGS") ||
            name.endsWith("_BOOTS") ||
            material == Material.ELYTRA -> true

            else -> false
        }
    }

    /**
     * 生成自定义装备
     * @param originalItem 原版物品
     * @param environment 世界环境类型
     * @return 自定义装备，如果无法生成则返回null
     */
    private fun generateCustomEquipment(
        originalItem: ItemStack,
        environment: org.bukkit.World.Environment?
    ): ItemStack? {
        val material = originalItem.type
        val materialName = material.name.lowercase()

        // 判断是否为护甲（需要区分轻重）
        val isArmor = materialName.endsWith("_helmet") ||
                      materialName.endsWith("_chestplate") ||
                      materialName.endsWith("_leggings") ||
                      materialName.endsWith("_boots")

        // 判断是否为需要区分轻重的材质
        val hasLightHeavyVariant = materialName.startsWith("chain_") ||
                                    materialName.startsWith("iron_") ||
                                    materialName.startsWith("golden_") ||
                                    materialName.startsWith("diamond_") ||
                                    materialName.startsWith("netherite_")

        // 根据材质生成装备ID
        val equipmentId = when {
            // 护甲 - 需要区分轻重
            isArmor && hasLightHeavyVariant -> {
                // 根据世界类型和战利品来源决定轻重比例
                val lightArmorChance = when {
                    // 主世界：80% 轻甲
                    environment == org.bukkit.World.Environment.NORMAL -> 0.80
                    // 地狱：70% 轻甲
                    environment == org.bukkit.World.Environment.NETHER -> 0.70
                    // 末地：60% 轻甲
                    environment == org.bukkit.World.Environment.THE_END -> 0.60
                    // 默认 80% 轻甲
                    else -> 0.80
                }
                
                // 根据概率决定生成轻甲还是重甲
                if (kotlin.random.Random.nextDouble() < lightArmorChance) {
                    "light_$materialName"
                } else {
                    "heavy_$materialName"
                }
            }
            // 其他情况
            else -> materialName
        }

        // 创建自定义装备
        val customEquipment = EquipmentManager.createEquipment(equipmentId) ?: return null

        // 保留原装备的耐久度消耗
        val originalMeta = originalItem.itemMeta
        val customMeta = customEquipment.itemMeta

        if (originalMeta is Damageable && customMeta is Damageable) {
            // 直接使用原装备的耐久度消耗值
            customMeta.damage = originalMeta.damage
            customEquipment.itemMeta = customMeta
        }

        // 保留并转移原装备的附魔
        transferEnchantments(originalItem, customEquipment)

        return customEquipment
    }

    /**
     * 转移附魔从原装备到自定义装备
     * 完全保留所有附魔，不受槽位限制
     * 仍然会检查附魔是否适用于装备类型和词条冲突
     * @param source 源装备
     * @param target 目标装备
     */
    private fun transferEnchantments(source: ItemStack, target: ItemStack) {
        EnchantmentUtil.transferEnchantmentsIgnoreSlotLimit(source, target)
    }
}
