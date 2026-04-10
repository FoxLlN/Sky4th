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
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * 试炼处刑者：近战精英怪。
 *
 * - 等级: 精英
 * - 原型: 卫道士
 * - 血量: 50
 * - 特性: 血量 <50% 时获得一次性抗性 I 20s + 力量 I 10s
 * - 护甲: 铁胸甲
 * - 武器: 锋利 I 石斧
 */
// 从MonsterHeadConfig获取怪物ID和名称
private const val MONSTER_ID = "elite_vindicator"

object EliteVindicator {
    private val DISPLAY_NAME: String
        get() = MonsterHeadConfig.getPlainDisplayName(MONSTER_ID) ?: "试炼处刑者" // 默认值，如果配置中未找到

    private val definition: MonsterDefinition by lazy {
        MonsterDefinition(
            id = MONSTER_ID,
            displayName = DISPLAY_NAME,
            level = MonsterLevel.ELITE,
            entityType = EntityType.VINDICATOR,
            maxHealth = 50.0,
            knockbackResistance = 0.4,
            armorItems = listOf(
                null, // helmet
                ItemStack(Material.IRON_CHESTPLATE),
                null, // leggings
                null  // boots
            ),
            weapon = createAxe(),
            extraConfigurator = ::configureEntity
        )
    }

    private fun createAxe(): ItemStack {
        val axe = ItemStack(Material.STONE_AXE)
        val meta = axe.itemMeta
        val sharpness = Enchantment.SHARPNESS
        if (sharpness != null) {
            meta.addEnchant(sharpness, 1, true)
        }
        axe.itemMeta = meta
        return axe
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
        MonsterMechanicRegistry.register(EliteVindicatorMechanic)
    }
}

/**
 * 试炼处刑者的特殊机制：血量 <50% 时获得一次性抗性 I 20s + 力量 I 10s
 */
object EliteVindicatorMechanic : MonsterMechanic {
    override val monsterId = MONSTER_ID

    override fun onMeleeHit(monster: LivingEntity, victim: LivingEntity, event: EntityDamageByEntityEvent) {
        val maxHealth = monster.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue ?: return
        if (monster.health / maxHealth <= 0.5) {
            val resistance = org.bukkit.Registry.POTION_EFFECT_TYPE.get(org.bukkit.NamespacedKey.minecraft("resistance"))
            val strength = org.bukkit.Registry.POTION_EFFECT_TYPE.get(org.bukkit.NamespacedKey.minecraft("strength"))
            if (resistance != null && !monster.hasPotionEffect(resistance)) {
                monster.addPotionEffect(PotionEffect(resistance, 20 * 20, 0, false, true))
            }
            if (strength != null && !monster.hasPotionEffect(strength)) {
                monster.addPotionEffect(PotionEffect(strength, 10 * 20, 0, false, true))
            }
        }
    }
}

