
package sky4th.dungeon.config

/**
 * 容器物品数量范围配置
 *
 * @property volumeRange 容器体积范围（格式："min-max"）
 * @property minItems 最小物品数量
 * @property maxItems 最大物品数量
 */
data class ContainerItemCountConfig(
    val volumeRange: String,
    val minItems: Int,
    val maxItems: Int
) {
    init {
        require(minItems >= 0) { "minItems must be non-negative" }
        require(maxItems >= minItems) { "maxItems must be greater than or equal to minItems" }
        require(volumeRange.isNotBlank()) { "volumeRange must not be blank" }
    }

    /**
     * 检查容器体积是否匹配此配置
     * @param volume 容器体积
     * @return 是否匹配
     */
    fun matches(volume: Int): Boolean {
        val parts = volumeRange.split("-")
        if (parts.size != 2) return false
        val min = parts[0].toIntOrNull() ?: return false
        val max = parts[1].toIntOrNull() ?: return false
        return volume in min..max
    }
}

/**
 * 出生点配置
 *
 * @property x X坐标
 * @property y Y坐标
 * @property z Z坐标
 * @property name 出生点名称
 */
data class SpawnPoint(
    val x: Double,
    val y: Double,
    val z: Double,
    val name: String = ""
) {
    /**
     * 转换为Location对象
     * @param world 世界对象，可为null
     * @return Location对象
     */
    fun toLocation(world: org.bukkit.World?): org.bukkit.Location {
        return org.bukkit.Location(world, x, y, z)
    }
}

/**
 * 区域配置
 *
 * @property minX 最小X坐标
 * @property minY 最小Y坐标
 * @property minZ 最小Z坐标
 * @property maxX 最大X坐标
 * @property maxY 最大Y坐标
 * @property maxZ 最大Z坐标
 * @property world 世界名称
 * @property name 区域名称
 */
data class Region(
    val minX: Int,
    val minY: Int,
    val minZ: Int,
    val maxX: Int,
    val maxY: Int,
    val maxZ: Int,
    val world: String,
    val name: String = ""
) {
    init {
        require(minX <= maxX) { "minX must be less than or equal to maxX" }
        require(minY <= maxY) { "minY must be less than or equal to maxY" }
        require(minZ <= maxZ) { "minZ must be less than or equal to maxZ" }
    }

    /**
     * 检查位置是否在区域内
     * @param location 位置
     * @return 是否在区域内
     */
    fun contains(location: org.bukkit.Location): Boolean {
        val locationWorld = location.world?.name ?: return false
        // 检查世界名称是否匹配
        // 支持两种匹配方式：
        // 1. 完全匹配：world 名称与配置的 world 完全相同
        // 2. 前缀匹配：world 名称以配置的 world 开头（例如配置的 world 是 "dungeon1"，实际世界是 "dungeon1_1_world"）
        val worldMatches = locationWorld == world || locationWorld.startsWith("${world}_")
        if (!worldMatches) return false

        val x = location.blockX
        val y = location.blockY
        val z = location.blockZ
        return x in minX..maxX && y in minY..maxY && z in minZ..maxZ
    }
}

/**
 * 怪物生成点配置
 *
 * @property id 生成点ID
 * @property monsterId 怪物ID
 * @property x X坐标
 * @property y Y坐标
 * @property z Z坐标
 * @property count 生成数量
 * @property name 生成点名称
 */
data class MonsterSpawnConfig(
    val id: String,
    val monsterId: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val count: Int = 1,
    val name: String = ""
) {
    init {
        require(id.isNotBlank()) { "id must not be blank" }
        require(monsterId.isNotBlank()) { "monsterId must not be blank" }
        require(count > 0) { "count must be positive" }
    }

    /**
     * 将配置转换为Location对象
     * @param world 世界对象
     * @return Location对象
     */
    fun toLocation(world: org.bukkit.World?): org.bukkit.Location {
        return org.bukkit.Location(world, x, y, z)
    }
}

