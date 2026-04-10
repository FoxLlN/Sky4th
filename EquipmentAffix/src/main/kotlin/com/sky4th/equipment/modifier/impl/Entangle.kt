
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.Material
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import sky4th.core.api.MarkAPI
import kotlin.random.Random

/**
 * 缠绕词条
 * 效果：满血必定触发，否则攻击时有概率缠绕目标造成减速
 * 1级：满血必定触发，否则15%概率使目标缓慢II 2秒
 * 2级：满血必定触发，否则18%概率使目标缓慢II 2.5秒
 * 3级：满血必定触发，否则21%概率使目标缓慢II 3秒
 */
class Entangle : com.sky4th.equipment.modifier.ConfiguredModifier("entangle") {

    companion object {
        // 每级配置：(触发概率, 持续时间)
        private val CONFIG = arrayOf(
            0.15 to 2.0,       // 15%概率，持续2秒
            0.18 to 2.5,       // 18%概率，持续2.5秒
            0.21 to 3.0        // 21%概率，持续3秒
        )
        // 标记显示物品（蛛网）
        private val MARK_ITEM = ItemStack(Material.COBWEB)
        // 存储每个实体的粒子任务
        private val particleTasks = mutableMapOf<org.bukkit.entity.Entity, org.bukkit.scheduler.BukkitTask>()
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

        // 获取当前等级的配置
        val (chance, duration) = CONFIG.getOrNull(level - 1) ?: return

        // 检查受击者是否满血
        val isFullHealth = victim.health >= victim.maxHealth

        // 满血必定触发，否则按概率判定
        if (!isFullHealth && Random.nextDouble() > chance) return

        // 添加缓慢II效果（持续duration秒，等级1表示缓慢II）
        val slowEffect = PotionEffect(
            PotionEffectType.SLOWNESS,
            (duration * 20).toInt(),  // 转换为tick（1秒=20tick）
            1,  // 等级1表示缓慢II
            true,  // 有粒子
            false   // 无图标
        )
        victim.addPotionEffect(slowEffect)

        // 创建缠绕标记（使用词条ID作为标记ID）
        MarkAPI.createMark(victim, getAffixId(), MARK_ITEM, showToAllPlayers = true, duration = duration.toLong())

        // 初始时在腿部到脚下位置生成蛛网碎片效果
        val baseLocation = victim.location
        for (i in 0 until 15) {
            val offsetX = (Math.random() - 0.5) * 1.0
            val offsetY = Math.random() * 1.5  // 从脚下到腿部随机高度
            val offsetZ = (Math.random() - 0.5) * 1.0

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
                org.bukkit.Material.COBWEB.createBlockData()  // 使用蜘蛛网材质
            )
        }

        // 创建持续环绕的SPELL_MOB粒子效果
        val durationTicks = (duration * 20).toInt()  // 将持续时间转换为tick
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
                    return@Runnable
                }

                // 在目标脚底生成灰白色光点，模拟飘浮的蛛丝
                val targetLoc = victim.location.clone().add(0.0, 0.0, 0.0)
                victim.world.spawnParticle(
                    org.bukkit.Particle.ENTITY_EFFECT,
                    targetLoc,
                    2,               // 粒子数量
                    0.5, 0.3, 0.5,   // 偏移范围
                    0.0,             // 速度（无）
                    org.bukkit.Color.fromRGB(180, 180, 180) // 灰白色
                )

                // 偶尔生成白色烟雾，增加缠绕感
                if (Math.random() < 0.3) {
                    victim.world.spawnParticle(
                        org.bukkit.Particle.CLOUD,
                        targetLoc,
                        1,  // 粒子数量
                        0.3, 0.3, 0.3,  // 偏移范围
                        0.0   // 速度
                    )
                }

                currentTick += 3
            },
            0L,  // 初始延迟
            3L   // 重复间隔（每3tick生成一次）
        )
        
        // 存储粒子任务
        particleTasks[victim] = particleTask
    }
}
