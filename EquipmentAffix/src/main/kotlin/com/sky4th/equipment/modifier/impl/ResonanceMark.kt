package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import sky4th.core.api.MarkAPI
import org.bukkit.Material
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack

/**
 * 共鸣标记词条
 * 效果：攻击时，根据词条等级给目标打上易伤标记
 * 1级：给目标打上1级易伤标记，持续5秒
 * 2级：给目标打上2级易伤标记，持续5秒
 * 3级：给目标打上3级易伤标记，持续5秒
 * 高等级标记会覆盖低等级标记，同等级标记会刷新时间
 */
class ResonanceMark : com.sky4th.equipment.modifier.ConfiguredModifier("resonance_mark") {

    companion object {
        // 标记名称常量（支持1/2/3级）
        const val MARK_ID_1 = "vulnerability_mark_1"
        const val MARK_ID_2 = "vulnerability_mark_2"
        const val MARK_ID_3 = "vulnerability_mark_3"
        // 所有标记ID列表
        val ALL_MARK_IDS = listOf(MARK_ID_1, MARK_ID_2, MARK_ID_3)
        // 标记持续时间（秒）
        private const val MARK_DURATION_SECONDS = 5
        // 标记显示物品（紫水晶碎片）
        private val MARK_ITEM = ItemStack(Material.AMETHYST_SHARD)
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

        // 根据词条等级确定要应用的标记
        val targetMarkId = when (level) {
            1 -> MARK_ID_1
            2 -> MARK_ID_2
            3 -> MARK_ID_3
            else -> return
        }

        // 检查是否存在更高等级的标记
        val higherLevelMarks = when (level) {
            1 -> listOf(MARK_ID_2, MARK_ID_3)  // 1级检查2级和3级标记
            2 -> listOf(MARK_ID_3)  // 2级检查3级标记
            3 -> emptyList()  // 3级是最高等级，无需检查
            else -> emptyList()
        }

        // 如果存在更高等级的标记，则不应用当前标记
        for (markId in higherLevelMarks) {
            if (MarkAPI.hasMark(victim, markId)) {
                return
            }
        }

        // 移除比当前等级低的标记（高等级标记会覆盖低等级标记）
        val marksToRemove = when (level) {
            1 -> emptyList()  // 1级不移除任何标记
            2 -> listOf(MARK_ID_1)  // 2级移除1级标记
            3 -> listOf(MARK_ID_1, MARK_ID_2)  // 3级移除1级和2级标记
            else -> emptyList()
        }
        for (markId in marksToRemove) {
            if (MarkAPI.hasMark(victim, markId)) {
                MarkAPI.removeMark(victim, markId)
            }
        }

        // 应用目标标记（如果已存在则刷新时间）
        MarkAPI.createMark(victim, targetMarkId, MARK_ITEM, showToAllPlayers = true, duration = MARK_DURATION_SECONDS.toLong())
    }

    override fun onRemove(player: Player) {
    }
}
