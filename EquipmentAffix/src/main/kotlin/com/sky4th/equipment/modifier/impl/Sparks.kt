
package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import org.bukkit.Material
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack
import sky4th.core.api.MarkAPI

/**
 * 火星词条
 * 效果：攻击时对目标叠加"火星"标记，引爆时造成额外伤害并点燃目标
 * 1级：3层引爆时造成3伤害并点燃目标1秒
 * 2级：3层引爆时造成4伤害并点燃目标2秒
 * 3级：3层引爆时造成5伤害并点燃目标3秒
 */
class Sparks : com.sky4th.equipment.modifier.ConfiguredModifier("sparks") {

    companion object {
        // 每级配置：(引爆伤害, 点燃持续时间)
        private val CONFIG = arrayOf(
            3.0 to 1.0,       // 3伤害，点燃1秒
            4.0 to 2.0,       // 4伤害，点燃2秒
            5.0 to 3.0        // 5伤害，点燃3秒
        )

        // 标记持续时间（秒）
        private const val MARK_DURATION_SECONDS = 10

        // 标记最大层数
        private const val MAX_STACKS = 3

        // 标记显示物品
        // 1层使用岩浆膏
        private val MARK_ITEM_1 = ItemStack(Material.MAGMA_CREAM)
        // 2层使用火焰弹
        private val MARK_ITEM_2 = ItemStack(Material.FIRE_CHARGE)

        // 不同层数的标记ID后缀（只需要1层和2层）
        private const val MARK_SUFFIX_1 = "_1"
        private const val MARK_SUFFIX_2 = "_2"
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

        if (event.cause == org.bukkit.event.entity.EntityDamageEvent.DamageCause.FIRE_TICK) return

        // 获取受击者
        val victim = event.entity
        if (victim !is LivingEntity) {
            return
        }

        // 获取当前等级的配置
        val (triggerDamage, igniteDuration) = CONFIG.getOrNull(level - 1) ?: return

        // 获取当前标记层数
        val currentStacks = getCurrentStacks(victim)

        // 移除旧的标记
        removeMarkByStacks(victim, currentStacks)

        // 增加标记层数
        val newStacks = (currentStacks + 1).coerceAtMost(MAX_STACKS)

        // 检查是否达到最大层数
        if (newStacks >= MAX_STACKS) {
            // 达到最大层数，直接引爆标记，不创建3层标记
            // 造成额外伤害
            val damageSource = org.bukkit.damage.DamageSource.builder(org.bukkit.damage.DamageType.ON_FIRE)
                .build()
            victim.damage(triggerDamage, damageSource)

            // 点燃目标
            victim.fireTicks = (igniteDuration * 20).toInt()

            // 移除2层标记
            removeMarkByStacks(victim, 2)
        } else {
            // 未达到最大层数，创建新的火星标记（只显示一个标记，根据层数显示不同阶段）
            createMarkByStacks(victim, newStacks)
        }
    }

    /**
     * 获取当前标记层数
     * @param entity 目标实体
     * @return 当前标记层数（0-2）
     */
    private fun getCurrentStacks(entity: LivingEntity): Int {
        val affixId = getAffixId()
        return when {
            MarkAPI.hasMark(entity, "$affixId$MARK_SUFFIX_2") -> 2
            MarkAPI.hasMark(entity, "$affixId$MARK_SUFFIX_1") -> 1
            else -> 0
        }
    }

    override fun onRemove(player: Player) {}

    /**
     * 根据层数创建标记（只显示一个标记，根据层数显示不同阶段）
     * @param entity 目标实体
     * @param stacks 标记层数（1或2）
     */
    private fun createMarkByStacks(entity: LivingEntity, stacks: Int) {
        val affixId = getAffixId()
        val (markId, markItem) = when (stacks) {
            1 -> "$affixId$MARK_SUFFIX_1" to MARK_ITEM_1
            2 -> "$affixId$MARK_SUFFIX_2" to MARK_ITEM_2
            else -> return
        }
        MarkAPI.createMark(entity, markId, markItem, showToAllPlayers = true, duration = MARK_DURATION_SECONDS.toLong())
    }

    /**
     * 根据层数移除标记（只移除一个标记）
     * @param entity 目标实体
     * @param stacks 标记层数（1或2）
     */
    private fun removeMarkByStacks(entity: LivingEntity, stacks: Int) {
        val affixId = getAffixId()
        val markId = when (stacks) {
            1 -> "$affixId$MARK_SUFFIX_1"
            2 -> "$affixId$MARK_SUFFIX_2"
            else -> return
        }
        MarkAPI.removeMark(entity, markId)
    }
}
