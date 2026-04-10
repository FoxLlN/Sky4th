package sky4th.dungeon.monster.instances.elite

import sky4th.dungeon.monster.core.MonsterDefinition
import sky4th.dungeon.monster.core.MonsterLevel
import sky4th.dungeon.monster.core.MonsterRegistry
import sky4th.dungeon.monster.core.MonsterMechanic
import sky4th.dungeon.monster.core.MonsterMechanicRegistry
import sky4th.dungeon.monster.head.MonsterHeadConfig
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Arrow
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack
import java.util.UUID

/**
 * 暗影射手：高输出远程怪。
 *
 * - 等级: 精英
 * - 原型: 骷髅
 * - 血量: 40
 * - 特性: 连续命中同一目标每次 +5% 伤害（最多 +20%，换目标/间隔太久重置）
 * - 武器: 力量 II 弓
 */
// 从MonsterHeadConfig获取怪物ID和名称
private const val MONSTER_ID = "elite_skeleton"

object EliteSkeleton {
    private val DISPLAY_NAME: String
        get() = MonsterHeadConfig.getPlainDisplayName(MONSTER_ID) ?: "暗影射手" // 默认值，如果配置中未找到

    private val definition: MonsterDefinition by lazy {
        MonsterDefinition(
            id = MONSTER_ID,
            displayName = DISPLAY_NAME,
            level = MonsterLevel.ELITE,
            entityType = EntityType.SKELETON,
            maxHealth = 40.0,
            knockbackResistance = 0.2,
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
            entity.customName(Component.text(DISPLAY_NAME, NamedTextColor.GOLD))
        }
        entity.isCustomNameVisible = true
    }

    fun register() {
        MonsterRegistry.register(definition)
        MonsterMechanicRegistry.register(EliteSkeletonMechanic)
    }
}

/**
 * 试炼射手的特殊机制：连续命中同一目标每次 +5% 伤害（最多 +20%，换目标/间隔太久重置）
 */
object EliteSkeletonMechanic : MonsterMechanic {
    override val monsterId = MONSTER_ID

    private data class EliteSkeletonState(
        var targetId: UUID?,
        var stacks: Int,
        var lastHitTick: Long
    )

    private val eliteSkeletonStates: MutableMap<UUID, EliteSkeletonState> = mutableMapOf()

    override fun onArrowHit(monster: LivingEntity, arrow: Arrow, victim: LivingEntity, event: EntityDamageByEntityEvent) {
        val shooterId = monster.uniqueId
        val now = Bukkit.getCurrentTick().toLong()
        val state = eliteSkeletonStates.getOrPut(shooterId) {
            EliteSkeletonState(null, 0, now)
        }

        // 如果换目标或间隔太久（>3 秒），重置层数
        if (state.targetId != victim.uniqueId || now - state.lastHitTick > 3 * 20) {
            state.targetId = victim.uniqueId
            state.stacks = 0
        }

        state.stacks = (state.stacks + 1).coerceAtMost(4) // 1~4 层 -> 5%~20%
        state.lastHitTick = now

        val bonusMultiplier = 1.0 + state.stacks * 0.05
        event.damage = event.damage * bonusMultiplier
    }
}

