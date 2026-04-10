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
import org.bukkit.entity.Arrow
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * 混沌女巫：高生存辅助怪。
 *
 * - 等级: 强化
 * - 原型: 女巫
 * - 血量: 30
 * - 特性: 命中玩家时随机选择一种效果施加
 */
// 从MonsterHeadConfig获取怪物ID和名称
private const val MONSTER_ID = "chaos_witch"

object EnhancedWitch {
    private val DISPLAY_NAME: String
        get() = MonsterHeadConfig.getPlainDisplayName(MONSTER_ID) ?: "混沌女巫" // 默认值，如果配置中未找到

    private val definition: MonsterDefinition by lazy {
        MonsterDefinition(
            id = MONSTER_ID,
            displayName = DISPLAY_NAME,
            level = MonsterLevel.ENHANCED,
            entityType = EntityType.WITCH,
            maxHealth = 30.0,
            knockbackResistance = 0.1,
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
    }

    fun register() {
        MonsterRegistry.register(definition)
        MonsterMechanicRegistry.register(EnhancedWitchMechanic)
    }
}

/**
 * 混沌女巫的特殊机制：命中玩家时随机选择一种效果施加
 */
object EnhancedWitchMechanic : MonsterMechanic {
    override val monsterId = MONSTER_ID

    private val effects = listOf(
        { player: Player ->
            val slow = org.bukkit.Registry.POTION_EFFECT_TYPE.get(org.bukkit.NamespacedKey.minecraft("slowness"))
            if (slow != null) player.addPotionEffect(PotionEffect(slow, 6 * 20, 0, false, true))
        },
        { player: Player ->
            val weakness = org.bukkit.Registry.POTION_EFFECT_TYPE.get(org.bukkit.NamespacedKey.minecraft("weakness"))
            if (weakness != null) player.addPotionEffect(PotionEffect(weakness, 6 * 20, 0, false, true))
        },
        { player: Player ->
            val poison = org.bukkit.Registry.POTION_EFFECT_TYPE.get(org.bukkit.NamespacedKey.minecraft("poison"))
            if (poison != null) player.addPotionEffect(PotionEffect(poison, 5 * 20, 0, false, true))
        },
        { player: Player -> player.damage(2.0) }, // 瞬间伤害
        { player: Player ->
            val heal = org.bukkit.Registry.POTION_EFFECT_TYPE.get(org.bukkit.NamespacedKey.minecraft("instant_health"))
            if (heal != null) player.addPotionEffect(PotionEffect(heal, 1, 0, false, true))
        }
    )

    private fun applyRandomEffect(player: Player) {
        val randomEffect = effects.random()
        randomEffect(player)
    }

    override fun onMeleeHit(monster: LivingEntity, victim: LivingEntity, event: EntityDamageByEntityEvent) {
        if (victim is Player) {
            applyRandomEffect(victim)
        }
    }

    override fun onArrowHit(monster: LivingEntity, arrow: Arrow, victim: LivingEntity, event: EntityDamageByEntityEvent) {
        if (victim is Player) {
            applyRandomEffect(victim)
        }
    }
}

