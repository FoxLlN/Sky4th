package com.sky4th.equipment

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import com.sky4th.equipment.data.NBTEquipmentDataManager
import sky4th.core.equipment.EquipmentProvider as CoreEquipmentProvider

/**
 * Component工具类
 */
private object ComponentUtils {
    private val serializer = LegacyComponentSerializer.legacySection()

    fun toString(component: Component): String {
        return serializer.serialize(component)
    }

    fun toComponent(text: String): Component {
        return serializer.deserialize(text)
    }
}

/**
 * 装备系统提供者实现
 *
 * 负责管理装备属性、词条、权限检查等功能
 * 使用NBT标签存储数据
 */
class EquipmentAffixProvider(private val plugin: EquipmentAffix) : CoreEquipmentProvider {

    companion object {
        // 装备属性键
        private const val REQUIRED_LEVEL_KEY = "required_level"
        private const val REQUIRED_PROFESSIONS_KEY = "required_professions"
        private const val REQUIRED_PERMISSIONS_KEY = "required_permissions"

        // NBT键
        private val KEY_REQUIRED_LEVEL = org.bukkit.NamespacedKey("sky_equipment", "required_level")
        private val KEY_REQUIRED_PROFESSIONS = org.bukkit.NamespacedKey("sky_equipment", "required_professions")
        private val KEY_REQUIRED_PERMISSIONS = org.bukkit.NamespacedKey("sky_equipment", "required_permissions")
    }

    override fun getItemAttributes(item: ItemStack): Map<String, Any> {
        val attributes = mutableMapOf<String, Any>()

        // 获取等级要求
        val requiredLevel = getRequiredLevel(item)
        if (requiredLevel > 0) {
            attributes[REQUIRED_LEVEL_KEY] = requiredLevel
        }

        // 获取职业要求
        val requiredProfessions = getRequiredProfessions(item)
        if (requiredProfessions.isNotEmpty()) {
            attributes[REQUIRED_PROFESSIONS_KEY] = requiredProfessions
        }

        // 获取权限要求
        val requiredPermissions = getRequiredPermissions(item)
        if (requiredPermissions.isNotEmpty()) {
            attributes[REQUIRED_PERMISSIONS_KEY] = requiredPermissions
        }

        // 获取所有词条
        val affixes = NBTEquipmentDataManager.getAffixes(item)
        if (affixes.isNotEmpty()) {
            attributes["affixes"] = affixes.keys.toList()
        }

        return attributes
    }

    override fun hasAffix(item: ItemStack, affixId: String): Boolean {
        return NBTEquipmentDataManager.getAffixes(item).containsKey(affixId)
    }

    override fun getAffixes(item: ItemStack): List<String> {
        return NBTEquipmentDataManager.getAffixes(item).keys.toList()
    }

    override fun addAffix(item: ItemStack, affixId: String): ItemStack {
        // 检查是否已有该词条
        if (hasAffix(item, affixId)) {
            return item
        }

        // 使用NBTEquipmentDataManager添加词条
        NBTEquipmentDataManager.setAffix(item, affixId, 1)
        // 清除缓存
        com.sky4th.equipment.cache.EquipmentAttributesCache.invalidate(item)
        return item
    }

    override fun removeAffix(item: ItemStack, affixId: String): ItemStack {
        // 使用NBTEquipmentDataManager移除词条
        NBTEquipmentDataManager.removeAffix(item, affixId)
        // 清除缓存
        com.sky4th.equipment.cache.EquipmentAttributesCache.invalidate(item)
        return item
    }

    override fun canUseEquipment(player: Player, item: ItemStack): Boolean {
        // 检查权限要求
        if (!meetsPermissionRequirement(player, item)) {
            return false
        }

        // 检查职业要求
        if (!meetsProfessionRequirement(player, item)) {
            return false
        }

        // 检查等级要求
        val requiredLevel = getRequiredLevel(item)
        if (requiredLevel > 0 && player.level < requiredLevel) {
            return false
        }

        return true
    }

