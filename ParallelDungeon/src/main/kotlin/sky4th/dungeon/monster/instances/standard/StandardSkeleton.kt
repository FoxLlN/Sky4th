package sky4th.dungeon.monster.instances.legendary

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
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack

/**
 * 地穴骷髅：远程普通小怪。
 *
 * - 等级: 标准
 * - 原型: 骷髅
 * - 血量: 30
 * - 特性: 受到远程伤害 -10%
 * - 武器: 弓
 */
// 从MonsterHeadConfig获取怪物ID和名称
private const val MONSTER_ID = "standard_skeleton"

object StandardSkeleton {
    private val DISPLAY_NAME: String
        get() = MonsterHeadConfig.getPlainDisplayName(MONSTER_ID) ?: "地穴骷髅" // 默认值，如果配置中未找到

    private val definition: MonsterDefinition by lazy {
        MonsterDefinition(
            id = MONSTER_ID,
            displayName = DISPLAY_NAME,
            level = MonsterLevel.STANDARD,
            entityType = EntityType.SKELETON,
            maxHealth = 30.0,
            knockbackResistance = 0.0,
            armorItems = emptyList(),
            weapon = createBow(),
            extraConfigurator = ::configureEntity
        )
    }

    private fun createBow(): ItemStack {
        val bow = ItemStack(Material.BOW)
        val meta = bow.itemMeta
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
            entity.customName(Component.text(DISPLAY_NAME, NamedTextColor.YELLOW))
        }
        entity.isCustomNameVisible = true
    }

    fun register() {
        MonsterRegistry.register(definition)
        MonsterMechanicRegistry.register(StandardSkeletonMechanic)
    }
}

/**
 * 地穴骷髅的特殊机制：受到远程伤害 -10%
 */
object StandardSkeletonMechanic : MonsterMechanic {
    override val monsterId = MONSTER_ID

    override fun onDamaged(monster: LivingEntity, event: EntityDamageByEntityEvent) {
        if (event.cause.toString().contains("PROJECTILE")) {
            event.damage = event.damage * 0.9
        }
    }
}

