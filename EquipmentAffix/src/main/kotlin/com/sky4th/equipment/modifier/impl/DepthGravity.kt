package com.sky4th.equipment.modifier.impl

import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import com.sky4th.equipment.util.AttributeModifierUtil
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 地心引力词条
 * 效果：挖掘速度随高度降低而提升
 * 从y=64开始，每下降20/10/10格，挖掘速度+1%/1%/2%（最高+20%）
 * 共3级，计算方式为SUM
 *
 * 注意：此词条使用PlayerMoveEvent监听玩家移动
 * 当Y坐标变化超过1格时更新挖掘速度加成
 */
class DepthGravity : com.sky4th.equipment.modifier.ConfiguredModifier("depth_gravity") {
    // 缓存每个玩家的当前Y坐标区间，用于判断是否需要更新
    private val playerYIntervalCache = ConcurrentHashMap<UUID, Int>()
    
    // 缓存每个玩家物品的加成值，用于避免重复更新
    // 使用物品的UUID作为key，避免使用hashCode可能导致的冲突
    private val itemBonusCache = ConcurrentHashMap<UUID, ConcurrentHashMap<UUID, Double>>()

    // 词条修饰符的命名空间键
    private val GRAVITY_MODIFIER_KEY = NamespacedKey("equipment_affix", "depth_gravity")

    // 基准高度
    private val BASE_Y = 64

    // 每级每20/10/10格的加成百分比
    private val BONUS_PER_INTERVAL_PER_LEVEL = listOf(0.01, 0.015, 0.02)  // 1%, 1.5%, 2%

    // 每级的间隔格数
    private val BLOCKS_PER_INTERVAL_PER_LEVEL = listOf(20, 15, 10)  // 20, 15, 10

    // 最大加成百分比
    private val MAX_BONUS = 0.20

    // 监听玩家移动事件和世界切换事件
    override fun getEventTypes(): List<Class<out Event>> =
        listOf(
            PlayerMoveEvent::class.java,
            org.bukkit.event.player.PlayerChangedWorldEvent::class.java
        )

    // 当词条进入活跃状态时，初始化地心引力效果
    override fun onInit(player: Player, item: ItemStack, level: Int) {
        // 立即计算一次加成
        updateSpeedBonus(player, item, level)
    }

    // 处理事件
    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: com.sky4th.equipment.modifier.config.PlayerRole
    ) {
        if (playerRole != com.sky4th.equipment.modifier.config.PlayerRole.SELF) return

        when (event) {
            is PlayerMoveEvent -> handlePlayerMove(event, player, item, level)
            is org.bukkit.event.player.PlayerChangedWorldEvent -> handleWorldChange(event, player, item, level)
        }
    }

    // 处理玩家移动事件
    private fun handlePlayerMove(
        event: PlayerMoveEvent,
        player: Player,
        item: ItemStack,
        level: Int
    ) {
        // 仅在主世界生效
        if (player.world.environment != org.bukkit.World.Environment.NORMAL) {
            // 如果不在主世界，清除加成
            removeGravityModifier(item)
            return
        }
        
        // 检查Y坐标是否变化超过1格
        val fromY = event.from.blockY
        val toY = event.to.blockY
        if (Math.abs(fromY - toY) < 1) {
            return
        }

        // 更新挖掘速度加成
        updateSpeedBonus(player, item, level)
    }

    // 处理世界切换事件
    private fun handleWorldChange(
        event: org.bukkit.event.player.PlayerChangedWorldEvent,
        player: Player,
        item: ItemStack,
        level: Int
    ) {
        // 仅在主世界生效
        if (player.world.environment != org.bukkit.World.Environment.NORMAL) {
            // 如果不在主世界，清除加成
            removeGravityModifier(item)
            return
        }

        // 切换到主世界时，重新计算加成
        updateSpeedBonus(player, item, level)
    }

    // 当词条被移除时，清除属性修饰符和缓存
    override fun onRemove(player: Player) {
        // 清除玩家的缓存
        itemBonusCache.remove(player.uniqueId)
        playerYIntervalCache.remove(player.uniqueId)
    }

    /**
     * 更新挖掘速度加成
     */
    private fun updateSpeedBonus(player: Player, item: ItemStack, level: Int) {
        // 获取物品的唯一标识（使用UUID作为key，避免hashCode冲突）
        val itemKey = getItemUUID(item) ?: return

        // 获取当前Y坐标
        val currentY = player.location.blockY

        // 确保level在有效范围内（1-3）
        val effectiveLevel = level.coerceIn(1, 3) - 1  // 转换为0-2的索引

        // 计算当前Y坐标所在的区间
        val blocksPerInterval = BLOCKS_PER_INTERVAL_PER_LEVEL[effectiveLevel]
        val currentInterval = if (currentY >= BASE_Y) 0 else (BASE_Y - currentY) / blocksPerInterval + 1

        // 获取玩家上次的区间
        val lastInterval = playerYIntervalCache.getOrDefault(player.uniqueId, -1)

        // 如果区间没有变化，直接返回
        if (currentInterval == lastInterval) {
            return
        }

        // 更新玩家的区间缓存
        playerYIntervalCache[player.uniqueId] = currentInterval

        // 计算挖掘速度加成
        val bonusPerInterval = BONUS_PER_INTERVAL_PER_LEVEL[effectiveLevel]
        val speedBonus = ((currentInterval - 1) * bonusPerInterval).coerceAtMost(MAX_BONUS).coerceAtLeast(0.0)

        // 获取玩家物品的加成缓存
        val playerCache = itemBonusCache.computeIfAbsent(player.uniqueId) { ConcurrentHashMap() }

        // 获取物品的上次加成值
        val lastBonus = playerCache[itemKey] ?: 0.0

        // 如果加成值没有变化，直接返回
        if (speedBonus == lastBonus) {
            return
        }

        // 更新缓存的加成值
        playerCache[itemKey] = speedBonus

        // 根据加成值更新或移除修饰符
        if (speedBonus > 0) {
            // 只有当加成大于0时才更新修饰符
            AttributeModifierUtil.updateItemAttribute(
                item,
                Attribute.PLAYER_BLOCK_BREAK_SPEED,
                GRAVITY_MODIFIER_KEY,
                speedBonus,
                AttributeModifier.Operation.ADD_NUMBER,
                org.bukkit.inventory.EquipmentSlotGroup.MAINHAND
            )
        } else {
            // 当加成小于等于0时，移除属性修饰符
            removeGravityModifier(item)
        }
    }

    /**
     * 获取物品的UUID
     * @param item 物品
     * @return 物品的UUID，如果不存在返回null
     */
    private fun getItemUUID(item: ItemStack): UUID? {
        val meta = item.itemMeta ?: return null
        // 使用Bukkit的PersistentDataContainer获取物品UUID
        val container = meta.persistentDataContainer
        val uuidKey = NamespacedKey("equipment_affix", "item_uuid")
        var uuid = container.get(uuidKey, org.bukkit.persistence.PersistentDataType.STRING)

        // 如果物品没有UUID，生成一个新的并保存
        if (uuid == null) {
            uuid = java.util.UUID.randomUUID().toString()
            container.set(uuidKey, org.bukkit.persistence.PersistentDataType.STRING, uuid)
            item.itemMeta = meta
        }

        return java.util.UUID.fromString(uuid)
    }

    /**
     * 移除地心引力修饰符
     */
    private fun removeGravityModifier(item: ItemStack) {
        AttributeModifierUtil.removeItemAttributeModifier(
            item,
            Attribute.PLAYER_BLOCK_BREAK_SPEED,
            GRAVITY_MODIFIER_KEY
        )
    }
}