    override fun canCraftEquipment(player: Player, item: ItemStack): Boolean {
        // 合成权限与使用权限相同
        return canUseEquipment(player, item)
    }

    override fun getRequiredLevel(item: ItemStack): Int {
        val meta = item.itemMeta ?: return 0
        val container = meta.persistentDataContainer
        return container.getOrDefault(KEY_REQUIRED_LEVEL, org.bukkit.persistence.PersistentDataType.INTEGER, 0)
    }

    fun setRequiredLevel(item: ItemStack, level: Int) {
        val meta = item.itemMeta ?: return
        val container = meta.persistentDataContainer
        if (level > 0) {
            container.set(KEY_REQUIRED_LEVEL, org.bukkit.persistence.PersistentDataType.INTEGER, level)
        } else {
            container.remove(KEY_REQUIRED_LEVEL)
        }
        item.itemMeta = meta
    }

    override fun getRequiredProfessions(item: ItemStack): List<String> {
        val meta = item.itemMeta ?: return emptyList()
        val container = meta.persistentDataContainer
        val professionsString = container.get(KEY_REQUIRED_PROFESSIONS, org.bukkit.persistence.PersistentDataType.STRING)
        return professionsString?.split(",")?.map { it.trim() } ?: emptyList()
    }

    fun setRequiredProfessions(item: ItemStack, professions: List<String>) {
        val meta = item.itemMeta ?: return
        val container = meta.persistentDataContainer
        if (professions.isNotEmpty()) {
            container.set(KEY_REQUIRED_PROFESSIONS, org.bukkit.persistence.PersistentDataType.STRING, professions.joinToString(","))
        } else {
            container.remove(KEY_REQUIRED_PROFESSIONS)
        }
        item.itemMeta = meta
    }

    override fun meetsProfessionRequirement(player: Player, item: ItemStack): Boolean {
        val requiredProfessions = getRequiredProfessions(item)
        if (requiredProfessions.isEmpty()) {
            return true
        }

        // 检查玩家是否有任一职业权限
        for (profession in requiredProfessions) {
            if (player.hasPermission("profession.$profession")) {
                return true
            }
        }

        return false
    }

    override fun getRequiredPermissions(item: ItemStack): List<String> {
        val meta = item.itemMeta ?: return emptyList()
        val container = meta.persistentDataContainer
        val permissionsString = container.get(KEY_REQUIRED_PERMISSIONS, org.bukkit.persistence.PersistentDataType.STRING)
        return permissionsString?.split(",")?.map { it.trim() } ?: emptyList()
    }

    fun setRequiredPermissions(item: ItemStack, permissions: List<String>) {
        val meta = item.itemMeta ?: return
        val container = meta.persistentDataContainer
        if (permissions.isNotEmpty()) {
            container.set(KEY_REQUIRED_PERMISSIONS, org.bukkit.persistence.PersistentDataType.STRING, permissions.joinToString(","))
        } else {
            container.remove(KEY_REQUIRED_PERMISSIONS)
        }
        item.itemMeta = meta
    }

    override fun meetsPermissionRequirement(player: Player, item: ItemStack): Boolean {
        val requiredPermissions = getRequiredPermissions(item)
        if (requiredPermissions.isEmpty()) {
            return true
        }

        // 检查玩家是否有所有权限
        for (permission in requiredPermissions) {
            if (!player.hasPermission(permission)) {
                return false
            }
        }

        return true
    }

    /**
     * 创建装备
     * @param equipmentId 装备ID
     * @return 装备物品，如果装备不存在则返回null
     */
    override fun createEquipment(equipmentId: String): ItemStack? {
        return com.sky4th.equipment.manager.EquipmentManager.createEquipment(equipmentId)
    }

    /**
     * 获取词条锻造模板
     * @param templateId 模板ID
     * @return 模板物品，如果不存在则返回null
     */
    override fun createAffixTemplate(templateId: String): ItemStack? {
        return com.sky4th.equipment.loader.AffixTemplateLoader.getAffixTemplate(templateId)
    }
}
