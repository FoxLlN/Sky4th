
package com.sky4th.equipment.listener.smithing

import org.bukkit.inventory.ItemStack
import com.sky4th.equipment.data.NBTEquipmentDataManager
import com.sky4th.equipment.manager.EquipmentManager
import com.sky4th.equipment.registry.EquipmentRegistry

/**
 * 装备升级辅助类
 * 提供装备升级的通用方法
 */
object EquipmentUpgradeHelper {

    /**
     * 升级装备
     * @param equipment 原装备
     * @param newEquipmentId 新装备ID
     * @return 升级后的装备，如果升级失败则返回null
     */
    fun upgradeEquipment(equipment: ItemStack, newEquipmentId: String): ItemStack? {
        // 获取原装备的属性
        val proficiencyLevel = NBTEquipmentDataManager.getProficiencyLevel(equipment)
        val proficiency = NBTEquipmentDataManager.getProficiency(equipment)

        // 计算总熟练度值（包括之前所有等级的熟练度）
        val levelRequirements = com.sky4th.equipment.config.ConfigManager.levelRequirements
        var totalProficiency = proficiency

        // 累加之前所有等级所需的熟练度
        for (level in 0 until proficiencyLevel) {
            if (level < levelRequirements.size) {
                totalProficiency += levelRequirements[level]
            }
        }

        // 升级时总熟练度只保留80%
        val retainedTotalProficiency = (totalProficiency * 0.8).toInt()

        // 根据折算后的总熟练度重新计算熟练度等级
        var newProficiencyLevel = 0
        var tempProficiency = retainedTotalProficiency

        // 从0级开始，逐级计算
        for (level in levelRequirements.indices) {
            if (tempProficiency >= levelRequirements[level]) {
                newProficiencyLevel = level + 1
                tempProficiency -= levelRequirements[level]
            } else {
                break
            }
        }

        val affixes = NBTEquipmentDataManager.getAffixes(equipment)

        // 获取原装备的Lore显示状态
        val isDetailed = isDetailedDescription(equipment)

        // 检查装备是否被改名
        val customName = getCustomDisplayName(equipment)

        // 获取原装备的纹饰（如果有）
        val originalTrim = getEquipmentTrim(equipment)

        // 获取新装备类型，以获取其默认属性和最大耐久度
        val newEquipmentType = EquipmentRegistry.getEquipmentType(newEquipmentId) ?: return null

        // 计算耐久度消耗，并应用到新装备上
        val durabilityDamage = (equipment.itemMeta as? org.bukkit.inventory.meta.Damageable)?.damage ?: 0

        // 获取原装备的附魔（直接从ItemMeta中获取）
        val enchantments = equipment.enchantments.toMap()

        // 创建新装备，使用新装备的默认属性，只增加熟练度、词条和附魔
        val newEquipment = EquipmentManager.createEquipment(
            equipmentId = newEquipmentId,
            proficiencyLevel = newProficiencyLevel,
            proficiency = tempProficiency,
            affixes = affixes,
            enchantments = enchantments,
            dodgeChance = newEquipmentType.defaultAttributes.dodgeChance,
            knockbackResistance = newEquipmentType.defaultAttributes.knockbackResistance,
            hungerPenalty = newEquipmentType.defaultAttributes.hungerPenalty,
            movementPenalty = newEquipmentType.defaultAttributes.movementPenalty,
            materialEffect = newEquipmentType.defaultAttributes.materialEffect
        )

        if (newEquipment == null) {
            return null
        }

        // 设置耐久度（保留原装备的耐久度消耗）
        if (durabilityDamage > 0) {
            val meta = newEquipment.itemMeta as? org.bukkit.inventory.meta.Damageable
            meta?.damage = durabilityDamage
            if (meta != null) {
                newEquipment.itemMeta = meta as org.bukkit.inventory.meta.ItemMeta
            }
        }

        // 确保附魔被正确应用到最终物品上
        enchantments.forEach { (enchant, level) ->
            newEquipment.addUnsafeEnchantment(enchant, level)
        }

        // 如果原装备有纹饰，应用到新装备上
        if (originalTrim != null) {
            applyTrim(newEquipment, originalTrim)
        }

        // 更新附魔显示为自定义描述
        updateDisplay(newEquipment, isDetailed)

        // 如果装备被改名，保留原名称
        if (customName != null) {
            val meta = newEquipment.itemMeta
            meta.displayName(customName)
            newEquipment.itemMeta = meta
        }

        // 对初始化型词条调用onSmithing方法（装备升级时isInit=true，因为这是新装备首次打上词条）
        affixes.forEach { (affixId, level) ->
            val modifier = com.sky4th.equipment.modifier.ModifierManager.instance.getModifier(affixId)
            if (modifier != null && modifier.isInitModifier()) {
                modifier.onSmithing(newEquipment, level, true)
            }
        }

        return newEquipment
    }

    /**
     * 获取装备的纹饰信息
     */
    fun getEquipmentTrim(equipment: ItemStack): Any? {
        val meta = equipment.itemMeta ?: return null
        return try {
            val trimField = meta.javaClass.getDeclaredField("trim")
            trimField.isAccessible = true
            trimField.get(meta)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 应用纹饰到装备
     */
    fun applyTrim(equipment: ItemStack, trim: Any) {
        val meta = equipment.itemMeta ?: return
        try {
            val trimField = meta.javaClass.getDeclaredField("trim")
            trimField.isAccessible = true
            trimField.set(meta, trim)
            equipment.itemMeta = meta
        } catch (e: Exception) {
            // 忽略错误
        }
    }

    /**
     * 获取物品当前的详细描述模式
     */
    fun isDetailedDescription(item: ItemStack): Boolean {
        val meta = item.itemMeta ?: return false
        val container = meta.persistentDataContainer
        val key = org.bukkit.NamespacedKey("sky_equipment", "detailed_lore")
        return container.getOrDefault(key, org.bukkit.persistence.PersistentDataType.BYTE, 0) == 1.toByte()
    }

    /**
     * 更新物品的显示
     */
    fun updateDisplay(item: ItemStack, isDetailed: Boolean) {
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

    /**
     * 获取装备的自定义显示名称
     */
    fun getCustomDisplayName(item: ItemStack): net.kyori.adventure.text.Component? {
        val meta = item.itemMeta ?: return null

        // 获取装备ID
        val equipmentId = NBTEquipmentDataManager.getEquipmentId(item) ?: return null

        // 获取装备类型
        val equipmentType = EquipmentRegistry.getEquipmentType(equipmentId) ?: return null

        // 获取默认显示名称并转换为Component
        val defaultNameComponent = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
            .deserialize(equipmentType.displayName)

        // 获取当前显示名称
        val currentName = meta.displayName() ?: return null
        // 将两个Component都转换为纯文本进行比较
        val plainSerializer = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
        val currentNameString = plainSerializer.serialize(currentName)
        val defaultNameString = plainSerializer.serialize(defaultNameComponent)

        // 如果当前名称与默认名称不同，说明被改过名
        return if (currentNameString != defaultNameString) currentName else null
    }
}
