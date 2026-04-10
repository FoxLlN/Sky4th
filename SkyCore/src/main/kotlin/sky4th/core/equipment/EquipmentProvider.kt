package sky4th.core.equipment

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*

/**
 * 装备系统提供者接口（equipment 包唯一契约）
 *
 * 实现类负责管理装备系统，包括装备属性、词条等。
 * 注册与未注册时的默认行为由 EquipmentService 统一处理，对外通过 EquipmentAPI 使用。
 */
interface EquipmentProvider {

    /**
     * 获取物品的所有属性
     * @param item 物品
     * @return 属性映射（属性名 -> 属性值）
     */
    fun getItemAttributes(item: ItemStack): Map<String, Any>

    /**
     * 检查物品是否有指定词条
     * @param item 物品
     * @param affixId 词条ID
     * @return 是否有该词条
     */
    fun hasAffix(item: ItemStack, affixId: String): Boolean

    /**
     * 获取物品的所有词条
     * @param item 物品
     * @return 词条ID列表
     */
    fun getAffixes(item: ItemStack): List<String>

    /**
     * 给物品添加词条
     * @param item 物品
     * @param affixId 词条ID
     * @return 添加后的物品（可能创建新的物品实例）
     */
    fun addAffix(item: ItemStack, affixId: String): ItemStack

    /**
     * 从物品移除词条
     * @param item 物品
     * @param affixId 词条ID
     * @return 移除后的物品（可能创建新的物品实例）
     */
    fun removeAffix(item: ItemStack, affixId: String): ItemStack

    /**
     * 检查玩家是否有权限使用指定装备
     * @param player 玩家
     * @param item 装备物品
     * @return 是否有权限使用
     */
    fun canUseEquipment(player: Player, item: ItemStack): Boolean

    /**
     * 检查玩家是否有权限合成指定装备
     * @param player 玩家
     * @param item 装备物品
     * @return 是否有权限合成
     */
    fun canCraftEquipment(player: Player, item: ItemStack): Boolean

    /**
     * 获取装备的等级要求
     * @param item 装备物品
     * @return 等级要求（如果没有等级要求返回0）
     */
    fun getRequiredLevel(item: ItemStack): Int

    /**
     * 获取装备的职业要求
     * @param item 装备物品
     * @return 职业要求列表（如果没有职业要求返回空列表）
     */
    fun getRequiredProfessions(item: ItemStack): List<String>

    /**
     * 检查玩家是否满足装备的职业要求
     * @param player 玩家
     * @param item 装备物品
     * @return 是否满足职业要求
     */
    fun meetsProfessionRequirement(player: Player, item: ItemStack): Boolean

    /**
     * 获取装备的权限要求
     * @param item 装备物品
     * @return 权限要求列表（如果没有权限要求返回空列表）
     */
    fun getRequiredPermissions(item: ItemStack): List<String>

    /**
     * 检查玩家是否满足装备的权限要求
     * @param player 玩家
     * @param item 装备物品
     * @return 是否满足权限要求
     */
    fun meetsPermissionRequirement(player: Player, item: ItemStack): Boolean

    /**
     * 创建装备
     * @param equipmentId 装备ID
     * @return 装备物品，如果装备不存在则返回null
     */
    fun createEquipment(equipmentId: String): ItemStack?

    /**
     * 创建词条锻造模板
     * @param templateId 模板ID
     * @return 模板物品，如果不存在则返回null
     */
    fun createAffixTemplate(templateId: String): ItemStack?
}
