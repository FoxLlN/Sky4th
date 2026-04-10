package com.sky4th.equipment.listener.smithing

import org.bukkit.inventory.ItemStack
import com.sky4th.equipment.data.NBTEquipmentDataManager
import com.sky4th.equipment.registry.EquipmentRegistry

/**
 * 装备升级处理器
 * 负责处理装备的同类升级（木制装备+原石→石质装备等）
 */
object EquipmentUpgradeHandler {

    /**
     * 处理装备升级
     * @param equipment 原装备
     * @param upgradePath 升级路径
     * @return 升级后的装备，如果升级失败则返回null
     */
    fun handle(equipment: ItemStack, upgradePath: SmithingManager.Companion.UpgradePath): ItemStack? {
        // 获取原装备ID
        val equipmentId = NBTEquipmentDataManager.getEquipmentId(equipment) ?: return null

        // 生成新装备ID（将源材料替换为目标材料）
        val newEquipmentId = generateNewEquipmentId(equipmentId, upgradePath) ?: return null

        // 检查新装备是否存在
        if (!EquipmentRegistry.hasEquipmentType(newEquipmentId)) {
            return null
        }

        // 使用共享的升级辅助类执行升级
        return EquipmentUpgradeHelper.upgradeEquipment(equipment, newEquipmentId)
    }

    /**
     * 生成新装备ID
     */
    private fun generateNewEquipmentId(equipmentId: String, upgradePath: SmithingManager.Companion.UpgradePath): String? {
        // 直接使用upgradePath中的sourceMaterial和targetMaterial
        return equipmentId.replaceFirst(upgradePath.sourceMaterial, upgradePath.targetMaterial)
    }
}
