
package com.sky4th.equipment.listener.smithing

import com.sky4th.equipment.data.NBTEquipmentDataManager
import com.sky4th.equipment.modifier.ModifierManager
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.event.inventory.PrepareSmithingEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/**
 * 词条锻造处理器
 * 负责处理词条锻造逻辑
 */
object AffixSmithingHandler {

    // NBT键定义
    private val KEY_AFFIX_ID = NamespacedKey("sky_equipment", "affix_id")
    private val KEY_UPGRADE_MATERIALS = NamespacedKey("sky_equipment", "upgrade_materials")

    /**
     * 处理词条锻造
     * @param event 锻造台事件
     * @param equipment 装备物品
     * @param mineral 矿物物品
     * @param template 模板物品
     */
    fun handle(event: PrepareSmithingEvent, equipment: ItemStack, mineral: ItemStack, template: ItemStack) {
        val templateMeta = template.itemMeta ?: return

        // 获取模板的词条ID
        val affixId = getAffixId(templateMeta) ?: return

        // 获取词条的冲突列表
        val modifier = ModifierManager.instance.getModifier(affixId)
        if (modifier == null) {
            event.result = null
            return
        }

        val conflictingAffixes = modifier.getConflictingAffixes()
        val applicableTo = modifier.getApplicableTo()

        // 获取装备ID
        val equipmentId = NBTEquipmentDataManager.getEquipmentId(equipment) ?: return

        // 获取装备类型
        val equipmentType = com.sky4th.equipment.registry.EquipmentRegistry.getEquipmentType(equipmentId) ?: return

        // 检查装备类别
        val allCategories = equipmentType.categories
        if (allCategories.none { it in applicableTo }) {
            event.result = null
            return
        }

        // 从词条配置中获取装备槽位限制
        val affixConfig = com.sky4th.equipment.modifier.AffixConfigManager.getAffixConfig(affixId)
        val equipmentSlots = affixConfig?.equipmentSlot ?: emptyList()
        if (equipmentSlots.isNotEmpty()) {
            // 检查装备材质是否在允许的槽位中
            val equipmentSlot = getEquipmentSlot(equipmentType.material)
            if (equipmentSlot !in equipmentSlots) {
                event.result = null
                return
            }
        }

        // 获取装备上已有的词条
        val existingAffixes = NBTEquipmentDataManager.getAffixes(equipment)

        // 检查是否已经有该词条
        if (existingAffixes.containsKey(affixId)) {
            // 获取当前等级
            val currentLevel = existingAffixes[affixId] ?: 0

            // 获取最大等级
            val maxLevel = getMaxLevel(templateMeta)

            // 如果已经达到最大等级，阻止锻造
            if (currentLevel >= maxLevel) {
                event.result = null
                return
            }

            // 检查升级材料
            val upgradeMaterials = getUpgradeMaterials(templateMeta)
            if (upgradeMaterials.isEmpty()) {
                event.result = null
                return
            }

            // 获取下一级所需的材料
            val nextLevel = currentLevel + 1
            val materials = upgradeMaterials.getOrNull(nextLevel - 1)

            if (materials == null || !checkMaterials(mineral, materials)) {
                event.result = null
                return
            }

            // 升级词条
            val upgradedEquipment = equipment.clone()

            // 清除instance_id，使物品被视为新实例
            NBTEquipmentDataManager.clearInstanceId(upgradedEquipment)
            NBTEquipmentDataManager.setAffix(upgradedEquipment, affixId, nextLevel)

            // 如果词条是初始化型词条，调用onSmithing方法（升级时isInit=false）
            if (modifier.isInitModifier()) {
                modifier.onSmithing(upgradedEquipment, nextLevel, false)
            }

            // 更新物品描述
            updateItemDisplay(upgradedEquipment)

            event.result = upgradedEquipment
            return
        }

        // 检查锻造槽位限制
        val maxSlots = NBTEquipmentDataManager.getMaxAffixSlots(equipment)
        if (maxSlots >= 0) {
            // 获取当前物品已有的附魔数量
            val currentEnchantments = existingAffixes.size
            val remainingSlots = maxSlots - currentEnchantments

            // 如果剩余槽位为0，取消锻造
            if (remainingSlots <= 0) {
                event.result = null
                return
            }
        }

        // 检查词条冲突
        for (conflictId in conflictingAffixes) {
            if (existingAffixes.containsKey(conflictId)) {
                // 存在冲突，阻止锻造
                event.result = null
                return
            }
        }

        // 检查附魔冲突
        val conflictingEnchantments = modifier.getConflictingEnchantments()
        if (conflictingEnchantments.isNotEmpty()) {
            val itemEnchantments = equipment.enchantments
            for (enchantment in conflictingEnchantments) {
                if (itemEnchantments.containsKey(enchantment)) {
                    // 存在冲突附魔，阻止锻造
                    event.result = null
                    return
                }
            }
        }

        // 检查第一级材料
        val upgradeMaterials = getUpgradeMaterials(templateMeta)
        if (upgradeMaterials.isEmpty()) {
            event.result = null
            return
        }

        val materials = upgradeMaterials.getOrNull(0)
        if (materials == null || !checkMaterials(mineral, materials)) {
            event.result = null
            return
        }

        // 添加新词条
        val newEquipment = equipment.clone()
        // 清除instance_id，使物品被视为新实例
        NBTEquipmentDataManager.clearInstanceId(newEquipment)
        NBTEquipmentDataManager.setAffix(newEquipment, affixId, 1)

        // 如果词条是初始化型词条，调用onSmithing方法（首次打上时isInit=true）
        if (modifier.isInitModifier()) {
            modifier.onSmithing(newEquipment, 1, true)
        }

        // 更新物品描述
        updateItemDisplay(newEquipment)

        event.result = newEquipment
    }


