package sky4th.core.model

import java.time.Instant
import java.time.Duration
import java.util.*

/**
 * 玩家身份信息
 */
data class PlayerIdentity(
    val uuid: UUID,
    val username: String,
    val firstLogin: Instant,          // 首次登录时间
    val lastLogin: Instant,           // 上次登录时间
    val playTime: Duration = Duration.ZERO,           // 总游戏时长
    val todayPlayTime: Duration = Duration.ZERO,  // 今日游戏时长
    val currentLifePlayTime: Duration = Duration.ZERO,  // 当前生命游戏时长
    val lastLifeStartTime: Instant? = null  // 当前生命开始时间（用于计算单命时长）
) {
    companion object {
        /**
         * 将Duration格式化为可读字符串
         */
        fun formatDuration(duration: Duration): String {
            val seconds = duration.seconds
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val secs = seconds % 60
            return String.format("%02d:%02d:%02d", hours, minutes, secs)
        }
    }
}
