package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import com.sky4th.equipment.util.AttributeModifierUtil
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.EquipmentSlotGroup
import sky4th.core.api.MarkAPI
import kotlin.random.Random

/**
 * 束缚词条
 * 效果：弓/弩命中目标后概率使目标定身
 * 1级：30%概率定身0.5秒
 * 2级：40%概率定身1秒
 * 3级：50%概率定身1.5秒
 * 冷却：5秒触发一次
 */
class Bind : com.sky4th.equipment.modifier.ConfiguredModifier("bind") {

    companion object {
        // 每级配置：(触发概率, 持续时间)
        private val CONFIG = arrayOf(
            0.30 to 0.5,       // 30%概率，持续0.5秒
            0.40 to 1.0,       // 40%概率，持续1秒
            0.50 to 1.5        // 50%概率，持续1.5秒
        )
        // 标记显示物品（线）
        private val MARK_ITEM = ItemStack(Material.STRING)
        // 束缚效果的命名空间键
        private val BIND_KEY = NamespacedKey("equipment_affix", "bind")
        // 冷却时间（秒）
        private const val COOLDOWN = 5L
        // 存储每个实体的冷却时间
        private val cooldowns = mutableMapOf<org.bukkit.entity.Entity, Long>()
        // 存储每个实体的粒子任务
        private val particleTasks = mutableMapOf<org.bukkit.entity.Entity, org.bukkit.scheduler.BukkitTask>()
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
        // 只处理实体攻击实体事件
        if (event !is EntityDamageByEntityEvent) {
            return
        }

        // 检查是否是弹射物造成的伤害
        val damager = event.damager
        if (!org.bukkit.entity.Projectile::class.java.isInstance(damager)) {
            return
        }
        val projectile = damager as org.bukkit.entity.Projectile

        // 确保弹射物的射击者是玩家
        if (projectile.shooter !is Player || projectile.shooter != player) {
            return
        }

        // 检查是否被盾牌完全格挡（伤害为0）
        if (event.damage <= 0) {
            return
        }

        // 获取受害者
        val victim = event.entity
        if (victim !is LivingEntity) {
            return
        }

        // 检查冷却时间
        val currentTime = System.currentTimeMillis()
        val lastBindTime = cooldowns[victim] ?: 0
        if (currentTime - lastBindTime < COOLDOWN * 1000) {
            return
        }

        // 获取当前等级的配置
        val (chance, duration) = CONFIG.getOrNull(level - 1) ?: return

        // 概率判定
        //if (Random.nextDouble() > chance) return

        // 更新冷却时间
        cooldowns[victim] = currentTime

        // 束缚目标的移动速度
        bindMovement(victim)

        // 跟踪被词条影响的实体
        com.sky4th.equipment.modifier.ModifierManager.instance.trackAffectedEntity(victim)

        // 创建束缚标记（使用词条ID作为标记ID）
        MarkAPI.createMark(victim, getAffixId(), MARK_ITEM, showToAllPlayers = true, duration = duration.toLong())

        // 计算持续时间对应的tick数
        val durationTicks = (duration * 20).toInt()

        // 初始时在目标周围生成束缚粒子效果
        val baseLocation = victim.location
        for (i in 0 until 15) {
            val offsetX = (Math.random() - 0.5) * 1.2
            val offsetY = Math.random() * 2.0  // 从脚到头顶随机高度
            val offsetZ = (Math.random() - 0.5) * 1.2

            victim.world.spawnParticle(
                org.bukkit.Particle.BLOCK,
                baseLocation.x + offsetX,
                baseLocation.y + offsetY,
                baseLocation.z + offsetZ,
                1,  // 粒子数量
                0.0,  // x偏移
                0.0,  // y偏移
                0.0,  // z偏移
                0.0,  // 速度
                org.bukkit.Material.WHITE_WOOL.createBlockData()  // 使用白色羊毛材质
            )
        }

        // 创建持续环绕的束缚粒子效果
        var currentTick = 0

        // 取消旧的粒子任务（如果有）
        particleTasks[victim]?.cancel()

        // 声明粒子任务变量
        lateinit var particleTask: org.bukkit.scheduler.BukkitTask

        particleTask = org.bukkit.Bukkit.getScheduler().runTaskTimer(
            com.sky4th.equipment.EquipmentAffix.instance,
            Runnable {
                if (currentTick >= durationTicks || victim.isDead) {
                    // 任务结束条件：达到持续时间或目标死亡
                    particleTask.cancel()
                    particleTasks.remove(victim)
                    // 解除束缚效果
                    unbindMovement(victim)
                    return@Runnable
                }

                // 在目标周围生成白色线状粒子
                val targetLoc = victim.location.clone().add(0.0, 0.5, 0.0)
                victim.world.spawnParticle(
                    org.bukkit.Particle.SWEEP_ATTACK,
                    targetLoc,
                    1,               // 粒子数量
                    0.5, 0.5, 0.5,   // 偏移范围
                    0.0              // 速度
                )

                currentTick += 2
            },
            0L,  // 初始延迟
            2L   // 重复间隔（每2tick生成一次）
        )

        // 存储粒子任务
        particleTasks[victim] = particleTask
    }

    /**
     * 束缚目标的移动速度
     */
    private fun bindMovement(victim: LivingEntity) {
        // 获取移动速度属性
        val movementSpeedAttribute = victim.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) ?: return

        // 获取当前移动速度值
        val currentSpeed = movementSpeedAttribute.value

        // 移除旧的束缚修饰符（如果有）
        val modifiers = movementSpeedAttribute.modifiers.toList()
        for (modifier in modifiers) {
            if (modifier.key == BIND_KEY) {
                movementSpeedAttribute.removeModifier(modifier)
            }
        }

        // 创建一个负的修饰符，将速度降为0
        val modifier = AttributeModifier(
            BIND_KEY,
            -currentSpeed,
            AttributeModifier.Operation.ADD_NUMBER,
            EquipmentSlotGroup.ANY
        )
        movementSpeedAttribute.addModifier(modifier)
    }

    /**
     * 解除目标的移动速度束缚
     */
    private fun unbindMovement(victim: LivingEntity) {
        // 获取移动速度属性
        val movementSpeedAttribute = victim.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) ?: return

        // 移除束缚修饰符
        val modifiers = movementSpeedAttribute.modifiers.toList()
        var hasModifier = false
        for (modifier in modifiers) {
            if (modifier.key == BIND_KEY) {
                movementSpeedAttribute.removeModifier(modifier)
                hasModifier = true
            }
        }

        // 如果移除了修饰符，停止跟踪
        if (hasModifier) {
            com.sky4th.equipment.modifier.ModifierManager.instance.untrackAffectedEntity(victim)
        }
    }
}
