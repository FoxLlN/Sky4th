
package sky4th.core.scoreboard

import java.util.*

/**
 * 计分板配置
 * 存储玩家的计分板显示偏好
 */
data class ScoreboardConfig(
    val uuid: UUID,
    val showTitle: Boolean = true,
    val showTodayPlayTime: Boolean = false,
    val showCurrentLifePlayTime: Boolean = true,
    val showTotalPlayTime: Boolean = false,
    val showQQGroup: Boolean = true,
    val showFooter: Boolean = true
) {
    companion object {
        /**
         * 创建默认配置
         */
        fun createDefault(uuid: UUID): ScoreboardConfig {
            return ScoreboardConfig(
                uuid = uuid,
                showTitle = true,
                showTodayPlayTime = false,
                showCurrentLifePlayTime = true,
                showTotalPlayTime = false,
                showQQGroup = true,
                showFooter = true
            )
        }
    }
}
