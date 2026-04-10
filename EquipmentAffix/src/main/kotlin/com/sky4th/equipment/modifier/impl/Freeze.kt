package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import com.sky4th.equipment.modifier.manager.FreezeManager
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.EquipmentSlotGroup
import sky4th.core.api.MarkAPI
import kotlin.random.Random

/**
 * 冰封词条
 * 效果：攻击时有概率冰封目标造成冻伤
 * 满血概率翻倍
 * 1级：攻击时16%概率冰封目标1.5秒
 * 2级：攻击时20%概率冰封目标2秒
 * 3级：攻击时24%概率冰封目标2.5秒
 * 
 * 冰封机制：
 * - 将目标的TicksFrozen设置为140+持续时间对应的tick数
 * - 持续时间结束后，TicksFrozen会降到阈值(140)以下
 */
class Freeze : com.sky4th.equipment.modifier.ConfiguredModifier("freeze") {

    companion object {
        // 每级配置：(触发概率, 持续时间)
        private val CONFIG = arrayOf(
            0.16 to 1.5,       // 16%概率，持续1.5秒
            0.20 to 2.0,       // 20%概率，持续2秒
            0.24 to 2.5        // 24%概率，持续2.5秒
        )
        // 标记显示物品（冰）
        private val MARK_ITEM = ItemStack(Material.ICE)
        // 冰冻效果的命名空间键
        private val FREEZE_KEY = NamespacedKey("equipment_affix", "freeze")

        // 冰冻阈值（Minecraft默认为140）
        private const val FREEZE_THRESHOLD = 140 + 10
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
        // 只处理实体伤害事件，且玩家必须是攻击者
        if (event !is EntityDamageByEntityEvent || playerRole != PlayerRole.ATTACKER) {
            return
        }
        
        if (event.cause == EntityDamageEvent.DamageCause.FREEZE) return

        // 获取受击者
        val victim = event.entity
        if (victim !is LivingEntity) {
            return
        }

        // 获取当前等级的配置
        val (chance, duration) = CONFIG.getOrNull(level - 1) ?: return

        // 检查受击者是否满血
        val isFullHealth = victim.health >= victim.maxHealth

        // 满血概率翻倍，否则按概率判定
        val finalChance = if (isFullHealth) chance * 2 else chance
        if (Random.nextDouble() > finalChance) return

        // 计算需要设置的TicksFrozen值
        // 基础值140 + 持续时间对应的tick数
        val durationTicks = (duration * 20).toInt()
        val targetTicksFrozen = FREEZE_THRESHOLD + (durationTicks * 2)

        // 设置目标的TicksFrozen
        victim.setFreezeTicks(targetTicksFrozen)

        // 将实体添加到冰冻管理器
        FreezeManager.addEntity(victim, getAffixId())

        // 跟踪被词条影响的实体
        com.sky4th.equipment.modifier.ModifierManager.instance.trackAffectedEntity(victim)

        // 冻结目标的移动速度
        freezeMovement(victim)

        // 创建冰封标记（使用词条ID作为标记ID）
        MarkAPI.createMark(victim, getAffixId(), MARK_ITEM, showToAllPlayers = true, duration = duration.toLong())

         // 检查是否穿戴皮革装备
        val hasLeatherArmor = victim.equipment?.let { equipment ->
            equipment.helmet?.type?.name?.startsWith("LEATHER") == true ||
            equipment.chestplate?.type?.name?.startsWith("LEATHER") == true ||
            equipment.leggings?.type?.name?.startsWith("LEATHER") == true ||
            equipment.boots?.type?.name?.startsWith("LEATHER") == true
        } ?: false
        // 如果没有穿戴皮革装备，才造成额外冻伤伤害
        if (!hasLeatherArmor) {
            // 创建冻伤伤害（冻伤是无来源伤害）
            val damageSource = org.bukkit.damage.DamageSource.builder(org.bukkit.damage.DamageType.FREEZE).build()
            // 造成冻伤伤害
            val freezeDamage = 1.0
            victim.damage(freezeDamage, damageSource)
        }

        // 初始时在目标周围生成冰霜粒子效果
        val baseLocation = victim.location
        for (i in 0 until 20) {
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
                org.bukkit.Material.ICE.createBlockData()  // 使用冰块材质
            )
        }

        // 创建持续环绕的霜冻粒子效果
        var currentTick = 0
        
        // 取消旧的粒子任务（如果有）
        particleTasks[victim]?.cancel()
        
        // 声明粒子任务变量
        lateinit var particleTask: org.bukkit.scheduler.BukkitTask
        
        particleTask = org.bukkit.Bukkit.getScheduler().runTaskTimer(
            com.sky4th.equipment.EquipmentAffix.instance,
            Runnable {
                if (currentTick >= durationTicks || victim.isDead || victim.freezeTicks <= 0) {
                    // 任务结束条件：达到持续时间, 目标死亡或冰冻结束
                    particleTask.cancel()
                    particleTasks.remove(victim)
                    // 清理所有效果：从列表中移除、移除标记、移除修饰符
                    FreezeManager.removeEntity(victim)
                    // 如果没有穿戴皮革装备，才造成额外冻伤伤害
                    if (!hasLeatherArmor && level >= 3) {
                        // 创建冻伤伤害（冻伤是无来源伤害）
                        val damageSource = org.bukkit.damage.DamageSource.builder(org.bukkit.damage.DamageType.FREEZE).build()
                        // 造成冻伤伤害
                        val freezeDamage = 1.0
                        victim.damage(freezeDamage, damageSource)
                    }
                    return@Runnable
                }

                // 在目标周围生成蓝色霜冻粒子
                val targetLoc = victim.location.clone().add(0.0, 0.5, 0.0)
                victim.world.spawnParticle(
                    org.bukkit.Particle.SNOWFLAKE,
                    targetLoc,
                    3,               // 粒子数量
                    0.5, 0.5, 0.5,   // 偏移范围
                    0.0              // 速度
                )

                // 偶尔生成白色烟雾，增加寒冷感
                if (Math.random() < 0.4) {
                    victim.world.spawnParticle(
                        org.bukkit.Particle.CLOUD,
                        targetLoc,
                        1,  // 粒子数量
                        0.3, 0.3, 0.3,  // 偏移范围
                        0.0   // 速度
                    )
                }

                currentTick += 2
            },
            0L,  // 初始延迟
            2L   // 重复间隔（每2tick生成一次）
        )
        
        // 存储粒子任务
        particleTasks[victim] = particleTask
    }

    /**
     * 冻结目标的移动速度
     */
    private fun freezeMovement(victim: LivingEntity) {
        // 获取移动速度属性
        val movementSpeedAttribute = victim.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) ?: return

        // 获取当前移动速度值
        val currentSpeed = movementSpeedAttribute.value

        // 移除旧的冰冻修饰符（如果有）
        val modifiers = movementSpeedAttribute.modifiers.toList()
        for (modifier in modifiers) {
            if (modifier.key == FREEZE_KEY) {
                movementSpeedAttribute.removeModifier(modifier)
            }
        }

        // 创建一个负的修饰符，将速度降为0
        val modifier = AttributeModifier(
            org.bukkit.NamespacedKey("equipment_affix", "freeze"),
            -currentSpeed,
            AttributeModifier.Operation.ADD_NUMBER,
            EquipmentSlotGroup.ANY
        )
        movementSpeedAttribute.addModifier(modifier)
    }
}
