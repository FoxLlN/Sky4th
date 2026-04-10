
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import sky4th.core.api.MarkAPI
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * 碎骨词条
 * 效果：攻击时有概率降低目标最大生命值
 * 1级：9%概率使目标最大生命值降低10%（持续10秒，不可叠加可刷新）
 * 2级：12%概率使目标最大生命值降低10%（持续10秒，不可叠加可刷新）
 * 3级：15%概率使目标最大生命值降低10%（持续10秒，不可叠加可刷新）
 */
class BoneBreak : com.sky4th.equipment.modifier.ConfiguredModifier("bone_break") {

    companion object {
        // 标记持续时间（秒）
        private const val MARK_DURATION_SECONDS = 10
        // 最大生命值降低百分比（使用MULTIPLY_SCALAR_1操作）
        private const val MAX_HEALTH_REDUCTION = -0.10
        // 标记显示物品（骨头）
        private val MARK_ITEM = ItemStack(Material.BONE)
        // 每级的触发概率
        private val CHANCE = doubleArrayOf(0.09, 0.12, 0.15)
        // 属性修饰符键
        private val BONEBREAK_MODIFIER_KEY = NamespacedKey("equipment_affix", "bonebreak")
        // 存储实体的任务ID
        private val entityTasks = mutableMapOf<org.bukkit.entity.Entity, Int>()
    }

    override fun getEventTypes(): List<Class<out Event>> =
        listOf(EntityDamageByEntityEvent::class.java)

    override fun handle(
        event: Event,
        player: Player,
        item: ItemStack,
        level: Int,
        playerRole: PlayerRole
    ) {
        // 只处理实体攻击实体事件，且玩家必须是攻击者
        if (event !is EntityDamageByEntityEvent || playerRole != PlayerRole.ATTACKER) {
            return
        }

        // 获取受害者
        val victim = event.entity
        if (victim !is LivingEntity) {
            return
        }

        // 检查是否被盾牌完全格挡（伤害为0）
        if (event.damage <= 0) {
            return
        }

        // 获取当前等级的触发概率
        val triggerChance = if (level - 1 in 0..2) CHANCE[level - 1] else return

        // 概率检查
        if (Random.nextDouble() >= triggerChance) return

        // 检查是否已有标记
        if (MarkAPI.hasMark(victim, getAffixId())) {
            // 取消旧任务
            val oldTaskId = entityTasks[victim]
            if (oldTaskId != null) {
                org.bukkit.Bukkit.getScheduler().cancelTask(oldTaskId)
            }

            // 创建新任务
            val taskId = org.bukkit.Bukkit.getScheduler().runTaskLater(
                com.sky4th.equipment.EquipmentAffix.instance,
                Runnable {
                    removeBoneBreakEffect(victim)
                },
                MARK_DURATION_SECONDS * 20L  // 10秒 = 200 ticks
            ).taskId
            entityTasks[victim] = taskId

            // 刷新标记
            MarkAPI.createMark(victim, getAffixId(), MARK_ITEM, showToAllPlayers = true, duration = MARK_DURATION_SECONDS.toLong())
            return
        }

        // 获取最大生命值属性
        val maxHealthAttr = victim.getAttribute(Attribute.GENERIC_MAX_HEALTH) ?: return

        // 添加属性修饰符来降低最大生命值
        val modifier = AttributeModifier(
            BONEBREAK_MODIFIER_KEY,
            MAX_HEALTH_REDUCTION,
            AttributeModifier.Operation.MULTIPLY_SCALAR_1
        )
        maxHealthAttr.addModifier(modifier)

        // 创建任务在10秒后移除效果
        val taskId = org.bukkit.Bukkit.getScheduler().runTaskLater(
            com.sky4th.equipment.EquipmentAffix.instance,
            Runnable {
                removeBoneBreakEffect(victim)
            },
            MARK_DURATION_SECONDS * 20L  // 10秒 = 200 ticks
        ).taskId
        entityTasks[victim] = taskId

        // 应用标记
        MarkAPI.createMark(victim, getAffixId(), MARK_ITEM, showToAllPlayers = true, duration = MARK_DURATION_SECONDS.toLong())
    }

    override fun onRemove(player: Player) {
        // 当词条移除时，移除玩家的属性修饰符和任务
        removeBoneBreakEffect(player)
    }

    private fun removeBoneBreakEffect(entity: org.bukkit.entity.Entity) {
        if (entity is LivingEntity) {
            // 移除属性修饰符
            val maxHealthAttr = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH) ?: return
            val existingModifier = maxHealthAttr.modifiers.find { it.key == BONEBREAK_MODIFIER_KEY }
            if (existingModifier != null) {
                maxHealthAttr.removeModifier(existingModifier)
            }
            // 移除任务
            entityTasks.remove(entity)
        }
    }
}
