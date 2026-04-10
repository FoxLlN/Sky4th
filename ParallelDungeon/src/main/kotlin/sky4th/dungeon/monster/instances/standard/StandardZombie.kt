package sky4th.dungeon.monster.instances.standard

import sky4th.dungeon.monster.core.MonsterDefinition
import sky4th.dungeon.monster.core.MonsterLevel
import sky4th.dungeon.monster.core.MonsterRegistry
import sky4th.dungeon.monster.core.MonsterMechanic
import sky4th.dungeon.monster.core.MonsterMechanicRegistry
import sky4th.dungeon.monster.head.MonsterHeadConfig
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack

/**
 * 地穴僵尸：标准僵尸
 *
 * - 怪物等级: 标准
 * - 名称: 地穴僵尸
 * - 原型: 僵尸
 * - 血量: 30
 * - 特性: 击退抗性
 * - 护甲: 无
 * - 武器: 无
 */
// 从MonsterHeadConfig获取怪物ID和名称
private const val MONSTER_ID = "standard_zombie"

object StandardZombie {
    private val DISPLAY_NAME: String
        get() = MonsterHeadConfig.getPlainDisplayName(MONSTER_ID) ?: "地穴僵尸" // 默认值，如果配置中未找到

    private val definition: MonsterDefinition by lazy {
        MonsterDefinition(
            id = MONSTER_ID,
            displayName = DISPLAY_NAME,
            level = MonsterLevel.STANDARD,
            entityType = EntityType.ZOMBIE,
            maxHealth = 30.0,
            knockbackResistance = 0.6,
            armorItems = emptyList(),
            weapon = null,
            extraConfigurator = ::configureEntity
        )
    }

    private fun configureEntity(entity: LivingEntity) {
        val rawDisplayName = MonsterHeadConfig.getRawDisplayName(MONSTER_ID)
        if (rawDisplayName != null) {
            // 使用配置中的显示名称（包含颜色代码）
            val component = MiniMessage.miniMessage().deserialize(rawDisplayName)
            entity.customName(component)
        } else {
            // 使用默认显示名称
            entity.customName(Component.text(DISPLAY_NAME, NamedTextColor.YELLOW))
        }
        entity.isCustomNameVisible = true
        
        // 确保是普通僵尸
        if (entity is org.bukkit.entity.Zombie) {
            // 设置为成年僵尸
            entity.setAge(0)
            
            // 清空手持物品
            entity.equipment.setItemInMainHand(org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR))
            entity.equipment.setItemInOffHand(org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR))
            
            // 确保不会成为鸡骑士
            entity.setShouldBurnInDay(true) // 设置为白天燃烧
        }
    }

    fun register() {
        MonsterRegistry.register(definition)
    }
}

