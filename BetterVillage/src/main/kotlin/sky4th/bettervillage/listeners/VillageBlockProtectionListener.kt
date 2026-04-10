package sky4th.bettervillage.listeners

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.IronGolem
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.entity.Snowman
import org.bukkit.entity.Villager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.block.EntityBlockFormEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType
import sky4th.bettervillage.BetterVillage
import sky4th.bettervillage.manager.VillageManager
import sky4th.bettervillage.config.ConfigManager

/**
 * 村庄方块保护监听器
 * 职责：
 * 1. 监听村庄内工作方块和床的破坏事件
 * 2. 取消破坏事件，不产生掉落物
 * 3. 提示玩家不能随意破坏村庄内设备
 * 4. 在周围64格内寻找铁傀儡，找到则传送到玩家附近空地并对玩家产生仇恨
 * 5. 如果没有铁傀儡，则生成1个铁傀儡在附近并对玩家产生仇恨
 * 6. 防止玩家在村庄内放置TNT、发射器、投掷器、岩浆、打火石、活塞、粘性活塞
 * 7. 防止破坏钟和庄稼（庄稼破坏后没有掉落物）
 * 8. 防止玩家在村庄内屠宰动物（动物死亡后没有掉落物，铁傀儡会攻击玩家）
 */
class VillageBlockProtectionListener : Listener {

    // PDC标记键
    companion object {
        private val TRADE_BANNED_KEY = NamespacedKey(BetterVillage.instance, "trade_banned")
        private val IRON_GOLEM_KILL_TIME_KEY = NamespacedKey(BetterVillage.instance, "iron_golem_kill_time")
        private val VILLAGER_KILL_TIME_KEY = NamespacedKey(BetterVillage.instance, "villager_kill_time")
        private val AGGRO_PRIORITY_KEY = NamespacedKey(BetterVillage.instance, "aggro_priority")
        private val AGGRO_START_TIME_KEY = NamespacedKey(BetterVillage.instance, "aggro_start_time")
        private val IRON_GOLEM_TELEPORT_COOLDOWN_KEY = NamespacedKey(BetterVillage.instance, "iron_golem_teleport_cooldown")
        private val RESPAWN_PROTECTION_KEY = NamespacedKey(BetterVillage.instance, "respawn_protection")

        // 交易禁令时长（从配置文件读取）
        private val VILLAGER_TRADE_BAN_DURATION get() = ConfigManager.getVillagerKillTradeBanDuration()
        private val IRON_GOLEM_TRADE_BAN_DURATION get() = ConfigManager.getIronGolemKillTradeBanDuration()

        // 铁傀儡传送冷却时间（10秒，单位：毫秒）
        private const val IRON_GOLEM_TELEPORT_COOLDOWN = 10 * 1000L

        // 仇恨优先级定义
        private const val AGGRO_PRIORITY_LOW = 1      // 破坏方块/屠宰动物：1分钟
        private const val AGGRO_PRIORITY_HIGH = 2     // 击杀村民：10分钟

        // 存储铁傀儡的仇恨任务ID映射：IronGolem UUID -> Task ID
        private val aggroTaskMap = mutableMapOf<String, Int>()

        // 存储玩家的交易禁令任务ID映射：Player UUID -> Task ID
        private val tradeBanTaskMap = mutableMapOf<String, Int>()
    }

    // 村庄工作方块列表
    private val workBlocks = setOf(
        // 农民
        Material.COMPOSTER,
        // 制箭师
        Material.FLETCHING_TABLE,
        // 牧师
        Material.BREWING_STAND,
        // 制图师
        Material.CARTOGRAPHY_TABLE,
        // 武器匠
        Material.GRINDSTONE,
        // 盔甲匠
        Material.BLAST_FURNACE,
        // 工具匠
        Material.SMITHING_TABLE,
        // 屠夫
        Material.SMOKER,
        // 渔夫
        Material.BARREL,
        // 图书管理员
        Material.LECTERN,
        // 石匠
        Material.STONECUTTER,
        // 皮匠
        Material.CAULDRON,
        // 牧羊人
        Material.LOOM
    )

    // 床的材质列表
    private val beds = setOf(
        Material.WHITE_BED, Material.ORANGE_BED, Material.MAGENTA_BED,
        Material.LIGHT_BLUE_BED, Material.YELLOW_BED, Material.LIME_BED,
        Material.PINK_BED, Material.GRAY_BED, Material.LIGHT_GRAY_BED,
        Material.CYAN_BED, Material.PURPLE_BED, Material.BLUE_BED,
        Material.BROWN_BED, Material.GREEN_BED, Material.RED_BED,
        Material.BLACK_BED
    )

