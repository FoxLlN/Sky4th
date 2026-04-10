
package com.sky4th.equipment.listener.smithing

import org.bukkit.inventory.ItemStack
import com.sky4th.equipment.data.NBTEquipmentDataManager

/**
 * 下界合金升级处理器
 * 负责将钻石装备升级为对应的装备系统的下界合金装备
 */
object NetheriteUpgradeHandler {

    /**
     * 处理下界合金升级
     * @param equipment 原装备
     * @return 升级后的装备，如果升级失败则返回null
     */
    fun handle(equipment: ItemStack): ItemStack? {
        // 检查是否为装备系统物品
        val equipmentId = NBTEquipmentDataManager.getEquipmentId(equipment) ?: return null

        // 查找对应的下界合金装备ID
        val netheriteEquipmentId = getNetheriteEquipmentId(equipmentId) ?: return null

        // 使用共享的升级辅助类执行升级
        return EquipmentUpgradeHelper.upgradeEquipment(equipment, netheriteEquipmentId)
    }

    /**
     * 将钻石装备ID转换为下界合金装备ID
     */
    private fun getNetheriteEquipmentId(equipmentId: String): String? {
        if (!equipmentId.contains("diamond")) {
            return null
        }
        return equipmentId.replace("diamond", "netherite")
    }
}
