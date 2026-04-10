package com.sky4th.equipment.listener

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import com.sky4th.equipment.EquipmentAffix
import com.sky4th.equipment.attributes.EquipmentAttributes
import com.sky4th.equipment.event.ProficiencyChangeEvent
import com.sky4th.equipment.manager.EquipmentManager
import com.sky4th.equipment.manager.LoreDisplayManager
import com.sky4th.equipment.data.NBTEquipmentDataManager
import com.sky4th.equipment.util.LanguageUtil.sendLang
import sky4th.core.api.LanguageAPI
import java.util.UUID

/**
 * 熟练度监听器
 * 监听装备使用事件，增加熟练度
 */
class ProficiencyListener(private val plugin: EquipmentAffix) : Listener {

    /**
     * 监听方块破坏事件（工具使用）
     * 剑/三叉戟/弓/弩/重锤不会在方块破坏时增加熟练度
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val item = player.inventory.itemInMainHand
        // 检查物品是否有熟练度属性
        if (!hasProficiency(item)) {
            return
        }

        if (item.type == Material.FISHING_ROD) {
            return
        }

        // 获取装备ID
        val equipmentId = NBTEquipmentDataManager.getEquipmentId(item) ?: return

        // 检查是否是工具类
        val equipmentType = com.sky4th.equipment.registry.EquipmentRegistry.getEquipmentType(equipmentId)
        if (equipmentType != null && com.sky4th.equipment.attributes.EquipmentCategory.TOOL in equipmentType.categories) {
            // 增加熟练度
            increaseProficiency(player, item)
        }
    }

    /**
     * 监听实体攻击事件（武器使用）
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onEntityDamage(event: EntityDamageByEntityEvent) {
        val damager = event.damager

        // 只处理玩家造成的伤害
        if (damager !is Player) {
            return
        }

        val item = damager.inventory.itemInMainHand

        // 检查物品是否有熟练度属性
        if (!hasProficiency(item)) {
            return
        }

        // 获取装备ID
        val equipmentId = NBTEquipmentDataManager.getEquipmentId(item) ?: return

        // 检查是否是弓或弩
        val equipmentType = com.sky4th.equipment.registry.EquipmentRegistry.getEquipmentType(equipmentId)
        if (equipmentType != null) {
            val categories = equipmentType.categories
            // 弓/弩不在这里增加熟练度，而是在ProjectileLaunchEvent中处理
            if (com.sky4th.equipment.attributes.EquipmentCategory.WEAPON in categories) {
                // 增加熟练度
                increaseProficiency(damager, item)
            }
        }
    }

    /**
     * 监听投射物射出事件（弓/弩/三叉戟投掷）
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onProjectileLaunch(event: org.bukkit.event.entity.ProjectileLaunchEvent) {
        val projectile = event.entity
        val shooter = projectile.shooter

        // 只处理玩家发射的投射物
        if (shooter !is Player) {
            return
        }
        // 获取玩家手中的武器
        val item = shooter.inventory.itemInMainHand

        // 检查物品是否有熟练度属性
        if (!hasProficiency(item)) {
            return
        }

        // 获取装备ID
        val equipmentId = NBTEquipmentDataManager.getEquipmentId(item) ?: return

        // 检查是否是弓/弩或三叉戟
        val equipmentType = com.sky4th.equipment.registry.EquipmentRegistry.getEquipmentType(equipmentId)
        if (equipmentType != null) {
            val categories = equipmentType.categories
            // 弓/弩或三叉戟才增加熟练度
            if (com.sky4th.equipment.attributes.EquipmentCategory.BOW in categories || 
                equipmentType.id == "trident") {
                increaseProficiency(shooter, item)
            }
        }
    }

    /**
     * 监听玩家右键事件（锄头/铲子使用）
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val item = player.inventory.itemInMainHand

        // 检查是否是右键方块
        if (event.action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return
        }

        val clickedBlock = event.clickedBlock ?: return
        val originalBlockType = clickedBlock.type

        // 检查物品是否有熟练度属性
        if (!hasProficiency(item)) {
            return
        }

        // 检查是否是锄头或铲子
        val itemType = item.type
        val isTool = when {
            itemType.name.endsWith("_HOE") -> true
            itemType.name.endsWith("_SHOVEL") -> true
            else -> false
        }

        if (!isTool) {
            return
        }

        // 延迟1 tick检查方块是否真的变化了
        plugin.server.scheduler.runTask(plugin, Runnable {
            if (clickedBlock.type != originalBlockType) {
                // 方块发生了变化，增加熟练度
                increaseProficiency(player, item)
            }
        })
    }

    /**
     * 监听钓鱼事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerFish(event: org.bukkit.event.player.PlayerFishEvent) {
        val player = event.player
        val item = player.inventory.itemInMainHand

        // 只在成功钓到物品时增加熟练度（包括鱼、宝藏、垃圾等）
        if (event.state != org.bukkit.event.player.PlayerFishEvent.State.CAUGHT_FISH) {
            return
        }

        // 检查是否真的钓到了物品
        val caught = event.caught
        if (caught == null) {
            return
        }

        // 检查物品是否有熟练度属性
        if (!hasProficiency(item)) {
            return
        }

        // 获取装备ID
        val equipmentId = NBTEquipmentDataManager.getEquipmentId(item) ?: return
        // 检查是否是工具类
        val equipmentType = com.sky4th.equipment.registry.EquipmentRegistry.getEquipmentType(equipmentId)
        if (equipmentType != null && item.type == Material.FISHING_ROD) {
            // 增加熟练度
            increaseProficiency(player, item)
        }
    }

    /**
     * 监听实体受伤事件（护甲，盾牌使用）
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onEntityDamage(event: EntityDamageEvent) {
        val entity = event.entity

        // 只处理玩家受伤
        if (entity !is Player) {
            return
        }

        // 检查所有护甲装备
        val armorItems = listOf(
            entity.inventory.helmet,
            entity.inventory.chestplate,
            entity.inventory.leggings,
            entity.inventory.boots
        )

        armorItems.forEach { item ->
            if (item != null && hasProficiency(item)) {
                increaseProficiency(entity, item)
            }
        }

        // 检查副手是否为盾牌
        val offHandItem = entity.inventory.itemInOffHand

        // 检查玩家是否成功格挡
        val blockDamage = event.getDamage(DamageModifier.BLOCKING)
        if (blockDamage == 0.0) {
            return
        }

        // 检查副手是否为盾牌且有熟练度
        if (offHandItem.type == Material.SHIELD && hasProficiency(offHandItem)) {
            increaseProficiency(entity, offHandItem)
        }
    }

    /**
     * 监听玩家加入服务器事件
     * 检查玩家是否处于滑翔状态，如果是则启动滑翔任务
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: org.bukkit.event.player.PlayerJoinEvent) {
        val player = event.player

        // 检查玩家是否处于滑翔状态
        if (!player.isGliding) {
            return
        }

        val chestplate = player.inventory.chestplate

        // 检查胸甲是否有熟练度属性
        if (chestplate == null || !hasProficiency(chestplate)) {
            return
        }

        // 获取装备ID
        val equipmentId = NBTEquipmentDataManager.getEquipmentId(chestplate) ?: return
        // 检查是否是鞘翅
        val equipmentType = com.sky4th.equipment.registry.EquipmentRegistry.getEquipmentType(equipmentId)
        if (equipmentType == null || com.sky4th.equipment.attributes.EquipmentCategory.ELYTRA !in equipmentType.categories) {
            return
        }

        // 启动滑翔任务
        startGlideTask(player, chestplate)
    }

    /**
     * 监听鞘翅滑翔事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onEntityToggleGlide(event: org.bukkit.event.entity.EntityToggleGlideEvent) {
        val entity = event.entity

        // 只处理玩家
        if (entity !is Player) {
            return
        }

        val player = entity
        val chestplate = player.inventory.chestplate

        // 检查胸甲是否有熟练度属性
        if (chestplate == null || !hasProficiency(chestplate)) {
            return
        }

        // 获取装备ID
        val equipmentId = NBTEquipmentDataManager.getEquipmentId(chestplate) ?: return
        // 检查是否是鞘翅
        val equipmentType = com.sky4th.equipment.registry.EquipmentRegistry.getEquipmentType(equipmentId)
        if (equipmentType == null || com.sky4th.equipment.attributes.EquipmentCategory.ELYTRA !in equipmentType.categories) {
            return
        }

        if (event.isGliding) {
            // 开始滑翔，启动定时任务
            startGlideTask(player, chestplate)
        } else {
            // 结束滑翔，停止定时任务
            stopGlideTask(player)
        }
    }

    // 存储每个玩家的滑翔任务
    private val glideTasks = java.util.concurrent.ConcurrentHashMap<UUID, org.bukkit.scheduler.BukkitTask>()

    /**
     * 启动滑翔定时任务
     */
    private fun startGlideTask(player: Player, chestplate: ItemStack) {
        // 如果已经有任务在运行，先停止
        stopGlideTask(player)

        // 启动新的定时任务，每秒增加一次熟练度
        val task = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            // 检查玩家是否还在滑翔
            if (!player.isGliding) {
                stopGlideTask(player)
                return@Runnable
            }

            // 检查胸甲是否还在
            val currentChestplate = player.inventory.chestplate
            if (currentChestplate == null) {
                stopGlideTask(player)
                return@Runnable
            }

            // 检查当前胸甲是否有熟练度属性
            if (!hasProficiency(currentChestplate)) {
                stopGlideTask(player)
                return@Runnable
            }

            // 获取装备ID
            val equipmentId = NBTEquipmentDataManager.getEquipmentId(currentChestplate) ?: run {
                stopGlideTask(player)
                return@Runnable
            }

            // 检查是否是鞘翅
            val equipmentType = com.sky4th.equipment.registry.EquipmentRegistry.getEquipmentType(equipmentId)
            if (equipmentType == null || com.sky4th.equipment.attributes.EquipmentCategory.ELYTRA !in equipmentType.categories) {
                stopGlideTask(player)
                return@Runnable
            }

            // 增加当前胸甲的熟练度
            increaseProficiency(player, currentChestplate)
        }, 20L, 2 * 20L) // 延迟1秒后开始，每2秒执行一次

        glideTasks[player.uniqueId] = task
    }

    /**
     * 停止滑翔定时任务
     */
    private fun stopGlideTask(player: Player) {
        val task = glideTasks.remove(player.uniqueId)
        task?.cancel()
    }

    /**
     * 增加装备熟练度
     */
    private fun increaseProficiency(player: Player, item: ItemStack) {

        val oldAttributes = EquipmentManager.getEquipmentAttributes(item)
        val newAttributes = oldAttributes.increaseProficiency()

        // 检查熟练度是否真的增加了
        if (newAttributes.proficiency == oldAttributes.proficiency) return

        // 保存显示模式
        val isDetailed = isDetailedDescription(item)

        // 获取当前的 itemMeta，一次性修改所有属性
        val meta = item.itemMeta ?: return
        val container = meta.persistentDataContainer

        // 一次性设置熟练度和等级，避免多次设置 itemMeta
        container.set(NBTEquipmentDataManager.KEY_PROFICIENCY_LEVEL, org.bukkit.persistence.PersistentDataType.INTEGER, newAttributes.proficiencyLevel)
        container.set(NBTEquipmentDataManager.KEY_PROFICIENCY, org.bukkit.persistence.PersistentDataType.INTEGER, newAttributes.proficiency)

        // 一次性保存 meta
        item.itemMeta = meta

        // 清除缓存
        com.sky4th.equipment.cache.EquipmentAttributesCache.invalidate(item)
        com.sky4th.equipment.cache.LoreDisplayCache.invalidate(item)

        // 触发熟练度变化事件
        val proficiencyEvent = ProficiencyChangeEvent(
            player = player,
            item = item,
            oldProficiency = oldAttributes.proficiency,
            newProficiency = newAttributes.proficiency,
            oldLevel = oldAttributes.proficiencyLevel,
            newLevel = newAttributes.proficiencyLevel
        )

        plugin.server.pluginManager.callEvent(proficiencyEvent)

        // 如果等级提升了，发送消息
        if (proficiencyEvent.isLevelUp()) {
            // 更新槽位数量
            com.sky4th.equipment.manager.SlotManager.updateSlots(item, newAttributes.proficiencyLevel)

            // 发送升级消息
            val levelName = LanguageAPI.getText(plugin, "proficiency.level.${com.sky4th.equipment.util.DisplayUtil.getProficiencyLevelKey(newAttributes.proficiencyLevel)}")
            // 使用LegacyComponentSerializer正确序列化Component，获取纯文本名称
            val equipmentName = item.itemMeta?.displayName()?.let {
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(it)
            } ?: "装备"
            player.sendLang(plugin, "proficiency.level-up",
                "equipment_name" to equipmentName,
                "level" to levelName)
        }
        
        // 刷新 Lore（原地修改）
        LoreDisplayManager.modifyItemLore(item, isDetailed)
    }

    /**
     * 获取物品当前的详细描述模式
     */
    private fun isDetailedDescription(item: ItemStack): Boolean {
        val meta = item.itemMeta ?: return false
        val container = meta.persistentDataContainer
        val key = org.bukkit.NamespacedKey("sky_equipment", "detailed_lore")
        return container.getOrDefault(key, org.bukkit.persistence.PersistentDataType.BYTE, 0) == 1.toByte()
    }
    /**
     * 检查物品是否有熟练度属性
     * 使用NBT标签检查
     */
    private fun hasProficiency(item: ItemStack): Boolean {
        return NBTEquipmentDataManager.isEquipment(item)
    }


}
