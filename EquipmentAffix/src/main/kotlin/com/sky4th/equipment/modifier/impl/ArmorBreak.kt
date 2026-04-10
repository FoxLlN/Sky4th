
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import com.sky4th.equipment.util.PlayereffectUtil
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack
import sky4th.core.api.MarkAPI

/**
 * 碎甲词条
 * 效果：攻击命中后，使目标护甲值降低，持续一定时间
 * 1级：降低8%护甲，持续3秒
 * 2级：降低12%护甲，持续4秒
 * 3级：降低15%护甲，持续5秒
 * 叠加规则：同一目标只能存在一个"碎甲"效果，新触发刷新持续时间（不叠加层数）
 */
class ArmorBreak : com.sky4th.equipment.modifier.ConfiguredModifier("armor_break") {

    companion object {
        // 碎甲效果的命名空间键
        private val ARMOR_BREAK_KEY = NamespacedKey("equipment_affix", "armor_break")
        // 每级配置：(降低护甲百分比，持续时间)
        private val CONFIG = arrayOf(
            0.08 to 3,       // 降低8%护甲，持续3秒
            0.12 to 4,       // 降低12%护甲，持续4秒
            0.15 to 5        // 降低15%护甲，持续5秒
        )
        // 标记显示物品（铁胸甲）
        private val MARK_ITEM = ItemStack(Material.IRON_CHESTPLATE)
    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(EntityDamageEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 只处理实体伤害事件，且玩家必须是攻击者
        if (event !is EntityDamageEvent || playerRole != PlayerRole.ATTACKER) {
            return
        }

        // 检查是否是实体攻击实体事件
        if (event !is org.bukkit.event.entity.EntityDamageByEntityEvent) {
            return
        }

        // 获取受击者
        val victim = event.entity
        if (victim !is LivingEntity) {
            return
        }

        // 只处理物理攻击（近战和弹射物）
        if (event.cause != EntityDamageEvent.DamageCause.ENTITY_ATTACK && 
            event.cause != EntityDamageEvent.DamageCause.PROJECTILE) {
            return
        }

        // 获取当前等级的护甲降低百分比和持续时间
        val (reductionPercentage, duration) = CONFIG.getOrNull(level - 1) ?: return

        // 获取目标的护甲属性
        val armorAttribute = victim.getAttribute(Attribute.GENERIC_ARMOR) ?: return

        // 计算要降低的护甲值
        val currentArmor = armorAttribute.value
        val reductionAmount = currentArmor * reductionPercentage

        // 检查是否已有碎甲效果
        val existingModifiers = armorAttribute.modifiers.toList()
        val hasExistingEffect = existingModifiers.any { it.key == ARMOR_BREAK_KEY }

        if (hasExistingEffect) {
            // 已有碎甲效果，刷新标记
            MarkAPI.createMark(victim, getAffixId(), MARK_ITEM, showToAllPlayers = true, duration = duration.toLong())
            return
        }

        // 移除旧的碎甲效果
        removeArmorBreakEffect(victim)

        // 添加新的碎甲效果
        val modifier = AttributeModifier(
            ARMOR_BREAK_KEY,
            -reductionAmount,
            AttributeModifier.Operation.ADD_NUMBER,
            org.bukkit.inventory.EquipmentSlotGroup.ANY
        )
        armorAttribute.addModifier(modifier)

        // 跟踪被词条影响的实体
        com.sky4th.equipment.modifier.ModifierManager.instance.trackAffectedEntity(victim)

        // 创建碎甲标记（MarkManager会自动处理过期时间）
        MarkAPI.createMark(victim, getAffixId(), MARK_ITEM, showToAllPlayers = true, duration = duration.toLong())
    }

    /**
     * 移除碎甲效果
     */
    private fun removeArmorBreakEffect(victim: LivingEntity) {
        val armorAttribute = victim.getAttribute(Attribute.GENERIC_ARMOR) ?: return

        // 移除碎甲修饰符
        val modifiers = armorAttribute.modifiers.toList()
        var hasModifier = false
        for (modifier in modifiers) {
            if (modifier.key == ARMOR_BREAK_KEY) {
                armorAttribute.removeModifier(modifier)
                hasModifier = true
            }
        }

        // 如果移除了修饰符，停止跟踪
        if (hasModifier) {
            com.sky4th.equipment.modifier.ModifierManager.instance.untrackAffectedEntity(victim)
        }
    }
}