    /**
     * 获取模板的词条ID
     * @return 大写的词条ID
     */
    private fun getAffixId(meta: org.bukkit.inventory.meta.ItemMeta): String? {
        val container = meta.persistentDataContainer
        return container.get(KEY_AFFIX_ID, PersistentDataType.STRING)
    }

    /**
     * 获取最大等级
     */
    private fun getMaxLevel(meta: org.bukkit.inventory.meta.ItemMeta): Int {
        // 从升级材料数量推断最大等级
        val upgradeMaterials = getUpgradeMaterials(meta)
        return upgradeMaterials.size
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

    /**
     * 检查材料是否匹配
     */
    private fun checkMaterials(mineral: ItemStack, requiredMaterials: List<Pair<Material, Int>>): Boolean {
        // 简化版本：只检查第一个材料
        if (requiredMaterials.isEmpty()) return false

        val (requiredMaterial, requiredCount) = requiredMaterials[0]

        // 检查矿物类型是否匹配
        if (mineral.type != requiredMaterial) return false

        // 检查数量是否足够
        if (mineral.amount < requiredCount) return false

        return true
    }

    /**
     * 更新物品的显示
     */
    private fun updateItemDisplay(item: ItemStack) {
        val meta = item.itemMeta ?: return

        // 隐藏原版附魔显示
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)

        // 获取当前是否处于详细模式
        val isDetailed = isDetailedDescription(item)

        // 更新物品meta
        item.itemMeta = meta

        // 使用LoreDisplayManager更新描述
        val modifiedItem = com.sky4th.equipment.manager.LoreDisplayManager.modifyItemLore(item, isDetailed)

        // 直接使用修改后的物品的所有属性
        item.itemMeta = modifiedItem.itemMeta
        item.amount = modifiedItem.amount
    }

    /**
     * 获取物品当前的详细描述模式
     * @return true = 详细模式，false = 简单模式
     */
    private fun isDetailedDescription(item: ItemStack): Boolean {
        val meta = item.itemMeta ?: return false
        val container = meta.persistentDataContainer
        val key = org.bukkit.NamespacedKey("sky_equipment", "detailed_lore")
        return container.getOrDefault(key, org.bukkit.persistence.PersistentDataType.BYTE, 0) == 1.toByte()
    }

    /**
     * 根据装备材质获取装备槽位
     * 根据材质名称的后缀判断装备槽位
     */
    private fun getEquipmentSlot(material: Material): String {
        return when {
            // 头盔
            material.name.endsWith("_HELMET") -> "HEAD"
            // 胸甲
            material.name.endsWith("_CHESTPLATE") -> "CHEST"
            // 护腿
            material.name.endsWith("_LEGGINGS") -> "LEGS"
            // 靴子
            material.name.endsWith("_BOOTS") -> "FEET"
            // 剑
            material.name.endsWith("_SWORD") -> "SWORD"
            // 斧
            material.name.endsWith("_AXE") -> "AXE"
            // 镐
            material.name.endsWith("_PICKAXE") -> "PICKAXE"
            // 铲
            material.name.endsWith("_SHOVEL") -> "SHOVEL"
            // 锄
            material.name.endsWith("_HOE") -> "HOE"
            // 弓
            material.name == "BOW" -> "BOW"
            // 弩
            material.name == "CROSSBOW" -> "CROSSBOW"
            // 三叉戟
            material.name == "TRIDENT" -> "TRIDENT"
            // 重锤
            material.name == "MACE" -> "MACE"
            // 钓鱼竿
            material.name == "FISHING_ROD" -> "FISHING_ROD"
            // 盾牌
            material.name == "SHIELD" -> "SHIELD"
            // 鞘翅
            material.name == "ELYTRA" -> "ELYTRA"
            // 其他
            else -> "NONE"
        }
    }
}
