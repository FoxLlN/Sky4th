package sky4th.dungeon.monster.core

import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack
import sky4th.dungeon.monster.core.MonsterLevel
import sky4th.dungeon.monster.core.MonsterMetadata

/**
 * 怪物的通用定义：ID、显示名、等级、血量、护甲、武器等。
 *
 * 每个具体怪物建议单独建一个 Kotlin 文件，里边声明一个 MonsterDefinition 并在启动时注册。
 */
data class MonsterDefinition(
    val id: String,
    val displayName: String,
    val level: MonsterLevel,
    val entityType: EntityType,
    val maxHealth: Double,
    val knockbackResistance: Double = 0.0,
    val armorItems: List<ItemStack?> = emptyList(),
    val weapon: ItemStack? = null,
    val extraConfigurator: (LivingEntity) -> Unit = {}
) {
    init {
        require(id.isNotBlank()) { "monster id must not be blank" }
    }

    /**
     * 在指定位置生成一个怪物实例，并应用所有属性与 PDC 标记。
     */
    fun spawnAt(location: Location): LivingEntity {
        val world = location.world
            ?: error("Cannot spawn monster '$id' at location without world")
        val entity = world.spawnEntity(location, entityType) as LivingEntity

        // 基础血量
        entity.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue = maxHealth
        entity.health = maxHealth

        // 击退抗性
        entity.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)?.baseValue = knockbackResistance

        // PDC 标记
        MonsterMetadata.tagMonster(entity, id, level)

        // 额外个性化配置（药水效果、AI、名字颜色等）
        extraConfigurator(entity)

        // 装备（在 extraConfigurator 之后设置，确保不会被覆盖）
        var equipment = entity.equipment
        if (equipment != null) {
            // 按 [头, 胸, 腿, 鞋] 顺序套用（如果提供数量不足就跳过后面的）
            armorItems.getOrNull(0)?.let { equipment.setHelmet(it) }
            armorItems.getOrNull(1)?.let { equipment.setChestplate(it) }
            armorItems.getOrNull(2)?.let { equipment.setLeggings(it) }
            armorItems.getOrNull(3)?.let { equipment.setBoots(it) }
            if (weapon != null) {
                equipment.setItemInMainHand(weapon)
            }
            // 禁用掉落，防止装备显示异常
            equipment.setDropChance(org.bukkit.inventory.EquipmentSlot.HAND, 0f)
            equipment.setDropChance(org.bukkit.inventory.EquipmentSlot.FEET, 0f)
            equipment.setDropChance(org.bukkit.inventory.EquipmentSlot.LEGS, 0f)
            equipment.setDropChance(org.bukkit.inventory.EquipmentSlot.CHEST, 0f)
            equipment.setDropChance(org.bukkit.inventory.EquipmentSlot.HEAD, 0f)
        }
        // 禁用捡装备能力
        entity.setCanPickupItems(false)

        return entity
    }
}
