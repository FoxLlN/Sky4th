package sky4th.core.database

import java.sql.Connection

/**
 * 数据库表结构定义
 * 集中管理所有数据库表的初始化和结构定义
 */
object DatabaseSchema {

    /**
     * 初始化所有数据库表
     */
    fun initializeAllTables(conn: Connection) {
        conn.autoCommit = false
        try {
            // 玩家身份表
            createPlayerIdentityTable(conn)

            // 玩家经济表
            createPlayerEconomyTable(conn)

            // 玩家位置表
            createPlayerLocationsTable(conn)

            // 玩家仓库表
            createPlayerStorageTable(conn)

            // 地牢科技树等级表
            createDungeonTechTable(conn)

            // 玩家属性表
            createPlayerAttributesTable(conn)

            // 玩家权限表
            createPlayerPermissionsTable(conn)

            // 村落数据表
            createVillageDataTable(conn)

            conn.commit()
        } catch (e: Exception) {
            conn.rollback()
            throw e
        }
    }

    /**
     * 创建玩家身份表
     */
    private fun createPlayerIdentityTable(conn: Connection) {
        conn.prepareStatement("""
            CREATE TABLE IF NOT EXISTS player_identity (
                uuid VARCHAR(36) PRIMARY KEY,
                username VARCHAR(16) NOT NULL,
                first_login BIGINT NOT NULL,
                last_login BIGINT NOT NULL,
                play_time_minutes BIGINT NOT NULL DEFAULT 0,
                INDEX idx_username (username)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """).execute()
    }

    /**
     * 创建玩家经济表
     */
    private fun createPlayerEconomyTable(conn: Connection) {
        conn.prepareStatement("""
            CREATE TABLE IF NOT EXISTS player_economy (
                uuid VARCHAR(36) PRIMARY KEY,
                credits DOUBLE NOT NULL DEFAULT 100.0,
                daily_earned DOUBLE NOT NULL DEFAULT 0.0,
                daily_spent DOUBLE NOT NULL DEFAULT 0.0,
                daily_limit DOUBLE NOT NULL DEFAULT 1000.0,
                total_earned DOUBLE NOT NULL DEFAULT 0.0,
                total_spent DOUBLE NOT NULL DEFAULT 0.0,
                FOREIGN KEY (uuid) REFERENCES player_identity(uuid) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """).execute()
    }

    /**
     * 创建玩家位置表
     */
    private fun createPlayerLocationsTable(conn: Connection) {
        conn.prepareStatement("""
            CREATE TABLE IF NOT EXISTS player_locations (
                uuid VARCHAR(36) PRIMARY KEY,
                last_location_world VARCHAR(255),
                last_location_x DOUBLE,
                last_location_y DOUBLE,
                last_location_z DOUBLE,
                last_location_yaw FLOAT,
                last_location_pitch FLOAT,
                base_location_world VARCHAR(255),
                base_location_x DOUBLE,
                base_location_y DOUBLE,
                base_location_z DOUBLE,
                base_location_yaw FLOAT,
                base_location_pitch FLOAT,
                FOREIGN KEY (uuid) REFERENCES player_identity(uuid) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """).execute()
    }

    /**
     * 创建玩家仓库表
     */
    private fun createPlayerStorageTable(conn: Connection) {
        conn.prepareStatement("""
            CREATE TABLE IF NOT EXISTS player_storage (
                uuid VARCHAR(36) NOT NULL,
                slot_index INT NOT NULL,
                entry_type VARCHAR(10) NOT NULL,
                loadout_id VARCHAR(64),
                count INT NOT NULL DEFAULT 1,
                durability INT,
                item_data TEXT,
                PRIMARY KEY (uuid, slot_index),
                INDEX idx_uuid (uuid),
                FOREIGN KEY (uuid) REFERENCES player_identity(uuid) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """).execute()
    }

    /**
     * 创建地牢科技树等级表
     */
    private fun createDungeonTechTable(conn: Connection) {
        conn.prepareStatement("""
            CREATE TABLE IF NOT EXISTS dungeon_tech (
                uuid VARCHAR(36) PRIMARY KEY,
                tech_level INT NOT NULL DEFAULT 0,
                FOREIGN KEY (uuid) REFERENCES player_identity(uuid) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """).execute()
    }