    // 禁止放置的方块列表
    private val forbiddenBlocks = setOf(
        Material.TNT,
        Material.DISPENSER,
        Material.DROPPER,
        Material.LAVA,
        Material.PISTON,
        Material.STICKY_PISTON
    )

    // 庄稼列表
    private val crops = setOf(
        Material.WHEAT,
        Material.CARROTS,
        Material.POTATOES,
        Material.BEETROOTS,
        Material.NETHER_WART,
        Material.PUMPKIN_STEM,
        Material.MELON_STEM,
        Material.COCOA,
        Material.SWEET_BERRY_BUSH,
        Material.BAMBOO,
        Material.SUGAR_CANE,
        Material.CACTUS,
        Material.KELP,
        Material.SEA_PICKLE,
        Material.BROWN_MUSHROOM,
        Material.RED_MUSHROOM
    )

    // 动物列表
    private val animals = setOf(
        org.bukkit.entity.EntityType.COW,
        org.bukkit.entity.EntityType.PIG,
        org.bukkit.entity.EntityType.SHEEP,
        org.bukkit.entity.EntityType.CHICKEN,
        org.bukkit.entity.EntityType.HORSE,
        org.bukkit.entity.EntityType.DONKEY,
        org.bukkit.entity.EntityType.MULE,
        org.bukkit.entity.EntityType.LLAMA,
        org.bukkit.entity.EntityType.TRADER_LLAMA,
        org.bukkit.entity.EntityType.WOLF,
        org.bukkit.entity.EntityType.CAT,
        org.bukkit.entity.EntityType.OCELOT,
        org.bukkit.entity.EntityType.PARROT,
        org.bukkit.entity.EntityType.RABBIT,
        org.bukkit.entity.EntityType.FOX,
        org.bukkit.entity.EntityType.TURTLE,
        org.bukkit.entity.EntityType.PANDA,
        org.bukkit.entity.EntityType.POLAR_BEAR,
        org.bukkit.entity.EntityType.BEE,
        org.bukkit.entity.EntityType.GOAT,
        org.bukkit.entity.EntityType.AXOLOTL,
        org.bukkit.entity.EntityType.FROG,
        org.bukkit.entity.EntityType.TADPOLE,
        org.bukkit.entity.EntityType.SNIFFER
    )

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockDropItem(event: BlockDropItemEvent) {
        val block = event.block
        val location = block.location
        
        // 检查是否是庄稼
        val isCrop = crops.contains(block.type)
        if (!isCrop) return

        // 检查是否在村庄内
        val village = VillageManager.getVillageByLocation(location) ?: return
    
        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockBreak(event: BlockBreakEvent) {
        val block = event.block
        val player = event.player
        val location = block.location

        // 检查是否在村庄内
        val village = VillageManager.getVillageByLocation(location) ?: return

        // 检查是否是工作方块、床、钟或庄稼
        val isWorkBlock = workBlocks.contains(block.type)
        val isBed = beds.contains(block.type)
        val isBell = block.type == Material.BELL
        val isCrop = crops.contains(block.type)

        if (!isWorkBlock && !isBed && !isBell && !isCrop) return

        // 如果是庄稼，不取消破坏，但通过BlockDropItemEvent阻止掉落物生成
        if (isCrop) {
            // 标记这个方块位置，在BlockDropItemEvent中使用
            return
        }

        // 取消破坏事件
        event.isCancelled = true

        // 清除可能的掉落物（在方块周围1格范围内查找并删除掉落物）
        event.block.world.getNearbyEntities(event.block.location, 1.0, 1.0, 1.0)
            .filter { it is org.bukkit.entity.Item }
            .forEach { it.remove() }

        // 提示玩家
        player.sendMessage("§c你不能随意破坏村庄内的设备！")

        // 在玩家周围寻找铁傀儡（使用玩家位置而不是方块位置）
        val ironGolem = findNearbyIronGolem(player.location, 64)

        if (ironGolem != null) {
            // 计算铁傀儡与玩家的距离
            val distance = ironGolem.location.distance(player.location)
            
            if (distance > 5) {
                // 铁傀儡在5格范围外，传送到玩家附近空地
                val spawnLocation = findSafeSpawnLocation(player.location, 5)
                if (spawnLocation != null) {
                    // 检查传送冷却
                    if (canTeleportIronGolem(ironGolem)) {
                        ironGolem.teleport(spawnLocation)
                        recordIronGolemTeleport(ironGolem)
                        // 让铁傀儡对玩家产生仇恨，持续1分钟（低优先级）
                        val aggroSet = setIronGolemAggro(ironGolem, player, AGGRO_PRIORITY_LOW, 60L)
                        if (aggroSet) {
                            player.sendMessage("§c铁傀儡被激怒了！")
                            // 添加临时交易禁令标记
                            player.persistentDataContainer.set(TRADE_BANNED_KEY, PersistentDataType.BOOLEAN, true)
                        }

                        // 取消之前的交易禁令任务
                        val playerUUID = player.uniqueId.toString()
                        tradeBanTaskMap[playerUUID]?.let { taskId ->
                            Bukkit.getScheduler().cancelTask(taskId)
                            tradeBanTaskMap.remove(playerUUID)
                        }

                        // 1分钟后清除交易禁令标记
                        val taskId = Bukkit.getScheduler().runTaskLater(BetterVillage.instance, Runnable {
                            player.persistentDataContainer.remove(TRADE_BANNED_KEY)
                            tradeBanTaskMap.remove(playerUUID)
                        }, 60L * 20L).taskId
                        tradeBanTaskMap[playerUUID] = taskId
                    }
                }
            } else {
                // 铁傀儡在5格范围内，保持不动，只设置仇恨
                val aggroSet = setIronGolemAggro(ironGolem, player, AGGRO_PRIORITY_LOW, 60L)
                if (aggroSet) {
                    player.sendMessage("§c铁傀儡被激怒了！")
                    // 添加临时交易禁令标记
                    player.persistentDataContainer.set(TRADE_BANNED_KEY, PersistentDataType.BOOLEAN, true)
                }

                // 取消之前的交易禁令任务
                val playerUUID = player.uniqueId.toString()
                tradeBanTaskMap[playerUUID]?.let { taskId ->
                    Bukkit.getScheduler().cancelTask(taskId)
                    tradeBanTaskMap.remove(playerUUID)
                }

                // 1分钟后清除交易禁令标记
                val taskId = Bukkit.getScheduler().runTaskLater(BetterVillage.instance, Runnable {
                    player.persistentDataContainer.remove(TRADE_BANNED_KEY)
                    tradeBanTaskMap.remove(playerUUID)
                }, 60L * 20L).taskId
                tradeBanTaskMap[playerUUID] = taskId
            }
        } else {
            // 没有铁傀儡，生成1个铁傀儡
            val ironGolem = spawnIronGolem(player.location, 5)

            if (ironGolem != null) {
                // 让铁傀儡对玩家产生仇恨，持续1分钟（低优先级）
                setIronGolemAggro(ironGolem, player, AGGRO_PRIORITY_LOW, 60L)
                player.sendMessage("§c村庄召唤了铁傀儡来保护设备！")
                // 添加临时交易禁令标记
                player.persistentDataContainer.set(TRADE_BANNED_KEY, PersistentDataType.BOOLEAN, true)

                // 取消之前的交易禁令任务
                val playerUUID = player.uniqueId.toString()
                tradeBanTaskMap[playerUUID]?.let { taskId ->
                    Bukkit.getScheduler().cancelTask(taskId)
                    tradeBanTaskMap.remove(playerUUID)
                }

                // 1分钟后清除交易禁令标记
                val taskId = Bukkit.getScheduler().runTaskLater(BetterVillage.instance, Runnable {
                    player.persistentDataContainer.remove(TRADE_BANNED_KEY)
                    tradeBanTaskMap.remove(playerUUID)
                }, 60L * 20L).taskId
                tradeBanTaskMap[playerUUID] = taskId
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val block = event.block
        val player = event.player
        val location = block.location

        // 检查是否在村庄内
        val village = VillageManager.getVillageByLocation(location) ?: return

        // 检查是否是禁止放置的方块
        if (forbiddenBlocks.contains(block.type)) {
            // 取消放置事件
            event.isCancelled = true

            // 提示玩家
            player.sendMessage("§c你不能在村庄内放置此方块！")
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val location = player.location

        // 检查是否在村庄内
        val village = VillageManager.getVillageByLocation(location) ?: return

        // 检查是否是打火石
        if (event.material == Material.FLINT_AND_STEEL) {
            // 取消使用事件
            event.isCancelled = true

            // 提示玩家
            player.sendMessage("§c你不能在村庄内使用打火石！")
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityDeath(event: EntityDeathEvent) {
        val entity = event.entity
        val location = entity.location

        // 检查是否在村庄内
        val village = VillageManager.getVillageByLocation(location) ?: return

        // 检查是否是村民
        if (entity is Villager) {
            // 检查是否是玩家杀死的
            if (entity.killer is Player) {
                val player = entity.killer as Player
                // 记录击杀时间戳
                player.persistentDataContainer.set(VILLAGER_KILL_TIME_KEY, PersistentDataType.LONG, System.currentTimeMillis())
                // 提示玩家
                val remainingTime = VILLAGER_TRADE_BAN_DURATION / 1000 / 60
                player.sendMessage("§c你击杀了村民！$remainingTime 分钟内无法与村民交易！铁傀儡会追杀你！")
                
                // 在周围寻找铁傀儡
                val ironGolem = findNearbyIronGolem(location, 64)
                
                if (ironGolem != null) {
                    // 找到铁傀儡，传送到玩家附近空地
                    val spawnLocation = findSafeSpawnLocation(player.location, 5)
                    if (spawnLocation != null) {
                        // 检查传送冷却
                        if (canTeleportIronGolem(ironGolem)) {
                            ironGolem.teleport(spawnLocation)
                            recordIronGolemTeleport(ironGolem)
                            // 让铁傀儡对玩家产生仇恨，持续10分钟
                            setIronGolemAggro(ironGolem, player, AGGRO_PRIORITY_HIGH, VILLAGER_TRADE_BAN_DURATION / 1000)
                            player.sendMessage("§c铁傀儡被激怒了！")
                        }
                    }
                } else {
                    // 没有铁傀儡，生成1个铁傀儡
                    val ironGolem = spawnIronGolem(player.location, 5)
                    
                    if (ironGolem != null) {
                        // 让铁傀儡对玩家产生仇恨，持续10分钟（高优先级）
                        setIronGolemAggro(ironGolem, player, AGGRO_PRIORITY_HIGH, VILLAGER_TRADE_BAN_DURATION / 1000)
                        player.sendMessage("§c村庄召唤了铁傀儡来保护村民！")
                    }
                }
            }
            return
        }
        // 检查是否是铁傀儡
        if (entity is IronGolem) {
            // 检查是否是玩家杀死的
            if (entity.killer is Player) {
                val player = entity.killer as Player
                // 记录击杀时间戳
                player.persistentDataContainer.set(IRON_GOLEM_KILL_TIME_KEY, PersistentDataType.LONG, System.currentTimeMillis())
                // 提示玩家
                val remainingTime = IRON_GOLEM_TRADE_BAN_DURATION / 1000 / 60
                player.sendMessage("§c你击杀了铁傀儡！$remainingTime 分钟内无法与村民交易！")
            }
            return
        }

        // 检查是否是动物
        if (!animals.contains(entity.type)) return

        // 检查是否是玩家杀死的
        if (entity.killer !is Player) return

        val player = entity.killer as Player

        // 清除掉落物
        event.drops.clear()

        // 提示玩家
        player.sendMessage("§c你不能在村庄内屠宰动物！")

        // 在周围寻找铁傀儡
        val ironGolem = findNearbyIronGolem(location, 64)

        if (ironGolem != null) {
            // 找到铁傀儡，传送到玩家附近空地
            val spawnLocation = findSafeSpawnLocation(player.location, 5)
            if (spawnLocation != null) {
                // 检查传送冷却
                if (canTeleportIronGolem(ironGolem)) {
                    ironGolem.teleport(spawnLocation)
                    recordIronGolemTeleport(ironGolem)
                    // 让铁傀儡对玩家产生仇恨，持续1分钟（低优先级）
                    val aggroSet = setIronGolemAggro(ironGolem, player, AGGRO_PRIORITY_LOW, 60L)
                    if (aggroSet) {
                        player.sendMessage("§c铁傀儡被激怒了！")
                        // 添加临时交易禁令标记
                        player.persistentDataContainer.set(TRADE_BANNED_KEY, PersistentDataType.BOOLEAN, true)
                    
                    // 取消之前的交易禁令任务
                    val playerUUID = player.uniqueId.toString()
                    tradeBanTaskMap[playerUUID]?.let { taskId ->
                        Bukkit.getScheduler().cancelTask(taskId)
                        tradeBanTaskMap.remove(playerUUID)
                    }
                    
                    // 1分钟后清除交易禁令标记
                    val taskId = Bukkit.getScheduler().runTaskLater(BetterVillage.instance, Runnable {
                        player.persistentDataContainer.remove(TRADE_BANNED_KEY)
                        tradeBanTaskMap.remove(playerUUID)
                    }, 60L * 20L).taskId
                    tradeBanTaskMap[playerUUID] = taskId
                    } 
                }
            }
        } else {
            // 没有铁傀儡，生成1个铁傀儡
            val ironGolem = spawnIronGolem(player.location, 5)

            if (ironGolem != null) {
                // 让铁傀儡对玩家产生仇恨，持续1分钟
                setIronGolemAggro(ironGolem, player, AGGRO_PRIORITY_LOW, 60L)
                player.sendMessage("§c村庄召唤了铁傀儡来保护动物！")

                // 添加临时交易禁令标记
                player.persistentDataContainer.set(TRADE_BANNED_KEY, PersistentDataType.BOOLEAN, true)

                // 取消之前的交易禁令任务
                val playerUUID = player.uniqueId.toString()
                tradeBanTaskMap[playerUUID]?.let { taskId ->
                    Bukkit.getScheduler().cancelTask(taskId)
                    tradeBanTaskMap.remove(playerUUID)
                }

                // 1分钟后清除交易禁令标记
                val taskId = Bukkit.getScheduler().runTaskLater(BetterVillage.instance, Runnable {
                    player.persistentDataContainer.remove(TRADE_BANNED_KEY)
                    tradeBanTaskMap.remove(playerUUID)
                }, 60L * 20L).taskId
                tradeBanTaskMap[playerUUID] = taskId
            }
        }
    }

    /**
     * 在指定半径内寻找铁傀儡
     * @param location 搜索中心位置
     * @param radius 搜索半径
     * @return 找到的铁傀儡，如果没有则返回null
     */
    private fun findNearbyIronGolem(location: Location, radius: Int): IronGolem? {
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
     * 如果找不到，则破坏3x3x3范围内的方块来生成傀儡
     */
    private fun findSafeSpawnLocation(location: Location, radius: Int): Location? {
        val world = location.world ?: return null

        // 尝试在半径范围内寻找安全位置
        for (x in -radius..radius) {
            for (y in -1..1) {
                for (z in -radius..radius) {
                    val testLocation = Location(world, location.x + x, location.y + y, location.z + z)
                    if (isSafeLocation(testLocation)) {
                        return testLocation
                    }
                }
            }
        }

        // 如果找不到安全位置，破坏玩家周围3x3x3的方块来生成傀儡
        return clearAreaForSpawn(location)
    }

    /**
     * 检查位置是否安全（可以生成实体）
     * 铁傀儡碰撞体积约为 2.9 x 2.7 x 2.9，需要检查更大的空间
     */
    private fun isSafeLocation(location: Location): Boolean {
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
     * 清除玩家周围3x3x3的方块来生成傀儡
     * 破坏从y-1到y+2，x-1到x+1，z-1到z+1范围内的非空气方块
     */
    private fun clearAreaForSpawn(location: Location): Location {
        val world = location.world ?: return location

        // 破坏3x3x3范围内的方块（从y-1到y+2，x-1到x+1，z-1到z+1）
        for (x in -1..1) {
            for (y in -1..2) {
                for (z in -1..1) {
                    val checkLocation = location.clone().add(x.toDouble(), y.toDouble(), z.toDouble())
                    val block = checkLocation.block

                    // 破坏非空气方块
                    if (block.type != Material.AIR) {
                        block.type = Material.AIR
                    }
                }
            }
        }

        return location
    }

    /**
     * 在指定位置附近生成铁傀儡
     */
    private fun spawnIronGolem(location: Location, radius: Int): IronGolem? {
        val world = location.world ?: return null
        val spawnLocation = findSafeSpawnLocation(location, radius) ?: return null

        return world.spawn(spawnLocation, IronGolem::class.java)
    }

    /**
     * 设置铁傀儡对玩家的仇恨
     * @param ironGolem 铁傀儡
     * @param player 目标玩家
     * @param priority 仇恨优先级（AGGRO_PRIORITY_LOW 或 AGGRO_PRIORITY_HIGH）
     * @param durationSeconds 仇恨持续时间（秒）
     * @return 是否成功设置仇恨（如果已有更高优先级的仇恨，则返回false）
     */
    private fun setIronGolemAggro(ironGolem: IronGolem, player: Player, priority: Int, durationSeconds: Long): Boolean {
        // 检查玩家是否在重生保护期内
        if (player.persistentDataContainer.has(RESPAWN_PROTECTION_KEY, PersistentDataType.BOOLEAN)) {
            // 玩家在保护期内，不设置仇恨
            return false
        }

        val golemUUID = ironGolem.uniqueId.toString()

        // 检查是否已有更高优先级的仇恨
        val currentPriority = ironGolem.persistentDataContainer.get(AGGRO_PRIORITY_KEY, PersistentDataType.INTEGER)
        if (currentPriority != null && currentPriority >= priority) {
            // 当前仇恨优先级更高或相同，不更新
            return false
        }

        // 取消之前的仇恨任务
        aggroTaskMap[golemUUID]?.let { taskId ->
            Bukkit.getScheduler().cancelTask(taskId)
            aggroTaskMap.remove(golemUUID)
        }

        // 设置新的仇恨
        ironGolem.target = player
        ironGolem.persistentDataContainer.set(AGGRO_PRIORITY_KEY, PersistentDataType.INTEGER, priority)
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

        return true
    }

    /**
     * 清除铁傀儡的仇恨
     * @param ironGolem 铁傀儡
     */
    private fun clearIronGolemAggro(ironGolem: IronGolem) {
        val golemUUID = ironGolem.uniqueId.toString()

        // 取消仇恨任务
        aggroTaskMap[golemUUID]?.let { taskId ->
            Bukkit.getScheduler().cancelTask(taskId)
            aggroTaskMap.remove(golemUUID)
        }

        // 清除仇恨
        ironGolem.target = null
        ironGolem.persistentDataContainer.remove(AGGRO_PRIORITY_KEY)
        ironGolem.persistentDataContainer.remove(AGGRO_START_TIME_KEY)

        // 通过反射清除铁傀儡的内部仇恨状态
        try {
            // 获取 CraftIronGolem 实例
            val craftEntityClass = Class.forName("org.bukkit.craftbukkit.entity.CraftIronGolem")
            if (craftEntityClass.isInstance(ironGolem)) {
                // 获取 getHandle 方法
                val getHandleMethod = craftEntityClass.getMethod("getHandle")
                val handle = getHandleMethod.invoke(ironGolem)

                // 获取 targetSelector 字段
                val targetSelectorField = handle.javaClass.getDeclaredField("targetSelector")
                targetSelectorField.isAccessible = true
                val targetSelector = targetSelectorField.get(handle)

                // 获取 availableGoals 字段
                val availableGoalsField = targetSelector.javaClass.getDeclaredField("d")
                availableGoalsField.isAccessible = true
                @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                val availableGoals = availableGoalsField.get(targetSelector) as java.util.Collection<*>

                // 清除所有目标选择器目标
                availableGoals.clear()
            }
        } catch (e: Exception) {
            // 忽略反射错误，至少已经清除了 target 属性
        }
    }

    /**
     * 检查铁傀儡是否可以传送
     * @param ironGolem 铁傀儡
     * @return 是否可以传送
     */
    private fun canTeleportIronGolem(ironGolem: IronGolem): Boolean {
        val lastTeleportTime = ironGolem.persistentDataContainer.get(
            IRON_GOLEM_TELEPORT_COOLDOWN_KEY,
            PersistentDataType.LONG
        ) ?: return true

        val currentTime = System.currentTimeMillis()
        return (currentTime - lastTeleportTime) >= IRON_GOLEM_TELEPORT_COOLDOWN
    }

    /**
     * 记录铁傀儡传送时间
     * @param ironGolem 铁傀儡
     */
    private fun recordIronGolemTeleport(ironGolem: IronGolem) {
        ironGolem.persistentDataContainer.set(
            IRON_GOLEM_TELEPORT_COOLDOWN_KEY,
            PersistentDataType.LONG,
            System.currentTimeMillis()
        )
    }

    /**
     * 监听玩家死亡事件
     * 当玩家死亡时，清除所有铁傀儡对该玩家的仇恨
     * 只在玩家附近64格范围内查找铁傀儡，避免遍历所有实体
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerDeath(event: org.bukkit.event.entity.PlayerDeathEvent) {
        val player = event.entity
        val location = player.location
        val world = location.world ?: return

        // 只在玩家附近64格范围内查找铁傀儡
        val nearbyEntities = world.getNearbyEntities(location, 64.0, 64.0, 64.0)

        for (entity in nearbyEntities) {
            if (entity is IronGolem) {
                // 检查铁傀儡的目标是否是该玩家
                if (entity.target == player) {
                    // 清除仇恨
                    clearIronGolemAggro(entity)
                }
            }
        }

        // 记录玩家死亡时间，用于重生时检查
        player.persistentDataContainer.set(
            NamespacedKey(BetterVillage.instance, "player_death_time"),
            PersistentDataType.LONG,
            System.currentTimeMillis()
        )

        // 记录玩家死亡位置，用于重生时清除更大范围内的铁傀儡仇恨
        player.persistentDataContainer.set(
            NamespacedKey(BetterVillage.instance, "player_death_location"),
            PersistentDataType.STRING,
            "${location.world?.name}:${location.x}:${location.y}:${location.z}"
        )
    }

    /**
     * 监听玩家重生事件
     * 当玩家重生时，清除附近铁傀儡对该玩家的仇恨
     * 防止铁傀儡重新获得对玩家的仇恨
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerRespawn(event: org.bukkit.event.player.PlayerRespawnEvent) {
        val player = event.player

        // 设置重生保护标记，持续10秒
        player.persistentDataContainer.set(RESPAWN_PROTECTION_KEY, PersistentDataType.BOOLEAN, true)

        // 10秒后移除保护标记
        Bukkit.getScheduler().runTaskLater(BetterVillage.instance, Runnable {
            player.persistentDataContainer.remove(RESPAWN_PROTECTION_KEY)
        }, 10L * 20L)

        // 在保护期内每2秒检查一次，清除铁傀儡的仇恨
        var taskId = 0
        taskId = Bukkit.getScheduler().runTaskTimer(BetterVillage.instance, Runnable {
            // 检查玩家是否还在保护期内
            if (!player.persistentDataContainer.has(RESPAWN_PROTECTION_KEY, PersistentDataType.BOOLEAN)) {
                // 不在保护期内，取消任务
                Bukkit.getScheduler().cancelTask(taskId)
                return@Runnable
            }

            // 清除玩家附近铁傀儡的仇恨
            val location = player.location
            val world = location.world ?: return@Runnable

            val nearbyEntities = world.getNearbyEntities(location, 64.0, 64.0, 64.0)

            for (entity in nearbyEntities) {
                if (entity is IronGolem) {
                    // 检查铁傀儡的目标是否是该玩家
                    if (entity.target == player) {
                        // 清除仇恨
                        clearIronGolemAggro(entity)
                    }
                }
            }
        }, 20L, 40L).taskId

        // 延迟1秒执行，确保玩家已经重生
        Bukkit.getScheduler().runTaskLater(BetterVillage.instance, Runnable {
            val location = player.location
            val world = location.world ?: return@Runnable

            // 只在玩家附近64格范围内查找铁傀儡
            val nearbyEntities = world.getNearbyEntities(location, 64.0, 64.0, 64.0)

            for (entity in nearbyEntities) {
                if (entity is IronGolem) {
                    // 检查铁傀儡的目标是否是该玩家
                    if (entity.target == player) {
                        // 清除仇恨
                        clearIronGolemAggro(entity)
                    }
                }
            }

            // 检查玩家死亡位置，清除死亡位置的铁傀儡仇恨
            val deathLocationStr = player.persistentDataContainer.get(
                NamespacedKey(BetterVillage.instance, "player_death_location"),
                PersistentDataType.STRING
            )

            if (deathLocationStr != null) {
                val parts = deathLocationStr.split(":")
                if (parts.size == 4) {
                    try {
                        val deathWorld = Bukkit.getWorld(parts[0])
                        if (deathWorld != null) {
                            val deathLocation = Location(
                                deathWorld,
                                parts[1].toDouble(),
                                parts[2].toDouble(),
                                parts[3].toDouble()
                            )

                            // 在死亡位置附近64格范围内查找铁傀儡
                            val deathNearbyEntities = deathWorld.getNearbyEntities(deathLocation, 64.0, 64.0, 64.0)

                            for (entity in deathNearbyEntities) {
                                if (entity is IronGolem) {
                                    // 检查铁傀儡的目标是否是该玩家
                                    if (entity.target == player) {
                                        // 清除仇恨
                                        clearIronGolemAggro(entity)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // 忽略解析错误
                    }
                }

                // 清除死亡位置记录
                player.persistentDataContainer.remove(
                    NamespacedKey(BetterVillage.instance, "player_death_location")
                )
            }
        }, 20L)
    }

    /**
     * 监听实体攻击实体事件
     * 防止铁傀儡在玩家重生保护期内攻击玩家
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityDamageByEntity(event: org.bukkit.event.entity.EntityDamageByEntityEvent) {
        val damager = event.damager
        val victim = event.entity

        // 检查攻击者是否是铁傀儡
        if (damager !is IronGolem) return

        // 检查受害者是否是玩家
        if (victim !is Player) return

        // 检查玩家是否在重生保护期内
        if (victim.persistentDataContainer.has(RESPAWN_PROTECTION_KEY, PersistentDataType.BOOLEAN)) {
            // 玩家在保护期内，取消伤害并清除铁傀儡的仇恨
            event.isCancelled = true
            clearIronGolemAggro(damager)
        }
    }

    /**
     * 监听玩家移动事件
     * 确保在玩家重生保护期内铁傀儡不会追踪玩家
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerMove(event: org.bukkit.event.player.PlayerMoveEvent) {
        val player = event.player

        // 检查玩家是否在重生保护期内
        if (!player.persistentDataContainer.has(RESPAWN_PROTECTION_KEY, PersistentDataType.BOOLEAN)) return

        // 检查玩家是否真的移动了（不仅仅是转头）
        if (event.from.x == event.to.x && event.from.y == event.to.y && event.from.z == event.to.z) return

        // 清除玩家附近铁傀儡的仇恨
        val location = player.location
        val world = location.world ?: return

        val nearbyEntities = world.getNearbyEntities(location, 64.0, 64.0, 64.0)

        for (entity in nearbyEntities) {
            if (entity is IronGolem) {
                // 检查铁傀儡的目标是否是该玩家
                if (entity.target == player) {
                    // 清除仇恨
                    clearIronGolemAggro(entity)
                }
            }
        }
    }

    /**
     * 监听玩家与村民的交易事件
     * 如果玩家有临时交易禁令标记或在铁傀儡击杀禁令期内，则取消交易
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerInteractVillager(event: PlayerInteractEntityEvent) {
        // 检查玩家是否在重生保护期内
        if (event.player.persistentDataContainer.has(RESPAWN_PROTECTION_KEY, PersistentDataType.BOOLEAN)) {
            // 玩家在保护期内，不处理交易
            return
        }
        // 检查被右键点击的实体是不是村民
        val villager = event.rightClicked as? Villager ?: return
        val player = event.player

        // 检查玩家是否有临时交易禁令标记（激怒铁傀儡）
        if (player.persistentDataContainer.has(TRADE_BANNED_KEY, PersistentDataType.BOOLEAN)) {
            // 取消点击事件
            event.isCancelled = true
            // 提示玩家
            player.sendMessage("§c你因激怒铁傀儡而被禁止交易！请等待1分钟。")
            return
        }

        // 检查玩家是否在击杀村民禁令期内
        val villagerKillTime = player.persistentDataContainer.get(VILLAGER_KILL_TIME_KEY, PersistentDataType.LONG)
        if (villagerKillTime != null) {
            val currentTime = System.currentTimeMillis()
            val elapsed = currentTime - villagerKillTime
            if (elapsed < VILLAGER_TRADE_BAN_DURATION) {
                // 还在禁令期内
                event.isCancelled = true
                val remainingMinutes = ((VILLAGER_TRADE_BAN_DURATION - elapsed) / 1000 / 60).toInt()
                player.sendMessage("§c你因击杀村民而被禁止交易！还需等待 $remainingMinutes 分钟。")
                return
            } else {
                // 禁令期已过，清除标记
                player.persistentDataContainer.remove(VILLAGER_KILL_TIME_KEY)
            }
        }

        // 检查玩家是否在铁傀儡击杀禁令期内
        val killTime = player.persistentDataContainer.get(IRON_GOLEM_KILL_TIME_KEY, PersistentDataType.LONG)
        if (killTime != null) {
            val currentTime = System.currentTimeMillis()
            val elapsed = currentTime - killTime
            if (elapsed < IRON_GOLEM_TRADE_BAN_DURATION) {
                // 还在禁令期内
                event.isCancelled = true
                val remainingMinutes = ((IRON_GOLEM_TRADE_BAN_DURATION - elapsed) / 1000 / 60).toInt()
                player.sendMessage("§c你因击杀铁傀儡而被禁止交易！还需等待 $remainingMinutes 分钟。")
            } else {
                // 禁令期已过，清除标记
                player.persistentDataContainer.remove(IRON_GOLEM_KILL_TIME_KEY)
            }
        }
    }

    /**
     * 清理所有仇恨和临时标记
     * 在服务器关闭时调用，确保没有残留的仇恨和临时标记
     */
    fun cleanupAll() {
        // 取消所有仇恨任务
        aggroTaskMap.values.forEach { taskId ->
            Bukkit.getScheduler().cancelTask(taskId)
        }
        aggroTaskMap.clear()

        // 取消所有交易禁令任务
        tradeBanTaskMap.values.forEach { taskId ->
            Bukkit.getScheduler().cancelTask(taskId)
        }
        tradeBanTaskMap.clear()

        // 清除所有铁傀儡的仇恨
        Bukkit.getWorlds().forEach { world ->
            world.entities.filterIsInstance<IronGolem>().forEach { ironGolem ->
                clearIronGolemAggro(ironGolem)
            }
        }

        // 清除所有在线玩家的临时标记
        Bukkit.getOnlinePlayers().forEach { player ->
            // 清除临时交易禁令标记
            player.persistentDataContainer.remove(TRADE_BANNED_KEY)
            // 清除重生保护标记
            player.persistentDataContainer.remove(RESPAWN_PROTECTION_KEY)
            // 清除死亡位置记录
            player.persistentDataContainer.remove(
                NamespacedKey(BetterVillage.instance, "player_death_location")
            )
            player.persistentDataContainer.remove(
                NamespacedKey(BetterVillage.instance, "player_death_time")
            )
        }
    }
}