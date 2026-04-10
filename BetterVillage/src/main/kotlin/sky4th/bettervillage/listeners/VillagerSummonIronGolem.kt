package sky4th.bettervillage.listeners

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import org.bukkit.entity.IronGolem
import org.bukkit.event.Listener
import org.bukkit.event.EventPriority
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.persistence.PersistentDataType
import sky4th.bettervillage.BetterVillage
import sky4th.bettervillage.manager.VillageManager

/**
 * 村民铁傀儡保护监听器
 * 职责：
 * 1. 监听村民被攻击事件
 * 2. 如果村民被攻击，则召唤铁傀儡保护村民
 */
class VillagerSummonIronGolemListener : Listener {
    
    // 铁傀儡生成冷却时间（10分钟，单位：毫秒）
    private val IRON_GOLEM_SUMMON_COOLDOWN = 10 * 60 * 1000L

    // 铁傀儡传送冷却时间（10秒，单位：毫秒）
    private val IRON_GOLEM_TELEPORT_COOLDOWN = 10 * 1000L

    // PDC键：记录上次召唤铁傀儡的时间
    private val LAST_SUMMON_TIME_KEY = BetterVillage.namespacedKey("last_summon_time")

    // PDC键：记录铁傀儡上次传送的时间
    private val IRON_GOLEM_TELEPORT_COOLDOWN_KEY = BetterVillage.namespacedKey("iron_golem_teleport_cooldown")

    // PDC键：记录铁傀儡的仇恨优先级
    private val AGGRO_PRIORITY_KEY = BetterVillage.namespacedKey("aggro_priority")

    // PDC键：记录铁傀儡的仇恨开始时间
    private val AGGRO_START_TIME_KEY = BetterVillage.namespacedKey("aggro_start_time")

    // PDC键：记录玩家重生保护期
    private val RESPAWN_PROTECTION_KEY = BetterVillage.namespacedKey("respawn_protection")

    // 存储铁傀儡的仇恨任务ID映射：IronGolem UUID -> Task ID
    private val aggroTaskMap = mutableMapOf<String, Int>()

    
    /**
     * 村民受到攻击事件监听
     * 当村民受到攻击时，在64格内寻找铁傀儡
     * 如果找到铁傀儡，将其传送到村民附近（无需冷却）
     * 如果没有找到铁傀儡，则生成一个新的铁傀儡（需要冷却）
     * 生成新铁傀儡有10分钟的冷却时间（每个村民独立冷却）
     * 如果攻击者是玩家，铁傀儡会对玩家产生1分钟的仇恨
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onVillagerDamage(event: EntityDamageByEntityEvent) {
        val villager = event.entity as? Villager ?: return

        // 检查是否在村庄内
        val village = VillageManager.getVillageByLocation(villager.location) ?: return

        // 检查攻击者是否是玩家
        val player = event.damager as? Player
        val isPlayerAttack = player != null

        // 在64格内寻找铁傀儡
        val ironGolem = findNearbyIronGolem(villager.location, 64)

        if (ironGolem != null) {
            // 计算铁傀儡与村民的距离
            val distance = ironGolem.location.distance(villager.location)
            
            if (distance > 5) {
                // 铁傀儡在5格范围外，传送到村民附近（无需冷却）
                val spawnLocation = findSafeSpawnLocation(villager.location, 5)
                if (spawnLocation != null) {
                    ironGolem.teleport(spawnLocation)

                    // 如果攻击者是玩家，设置铁傀儡对玩家的仇恨
                    if (isPlayerAttack) {
                        setIronGolemAggro(ironGolem, player, 60L)
                    }
                }
            } else {
                // 铁傀儡在5格范围内，保持不动，只设置仇恨
                // 如果攻击者是玩家，设置铁傀儡对玩家的仇恨
                if (isPlayerAttack) {
                    setIronGolemAggro(ironGolem, player, 60L)
                }
            }
        } else {
            // 没有铁傀儡，检查冷却时间
            val lastSummonTime = villager.persistentDataContainer.get(LAST_SUMMON_TIME_KEY, PersistentDataType.LONG)
            val currentTime = System.currentTimeMillis()

            if (lastSummonTime != null && (currentTime - lastSummonTime) < IRON_GOLEM_SUMMON_COOLDOWN) {
                // 还在冷却时间内，不处理
                return
            }

            // 生成1个铁傀儡
            val ironGolem = spawnIronGolem(villager.location, 5)

            if (ironGolem != null) {
                // 记录召唤时间
                villager.persistentDataContainer.set(LAST_SUMMON_TIME_KEY, PersistentDataType.LONG, currentTime)

                // 如果攻击者是玩家，设置铁傀儡对玩家的仇恨
                if (isPlayerAttack) {
                    setIronGolemAggro(ironGolem, player, 60L)
                }
            }
        }
    }

    /**
     * 在指定半径内寻找铁傀儡
     * @param location 搜索中心位置
     * @param radius 搜索半径
     * @return 找到的铁傀儡，如果没有则返回null
     */
    private fun findNearbyIronGolem(location: org.bukkit.Location, radius: Int): IronGolem? {
        val world = location.world ?: return null
        val nearbyEntities = world.getNearbyEntities(location, radius.toDouble(), radius.toDouble(), radius.toDouble())

        for (entity in nearbyEntities) {
            if (entity is IronGolem) {
                val distance = entity.location.distance(location)
                // 返回指定半径内的铁傀儡
                if (distance <= radius) {
                    return entity
                }
            }
        }
        return null
    }

