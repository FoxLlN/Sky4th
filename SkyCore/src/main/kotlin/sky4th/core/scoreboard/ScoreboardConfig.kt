package sky4th.core.scoreboard

import java.util.*

/**
 * 计分板配置
 * 存储玩家的计分板显示偏好
 */
data class ScoreboardConfig(
    val uuid: UUID,
    val customFilters: MutableSet<String> = mutableSetOf("current_life_playtime", "credits")
) {
    companion object {
        /**
         * 创建默认配置
         */
        fun createDefault(uuid: UUID): ScoreboardConfig {
            return ScoreboardConfig(
                uuid = uuid,
                customFilters = mutableSetOf("current_life_playtime", "credits")
            )
        }
    }
}
