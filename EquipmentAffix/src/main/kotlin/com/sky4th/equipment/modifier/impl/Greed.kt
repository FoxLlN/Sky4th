package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable

/**
 * 贪婪词条
 * 效果：击杀生物时，消耗一定耐久度掉落物全部转化为金粒
 * 耐久消耗：等级1消耗20耐久，等级2消耗16耐久，等级3消耗12耐久
 * 限制条件：
 * 1. 耐久度不足时不转换
 * 2. 金粒和金锭不转换
 */
class Greed : com.sky4th.equipment.modifier.ConfiguredModifier("greed") {

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(EntityDeathEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 处理生物死亡事件，排除玩家死亡
        if (event !is EntityDeathEvent || playerRole != PlayerRole.ATTACKER || event.entity is Player) {
            return
        }

        // 获取物品的耐久度元数据
        val itemMeta = item.itemMeta as? Damageable ?: return
        val currentDurability = itemMeta.damage
        val maxDurability = item.type.maxDurability
        // 根据等级计算每个掉落物消耗的耐久度
        val durabilityCostPerItem = when (level) {
            1 -> 20
            2 -> 16
            3 -> 12
            else -> 20 // 默认值
        }

        // 计算剩余可用耐久度
        val remainingDurability = maxDurability - currentDurability

        // 如果没有足够的耐久度转换任何物品，直接返回
        if (remainingDurability < durabilityCostPerItem) {
            return
        }

        // 记录总消耗的耐久度和生成的金粒数量
        var totalDurabilityCost = 0
        var totalGoldNuggets = 0
        val itemsToRemove = mutableListOf<ItemStack>()

        // 直接循环event.drops，按原始顺序逐个检查转换
        for (drop in event.drops) {
            // 排除金粒和金锭
            if (drop.type == Material.GOLD_NUGGET || drop.type == Material.GOLD_INGOT) {
                continue
            }

            // 计算当前掉落物需要的耐久度（每个物品单位消耗一次耐久）
            val costForThisDrop = drop.amount * durabilityCostPerItem

            // 检查剩余耐久是否足够转换当前物品，同时确保至少保留1点耐久度
            if (totalDurabilityCost + costForThisDrop > remainingDurability - 1) {
                // 计算还可以转换多少个（确保至少保留1点耐久度）
                val remainingItemsCanConvert = ((remainingDurability - 1) - totalDurabilityCost) / durabilityCostPerItem
                if (remainingItemsCanConvert > 0) {
                    // 部分转换
                    totalGoldNuggets += remainingItemsCanConvert
                    totalDurabilityCost += remainingItemsCanConvert * durabilityCostPerItem

                    // 修改掉落物数量为剩余未转换的数量
                    drop.amount = drop.amount - remainingItemsCanConvert
                } else {
                }
                break
            }

            // 转换当前物品为金粒
            totalGoldNuggets += drop.amount
            totalDurabilityCost += costForThisDrop
            itemsToRemove.add(drop)
        }

        // 如果没有转换任何物品，直接返回
        if (itemsToRemove.isEmpty()) {
            return
        }

        // 使用迭代器安全移除
        val iterator = event.drops.iterator()
        var removedCount = 0
        while (iterator.hasNext()) {
            val drop = iterator.next()
            if (itemsToRemove.contains(drop)) {
                iterator.remove()
                removedCount++
            }
        }

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
            event.drops.add(goldNugget)
            remaining -= amount
        }
    }
}