    /**
     * 在指定位置附近寻找安全的生成位置
     */
    private fun findSafeSpawnLocation(location: org.bukkit.Location, radius: Int): org.bukkit.Location? {
        val world = location.world ?: return null

        // 尝试在半径范围内寻找安全位置
        for (x in -radius..radius) {
            for (y in -1..1) {
                for (z in -radius..radius) {
                    val testLocation = org.bukkit.Location(world, location.x + x, location.y + y, location.z + z)
                    if (isSafeLocation(testLocation)) {
                        return testLocation
                    }
                }
            }
        }
        return null
    }

    /**
     * 检查位置是否安全（可以生成实体）
     */
    private fun isSafeLocation(location: org.bukkit.Location): Boolean {
        val world = location.world ?: return false

        // 铁傀儡碰撞体积约为 2.9 x 2.7 x 2.9
        // 需要检查3x3x3的空间（从y-1到y+2，x-1到x+1，z-1到z+1）
        for (x in -1..1) {
            for (y in -1..2) {
                for (z in -1..1) {
                    val checkLocation = location.clone().add(x.toDouble(), y.toDouble(), z.toDouble())
                    val block = checkLocation.block

                    // y=-1的位置必须是固体方块（作为支撑）
                    if (y == -1) {
                        if (!block.type.isSolid) return false
                    } else {
                        // 其他位置必须是空气
                        if (!block.type.isAir) return false
                    }
                }
            }
        }

        return true
    }

    /**
     * 在指定位置附近生成铁傀儡
     */
    private fun spawnIronGolem(location: org.bukkit.Location, radius: Int): IronGolem? {
        val world = location.world ?: return null
        val spawnLocation = findSafeSpawnLocation(location, radius) ?: return null

        return world.spawn(spawnLocation, IronGolem::class.java)
    }

    /**
     * 设置铁傀儡对玩家的仇恨
     * @param ironGolem 铁傀儡
     * @param player 目标玩家
     * @param durationSeconds 仇恨持续时间（秒）
     */
    private fun setIronGolemAggro(ironGolem: IronGolem, player: org.bukkit.entity.Player, durationSeconds: Long) {
        // 检查玩家是否在重生保护期内
        if (player.persistentDataContainer.has(RESPAWN_PROTECTION_KEY, PersistentDataType.BOOLEAN)) {
            // 玩家在保护期内，不设置仇恨
            return
        }

        val golemUUID = ironGolem.uniqueId.toString()

        // 取消之前的仇恨任务
        aggroTaskMap[golemUUID]?.let { taskId ->
            Bukkit.getScheduler().cancelTask(taskId)
            aggroTaskMap.remove(golemUUID)
        }

        // 设置新的仇恨
        ironGolem.target = player
        ironGolem.persistentDataContainer.set(AGGRO_PRIORITY_KEY, PersistentDataType.INTEGER, 1)
        ironGolem.persistentDataContainer.set(AGGRO_START_TIME_KEY, PersistentDataType.LONG, System.currentTimeMillis())

        // 创建新的仇恨清除任务
        val taskId = Bukkit.getScheduler().runTaskLater(BetterVillage.instance, Runnable {
            // 检查铁傀儡是否仍然存在
            if (!ironGolem.isValid || ironGolem.isDead) {
                aggroTaskMap.remove(golemUUID)
                return@Runnable
            }

            ironGolem.target = null
            ironGolem.persistentDataContainer.remove(AGGRO_PRIORITY_KEY)
            ironGolem.persistentDataContainer.remove(AGGRO_START_TIME_KEY)
            aggroTaskMap.remove(golemUUID)
        }, durationSeconds * 20L).taskId

        aggroTaskMap[golemUUID] = taskId
    }

    /**
     * 清理所有仇恨
     * 在服务器关闭时调用，确保没有残留的仇恨
     */
    fun cleanupAll() {
        // 取消所有仇恨任务
        aggroTaskMap.values.forEach { taskId ->
            Bukkit.getScheduler().cancelTask(taskId)
        }
        aggroTaskMap.clear()

        // 清除所有铁傀儡的仇恨
        Bukkit.getWorlds().forEach { world ->
            world.entities.filterIsInstance<IronGolem>().forEach { ironGolem ->
                ironGolem.target = null
                ironGolem.persistentDataContainer.remove(AGGRO_PRIORITY_KEY)
                ironGolem.persistentDataContainer.remove(AGGRO_START_TIME_KEY)
            }
        }
    }
}