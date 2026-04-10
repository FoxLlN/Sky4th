package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable

/**
 * 点金词条
 * 效果：破坏方块时，消耗10点耐久度将掉落物转换为金粒
 * 耐久消耗：每个掉落物转换消耗10点耐久
 * 限制条件：
 * 1. 耐久度不足时部分转换
 * 2. 金粒和金锭不转换
 */
class GoldTouch : com.sky4th.equipment.modifier.ConfiguredModifier("gold_touch") {

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(BlockDropItemEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 处理方块掉落物事件
        if (event !is BlockDropItemEvent || playerRole != PlayerRole.SELF) {
            return
        }

        // 获取物品的耐久度元数据
        val itemMeta = item.itemMeta as? Damageable ?: return
        val currentDurability = itemMeta.damage
        val maxDurability = item.type.maxDurability

        // 每个掉落物转换消耗的耐久度
        val durabilityCostPerItem = 10

        // 计算剩余可用耐久度
        val remainingDurability = maxDurability - currentDurability

        // 如果没有足够的耐久度转换任何物品，直接返回
        if (remainingDurability < durabilityCostPerItem) {
            return
        }

        // 记录总消耗的耐久度和生成的金粒数量
        var totalDurabilityCost = 0
        var totalGoldNuggets = 0
        val itemsToRemove = mutableListOf<org.bukkit.entity.Item>()

        // 直接循环event.items，按原始顺序逐个检查转换
        for (drop in event.items) {
            val dropItem = drop.itemStack
            // 排除金粒和金锭
            if (dropItem.type == Material.GOLD_NUGGET || dropItem.type == Material.GOLD_INGOT) {
                continue
            }

            // 计算当前掉落物需要的耐久度（每个物品单位消耗一次耐久）
            val costForThisDrop = dropItem.amount * durabilityCostPerItem

            // 检查剩余耐久是否足够转换当前物品，同时确保至少保留1点耐久度
            if (totalDurabilityCost + costForThisDrop > remainingDurability - 1) {
                // 计算还可以转换多少个（确保至少保留1点耐久度）
                val remainingItemsCanConvert = ((remainingDurability - 1) - totalDurabilityCost) / durabilityCostPerItem
                if (remainingItemsCanConvert > 0) {
                    // 部分转换：将掉落物添加到移除列表
                    itemsToRemove.add(drop)
                    
                    // 记录转换的金粒数量和耐久消耗
                    totalGoldNuggets += remainingItemsCanConvert
                    totalDurabilityCost += remainingItemsCanConvert * durabilityCostPerItem

                    // 如果还有未转换的物品，创建新的掉落物实体
                    val unconvertedAmount = dropItem.amount - remainingItemsCanConvert
                    if (unconvertedAmount > 0) {
                        val newItemStack = ItemStack(dropItem.type, unconvertedAmount)
                        event.block.location.world?.dropItemNaturally(event.block.location, newItemStack)
                    }
                }
                break
            }

            // 转换当前物品为金粒
            totalGoldNuggets += dropItem.amount
            totalDurabilityCost += costForThisDrop
            itemsToRemove.add(drop)
        }

        // 如果没有转换任何物品，直接返回
        if (itemsToRemove.isEmpty()) {
            return
        }

        // 移除原有的掉落物
        event.items.removeAll(itemsToRemove)

        // 消耗耐久度
        val newDamage = currentDurability + totalDurabilityCost

        // 重新获取物品的ItemMeta以确保获取最新的引用
        val updatedItemMeta = item.itemMeta as? Damageable ?: return
        updatedItemMeta.damage = newDamage
        item.itemMeta = updatedItemMeta

        // 生成金粒（每次最多生成64个）
        var remaining = totalGoldNuggets
        while (remaining > 0) {
            val amount = minOf(remaining, 64)
            val goldNugget = ItemStack(Material.GOLD_NUGGET, amount)
            event.block.location.world?.dropItemNaturally(event.block.location, goldNugget)
            remaining -= amount
        }
    }
}
