
package sky4th.dungeon.player

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import sky4th.dungeon.Dungeon
import sky4th.dungeon.config.ConfigManager
import sky4th.dungeon.util.LanguageUtil.sendLang
import sky4th.dungeon.util.LanguageUtil.sendLangBroad
import java.util.*

/**
 * 玩家死亡事件监听器
 * 处理玩家在地牢中死亡时的逻辑
 */
class PlayerDeathListener(
    private val playerManager: PlayerManager,
    private val backpackManager: BackpackManager,
    private val configManager: ConfigManager
) : Listener {

    // 存储死亡玩家的物品和品级
    private val deathPlayerItems: MutableMap<UUID, List<Pair<ItemStack, String>>> = mutableMapOf()
    // 存储死亡玩家的背包现金
    private val deathPlayerCash: MutableMap<UUID, Int> = mutableMapOf()
    // 存储不生成头颅的玩家列表（倒计时到期导致死亡）
    private val noHeadPlayers: MutableSet<UUID> = mutableSetOf()
    
    // 死亡UI引用
    private var deathUI: PlayerDeathUI? = null

    // 倒地玩家管理器引用
    private var downedPlayerManager: DownedPlayerManager? = null

    // 物品品级键
    private val lootIdKey: NamespacedKey by lazy {
        NamespacedKey(Dungeon.instance, "dungeon_loot_id")
    }
    private val loadoutPriceKey: NamespacedKey by lazy {
        NamespacedKey(Dungeon.instance, "loadout_price")
    }
    private val loadoutSetKey: NamespacedKey by lazy {
        NamespacedKey(Dungeon.instance, "loadout_set")
    }
    private val loadoutShopIdKey: NamespacedKey by lazy {
        NamespacedKey(Dungeon.instance, "loadout_shop_id")
    }
    private val loadoutTierKey: NamespacedKey by lazy {
        NamespacedKey(Dungeon.instance, "loadout_tier")
    }

    companion object {
        // 默认品级为史诗
        const val DEFAULT_TIER = "rare"
    }

    /**
     * 标记玩家死亡时不生成头颅
     * @param playerUuid 玩家UUID
     */
    fun setNoHead(playerUuid: UUID) {
        noHeadPlayers.add(playerUuid)
    }

    /**
     * 取消标记玩家死亡时不生成头颅
     * @param playerUuid 玩家UUID
     */
    fun clearNoHead(playerUuid: UUID) {
        noHeadPlayers.remove(playerUuid)
    }

    /**
     * 玩家死亡事件
     * 如果玩家在地牢中死亡，则：
     * 1. 清除死亡掉落物
     * 2. 退出地牢
     * 3. 在死亡位置生成玩家头颅
     * 4. 头颅中包含玩家背包中的物品（只保留：物品栏、背包下两行中间的5个+副手）
     */
    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        if (!playerManager.isPlayerInDungeon(player)) {
            return
        }

        // 清除死亡掉落物
        event.drops.clear()
        // 获取击倒者信息
        var killerName: String? = null
        var weaponName: String? = null

        if (event.entity.lastDamageCause is org.bukkit.event.entity.EntityDamageByEntityEvent) {
            val damageEvent = event.entity.lastDamageCause as org.bukkit.event.entity.EntityDamageByEntityEvent
            val damager = damageEvent.damager

            when (damager) {
                is Player -> {
                    // 玩家击倒
                    killerName = damager.name
                    val weapon = damager.inventory.itemInMainHand
                    if (weapon.type != org.bukkit.Material.AIR) {
                        weaponName = if (weapon.hasItemMeta() && weapon.itemMeta?.hasDisplayName() == true) {
                            weapon.itemMeta?.displayName
                        } else {
                            weapon.type.name
                        }
                    }
                }
                is org.bukkit.entity.LivingEntity -> {
                    // 怪物击倒
                    killerName = damager.name
                }
                is org.bukkit.entity.Projectile -> {
                    // 投射物击倒（如箭矢）
                    val shooter = damager.shooter
                    when (shooter) {
                        is Player -> {
                            killerName = shooter.name
                            val weapon = shooter.inventory.itemInMainHand
                            if (weapon.type != org.bukkit.Material.AIR) {
                                weaponName = if (weapon.hasItemMeta() && weapon.itemMeta?.hasDisplayName() == true) {
                                    weapon.itemMeta?.displayName
                                } else {
                                    weapon.type.name
                                }
                            }
                        }
                        is org.bukkit.entity.LivingEntity -> {
                            killerName = shooter.name
                        }
                    }
                }
            }
        }

        // 尝试将玩家设置为倒地状态
        val downedPlayerManager = this.downedPlayerManager
        if (downedPlayerManager != null) {
            // 检查玩家是否刚刚放弃救援
            val wasGivingUp = downedPlayerManager.wasGivingUp(player.uniqueId)

            // 如果玩家刚刚放弃救援，不允许再次倒地
            if (!wasGivingUp) {
                val success = downedPlayerManager.setPlayerDowned(player, killerName, weaponName)
                if (success) {
                    // 倒地成功，取消死亡事件
                    event.isCancelled = true
                    return
                }
            } else {
                // 玩家刚刚放弃救援，直接死亡（死亡次数已经在倒地时增加过了）
                // 获取保存的击倒者信息
                val (savedKillerName, savedWeaponName) = downedPlayerManager.getGiveUpPlayerKiller(player.uniqueId)
                if (savedKillerName != null) {
                    killerName = savedKillerName
                    weaponName = savedWeaponName
                } 
                // 清除保存的击倒者信息
                downedPlayerManager.clearGiveUpPlayerKiller(player.uniqueId)
                
            }
            broadcastDeathMessage(player, killerName, weaponName)
            // 清理玩家的倒地状态（如果玩家在倒地期间死亡）
            downedPlayerManager.onPlayerDeath(player)
        } else {
            // downedPlayerManager为null，直死
            broadcastDeathMessage(player, killerName, weaponName)
        }

        // 倒地失败或downedPlayerManager为null，执行真正的死亡逻辑
        // 收集玩家背包中的物品（只保留：物品栏、背包下两行中间的5个+副手）
        val playerItems = collectPlayerItems(player)
        
        // 保存死亡玩家的背包现金
        val cashAmount = backpackManager.getPlayerCash(player)
        if (cashAmount > 0) {
            deathPlayerCash[player.uniqueId] = cashAmount
        }

        // 检查是否需要生成头颅
        val shouldCreateHead = !noHeadPlayers.contains(player.uniqueId)

        if (shouldCreateHead) {
            // 在死亡位置直接生成玩家头颅方块
            val deathHead = PlayerDeathHead.create(player.name, player.uniqueId)
            val location = player.location
            val block = location.block

            // 设置头颅方块
            block.type = Material.PLAYER_HEAD

            // 设置头颅的拥有者
            val skullState = block.state as? org.bukkit.block.Skull ?: return
            skullState.setOwningPlayer(player)
            skullState.update()
        }

        // 保存死亡玩家的物品数据
        saveDeathPlayerItems(player.uniqueId, playerItems)

        // 退出地牢（撤离失败）
        playerManager.teleportFromDungeon(player, false)

        // 清除noHead标记
        noHeadPlayers.remove(player.uniqueId)
        // 发送提示消息
        player.sendLang(Dungeon.instance, "death.message")

        // 确保玩家状态被更新为死亡状态
        // 通过playerManager获取teamManager，然后更新玩家状态
        val teamManager = playerManager.getTeamManager()
        val playerState = teamManager.getPlayerState(player.uniqueId)
        if (playerState != null) {
            playerState.isDead = true
        }
    }

    /**
     * 玩家右键点击死亡头颅方块事件
     * 打开玩家死亡UI
     */
    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player

        // 检查是否是右键点击方块
        if (event.action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return
        }

        // 防止双击触发：只处理主手点击
        if (event.hand != org.bukkit.inventory.EquipmentSlot.HAND) {
            return
        }

        val clickedBlock = event.clickedBlock ?: return

        // 检查点击的方块是否是玩家头颅
        if (clickedBlock.type != Material.PLAYER_HEAD && clickedBlock.type != Material.PLAYER_WALL_HEAD) {
            return
        }

        // 获取方块状态
        val skullState = clickedBlock.state as? org.bukkit.block.Skull ?: return

        // 获取头颅拥有者
        val owningPlayer = skullState.owningPlayer ?: return
        val deathPlayerUuid = owningPlayer.uniqueId
        
        // 打开死亡UI（使用与容器搜索相同的逻辑）
        deathUI?.openSearchUI(player, deathPlayerUuid, this)

        // 取消事件
        event.isCancelled = true
    }

    /**
     * 收集玩家背包中的物品
     * 只保留：物品栏（0-8格）、背包下两行中间的5个（20-24格和29-33格）+副手
     * 装备不会保存
     */
    private fun collectPlayerItems(player: Player): List<ItemStack> {
        val items = mutableListOf<ItemStack>()
        val inventory = player.inventory

        // 收集物品栏（0-8格）
        for (slot in 0..8) {
            val item = inventory.getItem(slot)
            if (item != null && !item.type.isAir && !backpackManager.isBlockedPlaceholder(item)) {
                items.add(item.clone())
            }
        }

        // 收集背包下两行中间的5个（20-24格和29-33格）
        val backpackSlots = listOf(20, 21, 22, 23, 24, 29, 30, 31, 32, 33)
        for (slot in backpackSlots) {
            val item = inventory.getItem(slot)
            if (item != null && !item.type.isAir && !backpackManager.isBlockedPlaceholder(item)) {
                items.add(item.clone())
            }
        }

        // 收集副手
        val offhandItem = inventory.itemInOffHand
        if (!offhandItem.type.isAir) {
            items.add(offhandItem.clone())
        }

        return items
    }

    /**
     * 保存死亡玩家的物品数据
     * 保存物品和品级到内存中
     */
    private fun saveDeathPlayerItems(playerUuid: UUID, items: List<ItemStack>) {
        val itemsWithTier = items.map { item ->
            val tier = getItemTier(item)
            item.clone() to tier
        }
        deathPlayerItems[playerUuid] = itemsWithTier
    }

    /**
     * 获取物品的品级
     * 如果物品没有品级，根据物品的材质类型从ConfigManager中查找匹配的物品配置
     */
    private fun getItemTier(item: ItemStack): String {
        val meta = item.itemMeta ?: return DEFAULT_TIER
        val pdc = meta.persistentDataContainer

        // 检查是否是地牢战利品
        val lootId = pdc.get(lootIdKey, PersistentDataType.STRING)
        if (lootId != null) {
            // 尝试从所有地牢配置中查找
            val dungeonConfigs = configManager.loadDungeonConfigs()
            var loot: sky4th.dungeon.config.LootItemConfig? = null
            for ((dungeonId, dungeonConfig) in dungeonConfigs) {
                loot = configManager.getLootItemById(dungeonId, lootId)
                if (loot != null) break
            }
            if (loot != null) {
                return loot.tier
            }
        }

        // 检查是否有配装品级标记
        val loadoutTier = pdc.get(loadoutTierKey, PersistentDataType.STRING)
        if (loadoutTier != null && loadoutTier.isNotBlank()) {
            return loadoutTier
        }

        // 检查是否是配装物品
        val hasLoadoutPrice = pdc.has(loadoutPriceKey, PersistentDataType.INTEGER)
        val hasLoadoutSet = pdc.has(loadoutSetKey, PersistentDataType.STRING)
        val hasLoadoutShopId = pdc.has(loadoutShopIdKey, PersistentDataType.STRING)

        if (hasLoadoutPrice || hasLoadoutSet || hasLoadoutShopId) {
            // 配装物品，尝试从配置中获取品级
            val shopId = pdc.get(loadoutShopIdKey, PersistentDataType.STRING)
            if (shopId != null) {
                val shopConfig = configManager.getLoadoutShopItemById(shopId)
                if (shopConfig != null && shopConfig.tier.isNotBlank()) {
                    return shopConfig.tier
                }
            }
            // 如果无法从配置中获取品级，返回史诗品级
            return DEFAULT_TIER
        }
        // 其他物品，返回史诗品级
        return DEFAULT_TIER
    }

    /**
     * 播报死亡消息
     */
    private fun broadcastDeathMessage(player: Player, killerName: String?, weaponName: String?) {
        Dungeon.instance.logger.info("玩家 ${player} 死亡")
        Dungeon.instance.logger.info("杀手 ${killerName} ")
        Dungeon.instance.logger.info("武器 ${weaponName} ")
        val instance = playerManager.getPlayerInstance(player) ?: return
        Dungeon.instance.logger.info("玩家实例获取 ${instance} ")
        val playerUuids = instance.getPlayers()
        playerUuids.forEach { uuid ->
            val targetPlayer = Bukkit.getPlayer(uuid)
            if (targetPlayer != null && targetPlayer.isOnline && targetPlayer != player) {
                if (killerName != null && weaponName != null) {
                    // 玩家击杀
                    targetPlayer.sendLangBroad(Dungeon.instance, "death.killed-by-player", "killername" to killerName,"weapon" to weaponName, "playername" to player.name)
                } else if (killerName != null) {
                    // 怪物击杀
                    targetPlayer.sendLangBroad(Dungeon.instance, "death.killed-by-monster", "monstername" to killerName, "playername" to player.name)
                } else {                        
                    targetPlayer.sendLangBroad(Dungeon.instance, "death.killed", "playername" to player.name)
                }
            }
        }
    }
    

    /**
     * 获取死亡玩家的物品和品级
     */
    fun getDeathPlayerItems(playerUuid: UUID): List<Pair<ItemStack, String>>? {
        return deathPlayerItems[playerUuid]
    }

    /**
     * 获取死亡玩家的背包现金
     */
    fun getDeathPlayerCash(playerUuid: UUID): Int {
        return deathPlayerCash[playerUuid] ?: 0
    }

    /**
     * 设置死亡玩家的背包现金
     */
    fun setDeathPlayerCash(playerUuid: UUID, amount: Int) {
        deathPlayerCash[playerUuid] = amount
    }
    
    /**
     * 设置死亡UI引用
     */
    fun setDeathUI(ui: PlayerDeathUI) {
        this.deathUI = ui
    }

    /**
     * 设置倒地玩家管理器引用
     */
    fun setDownedPlayerManager(manager: DownedPlayerManager) {
        this.downedPlayerManager = manager
    }

    /**
     * 清空所有死亡玩家数据
     * 用于地牢重置时清理内存中的死亡数据
     */
    fun clearAllDeathData() {
        deathPlayerItems.clear()
        deathPlayerCash.clear()
    }

    /**
     * 清理指定实例中的死亡玩家数据
     * @param instanceFullId 实例完整ID
     */
    fun clearForInstance(instanceFullId: String) {
        // 找出所有在该实例中的死亡玩家
        val playersToRemove = deathPlayerItems.filterKeys { playerUuid ->
            val player = Bukkit.getPlayer(playerUuid)
            if (player != null && player.isOnline) {
                val instance = playerManager.getPlayerInstance(player)
                instance?.getFullId() == instanceFullId
            } else {
                false
            }
        }.keys.toList()

        // 清理这些死亡玩家的数据
        playersToRemove.forEach { playerUuid ->
            deathPlayerItems.remove(playerUuid)
            deathPlayerCash.remove(playerUuid)
        }
    }
}