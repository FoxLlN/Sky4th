
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.util.AttributeModifierUtil
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/**
 * 财阀词条
 * 颜色: #0EC754
 * 效果：物品栏的绿宝石越多，挖掘速度越快
 * 每16/12/8个绿宝石增加1%速度，最多增加8%/16%/20%
 */
class Plutocrat : com.sky4th.equipment.modifier.ConfiguredModifier("plutocrat") {

    companion object {
        // 词条修饰符的命名空间键
        private val PLUTOCRAT_MODIFIER_KEY = NamespacedKey("equipment_affix", "plutocrat")
        // 缓存加成值的键
        private val CACHED_BONUS_KEY = NamespacedKey("equipment_affix", "plutocrat_bonus")
        // 每级配置：(每1%所需的绿宝石数量, 最大增伤)
        private val CONFIG = arrayOf(
            16 to 8,       // 1级: 16个, 8%
            12 to 16,      // 2级: 12个, 16%
            8 to 20        // 3级: 8个, 20%
        )
    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(
            InventoryCloseEvent::class.java,
            EntityPickupItemEvent::class.java
        )

    override fun onInit(player: Player, item: ItemStack, level: Int) {
        // 在词条进入活跃状态时，初始化检测物品栏的绿宝石数量
        updatePlutocratBonus(player, item, level)
    }

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: com.sky4th.equipment.modifier.config.PlayerRole
    ) {
        // 只处理玩家自身
        if (playerRole != com.sky4th.equipment.modifier.config.PlayerRole.SELF) {
            return
        }
        // 如果是捡起物品事件，延迟1tick处理，确保物品已经进入背包
        if (event is EntityPickupItemEvent) {
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                com.sky4th.equipment.EquipmentAffix.instance,
                Runnable {
                    updatePlutocratBonus(player, item, level)
                },
                1L
            )
            return
        }

        // 直接更新加成
        updatePlutocratBonus(player, item, level)
    }

    /**
     * 更新财阀词条的加成
     */
    private fun updatePlutocratBonus(player: Player, item: ItemStack, level: Int) {
        // 计算玩家物品栏中的绿宝石数量
        val emeraldCount = countEmeralds(player.inventory)

        // 获取当前等级的配置
        val (emeraldsPerPercent, maxBonusPercent) = CONFIG.getOrNull(level - 1) ?: return

        // 计算挖掘速度加成（按整数倍计算）
        val bonusPercent = (emeraldCount / emeraldsPerPercent) * 1  // 每1%是一个单位
        val cappedBonusPercent = minOf(bonusPercent, maxBonusPercent)

        // 转换为小数
        val cappedSpeedBonus = cappedBonusPercent / 100.0

        // 获取物品的元数据
        val meta = item.itemMeta ?: return

        // 从持久数据容器中获取上次缓存的加成值
        val container = meta.persistentDataContainer
        val lastBonus = container.get(CACHED_BONUS_KEY, PersistentDataType.DOUBLE) ?: Double.NEGATIVE_INFINITY

        // 如果加成值与上次相同，直接返回，避免NBT写入
        if (cappedSpeedBonus == lastBonus) {
            return
        }

        // 将新加成值存入容器
        container.set(CACHED_BONUS_KEY, PersistentDataType.DOUBLE, cappedSpeedBonus)
        item.itemMeta = meta

        // 根据加成值更新或移除修饰符
        if (cappedSpeedBonus > 0) {
            // 只有当加成大于0时才更新修饰符
            AttributeModifierUtil.updateItemAttribute(
                item,
                Attribute.PLAYER_BLOCK_BREAK_SPEED,
                PLUTOCRAT_MODIFIER_KEY,
                cappedSpeedBonus,
                AttributeModifier.Operation.ADD_NUMBER,
                org.bukkit.inventory.EquipmentSlotGroup.MAINHAND
            )
        } else {
            // 当加成小于等于0时，移除属性修饰符
            AttributeModifierUtil.removeItemAttributeModifier(
                item,
                Attribute.PLAYER_BLOCK_BREAK_SPEED,
                PLUTOCRAT_MODIFIER_KEY
            )
        }
    }

    /**
     * 计算玩家物品栏（快捷栏）中的绿宝石数量
     * 只计算最下面9格，不包括背包里的其他格子
     */
    private fun countEmeralds(inventory: org.bukkit.inventory.Inventory): Int {
        var count = 0
        // 只遍历快捷栏（索引0-8）
        for (i in 0..8) {
            val item = inventory.getItem(i)
            if (item != null && item.type == Material.EMERALD) {
                count += item.amount
            }
        }
        return count
    }
}