    /**
     * 创建玩家属性表
     */
    private fun createPlayerAttributesTable(conn: Connection) {
        conn.prepareStatement("""
            CREATE TABLE IF NOT EXISTS player_attributes (
                uuid VARCHAR(36) PRIMARY KEY,
                max_health DOUBLE NOT NULL DEFAULT 20.0,
                armor DOUBLE NOT NULL DEFAULT 0.0,
                dodge DOUBLE NOT NULL DEFAULT 0.0,
                knockback_resistance DOUBLE NOT NULL DEFAULT 0.0,
                hunger_consumption_multiplier DOUBLE NOT NULL DEFAULT 1.0,
                movement_speed_multiplier DOUBLE NOT NULL DEFAULT 1.0,
                exp_gain_multiplier DOUBLE NOT NULL DEFAULT 1.0,
                trade_discount DOUBLE NOT NULL DEFAULT 0.0,
                forging_success_rate DOUBLE NOT NULL DEFAULT 0.0,
                attack_damage DOUBLE NOT NULL DEFAULT 0.0,
                attack_speed DOUBLE NOT NULL DEFAULT 0.0,
                armor_toughness DOUBLE NOT NULL DEFAULT 0.0,
                luck DOUBLE NOT NULL DEFAULT 0.0,
                fall_damage_multiplier DOUBLE NOT NULL DEFAULT 1.0,
                max_absorption DOUBLE NOT NULL DEFAULT 0.0,
                safe_fall_distance DOUBLE NOT NULL DEFAULT 0.0,
                scale DOUBLE NOT NULL DEFAULT 1.0,
                step_height DOUBLE NOT NULL DEFAULT 0.6,
                gravity DOUBLE NOT NULL DEFAULT 0.08,
                jump_strength DOUBLE NOT NULL DEFAULT 0.42,
                burning_time DOUBLE NOT NULL DEFAULT 0.0,
                explosion_knockback_resistance DOUBLE NOT NULL DEFAULT 0.0,
                movement_efficiency DOUBLE NOT NULL DEFAULT 0.0,
                oxygen_bonus DOUBLE NOT NULL DEFAULT 0.0,
                water_movement_efficiency DOUBLE NOT NULL DEFAULT 0.0,
                block_break_speed DOUBLE NOT NULL DEFAULT 1.0,
                block_interaction_range DOUBLE NOT NULL DEFAULT 4.5,
                entity_interaction_range DOUBLE NOT NULL DEFAULT 3.0,
                sneaking_speed DOUBLE NOT NULL DEFAULT 0.3,
                submerged_mining_speed DOUBLE NOT NULL DEFAULT 0.2,
                sweeping_damage_ratio DOUBLE NOT NULL DEFAULT 0.0,
                talents JSON DEFAULT NULL,
                FOREIGN KEY (uuid) REFERENCES player_identity(uuid) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """).execute()
    }

    /**
     * 创建玩家权限表
     */
    private fun createPlayerPermissionsTable(conn: Connection) {
        conn.prepareStatement("""
            CREATE TABLE IF NOT EXISTS player_permissions (
                uuid VARCHAR(36) NOT NULL,
                name VARCHAR(255) NOT NULL,
                PRIMARY KEY (uuid, name),
                INDEX idx_uuid (uuid),
                FOREIGN KEY (uuid) REFERENCES player_identity(uuid) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """).execute()
    }

    /**
     * 创建村落数据表
     */
    private fun createVillageDataTable(conn: Connection) {
        conn.prepareStatement("""
            CREATE TABLE IF NOT EXISTS village_data (
                id VARCHAR(36) PRIMARY KEY,
                world VARCHAR(64) NOT NULL,
                chunk_x INT NOT NULL,
                chunk_z INT NOT NULL,
                min_x INT NOT NULL,
                min_y INT NOT NULL,
                min_z INT NOT NULL,
                max_x INT NOT NULL,
                max_y INT NOT NULL,
                max_z INT NOT NULL,
                level INT NOT NULL DEFAULT 1,
                last_raid_time BIGINT NOT NULL DEFAULT 0,
                last_loot_time BIGINT NOT NULL DEFAULT 0,
                allied_teams TEXT,
                hostile_teams TEXT,
                baby_villager_count INT NOT NULL DEFAULT 0,
                unemployed_villager_count INT NOT NULL DEFAULT 0,
                level1_villager_count INT NOT NULL DEFAULT 0,
                level2_villager_count INT NOT NULL DEFAULT 0,
                level3_villager_count INT NOT NULL DEFAULT 0,
                level4_villager_count INT NOT NULL DEFAULT 0,
                level5_villager_count INT NOT NULL DEFAULT 0,
                INDEX idx_world_chunk (world, chunk_x, chunk_z)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """).execute()
    }
}
