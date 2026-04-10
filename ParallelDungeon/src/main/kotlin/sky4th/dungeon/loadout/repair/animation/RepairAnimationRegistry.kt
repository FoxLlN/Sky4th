
package sky4th.dungeon.loadout.repair.animation

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import sky4th.dungeon.loadout.repair.animation.impl.GrindSwordAnimation

/**
 * 维修动画注册表
 * 管理所有维修动画配置和创建
 */
object RepairAnimationRegistry {
    private val animations = mutableMapOf<Material, RepairAnimationConfig>()

    /**
     * 初始化默认动画配置
     */
    fun initialize() {
        // 燧石磨剑动画
        registerAnimation(RepairAnimationConfig(
            repairItem = Material.FLINT,
            targetTypes = setOf(RepairTargetType.SWORD),
            durationTicks = 100,  // 5秒
            animationClass = GrindSwordAnimation::class.java
        ))

        // 磨刀石磨剑动画
        registerAnimation(RepairAnimationConfig(
            repairItem = Material.GRINDSTONE,
            targetTypes = setOf(RepairTargetType.SWORD),
            durationTicks = 80,  // 4秒
            animationClass = GrindSwordAnimation::class.java
        ))

        // 可以在这里添加更多维修物品配置...
    }

    /**
     * 注册维修动画配置
     */
    fun registerAnimation(config: RepairAnimationConfig) {
        animations[config.repairItem] = config
    }

    /**
     * 根据维修物品获取动画配置
     */
    fun getAnimationConfig(repairItem: Material): RepairAnimationConfig? {
        return animations[repairItem]
    }

    /**
     * 检查是否可以维修指定类型的物品
     */
    fun canRepair(repairItem: Material, targetType: RepairTargetType): Boolean {
        val config = animations[repairItem] ?: return false
        return config.canRepair(targetType)
    }

    /**
     * 创建维修动画实例
     */
    fun createAnimation(
        plugin: Plugin,
        player: Player,
        targetItem: ItemStack,
        repairItem: ItemStack
    ): RepairAnimation? {
        val config = getAnimationConfig(repairItem.type) ?: return null

        // 确定目标物品类型
        val targetType = determineTargetType(targetItem.type)
        if (!config.canRepair(targetType)) {
            return null
        }

        // 创建动画实例
        return try {
            val constructor = config.animationClass.getConstructor(
                Plugin::class.java,
                Player::class.java,
                ItemStack::class.java,
                ItemStack::class.java,
                RepairAnimationConfig::class.java
            )
            constructor.newInstance(plugin, player, targetItem, repairItem, config)
        } catch (e: Exception) {
            plugin.logger.severe("创建维修动画失败: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * 创建维修动画实例（使用指定的时长）
     */
    fun createAnimationWithDuration(
        plugin: Plugin,
        player: Player,
        targetItem: ItemStack,
        repairItem: ItemStack,
        durationTicks: Int
    ): RepairAnimation? {
        val baseConfig = getAnimationConfig(repairItem.type) ?: return null

        // 确定目标物品类型
        val targetType = determineTargetType(targetItem.type)
        if (!baseConfig.canRepair(targetType)) {
            return null
        }

        // 创建新的配置，使用传入的时长
        val config = RepairAnimationConfig(
            repairItem = baseConfig.repairItem,
            targetTypes = baseConfig.targetTypes,
            durationTicks = durationTicks,  // 使用传入的时长
            animationClass = baseConfig.animationClass
        )

        // 创建动画实例
        return try {
            val constructor = config.animationClass.getConstructor(
                Plugin::class.java,
                Player::class.java,
                ItemStack::class.java,
                ItemStack::class.java,
                RepairAnimationConfig::class.java
            )
            constructor.newInstance(plugin, player, targetItem, repairItem, config)
        } catch (e: Exception) {
            plugin.logger.severe("创建维修动画失败: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * 根据物品类型确定维修目标类型
     */
    fun determineTargetType(material: Material): RepairTargetType {
        return when {
            // 剑类
            material.name.endsWith("_SWORD") -> RepairTargetType.SWORD

            // 斧类
            material.name.endsWith("_AXE") -> RepairTargetType.AXE

            // 弓类
            material == Material.BOW -> RepairTargetType.BOW

            // 弩类
            material == Material.CROSSBOW -> RepairTargetType.CROSSBOW

            // 装备类
            material.name.endsWith("_HELMET") ||
            material.name.endsWith("_CHESTPLATE") ||
            material.name.endsWith("_LEGGINGS") ||
            material.name.endsWith("_BOOTS") -> RepairTargetType.ARMOR

            // 工具类
            material.name.endsWith("_PICKAXE") ||
            material.name.endsWith("_SHOVEL") ||
            material.name.endsWith("_HOE") -> RepairTargetType.TOOL

            // 默认为工具
            else -> RepairTargetType.TOOL
        }
    }

    /**
     * 获取所有已注册的维修物品
     */
    fun getAllRepairItems(): Set<Material> {
        return animations.keys
    }
}
