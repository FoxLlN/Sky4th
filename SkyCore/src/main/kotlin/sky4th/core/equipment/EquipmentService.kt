package sky4th.core.equipment

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*

/**
 * 装备服务（内部单例）
 *
 * 职责：持有唯一 EquipmentProvider、注册/注销、所有调用统一经此委托并处理未注册时的默认值。
 * 不对外暴露；对外统一使用 sky4th.core.api.EquipmentAPI。
 */
object EquipmentService {
    private var provider: EquipmentProvider? = null

    fun registerProvider(p: EquipmentProvider) {
        provider = p
    }

    fun unregisterProvider() {
        provider = null
    }

    fun getProvider(): EquipmentProvider? = provider
    fun isRegistered(): Boolean = provider != null

    fun getItemAttributes(item: ItemStack): Map<String, Any> = provider?.getItemAttributes(item) ?: emptyMap()
    fun hasAffix(item: ItemStack, affixId: String): Boolean = provider?.hasAffix(item, affixId) ?: false
    fun getAffixes(item: ItemStack): List<String> = provider?.getAffixes(item) ?: emptyList()
    fun addAffix(item: ItemStack, affixId: String): ItemStack = provider?.addAffix(item, affixId) ?: item
    fun removeAffix(item: ItemStack, affixId: String): ItemStack = provider?.removeAffix(item, affixId) ?: item

    fun canUseEquipment(player: Player, item: ItemStack): Boolean = provider?.canUseEquipment(player, item) ?: true
    fun canCraftEquipment(player: Player, item: ItemStack): Boolean = provider?.canCraftEquipment(player, item) ?: true

    fun getRequiredLevel(item: ItemStack): Int = provider?.getRequiredLevel(item) ?: 0
    fun getRequiredProfessions(item: ItemStack): List<String> = provider?.getRequiredProfessions(item) ?: emptyList()
    fun meetsProfessionRequirement(player: Player, item: ItemStack): Boolean = provider?.meetsProfessionRequirement(player, item) ?: true

    fun getRequiredPermissions(item: ItemStack): List<String> = provider?.getRequiredPermissions(item) ?: emptyList()
    fun meetsPermissionRequirement(player: Player, item: ItemStack): Boolean = provider?.meetsPermissionRequirement(player, item) ?: true

    fun createEquipment(equipmentId: String): ItemStack? = provider?.createEquipment(equipmentId)
    fun createAffixTemplate(templateId: String): ItemStack? = provider?.createAffixTemplate(templateId)
}
