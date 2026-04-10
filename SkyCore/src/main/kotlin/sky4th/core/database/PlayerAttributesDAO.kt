package sky4th.core.database

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import sky4th.core.model.PlayerAttributes
import java.sql.*
import java.util.*

/**
 * 玩家属性数据访问对象（DAO）
 * 负责玩家属性的数据库操作
 */
class PlayerAttributesDAO(private val databaseManager: DatabaseManager) {

    /**
     * 获取玩家属性
     * @param uuid 玩家UUID
     * @return 玩家属性，如果不存在则返回默认属性
     */
    fun getAttributes(uuid: UUID): PlayerAttributes {
        return databaseManager.getConnection().use { conn ->
            conn.prepareStatement("""
                SELECT uuid, dodge, hunger_consumption_multiplier,
                       exp_gain_multiplier, trade_discount, forging_success_rate, talents
                FROM player_attributes WHERE uuid = ?
            """).use { stmt ->
                stmt.setString(1, uuid.toString())
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        // 解析talents JSON字段
                        val gson = Gson()
                        val talentsJson = rs.getString("talents")
                        val talentType = object : TypeToken<List<String>>() {}.type
                        val talents: List<String> = if (talentsJson != null) {
                            gson.fromJson<List<String>>(talentsJson, talentType) ?: emptyList()
                        } else {
                            emptyList()
                        }

                        PlayerAttributes(
                            uuid = UUID.fromString(rs.getString("uuid")),
                            dodge = rs.getDouble("dodge"),
                            hungerConsumptionMultiplier = rs.getDouble("hunger_consumption_multiplier"),
                            expGainMultiplier = rs.getDouble("exp_gain_multiplier"),
                            tradeDiscount = rs.getDouble("trade_discount"),
                            forgingSuccessRate = rs.getDouble("forging_success_rate"),
                            talents = talents
                        )
                    } else {
                        PlayerAttributes.createDefault(uuid)
                    }
                }
            }
        }
    }

    /**
     * 保存玩家属性
     * @param attributes 玩家属性
     */
    fun saveAttributes(attributes: PlayerAttributes) {
        databaseManager.getConnection().use { conn ->
            // 将talents列表转换为JSON字符串
            val gson = Gson()
            val talentsJson = if (attributes.talents.isNotEmpty()) {
                gson.toJson(attributes.talents)
            } else {
                null
            }

            conn.prepareStatement("""
                INSERT INTO player_attributes (
                    uuid, dodge, hunger_consumption_multiplier,
                    exp_gain_multiplier, trade_discount, forging_success_rate, talents
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    dodge = VALUES(dodge),
                    hunger_consumption_multiplier = VALUES(hunger_consumption_multiplier),
                    exp_gain_multiplier = VALUES(exp_gain_multiplier),
                    trade_discount = VALUES(trade_discount),
                    forging_success_rate = VALUES(forging_success_rate),
                    talents = VALUES(talents)
            """).use { stmt ->
                stmt.setString(1, attributes.uuid.toString())
                stmt.setDouble(2, attributes.dodge)
                stmt.setDouble(3, attributes.hungerConsumptionMultiplier)
                stmt.setDouble(4, attributes.expGainMultiplier)
                stmt.setDouble(5, attributes.tradeDiscount)
                stmt.setDouble(6, attributes.forgingSuccessRate)
                stmt.setString(7, talentsJson)
                stmt.executeUpdate()
            }
        }
    }

    /**
     * 检查玩家属性是否存在
     * @param uuid 玩家UUID
     * @return 是否存在
     */
    fun attributesExists(uuid: UUID): Boolean {
        return databaseManager.getConnection().use { conn ->
            conn.prepareStatement("SELECT 1 FROM player_attributes WHERE uuid = ?").use { stmt ->
                stmt.setString(1, uuid.toString())
                stmt.executeQuery().next()
            }
        }
    }
}
