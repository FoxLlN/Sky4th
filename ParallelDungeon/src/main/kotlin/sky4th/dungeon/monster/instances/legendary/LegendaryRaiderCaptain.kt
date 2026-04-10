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
import org.bukkit.entity.Arrow
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID

/**
 * 史诗：试炼1队队长（小 Boss）。
 *
 * - 等级: 史诗
 * - 原型: 掠夺者
 * - 血量: 80
 * - 特性: 血量降到 60% / 30% 以下时各召唤一次小弟并获得短暂抗性
 * - 护甲: 锁链套 + 保护 I
 * - 武器: 快速装填 I + 多重射击 弩
 */
// 从MonsterHeadConfig获取怪物ID和名称
private const val MONSTER_ID = "legendary_raider_captain"

object LegendaryRaiderCaptain {
    private val DISPLAY_NAME: String
        get() = MonsterHeadConfig.getPlainDisplayName(MONSTER_ID) ?: "试炼1队队长" // 默认值，如果配置中未找到

    private val definition: MonsterDefinition by lazy {
        MonsterDefinition(
            id = MONSTER_ID,
            displayName = DISPLAY_NAME,
            level = MonsterLevel.LEGENDARY,
            entityType = EntityType.PILLAGER,
            maxHealth = 80.0,
            knockbackResistance = 0.6,
            armorItems = createArmor(),
            weapon = createCrossbow(),
            extraConfigurator = ::configureEntity
        )
    }

    private fun createArmor(): List<ItemStack> {
        fun chainWithProt(material: Material): ItemStack {
            val item = ItemStack(material)
            val meta = item.itemMeta
            val protection = Enchantment.PROTECTION
            if (protection != null) {
                meta.addEnchant(protection, 1, true)
            }
            item.itemMeta = meta
            return item
        }
        return listOf(
            chainWithProt(Material.CHAINMAIL_HELMET),
            chainWithProt(Material.CHAINMAIL_CHESTPLATE),
            chainWithProt(Material.CHAINMAIL_LEGGINGS),
            chainWithProt(Material.CHAINMAIL_BOOTS)
        )
    }

    private fun createCrossbow(): ItemStack {
        val crossbow = ItemStack(Material.CROSSBOW)
        val meta = crossbow.itemMeta
        meta.addEnchant(Enchantment.QUICK_CHARGE, 1, true)
        meta.addEnchant(Enchantment.MULTISHOT, 1, true)
        crossbow.itemMeta = meta
        return crossbow
    }

    private fun configureEntity(entity: LivingEntity) {
        val rawDisplayName = MonsterHeadConfig.getRawDisplayName(MONSTER_ID)
        if (rawDisplayName != null) {
            // 使用配置中的显示名称（包含颜色代码）
            val component = MiniMessage.miniMessage().deserialize(rawDisplayName)
            entity.customName(component)
        } else {
            // 使用默认显示名称
            entity.customName(Component.text(DISPLAY_NAME, NamedTextColor.RED))
        }
        entity.isCustomNameVisible = true
    }

    fun register() {
        MonsterRegistry.register(definition)
        MonsterMechanicRegistry.register(LegendaryRaiderCaptainMechanic)
    }
}

/**
 * 掠夺者统领的特殊机制：血量降到 60% / 30% 以下时各召唤一次小弟并获得短暂抗性
 */
object LegendaryRaiderCaptainMechanic : MonsterMechanic {
    override val monsterId = MONSTER_ID

    // 记录已触发过的血量阈值（避免重复触发）
    private val raiderCaptain60Triggered: MutableSet<UUID> = mutableSetOf()
    private val raiderCaptain30Triggered: MutableSet<UUID> = mutableSetOf()

    private fun handleThresholds(captain: LivingEntity) {
        val max = captain.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue ?: return
        val percent = captain.health / max
        val uuid = captain.uniqueId

        if (percent <= 0.6 && !raiderCaptain60Triggered.contains(uuid)) {
            raiderCaptain60Triggered.add(uuid)
            summonMinionNear(captain)
            val resistance = org.bukkit.Registry.POTION_EFFECT_TYPE.get(org.bukkit.NamespacedKey.minecraft("resistance"))
            if (resistance != null) {
                captain.addPotionEffect(PotionEffect(resistance, 10 * 20, 0, false, true))
            }
        }
        if (percent <= 0.3 && !raiderCaptain30Triggered.contains(uuid)) {
            raiderCaptain30Triggered.add(uuid)
            summonMinionNear(captain)
            val resistance = org.bukkit.Registry.POTION_EFFECT_TYPE.get(org.bukkit.NamespacedKey.minecraft("resistance"))
            if (resistance != null) {
                captain.addPotionEffect(PotionEffect(resistance, 10 * 20, 1, false, true))
            }
        }
    }

    private fun summonMinionNear(captain: LivingEntity) {
        val world = captain.world
        val random = kotlin.random.Random
        val loc = captain.location.clone().add(random.nextDouble() * 2 - 1, 0.0, random.nextDouble() * 2 - 1)
        // 召唤一个普通掠夺者作为小弟
        world.spawnEntity(loc, EntityType.PILLAGER)
    }

    override fun onMeleeHit(monster: LivingEntity, victim: LivingEntity, event: EntityDamageByEntityEvent) {
        handleThresholds(monster)
    }

    override fun onArrowHit(monster: LivingEntity, arrow: Arrow, victim: LivingEntity, event: EntityDamageByEntityEvent) {
        handleThresholds(monster)
    }
}

