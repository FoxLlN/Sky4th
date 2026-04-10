package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.util.AttributeModifierUtil
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.persistence.PersistentDataType

/**
 * 砥柱词条
 * 效果：满耐久时挖掘速度+50%，耐久度每降低2%，挖掘速度降低1%
 */
class Pillar : com.sky4th.equipment.modifier.ConfiguredModifier("pillar") {

    companion object {
        // 词条修饰符的命名空间键
        private val DURABLE_MODIFIER_KEY = NamespacedKey("equipment_affix", "pillar")
        // 缓存加成值的键
        private val CACHED_BONUS_KEY = NamespacedKey("equipment_affix", "pillar_bonus")
    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(PlayerItemDamageEvent::class.java, PlayerItemHeldEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: com.sky4th.equipment.modifier.config.PlayerRole
    ) {
        if (playerRole != com.sky4th.equipment.modifier.config.PlayerRole.SELF) {
            return
        }
        // 处理物品手持事件
        if (event is PlayerItemHeldEvent) {
            handleItemHeld(event, player, item)
            return
        }

        // 处理物品耐久度变化事件
        if (event is PlayerItemDamageEvent) {
            handleItemDamage(event, player, item)
            return
        }
    }

    private fun handleItemHeld(event: PlayerItemHeldEvent, player: Player, item: ItemStack) {
        val meta = item.itemMeta ?: return
        val container = meta.persistentDataContainer

        // 检查是否已经初始化过（通过检查是否有缓存的加成值）
        if (container.has(CACHED_BONUS_KEY, PersistentDataType.DOUBLE)) {
            return
        }

        // 计算并更新挖掘速度加成
        updateSpeedBonus(item, meta, container)
    }

    private fun handleItemDamage(event: PlayerItemDamageEvent, player: Player, item: ItemStack) {
        val meta = item.itemMeta ?: return
        val container = meta.persistentDataContainer

        // 从持久数据容器中获取上次缓存的加成值
        val lastBonus = container.get(CACHED_BONUS_KEY, PersistentDataType.DOUBLE)

        // 计算并更新挖掘速度加成（传入上次缓存的值以避免不必要的NBT写入）
        updateSpeedBonus(item, meta, container, lastBonus)
    }

    private fun updateSpeedBonus(item: ItemStack, meta: org.bukkit.inventory.meta.ItemMeta, container: org.bukkit.persistence.PersistentDataContainer, cachedBonus: Double? = null) {
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

        // 计算挖掘速度加成
        // 满耐久时+50%，每损失2%耐久度减少1%
        val speedBonus = 0.5 - (damagePercent / 2.0) * 0.01

        // 如果提供了缓存的加成值且与当前值相同，直接返回，避免NBT写入
        if (cachedBonus != null && speedBonus == cachedBonus) {
            return
        }

        // 将新加成值存入容器
        container.set(CACHED_BONUS_KEY, PersistentDataType.DOUBLE, speedBonus)

        // 根据加成值更新或移除修饰符
        if (speedBonus > 0) {
            // 只有当加成大于0时才更新修饰符
            AttributeModifierUtil.updateItemAttribute(
                item,
                Attribute.PLAYER_BLOCK_BREAK_SPEED,
                DURABLE_MODIFIER_KEY,
                speedBonus,
                AttributeModifier.Operation.ADD_NUMBER,
                org.bukkit.inventory.EquipmentSlotGroup.MAINHAND
            )
        } else {
            // 当加成小于等于0时，移除属性修饰符
            AttributeModifierUtil.removeItemAttributeModifier(
                item,
                Attribute.PLAYER_BLOCK_BREAK_SPEED,
                DURABLE_MODIFIER_KEY
            )
        }
    }
}
