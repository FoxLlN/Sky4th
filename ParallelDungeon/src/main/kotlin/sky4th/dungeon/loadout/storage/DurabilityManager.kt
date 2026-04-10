package sky4th.dungeon.loadout.storage

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.plugin.java.JavaPlugin
import sky4th.core.model.StorageEntry
import sky4th.dungeon.config.ConfigManager

/**
 * 耐久度管理器：统一处理装备和套装的耐久度逻辑
 */
object DurabilityManager {

    /**
     * 装备部位类型
     */
    enum class EquipmentSlot(val displayName: String) {
        HELMET("头盔"),
        CHESTPLATE("胸甲"),
        LEGGINGS("腿甲"),
        BOOTS("靴子");

        companion object {
            /**
             * 根据材质获取装备部位
             */
            fun fromMaterial(material: Material): EquipmentSlot? {
                return when (material) {
                    Material.LEATHER_HELMET, Material.CHAINMAIL_HELMET, Material.IRON_HELMET,
                    Material.DIAMOND_HELMET, Material.GOLDEN_HELMET, Material.NETHERITE_HELMET -> HELMET
                    Material.LEATHER_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE, Material.IRON_CHESTPLATE,
                    Material.DIAMOND_CHESTPLATE, Material.GOLDEN_CHESTPLATE, Material.NETHERITE_CHESTPLATE -> CHESTPLATE
                    Material.LEATHER_LEGGINGS, Material.CHAINMAIL_LEGGINGS, Material.IRON_LEGGINGS,
                    Material.DIAMOND_LEGGINGS, Material.GOLDEN_LEGGINGS, Material.NETHERITE_LEGGINGS -> LEGGINGS
                    Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS,
                    Material.DIAMOND_BOOTS, Material.GOLDEN_BOOTS, Material.NETHERITE_BOOTS -> BOOTS
                    else -> null
                }
            }
        }
    }

    /**
     * 套装耐久度信息
     */
    data class SetDurabilityInfo(
        val slotDurabilities: Map<EquipmentSlot, Pair<Int, Int>>
    ) {
        /**
         * 计算总耐久度比例（0-100）
         */
        fun calculateOverallRatio(): Int {
            if (slotDurabilities.isEmpty()) return 100
            val totalCurrent = slotDurabilities.values.sumOf { it.first }
            val totalMax = slotDurabilities.values.sumOf { it.second }
            return if (totalMax == 0) 100 else ((totalCurrent.toDouble() / totalMax.toDouble()) * 100).toInt()
        }

        /**
         * 转换为字符串格式：部位:当前/最大,部位:当前/最大
         */
        fun toStringFormat(): String {
            return slotDurabilities.entries
                .sortedBy { it.key.displayName }
                .joinToString(",") { entry ->
                    val slot = entry.key
                    val (current, max) = entry.value
                    "${slot.displayName}:$current/$max"
                }
        }

        companion object {
            /**
             * 从字符串解析耐久度信息
             */
            fun fromString(durabilityInfo: String): SetDurabilityInfo? {
                val slotDurabilities = mutableMapOf<EquipmentSlot, Pair<Int, Int>>()
                durabilityInfo.split(",").forEach { part ->
                    try {
                        val (slotName, values) = part.split(":")
                        val (current, max) = values.split("/")
                        val slot = EquipmentSlot.entries.find { it.displayName == slotName }
                        if (slot != null) {
                            slotDurabilities[slot] = current.toInt() to max.toInt()
                        }
                    } catch (e: Exception) {
                        // 忽略解析失败的部分
                    }
                }
                return if (slotDurabilities.isNotEmpty()) {
                    SetDurabilityInfo(slotDurabilities)
                } else {
                    null
                }
            }

            /**
             * 创建满耐久的套装信息
             */
            fun createFullDurability(materials: List<Material>): SetDurabilityInfo {
                val slotDurabilities = mutableMapOf<EquipmentSlot, Pair<Int, Int>>()
                materials.forEach { material ->
                    val slot = EquipmentSlot.fromMaterial(material)
                    if (slot != null && material.maxDurability > 0) {
                        val maxDur = material.maxDurability.toInt()
                        slotDurabilities[slot] = maxDur to maxDur
                    }
                }
                return SetDurabilityInfo(slotDurabilities)
            }
        }
    }

    /**
     * 应用耐久度到物品
     * @param item 物品
     * @param durability 耐久度值（单件）或耐久度比例（套装，0-100）
     * @param isSet 是否是套装
     */
    fun applyDurability(item: ItemStack, durability: Int?, isSet: Boolean = false) {
        if (durability == null || item.type.maxDurability <= 0) return
        val maxD = item.type.maxDurability.toInt()

        val displayDurability = if (isSet) {
            // 套装：durability是比例（0-100），按比例计算显示耐久度
            maxD * durability / 100
        } else {
            // 单件：直接使用耐久度值
            durability
        }

        item.editMeta { meta ->
            (meta as? Damageable)?.setDamage((maxD - displayDurability).coerceIn(0, maxD))
        }
    }

