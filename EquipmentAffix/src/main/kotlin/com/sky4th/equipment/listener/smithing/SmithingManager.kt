
package com.sky4th.equipment.listener.smithing

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareSmithingEvent
import org.bukkit.inventory.ItemStack
import com.sky4th.equipment.data.NBTEquipmentDataManager
import com.sky4th.equipment.manager.EquipmentManager
import com.sky4th.equipment.registry.EquipmentRegistry

/**
 * 锻造台管理器
 * 统一处理所有锻造台相关逻辑
 * 包括装备升级、工具升级和词条锻造
 */
class SmithingManager : Listener {

    companion object {
        // 定义装备升级路径：源材料 -> 目标材料 -> 升级所需矿物
        // 格式：source-target -> UpgradePath(source, target, materials)
        val equipmentUpgradePaths = mapOf(
            // 木制升级路径（支持圆石、深板岩圆石、黑石）
            "wooden-stone" to UpgradePath(
                sourceMaterial = "wooden",
                targetMaterial = "stone",
                upgradeMaterial = listOf(
                    Material.COBBLESTONE,
                    Material.COBBLED_DEEPSLATE,
                    Material.BLACKSTONE
                )
            ),
            // 石制升级路径
            "stone-iron" to UpgradePath(
                sourceMaterial = "stone",
                targetMaterial = "iron",
                upgradeMaterial = listOf(Material.IRON_INGOT)
            ),
            // 锁链升级路径
            "chain-iron" to UpgradePath(
                sourceMaterial = "chain",
                targetMaterial = "iron",
                upgradeMaterial = listOf(Material.IRON_INGOT)
            ),
            // 铁制升级路径（铁质 -> 钻石）
            "iron-diamond" to UpgradePath(
                sourceMaterial = "iron",
                targetMaterial = "diamond",
                upgradeMaterial = listOf(Material.DIAMOND)
            ),
            // 铁制升级路径（铁质 -> 金质）
            "iron-golden" to UpgradePath(
                sourceMaterial = "iron",
                targetMaterial = "golden",
                upgradeMaterial = listOf(Material.GOLD_INGOT)
            )
        )

        /**
         * 升级路径数据类
         */
        data class UpgradePath(
            val sourceMaterial: String,              // 源材料名称
            val targetMaterial: String,                // 目标材料名称
            val upgradeMaterial: List<Material>       // 升级所需矿物列表（支持多个）
        )
    }

    @EventHandler
    fun onPrepareSmithing(event: PrepareSmithingEvent) {
        val inventory = event.inventory

        // 获取输入物品
        val equipment = inventory.inputEquipment ?: return
        val mineral = inventory.inputMineral ?: return

        // 检查是否为装备系统物品
        if (!NBTEquipmentDataManager.isEquipment(equipment)) return

        val template = inventory.inputTemplate

        if (template == null) {
            val equipmentId = NBTEquipmentDataManager.getEquipmentId(equipment) ?: return
            val upgradePath = findToolUpgradePath(equipmentId, mineral.type)

            if (upgradePath != null) {
                // 根据装备类型确定所需材料数量
                val requiredAmount = when {
                    // 工具类
                    equipmentId.contains("_shovel") || equipmentId.contains("_hoe") || equipmentId.contains("_sword") -> 1
                    equipmentId.contains("_axe") || equipmentId.contains("_pickaxe") -> 2
                    // 轻型盔甲（以 light_ 开头）
                    equipmentId.startsWith("light_") && equipmentId.endsWith("helmet") -> 4
                    equipmentId.startsWith("light_") && equipmentId.endsWith("chestplate") -> 7
                    equipmentId.startsWith("light_") && equipmentId.endsWith("leggings") -> 6
                    equipmentId.startsWith("light_") && equipmentId.endsWith("boots") -> 3
                    // 重型盔甲（以 heavy_ 开头）
                    equipmentId.startsWith("heavy_") && equipmentId.endsWith("helmet") -> 10
                    equipmentId.startsWith("heavy_") && equipmentId.endsWith("chestplate") -> 15
                    equipmentId.startsWith("heavy_") && equipmentId.endsWith("leggings") -> 14
                    equipmentId.startsWith("heavy_") && equipmentId.endsWith("boots") -> 9
                    // 默认
                    else -> 1
                }

                // 检查材料数量是否足够
                if (mineral.amount >= requiredAmount) {
                    val upgradedEquipment = EquipmentUpgradeHandler.handle(equipment, upgradePath)
                    event.result = upgradedEquipment
                }
            }
            
            return
        }

        val templateMeta = template.itemMeta ?: return

        // 处理词条锻造模板
        if (template.type == Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE && templateMeta.hasCustomModelData()) {
            // 由AffixSmithingListener处理
            AffixSmithingHandler.handle(event, equipment, mineral, template)
            return
        }

        // 处理下界合金升级
        if (template.type == Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE && !templateMeta.hasCustomModelData()) {
            if (mineral.type == Material.NETHERITE_INGOT) {
                val upgradedEquipment = NetheriteUpgradeHandler.handle(equipment)
                event.result = upgradedEquipment
                return
            }
        }

        // 处理纹饰装饰
        if (template.type != Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE) {
            val originalResult = event.result ?: return
            NBTEquipmentDataManager.clearInstanceId(originalResult)
            val trimmedEquipment = originalResult.clone()
            val isDetailed = isDetailedDescription(equipment)
            updateDisplay(trimmedEquipment, isDetailed)
            event.result = trimmedEquipment
            return
        }

        // 没有匹配的升级模板，阻止升级
        event.result = null
    }

    /**
     * 查找工具升级路径
     */
    private fun findToolUpgradePath(equipmentId: String, mineralType: Material): Companion.UpgradePath? {
        for ((key, path) in Companion.equipmentUpgradePaths) {
            // 检查源材料是否匹配
            if (equipmentId.contains(path.sourceMaterial) && mineralType in path.upgradeMaterial) {
                return path
            }
        }
        return null
    }

    /**
     * 获取物品当前的详细描述模式
     */
    private fun isDetailedDescription(item: ItemStack): Boolean {
        val meta = item.itemMeta ?: return false
        val container = meta.persistentDataContainer
        val key = org.bukkit.NamespacedKey("sky_equipment", "detailed_lore")
        return container.getOrDefault(key, org.bukkit.persistence.PersistentDataType.BYTE, 0) == 1.toByte()
    }

    /**
     * 更新物品的显示
     */
    private fun updateDisplay(item: ItemStack, isDetailed: Boolean) {
        val meta = item.itemMeta ?: return

        // 隐藏原版附魔显示
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)

        // 隐藏原版纹饰显示
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ARMOR_TRIM)

        // 更新物品meta
        item.itemMeta = meta

        // 使用LoreDisplayManager更新描述
        val modifiedItem = com.sky4th.equipment.manager.LoreDisplayManager.modifyItemLore(item, isDetailed)

        // 直接使用修改后的物品的所有属性
        item.itemMeta = modifiedItem.itemMeta
        item.amount = modifiedItem.amount
    }
}
