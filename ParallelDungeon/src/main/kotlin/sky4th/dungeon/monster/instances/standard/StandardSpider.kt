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
import org.bukkit.attribute.Attribute
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * 地穴蜘蛛：近战毒伤小怪。
 *
 * - 等级: 标准
 * - 原型: 蜘蛛
 * - 血量: 20
 * - 特性: 攻击附带中毒 I 5s
 */
// 从MonsterHeadConfig获取怪物ID和名称
private const val MONSTER_ID = "standard_spider"

object StandardSpider {
    private val DISPLAY_NAME: String
        get() = MonsterHeadConfig.getPlainDisplayName(MONSTER_ID) ?: "地穴蜘蛛" // 默认值，如果配置中未找到

    private val definition: MonsterDefinition by lazy {
        MonsterDefinition(
            id = MONSTER_ID,
            displayName = DISPLAY_NAME,
            level = MonsterLevel.STANDARD,
            entityType = EntityType.SPIDER,
            maxHealth = 20.0,
            knockbackResistance = 0.0,
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
    }

    fun register() {
        MonsterRegistry.register(definition)
        MonsterMechanicRegistry.register(StandardSpiderMechanic)
    }
}

/**
 * 地穴蜘蛛的特殊机制：攻击命中时对玩家施加中毒 I 5s
 */
object StandardSpiderMechanic : MonsterMechanic {
    override val monsterId = MONSTER_ID

    override fun onMeleeHit(monster: LivingEntity, victim: LivingEntity, event: EntityDamageByEntityEvent) {
        if (victim is Player) {
            victim.addPotionEffect(PotionEffect(PotionEffectType.POISON, 5 * 20, 0, false, true))
        }
    }
}

