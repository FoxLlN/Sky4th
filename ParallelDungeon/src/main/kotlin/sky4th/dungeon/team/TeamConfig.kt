
package sky4th.dungeon.team

/**
 * 队伍相关配置
 */
data class TeamConfig(
    val maxTeamSize: Int = 3, // 最大队伍人数
    val helpDistance: Int = 32, // 呼救/救援距离
    val maxDeathCount: Int = 3, // 最大死亡次数
    val helpDuration: Int = 5, // 救援需要的时间（秒）
    val downDuration: Int = 120 // 倒地持续时间（秒）
)
