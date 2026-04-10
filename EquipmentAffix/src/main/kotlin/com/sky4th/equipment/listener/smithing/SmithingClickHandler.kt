
package com.sky4th.equipment.listener.smithing

import com.sky4th.equipment.data.NBTEquipmentDataManager
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/**
 * 锻造台点击处理器
 * 负责处理锻造台的点击事件，包括词条锻造和工具升级
 */
object SmithingClickHandler : Listener {

    // NBT键定义（从AffixSmithingHandler复制）
    private val KEY_AFFIX_ID = NamespacedKey("sky_equipment", "affix_id")
    private val KEY_UPGRADE_MATERIALS = NamespacedKey("sky_equipment", "upgrade_materials")

    /**
     * 处理锻造台点击事件
     */
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        // 只处理锻造台点击事件
        if (event.clickedInventory?.type != org.bukkit.event.inventory.InventoryType.SMITHING) return

        // 只处理输出槽（槽位3）
        if (event.slot != 3) return

        val inventory = event.clickedInventory as org.bukkit.inventory.SmithingInventory
        val currentItem = inventory.getItem(3) ?: return

        // 检查是否为我们的装备系统物品
        if (!NBTEquipmentDataManager.isEquipment(currentItem)) return

        // 获取模板槽（槽位0）
        val template = inventory.getItem(0)
        val mineral = inventory.getItem(2) ?: return

        if (template != null) {
            // 处理词条锻造
            handleAffixSmithingClick(event, inventory, currentItem, mineral, template)
        } else {
            // 处理工具升级
            handleToolUpgradeClick(event, inventory, currentItem, mineral)
        }
    }

    /**
     * 处理词条锻造点击事件
     */
    private fun handleAffixSmithingClick(
        event: InventoryClickEvent,
        inventory: org.bukkit.inventory.SmithingInventory,
        currentItem: ItemStack,
        mineral: ItemStack,
        template: ItemStack
    ) {
        val templateMeta = template.itemMeta ?: return

        // 检查是否为词条锻造模板（有customModelData）
        if (template.type != Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE || !templateMeta.hasCustomModelData()) {
            return
        }

        val player = event.whoClicked as org.bukkit.entity.Player
        val cursor = player.itemOnCursor

        // 处理光标物品
        if (!cursor.isEmpty) {
            // 如果光标有物品，尝试合并（需要相同物品且可堆叠）
            if (!cursor.isSimilar(currentItem) || cursor.amount + currentItem.amount > cursor.maxStackSize) {
                return // 无法合并，取消
            }
            cursor.amount = cursor.amount + currentItem.amount
        } else {
            // 光标为空，直接设置
            player.setItemOnCursor(currentItem.clone())
        }

        // 获取升级材料
        val upgradeMaterials = getUpgradeMaterials(templateMeta)
        if (upgradeMaterials.isEmpty()) return

        // 获取模板的词条ID
        val affixId = getAffixId(templateMeta) ?: return

        // 获取当前物品的词条等级
        val existingAffixes = NBTEquipmentDataManager.getAffixes(currentItem)
        val nextLevel = existingAffixes[affixId] ?: 0
        // 获取下一级所需的材料
        val materials = upgradeMaterials.getOrNull(nextLevel - 1) ?: return

        // 消耗材料
        val requiredAmount = materials[0].second
        mineral.amount = mineral.amount - requiredAmount
        // 更新材料槽（确保修改生效）
        inventory.setItem(2, if (mineral.amount <= 0) null else mineral)

        // 清空输出槽 & 工具槽
        inventory.setItem(1, null)
        inventory.setItem(3, null)

        // 取消原版事件
        event.isCancelled = true
    }

    /**
     * 处理工具升级点击事件
     */
    private fun handleToolUpgradeClick(
        event: InventoryClickEvent,
        inventory: org.bukkit.inventory.SmithingInventory,
        currentItem: ItemStack,
        mineral: ItemStack
    ) {
        val player = event.whoClicked as org.bukkit.entity.Player
        val cursor = player.itemOnCursor

        // 处理光标物品
        if (!cursor.isEmpty) {
            // 如果光标有物品，尝试合并（需要相同物品且可堆叠）
            if (!cursor.isSimilar(currentItem) || cursor.amount + currentItem.amount > cursor.maxStackSize) {
                return // 无法合并，取消
            }
            cursor.amount = cursor.amount + currentItem.amount
        } else {
            // 光标为空，直接设置
            player.setItemOnCursor(currentItem.clone())
        }

        // 获取原装备ID
        val equipmentId = NBTEquipmentDataManager.getEquipmentId(currentItem) ?: return

        // 根据工具类型确定所需材料数量
        // 注意：equipmentId会改变（如wooden_sword -> stone_sword -> iron_sword -> diamond_sword）
        // 但工具类型后缀（_SWORD、_AXE等）保持不变
        // 盔甲ID格式：{类型}_{材料}_{部位}，例如 heavy_chain_leggings
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
        if (mineral.amount < requiredAmount) {
            return
        }

        // 消耗材料
        mineral.amount = mineral.amount - requiredAmount
        // 更新材料槽（确保修改生效）
        inventory.setItem(2, if (mineral.amount <= 0) null else mineral)

        // 清空输出槽 & 工具槽
        inventory.setItem(1, null)
        inventory.setItem(3, null)

        // 取消原版事件
        event.isCancelled = true
    }

    /**
     * 获取模板的词条ID
     */
    private fun getAffixId(meta: org.bukkit.inventory.meta.ItemMeta): String? {
        val container = meta.persistentDataContainer
        return container.get(KEY_AFFIX_ID, PersistentDataType.STRING)
    }

    /**
     * 获取升级材料列表
     */
    private fun getUpgradeMaterials(meta: org.bukkit.inventory.meta.ItemMeta): List<List<Pair<Material, Int>>> {
        val container = meta.persistentDataContainer
        val materialsText = container.get(KEY_UPGRADE_MATERIALS, PersistentDataType.STRING) ?: return emptyList()

        val result = mutableListOf<List<Pair<Material, Int>>>()
        val levels = materialsText.split(";")

        for (level in levels) {
            val materials = mutableListOf<Pair<Material, Int>>()
            val items = level.split(",")

            for (item in items) {
                val parts = item.split("*")
                if (parts.isNotEmpty()) {
                    val materialName = parts[0].trim()
                    val count = if (parts.size > 1) parts[1].trim().toIntOrNull() ?: 1 else 1

                    val material = Material.matchMaterial(materialName)
                    if (material != null) {
                        materials.add(Pair(material, count))
                    }
                }
            }

            if (materials.isNotEmpty()) {
                result.add(materials)
            }
        }

        return result
    }
}
