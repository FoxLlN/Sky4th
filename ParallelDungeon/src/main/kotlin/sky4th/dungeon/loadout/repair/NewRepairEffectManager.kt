
package sky4th.dungeon.loadout.repair

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import sky4th.dungeon.loadout.repair.animation.RepairAnimation
import sky4th.dungeon.loadout.repair.animation.RepairAnimationRegistry

/**
 * 新的维修效果管理器
 * 使用结构化的动画系统
 */
class NewRepairEffectManager(private val plugin: Plugin) {

    private val activeEffects = mutableMapOf<Player, RepairAnimation>()

    init {
        // 初始化动画注册表
        RepairAnimationRegistry.initialize()
    }

    /**
     * 开始显示维修特效
     * @param player 玩家
     * @param targetItem 目标物品（要维修的物品）
     * @param repairItem 维修物品
     * @param durationTicks 维修时长（tick）
     * @return 是否成功启动动画
     */
    fun startRepairEffect(player: Player, targetItem: ItemStack, repairItem: ItemStack, durationTicks: Int): Boolean {
        plugin.logger.info("[NewRepairEffectManager] 开始显示维修特效，玩家: ${player.name}")
        plugin.logger.info("[NewRepairEffectManager] 目标物品: ${targetItem.type}, 维修物品: ${repairItem.type}, 维修时长: $durationTicks tick")

        // 如果已有特效，先停止
        stopRepairEffect(player)

        // 创建动画实例，使用传入的维修时长
        val animation = RepairAnimationRegistry.createAnimationWithDuration(plugin, player, targetItem, repairItem, durationTicks)

        if (animation == null) {
            plugin.logger.warning("[NewRepairEffectManager] 无法创建维修动画，可能不支持的物品类型")
            return false
        }

        activeEffects[player] = animation
        animation.start()

        plugin.logger.info("[NewRepairEffectManager] 维修动画启动成功，时长: $durationTicks tick")
        return true
    }

    /**
     * 停止维修特效
     */
    fun stopRepairEffect(player: Player) {
        plugin.logger.info("[NewRepairEffectManager] 停止维修特效，玩家: ${player.name}")
        activeEffects.remove(player)?.stop()
    }

    /**
     * 检查是否可以维修指定物品
     */
    fun canRepair(repairItem: ItemStack, targetItem: ItemStack): Boolean {
        val targetType = RepairAnimationRegistry.determineTargetType(targetItem.type)
        return RepairAnimationRegistry.canRepair(repairItem.type, targetType)
    }

    /**
     * 获取维修时长（tick）
     */
    fun getRepairDuration(repairItem: ItemStack, targetItem: ItemStack): Int? {
        val config = RepairAnimationRegistry.getAnimationConfig(repairItem.type) ?: return null
        val targetType = RepairAnimationRegistry.determineTargetType(targetItem.type)

        return if (config.canRepair(targetType)) {
            config.durationTicks
        } else {
            null
        }
    }

    /**
     * 清理所有活动动画（插件禁用时调用）
     */
    fun cleanup() {
        plugin.logger.info("[NewRepairEffectManager] 清理所有活动动画")
        activeEffects.values.forEach { it.stop() }
        activeEffects.clear()
    }
}
