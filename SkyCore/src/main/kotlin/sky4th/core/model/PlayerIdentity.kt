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
    val playTime: Duration,           // 总游戏时长（分钟）
    val todayPlayTime: Duration = Duration.ZERO,  // 今日游戏时长（分钟）
    val currentLifePlayTime: Duration = Duration.ZERO,  // 当前生命游戏时长（分钟）
    val lastLifeStartTime: Instant? = null  // 当前生命开始时间（用于计算单命时长）
)