/**
 * 科技树等级加成配置
 *
 * @property health 生命值加成（半心数）
 * @property playerDamageReduction 玩家受到的伤害减免（0.0-1.0）
 */
data class TechLevelBonus(
    val health: Int = 0,
    val playerDamageReduction: Double = 0.0
) {
    init {
        require(health >= 0) { "health must be non-negative" }
        require(playerDamageReduction in 0.0..1.0) { "playerDamageReduction must be in [0, 1]" }
    }
}

/**
 * 容器配置
 *
 * @property id 容器ID
 * @property region 容器所在区域
 * @property level 容器等级
 * @property name 容器名称
 * @property texture 容器UI头颅纹理
 */
data class ContainerConfig(
    val id: String,
    val region: Region,
    val level: Int = 1,
    val name: String = "",
    val texture: String = ""
) {
    init {
        require(id.isNotBlank()) { "id must not be blank" }
        require(level > 0) { "level must be positive" }
    }
}

/**
 * 现金物品配置
 *
 * @property id 物品ID
 * @property name 物品名称
 * @property description 物品描述
 * @property texture 物品头颅纹理
 * @property minCredits 最小信用点数
 * @property maxCredits 最大信用点数
 */
data class CashItemConfig(
    val id: String,
    val name: String,
    val description: String,
    val texture: String,
    val minCredits: Int,
    val maxCredits: Int
) {
    init {
        require(id.isNotBlank()) { "id must not be blank" }
        require(name.isNotBlank()) { "name must not be blank" }
        require(texture.isNotBlank()) { "texture must not be blank" }
        require(minCredits >= 0) { "minCredits must be non-negative" }
        require(maxCredits >= minCredits) { "maxCredits must be greater than or equal to minCredits" }
    }
}

/**
 * 怪物头颅配置
 *
 * @property monsterId 怪物ID
 * @property headType 头颅类型（standard/custom）
 * @property material 原版材质（当headType为standard时使用）
 * @property texture 自定义纹理（当headType为custom时使用）
 * @property displayName 显示名称
 */
data class MonsterHeadConfig(
    val monsterId: String,
    val headType: String,
    val material: String = "",
    val texture: String = "",
    val displayName: String
) {
    init {
        require(monsterId.isNotBlank()) { "monsterId must not be blank" }
        require(displayName.isNotBlank()) { "displayName must not be blank" }
        require(headType in listOf("standard", "custom")) { "headType must be 'standard' or 'custom'" }
        if (headType == "standard") {
            require(material.isNotBlank()) { "material must not be blank when headType is 'standard'" }
        } else {
            require(texture.isNotBlank()) { "texture must not be blank when headType is 'custom'" }
        }
    }
}

/**
 * 掉落物配置
 *
 * @property id 物品ID
 * @property name 物品名称
 * @property description 物品描述
 * @property descriptionLore 物品描述Lore
 * @property tier 品级
 * @property price 价格
 * @property material 材质
 * @param isshop 是否在商店出售
 * @param minAmount 最小数量
 * @param maxAmount 最大数量
 * @param cashId 关联的现金物品ID
 */
data class LootItemConfig(
    val id: String,
    val name: String,
    val description: String,
    val descriptionLore: List<String>,
    val tier: String,
    val price: Int,
    val material: String,
    val isshop: Boolean = false,
    val minAmount: Int = 1,
    val maxAmount: Int = 1,
    val cashId: String = ""
) {
    init {
        require(id.isNotBlank()) { "id must not be blank" }
        require(name.isNotBlank()) { "name must not be blank" }
        // 只有当 cashId 为空时，material 才必须不为空
        if (cashId.isEmpty()) {
            require(material.isNotBlank()) { "material must not be blank when cashId is empty" }
        }
        require(price >= 0) { "price must be non-negative" }
        require(minAmount > 0) { "minAmount must be positive" }
        require(maxAmount >= minAmount) { "maxAmount must be greater than or equal to minAmount" }
    }
}
