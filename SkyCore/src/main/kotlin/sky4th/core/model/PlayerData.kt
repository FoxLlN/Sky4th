package sky4th.core.model

import java.util.*

/**
 * 完整的玩家数据模型
 * 包含所有玩家相关的信息
 */
data class PlayerData(
    val identity: PlayerIdentity,
    var economy: PlayerEconomy,
    var locations: PlayerLocations
) {
    companion object {
        /**
         * 创建新玩家的默认数据
         */
        fun createNew(uuid: UUID, username: String): PlayerData {
            val now = java.time.Instant.now()
            return PlayerData(
                identity = PlayerIdentity(
                    uuid = uuid,
                    username = username,
                    firstLogin = now,
                    lastLogin = now,
                    playTime = java.time.Duration.ZERO
                ),
                economy = PlayerEconomy(),
                locations = PlayerLocations()
            )
        }
    }
}
