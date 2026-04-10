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
import org.bukkit.inventory.ItemStack

/**
 * 试炼盾卫：近战重装小怪。
 *
 * - 等级: 精英
 * - 原型: 僵尸
 * - 血量: 50
 * - 护甲: 锁链胸甲
 * - 武器: 铁剑 + 盾
 */
// 从MonsterHeadConfig获取怪物ID和名称
private const val MONSTER_ID = "elite_zombie"

object EliteZombie {
    private val DISPLAY_NAME: String
        get() = MonsterHeadConfig.getPlainDisplayName(MONSTER_ID) ?: "试炼盾卫" // 默认值，如果配置中未找到

    private val definition: MonsterDefinition by lazy {
        MonsterDefinition(
            id = MONSTER_ID,
            displayName = DISPLAY_NAME,
            level = MonsterLevel.ELITE,
            entityType = EntityType.ZOMBIE,
            maxHealth = 50.0,
            knockbackResistance = 0.5,
            armorItems = listOf(
                null, // helmet
                ItemStack(Material.CHAINMAIL_CHESTPLATE),
                null, // leggings
                null  // boots
            ),
            weapon = createSword(),
            extraConfigurator = ::configureEntity
        )
    }

    private fun createSword(): ItemStack {
        val sword = ItemStack(Material.IRON_SWORD)
        sword.editMeta { meta ->
            val sharpness = Enchantment.SHARPNESS
            if (sharpness != null) {
                meta.addEnchant(sharpness, 1, true)
            }
        }
        return sword
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
        // 盾牌放在副手
        var equipment = entity.equipment
        if (equipment != null) {
            equipment.setItemInOffHand(ItemStack(Material.SHIELD))
        }
    }

    fun register() {
        MonsterRegistry.register(definition)
    }
}