    /**
     * 应用套装耐久度到物品列表
     * @param items 物品列表
     * @param durabilityInfo 耐久度信息字符串
     * @return 返回实际应用的物品列表（跳过耐久度为0的部位）
     */
    fun applySetDurability(items: List<ItemStack>, durabilityInfo: String, plugin: JavaPlugin): List<ItemStack> {
        val info = SetDurabilityInfo.fromString(durabilityInfo) ?: return items

        val result = mutableListOf<ItemStack>()
        items.forEach { item ->
            val slot = EquipmentSlot.fromMaterial(item.type)
            if (slot != null && info.slotDurabilities.containsKey(slot)) {
                val (current, max) = info.slotDurabilities[slot]!!
                // 跳过耐久度为0的部位
                if (current <= 0) {
                    plugin.logger.info("[DurabilityManager] 跳过耐久度为0的部位: ${slot.displayName}")
                    return@forEach
                }
                if (item.type.maxDurability > 0) {
                    val maxD = item.type.maxDurability.toInt()
                    val displayDurability = current.coerceIn(0, maxD)
                    item.editMeta { meta ->
                        (meta as? Damageable)?.setDamage((maxD - displayDurability).coerceIn(0, maxD))
                    }
                }
            }
            result.add(item)
        }
        return result
    }

    /**
     * 从物品获取耐久度
     * @param item 物品
     * @param isSet 是否是套装
     * @return 耐久度值（单件）或耐久度比例（套装，0-100），如果物品没有耐久度则返回null
     */
    fun getDurability(item: ItemStack, isSet: Boolean = false): Int? {
        if (item.type.maxDurability <= 0) return null
        val meta = item.itemMeta as? Damageable ?: return null
        val maxD = item.type.maxDurability.toInt()
        val current = maxD - meta.damage

        return if (isSet) {
            // 套装：返回比例（0-100）
            ((current.toDouble() / maxD.toDouble()) * 100).toInt()
        } else {
            // 单件：返回实际耐久度值
            current
        }
    }

    /**
     * 生成套装的唯一标识符，包含loadoutId和各部位耐久度信息
     * 用于区分不同耐久度的同种套装
     * @param loadoutId 套装ID
     * @param durabilityInfo 耐久度信息
     * @return 唯一标识符
     */
    fun generateSetUniqueId(loadoutId: String, durabilityInfo: String): String {
        val info = SetDurabilityInfo.fromString(durabilityInfo)
        val ratio = info?.calculateOverallRatio() ?: 100
        return "${loadoutId}_dur_${ratio}"
    }

    /**
     * 从StorageEntry获取套装耐久度信息
     * @param entry 仓库条目
     * @return 套装耐久度信息，如果不是套装或没有耐久度信息则返回null
     */
    fun getSetDurabilityInfo(entry: StorageEntry): SetDurabilityInfo? {
        if (entry.itemData.isNullOrBlank()) return null
        return SetDurabilityInfo.fromString(entry.itemData!!)
    }

    /**
     * 从ItemStack的PersistentDataContainer获取套装耐久度信息
     * @param item 物品堆
     * @param plugin 插件实例
     * @return 套装耐久度信息，如果没有则返回null
     */
    fun getSetDurabilityFromPDC(item: ItemStack, plugin: JavaPlugin): SetDurabilityInfo? {
        val meta = item.itemMeta ?: return null
        val pdc = meta.persistentDataContainer
        val durabilityData = pdc.get(
            org.bukkit.NamespacedKey(plugin, "dungeon_set_durability"),
            org.bukkit.persistence.PersistentDataType.STRING
        )
        return if (!durabilityData.isNullOrBlank()) {
            SetDurabilityInfo.fromString(durabilityData)
        } else {
            null
        }
    }

    /**
     * 将套装耐久度信息存储到ItemStack的PersistentDataContainer
     * @param item 物品堆
     * @param durabilityInfo 耐久度信息
     * @param plugin 插件实例
     */
    fun setSetDurabilityToPDC(item: ItemStack, durabilityInfo: SetDurabilityInfo, plugin: JavaPlugin) {
        item.editMeta { meta ->
            val pdc = meta.persistentDataContainer
            val durabilityKey = org.bukkit.NamespacedKey(plugin, "dungeon_set_durability")
            pdc.set(durabilityKey, org.bukkit.persistence.PersistentDataType.STRING, durabilityInfo.toStringFormat())
        }
    }
}
