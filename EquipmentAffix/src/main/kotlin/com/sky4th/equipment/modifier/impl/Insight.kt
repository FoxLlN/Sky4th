package com.sky4th.equipment.modifier.impl

import com.sky4th.equipment.modifier.config.PlayerRole
import sky4th.core.api.MarkAPI
import org.bukkit.Material
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * 洞悉词条
 * 效果：受到攻击时，有概率对攻击者进行标记，使其下次受到的伤害提升
 * 1级：受到攻击时，15%概率标记攻击者6秒
 * 2级：受到攻击时，20%概率标记攻击者9秒
 * 3级：受到攻击时，25%概率标记攻击者12秒
 * 高等级标记会覆盖低等级标记，同等级标记会刷新时间
 */
class Insight : com.sky4th.equipment.modifier.ConfiguredModifier("insight") {

    companion object {
        // 标记名称常量（与共鸣共用）
        const val MARK_ID_1 = "vulnerability_mark_1"
        const val MARK_ID_2 = "vulnerability_mark_2"
        const val MARK_ID_3 = "vulnerability_mark_3"
        // 所有标记ID列表
        val ALL_MARK_IDS = listOf(MARK_ID_1, MARK_ID_2, MARK_ID_3)
        // 每级配置：(触发概率，持续时间)
        private val CONFIG = arrayOf(
            0.15 to 60,      // 15%概率，持续6秒
            0.20 to 9,      // 20%概率，持续9秒
            0.25 to 12      // 25%概率，持续12秒
        )
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
        // 只处理实体攻击实体事件，且玩家必须是受害者
        if (event !is EntityDamageByEntityEvent || playerRole != PlayerRole.DEFENDER) {
            return
        }

        // 获取攻击者
        val attacker = event.damager
        if (attacker !is LivingEntity) {
            return
        }

        // 根据词条等级确定要应用的标记
        val targetMarkId = when (level) {
            1 -> MARK_ID_1
            2 -> MARK_ID_2
            3 -> MARK_ID_3
            else -> return
        }

        // 检查是否触发概率
        val (chance, duration) = CONFIG.getOrNull(level - 1) ?: return
        //if (Random.nextDouble() > chance)return
        
        // 检查是否存在更高等级的标记
        val higherLevelMarks = when (level) {
            1 -> listOf(MARK_ID_2, MARK_ID_3)  // 1级检查2级和3级标记
            2 -> listOf(MARK_ID_3)  // 2级检查3级标记
            3 -> emptyList()  // 3级是最高等级，无需检查
            else -> emptyList()
        }

        // 如果存在更高等级的标记，则不应用当前标记
        for (markId in higherLevelMarks) {
            if (MarkAPI.hasMark(attacker, markId)) {
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
            if (MarkAPI.hasMark(attacker, markId)) {
                MarkAPI.removeMark(attacker, markId)
            }
        }

        // 应用目标标记（如果已存在则刷新时间）
        MarkAPI.createMark(attacker, targetMarkId, MARK_ITEM, showToAllPlayers = true, duration = duration.toLong())
    }

    override fun onRemove(player: Player) {
    }
}
