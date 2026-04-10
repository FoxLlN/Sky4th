
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.util.AttributeModifierUtil
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.persistence.PersistentDataType

/**
 * 绝境词条
 * 效果：耐久度越低，攻击速度越快，耐久度每降2%，攻击速度提升1%（最多提升50%）
 */
class Desperate : com.sky4th.equipment.modifier.ConfiguredModifier("desperate") {

    companion object {
        // 词条修饰符的命名空间键
        private val DESPERATE_MODIFIER_KEY = NamespacedKey("equipment_affix", "desperate")
        // 缓存加成值的键
        private val CACHED_BONUS_KEY = NamespacedKey("equipment_affix", "desperate_bonus")
    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(PlayerItemDamageEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: com.sky4th.equipment.modifier.config.PlayerRole
    ) {
        // 只处理物品耐久度变化事件
        if (event !is PlayerItemDamageEvent) {
            return
        }

        if (playerRole != com.sky4th.equipment.modifier.config.PlayerRole.SELF) {
            return
        }

        val meta = item.itemMeta ?: return
        val damageable = meta as? Damageable ?: return

        // 获取物品最大耐久度
        val maxDurability = item.type.maxDurability
        if (maxDurability <= 0) {
            return
        }

        // 获取当前耐久度损失
        val currentDamage = damageable.damage

        // 计算耐久度损失百分比
        val damagePercent = (currentDamage.toDouble() / maxDurability) * 100

        // 计算攻击速度加成
        // 耐久度每降低2%，攻击速度提升1%，最多提升50%
        val speedBonus = (damagePercent / 2.0) * 0.01
        val cappedSpeedBonus = minOf(speedBonus, 0.5)

        // 从持久数据容器中获取上次缓存的加成值
        val container = meta.persistentDataContainer
        val lastBonus = container.get(CACHED_BONUS_KEY, PersistentDataType.DOUBLE) ?: Double.NEGATIVE_INFINITY

        // 如果加成值与上次相同，直接返回，避免NBT写入
        if (cappedSpeedBonus == lastBonus) {
            return
        }

        // 将新加成值存入容器
        container.set(CACHED_BONUS_KEY, PersistentDataType.DOUBLE, cappedSpeedBonus)

        // 根据加成值更新或移除修饰符
        if (cappedSpeedBonus > 0) {
            // 只有当加成大于0时才更新修饰符
            AttributeModifierUtil.updateItemAttribute(
                item,
                Attribute.GENERIC_ATTACK_SPEED,
                DESPERATE_MODIFIER_KEY,
                cappedSpeedBonus,
                AttributeModifier.Operation.ADD_SCALAR,
                org.bukkit.inventory.EquipmentSlotGroup.MAINHAND
            )
        } else {
            // 当加成小于等于0时，移除属性修饰符
            AttributeModifierUtil.removeItemAttributeModifier(
                item,
                Attribute.GENERIC_ATTACK_SPEED,
                DESPERATE_MODIFIER_KEY
            )
        }
    }
}
