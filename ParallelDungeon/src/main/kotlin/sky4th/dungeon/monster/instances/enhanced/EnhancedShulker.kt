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
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * 无形贝：高防守控制怪。
 *
 * - 等级: 强化
 * - 原型: 潜影贝
 * - 血量: 40
 * - 特性: 隐身（这里用持续隐身效果实现）
 */
// 从MonsterHeadConfig获取怪物ID和名称
private const val MONSTER_ID = "enhanced_shulker"

object EnhancedShulker {
    private val DISPLAY_NAME: String
        get() = MonsterHeadConfig.getPlainDisplayName(MONSTER_ID) ?: "无形贝" // 默认值，如果配置中未找到

    private val definition: MonsterDefinition by lazy {
        MonsterDefinition(
            id = MONSTER_ID,
            displayName = DISPLAY_NAME,
            level = MonsterLevel.ENHANCED,
            entityType = EntityType.SHULKER,
            maxHealth = 40.0,
            knockbackResistance = 1.0,
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
            entity.customName(Component.text(DISPLAY_NAME, NamedTextColor.LIGHT_PURPLE))
        }
        entity.isCustomNameVisible = true
        entity.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, Int.MAX_VALUE, 0, false, false))
    }

    fun register() {
        MonsterRegistry.register(definition)
    }
}

