package sky4th.core.api

import sky4th.core.equipment.EquipmentProvider
import sky4th.core.equipment.EquipmentService
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * 装备系统对外 API（唯一入口）
 *
 * 所有调用统一委托给 EquipmentService，未注册时读操作返回默认值、写操作可抛异常。
 * 提供者由 EquipmentAffix 插件注册，其他插件通过本 API 使用装备系统。
 */
object EquipmentAPI {

    fun isAvailable(): Boolean = EquipmentService.isRegistered()
    fun getProvider(): EquipmentProvider? = EquipmentService.getProvider()
    fun registerProvider(provider: EquipmentProvider) = EquipmentService.registerProvider(provider)
    fun unregisterProvider() = EquipmentService.unregisterProvider()

    fun getItemAttributes(item: ItemStack): Map<String, Any> = EquipmentService.getItemAttributes(item)
    fun hasAffix(item: ItemStack, affixId: String): Boolean = EquipmentService.hasAffix(item, affixId)
    fun getAffixes(item: ItemStack): List<String> = EquipmentService.getAffixes(item)
    fun addAffix(item: ItemStack, affixId: String): ItemStack = EquipmentService.addAffix(item, affixId)
    fun removeAffix(item: ItemStack, affixId: String): ItemStack = EquipmentService.removeAffix(item, affixId)

    fun canUseEquipment(player: Player, item: ItemStack): Boolean = EquipmentService.canUseEquipment(player, item)
    fun canCraftEquipment(player: Player, item: ItemStack): Boolean = EquipmentService.canCraftEquipment(player, item)

    fun getRequiredLevel(item: ItemStack): Int = EquipmentService.getRequiredLevel(item)
    fun getRequiredProfessions(item: ItemStack): List<String> = EquipmentService.getRequiredProfessions(item)
    fun meetsProfessionRequirement(player: Player, item: ItemStack): Boolean = EquipmentService.meetsProfessionRequirement(player, item)

    fun getRequiredPermissions(item: ItemStack): List<String> = EquipmentService.getRequiredPermissions(item)
    fun meetsPermissionRequirement(player: Player, item: ItemStack): Boolean = EquipmentService.meetsPermissionRequirement(player, item)

    fun createEquipment(equipmentId: String): ItemStack? = EquipmentService.createEquipment(equipmentId)
    fun createAffixTemplate(templateId: String): ItemStack? = EquipmentService.createAffixTemplate(templateId)
}
