package sky4th.dungeon.monster.instances.enhanced

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
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack

/**
 * 试炼射手：更高输出的远程怪。
 *
 * - 等级: 强化
 * - 原型: 骷髅
 * - 血量: 35
 * - 武器: 力量 I 弓
 */
// 从MonsterHeadConfig获取怪物ID和名称
private const val MONSTER_ID = "enhanced_skeleton"

object EnhancedSkeleton {
    private val DISPLAY_NAME: String
        get() = MonsterHeadConfig.getPlainDisplayName(MONSTER_ID) ?: "试炼射手" // 默认值，如果配置中未找到

    private val definition: MonsterDefinition by lazy {
        MonsterDefinition(
            id = MONSTER_ID,
            displayName = DISPLAY_NAME,
            level = MonsterLevel.ENHANCED,
            entityType = EntityType.SKELETON,
            maxHealth = 35.0,
            knockbackResistance = 0.1,
            armorItems = emptyList(),
            weapon = createBow(),
            extraConfigurator = ::configureEntity
        )
    }

    private fun createBow(): ItemStack {
        val bow = ItemStack(Material.BOW)
        val meta = bow.itemMeta
        val sharpness = Enchantment.SHARPNESS
        if (sharpness != null) {
            meta.addEnchant(sharpness, 1, true)
        }
        bow.itemMeta = meta
        return bow
    }

    private fun configureEntity(entity: LivingEntity) {
        val rawDisplayName = MonsterHeadConfig.getRawDisplayName(MONSTER_ID)
        if (rawDisplayName != null) {
            // 使用配置中的显示名称（包含颜色代码）
            val component = MiniMessage.miniMessage().deserialize(rawDisplayName)
            entity.customName(component)
        } else {
            // 使用默认显示名称
            entity.customName(Component.text(DISPLAY_NAME, NamedTextColor.LIGHT_PURPLE))
        }
        entity.isCustomNameVisible = true
    }

    fun register() {
        MonsterRegistry.register(definition)
    }
}

