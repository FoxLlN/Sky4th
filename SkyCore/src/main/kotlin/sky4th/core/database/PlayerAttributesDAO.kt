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
                SELECT * FROM player_attributes WHERE uuid = ?
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
                            maxHealth = rs.getDouble("max_health"),
                            armor = rs.getDouble("armor"),
                            dodge = rs.getDouble("dodge"),
                            knockbackResistance = rs.getDouble("knockback_resistance"),
                            hungerConsumptionMultiplier = rs.getDouble("hunger_consumption_multiplier"),
                            movementSpeedMultiplier = rs.getDouble("movement_speed_multiplier"),
                            expGainMultiplier = rs.getDouble("exp_gain_multiplier"),
                            tradeDiscount = rs.getDouble("trade_discount"),
                            forgingSuccessRate = rs.getDouble("forging_success_rate"),
                            attackDamage = rs.getDouble("attack_damage"),
                            attackSpeed = rs.getDouble("attack_speed"),
                            armorToughness = rs.getDouble("armor_toughness"),
                            luck = rs.getDouble("luck"),
                            fallDamageMultiplier = rs.getDouble("fall_damage_multiplier"),
                            maxAbsorption = rs.getDouble("max_absorption"),
                            safeFallDistance = rs.getDouble("safe_fall_distance"),
                            scale = rs.getDouble("scale"),
                            stepHeight = rs.getDouble("step_height"),
                            gravity = rs.getDouble("gravity"),
                            jumpStrength = rs.getDouble("jump_strength"),
                            burningTime = rs.getDouble("burning_time"),
                            explosionKnockbackResistance = rs.getDouble("explosion_knockback_resistance"),
                            movementEfficiency = rs.getDouble("movement_efficiency"),
                            oxygenBonus = rs.getDouble("oxygen_bonus"),
                            waterMovementEfficiency = rs.getDouble("water_movement_efficiency"),
                            blockBreakSpeed = rs.getDouble("block_break_speed"),
                            blockInteractionRange = rs.getDouble("block_interaction_range"),
                            entityInteractionRange = rs.getDouble("entity_interaction_range"),
                            sneakingSpeed = rs.getDouble("sneaking_speed"),
                            submergedMiningSpeed = rs.getDouble("submerged_mining_speed"),
                            sweepingDamageRatio = rs.getDouble("sweeping_damage_ratio"),
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
                    uuid, max_health, armor, dodge, knockback_resistance,
                    hunger_consumption_multiplier, movement_speed_multiplier,
                    exp_gain_multiplier, trade_discount, forging_success_rate,
                    attack_damage, attack_speed, armor_toughness, luck,
                    fall_damage_multiplier, max_absorption, safe_fall_distance,
                    scale, step_height, gravity, jump_strength, burning_time,
                    explosion_knockback_resistance, movement_efficiency, oxygen_bonus,
                    water_movement_efficiency, block_break_speed, block_interaction_range,
                    entity_interaction_range, sneaking_speed, submerged_mining_speed,
                    sweeping_damage_ratio, talents
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    max_health = VALUES(max_health),
                    armor = VALUES(armor),
                    dodge = VALUES(dodge),
                    knockback_resistance = VALUES(knockback_resistance),
                    hunger_consumption_multiplier = VALUES(hunger_consumption_multiplier),
                    movement_speed_multiplier = VALUES(movement_speed_multiplier),
                    exp_gain_multiplier = VALUES(exp_gain_multiplier),
                    trade_discount = VALUES(trade_discount),
                    forging_success_rate = VALUES(forging_success_rate),
                    attack_damage = VALUES(attack_damage),
                    attack_speed = VALUES(attack_speed),
                    armor_toughness = VALUES(armor_toughness),
                    luck = VALUES(luck),
                    fall_damage_multiplier = VALUES(fall_damage_multiplier),
                    max_absorption = VALUES(max_absorption),
                    safe_fall_distance = VALUES(safe_fall_distance),
                    scale = VALUES(scale),
                    step_height = VALUES(step_height),
                    gravity = VALUES(gravity),
                    jump_strength = VALUES(jump_strength),
                    burning_time = VALUES(burning_time),
                    explosion_knockback_resistance = VALUES(explosion_knockback_resistance),
                    movement_efficiency = VALUES(movement_efficiency),
                    oxygen_bonus = VALUES(oxygen_bonus),
                    water_movement_efficiency = VALUES(water_movement_efficiency),
                    block_break_speed = VALUES(block_break_speed),
                    block_interaction_range = VALUES(block_interaction_range),
                    entity_interaction_range = VALUES(entity_interaction_range),
                    sneaking_speed = VALUES(sneaking_speed),
                    submerged_mining_speed = VALUES(submerged_mining_speed),
                    sweeping_damage_ratio = VALUES(sweeping_damage_ratio),
                    talents = VALUES(talents)
            """).use { stmt ->
                stmt.setString(1, attributes.uuid.toString())
                stmt.setDouble(2, attributes.maxHealth)
                stmt.setDouble(3, attributes.armor)
                stmt.setDouble(4, attributes.dodge)
                stmt.setDouble(5, attributes.knockbackResistance)
                stmt.setDouble(6, attributes.hungerConsumptionMultiplier)
                stmt.setDouble(7, attributes.movementSpeedMultiplier)
                stmt.setDouble(8, attributes.expGainMultiplier)
                stmt.setDouble(9, attributes.tradeDiscount)
                stmt.setDouble(10, attributes.forgingSuccessRate)
                stmt.setDouble(11, attributes.attackDamage)
                stmt.setDouble(12, attributes.attackSpeed)
                stmt.setDouble(13, attributes.armorToughness)
                stmt.setDouble(14, attributes.luck)
                stmt.setDouble(15, attributes.fallDamageMultiplier)
                stmt.setDouble(16, attributes.maxAbsorption)
                stmt.setDouble(17, attributes.safeFallDistance)
                stmt.setDouble(18, attributes.scale)
                stmt.setDouble(19, attributes.stepHeight)
                stmt.setDouble(20, attributes.gravity)
                stmt.setDouble(21, attributes.jumpStrength)
                stmt.setDouble(22, attributes.burningTime)
                stmt.setDouble(23, attributes.explosionKnockbackResistance)
                stmt.setDouble(24, attributes.movementEfficiency)
                stmt.setDouble(25, attributes.oxygenBonus)
                stmt.setDouble(26, attributes.waterMovementEfficiency)
                stmt.setDouble(27, attributes.blockBreakSpeed)
                stmt.setDouble(28, attributes.blockInteractionRange)
                stmt.setDouble(29, attributes.entityInteractionRange)
                stmt.setDouble(30, attributes.sneakingSpeed)
                stmt.setDouble(31, attributes.submergedMiningSpeed)
                stmt.setDouble(32, attributes.sweepingDamageRatio)
                stmt.setString(33, talentsJson)
                stmt.executeUpdate()
            }
        }
    }

    /**
     * 更新单个属性
     * @param uuid 玩家UUID
     * @param attributeName 属性名称
     * @param value 新值
     */
    fun updateAttribute(uuid: UUID, attributeName: String, value: Double) {
        databaseManager.getConnection().use { conn ->
            conn.prepareStatement("""
                UPDATE player_attributes SET $attributeName = ? WHERE uuid = ?
            """).use { stmt ->
                stmt.setDouble(1, value)
                stmt.setString(2, uuid.toString())
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
