package sky4th.dungeon.loadout.equipment

import java.util.*

/**
 * 配装套装主动技能「本局已使用」状态，供计分板等读取。
 * 每个玩家在一局游戏中只能使用一个套装的主动技能，一旦使用了某个套装的技能，就不能再使用其他套装的技能。
 * 如铁壁：每局 2 次，离开副本时清空。
 * 如斥候：每局 2 次，激活中不可重复使用，离开副本时清空。
 * 如学者：每局 2 次，离开副本时清空。
 */
object LoadoutSetSkillState {
    /** 玩家本局已使用的套装ID（每个玩家只能使用一个套装的主动技能） */
    private val playerUsedSetId: MutableMap<UUID, String> = mutableMapOf()

    /** 铁壁主动技能每局 2 次，记录已使用次数 */
    private val tiebiSkillUsedCount: MutableMap<UUID, Int> = mutableMapOf()
    private const val TIEBI_MAX_USES = 2

    /** 铁壁主动技能抗性效果结束时间（毫秒时间戳），用于计分板显示剩余时间 */
    private val tiebiSkillEndTime: MutableMap<UUID, Long> = mutableMapOf()

    fun getTiebiSkillRemaining(playerId: UUID): Int =
        (TIEBI_MAX_USES - (tiebiSkillUsedCount[playerId] ?: 0)).coerceIn(0, TIEBI_MAX_USES)

    fun isTiebiSkillUsed(playerId: UUID): Boolean = (tiebiSkillUsedCount[playerId] ?: 0) > 0

    /** 记录铁壁技能使用时的效果结束时间（当前时间 + 40 秒） */
    fun setTiebiSkillEndTime(playerId: UUID, endTimeMillis: Long) {
        tiebiSkillEndTime[playerId] = endTimeMillis
    }

    /** 铁壁主动技能抗性剩余秒数，未使用或已过期返回 0 */
    fun getTiebiSkillRemainingSeconds(playerId: UUID): Int {
        val end = tiebiSkillEndTime[playerId] ?: return 0
        val remaining = (end - System.currentTimeMillis()) / 1000
        return remaining.toInt().coerceAtLeast(0)
    }

    /** 检查铁壁技能效果是否正在生效 */
    fun isTiebiSkillActive(playerId: UUID): Boolean {
        val end = tiebiSkillEndTime[playerId] ?: return false
        return System.currentTimeMillis() < end
    }

    /** 检查是否有任何套装技能效果正在生效 */
    fun isAnySetSkillActive(playerId: UUID): Boolean {
        return isTiebiSkillActive(playerId) ||
               chikeQualityBoost.contains(playerId) ||
               youxiaRangedBoost.contains(playerId)
    }

    fun useTiebiSkill(playerId: UUID): Boolean {
        val used = tiebiSkillUsedCount[playerId] ?: 0
        if (used >= TIEBI_MAX_USES) return false
        tiebiSkillUsedCount[playerId] = used + 1
        return true
    }

    fun clearTiebiSkillUsed(playerId: UUID) {
        tiebiSkillUsedCount.remove(playerId)
        tiebiSkillEndTime.remove(playerId)
    }

    /** 检查玩家是否可以使用指定套装的技能（未使用过任何套装，或使用的是同一套装） */
    fun canUseSetSkill(playerId: UUID, setId: String): Boolean {
        val usedSetId = playerUsedSetId[playerId]
        return usedSetId == null || usedSetId == setId
    }

    /** 记录玩家使用了指定套装的技能 */
    fun recordSetSkillUse(playerId: UUID, setId: String) {
        if (playerUsedSetId[playerId] == null) {
            playerUsedSetId[playerId] = setId
        }
    }

    /** 清理玩家的套装使用记录（离开副本时调用） */
    fun clearPlayerSetSkill(playerId: UUID) {
        playerUsedSetId.remove(playerId)
    }

    /** 斥候主动技能每局 2 次，记录已使用次数 */
    private val chikeSkillUsedCount: MutableMap<UUID, Int> = mutableMapOf()
    private const val CHIKE_MAX_USES = 2

    fun getChikeSkillRemaining(playerId: UUID): Int =
        (CHIKE_MAX_USES - (chikeSkillUsedCount[playerId] ?: 0)).coerceIn(0, CHIKE_MAX_USES)

    fun useChikeSkill(playerId: UUID): Boolean {
        val used = chikeSkillUsedCount[playerId] ?: 0
        if (used >= CHIKE_MAX_USES) return false
        chikeSkillUsedCount[playerId] = used + 1
        return true
    }

    fun clearChikeSkillUsed(playerId: UUID) {
        chikeSkillUsedCount.remove(playerId)
        chikeQualityBoost.remove(playerId)
    }

    /** 斥候：下一次搜索高品质物品概率提升（触发主动技能时设置，开始搜索时消耗）；处于激活中时不可再次使用同名技能 */
    val chikeQualityBoost: MutableSet<UUID> = mutableSetOf()

    /** 学者主动技能每局 2 次，记录已使用次数 */
    private val xuezheSkillUsedCount: MutableMap<UUID, Int> = mutableMapOf()
    private const val XUEZHE_MAX_USES = 2

    fun getXuezheSkillRemaining(playerId: UUID): Int =
        (XUEZHE_MAX_USES - (xuezheSkillUsedCount[playerId] ?: 0)).coerceIn(0, XUEZHE_MAX_USES)

    fun useXuezheSkill(playerId: UUID): Boolean {
        val used = xuezheSkillUsedCount[playerId] ?: 0
        if (used >= XUEZHE_MAX_USES) return false
        xuezheSkillUsedCount[playerId] = used + 1
        return true
    }

    fun clearXuezheSkillUsed(playerId: UUID) {
        xuezheSkillUsedCount.remove(playerId)
    }

    /** 游侠主动技能每局 2 次，记录已使用次数 */
    private val youxiaSkillUsedCount: MutableMap<UUID, Int> = mutableMapOf()
    private const val YOUXIA_MAX_USES = 2

    fun getYouxiaSkillRemaining(playerId: UUID): Int =
        (YOUXIA_MAX_USES - (youxiaSkillUsedCount[playerId] ?: 0)).coerceIn(0, YOUXIA_MAX_USES)

    fun useYouxiaSkill(playerId: UUID): Boolean {
        val used = youxiaSkillUsedCount[playerId] ?: 0
        if (used >= YOUXIA_MAX_USES) return false
        youxiaSkillUsedCount[playerId] = used + 1
        return true
    }

    fun clearYouxiaSkillUsed(playerId: UUID) {
        youxiaSkillUsedCount.remove(playerId)
        youxiaRangedBoost.remove(playerId)
    }

    /** 游侠：下次远程伤害+120%（触发主动技能时设置，造成远程伤害时消耗） */
    val youxiaRangedBoost: MutableSet<UUID> = mutableSetOf()

    /** 萨满主动技能每局 2 次，记录已使用次数 */
    private val samanSkillUsedCount: MutableMap<UUID, Int> = mutableMapOf()
    private const val SAMAN_MAX_USES = 2

    fun getSamanSkillRemaining(playerId: UUID): Int =
        (SAMAN_MAX_USES - (samanSkillUsedCount[playerId] ?: 0)).coerceIn(0, SAMAN_MAX_USES)

    fun useSamanSkill(playerId: UUID): Boolean {
        val used = samanSkillUsedCount[playerId] ?: 0
        if (used >= SAMAN_MAX_USES) return false
        samanSkillUsedCount[playerId] = used + 1
        return true
    }

    fun clearSamanSkillUsed(playerId: UUID) {
        samanSkillUsedCount.remove(playerId)
    }
}
