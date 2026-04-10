
package com.sky4th.equipment.modifier

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.enchantment.PrepareItemEnchantEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.entity.*
import org.bukkit.event.player.*
import org.bukkit.inventory.ItemStack
import org.bukkit.attribute.Attribute
import com.sky4th.equipment.EquipmentAffix
import com.sky4th.equipment.data.NBTEquipmentDataManager
import com.sky4th.equipment.event.PlayerDodgeEvent
import com.sky4th.equipment.util.AttributeModifierUtil
import com.sky4th.equipment.attributes.EquipmentCategory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 词条管理器
 * 负责管理所有活跃的词条实例，处理事件分发
 */
class ModifierManager(private val plugin: EquipmentAffix) {

    companion object {
        // 单例实例
        @JvmStatic
        lateinit var instance: ModifierManager
            private set
    }

    // 按事件类型索引的活跃词条列表
    private val activeModifiers = ConcurrentHashMap<Class<out Event>, MutableList<ModifierInstance>>()

    // 按玩家UUID索引的活跃词条列表
    private val playerModifiers = ConcurrentHashMap<UUID, MutableList<ModifierInstance>>()

    // 已注册的词条实现
    val modifierImplementations = ConcurrentHashMap<String, Modifier>()

    // 跟踪被词条影响的实体（用于清理）
    private val affectedEntities = ConcurrentHashMap<UUID, org.bukkit.entity.LivingEntity>()

    init {
        instance = this
    }

    /**
     * 注册词条实现
     * @param modifier 词条实现
     */
    fun registerModifier(modifier: Modifier) {
        modifierImplementations[modifier.getAffixId()] = modifier
    }

    /**
     * 获取词条实现
     * @param affixId 词条ID
     * @return 词条实现，如果不存在则返回null
     */
    fun getModifier(affixId: String): Modifier? {
        return modifierImplementations[affixId]
    }

    /**
     * 检查词条是否已注册实现
     * @param affixId 词条ID
     * @return 如果已注册返回true，否则返回false
     */
    fun hasModifier(affixId: String): Boolean {
        return modifierImplementations.containsKey(affixId)
    }

    /**
     * 获取玩家的词条实例列表（用于调试）
     * @param player 玩家
     * @return 玩家的词条实例列表，如果不存在则返回null
     */
    fun getPlayerModifiers(player: Player): List<ModifierInstance>? {
        return playerModifiers[player.uniqueId]?.toList()
    }

    /**
     * 更新玩家的所有活跃词条
     * @param player 玩家
     * 优化版本：智能更新，只清除真正被移除的词条，只在真正添加新词条时才调用onInit
     */
    fun updatePlayerModifiers(player: Player) {
        com.sky4th.equipment.monitor.PerformanceMonitorHelper.monitor("modifier_update") {
            // 获取玩家当前的词条实例
            val oldInstances = playerModifiers[player.uniqueId]?.toList() ?: emptyList()

            // 获取玩家所有装备物品及其槽位
            val equipmentItems = getPlayerEquipmentItemsWithSlots(player)

            // 收集新词条
            val newInstances = mutableListOf<ModifierInstance>()
            equipmentItems.forEach { (item, slot) ->
                // 处理材质效果词条
                val materialEffect = NBTEquipmentDataManager.getMaterialEffect(item)
                if (materialEffect != "NONE") {
                    val modifier = getModifier(materialEffect)
                    if (modifier != null) {
                        // 检查触发槽位
                        if (shouldTriggerInSlot(modifier, slot)) {
                            newInstances.add(ModifierInstance(modifier, player, item, 1, slot))
                        }
                    }
                }

                // 处理普通词条
                val affixes = NBTEquipmentDataManager.getAffixes(item)
                affixes.forEach { (affixId, level) ->
                    val modifier = getModifier(affixId)
                    if (modifier != null) {
                        // 检查触发槽位
                        if (shouldTriggerInSlot(modifier, slot)) {
                            newInstances.add(ModifierInstance(modifier, player, item, level, slot))
                        }
                    }
                }
            }

            // 比对旧词条和新词条，找出需要移除的词条
            val oldAffixIds = oldInstances.map { it.getAffixId() }.toSet()
            val newAffixIds = newInstances.map { it.getAffixId() }.toSet()
            val removedAffixIds = oldAffixIds - newAffixIds
            val addedAffixIds = newAffixIds - oldAffixIds

            // 只移除真正被移除的词条
            if (removedAffixIds.isNotEmpty()) {
                removeSpecificModifiers(player, removedAffixIds)
            }

            // 清除玩家的所有词条索引
            playerModifiers.remove(player.uniqueId)
            activeModifiers.values.forEach { it.removeIf { it.player.uniqueId == player.uniqueId } }

            // 添加新词条，只对真正新增的词条调用onInit
            newInstances.forEach { instance ->
                addModifierInstance(instance, instance.getAffixId() in addedAffixIds)
            }

            // 调试信息：显示玩家身上的词条（合并相同词条）一下都可以删除
            val finalInstances = playerModifiers[player.uniqueId]
            if (finalInstances != null && finalInstances.isNotEmpty()) {
                println("=== 玩家 ${player.name} 身上的词条 ===")
                // 按词条ID分组
                val groupedModifiers = finalInstances.groupBy { it.getAffixId() }
                groupedModifiers.forEach { (affixId, instances) ->
                    val modifier = getModifier(affixId)
                    val calculationMode = modifier?.getCalculationMode()?.name ?: "HIGHEST"
                    val applicableTo = modifier?.getApplicableTo()?.map { it.name }?.joinToString(", ") ?: "无限制"
                    val equipmentInfo = instances.map { instance ->
                        val item = instance.item
                        val equipmentId = NBTEquipmentDataManager.getEquipmentId(item)
                        val equipmentType = equipmentId?.let { com.sky4th.equipment.registry.EquipmentRegistry.getEquipmentType(it) }
                        val slot = when {
                            player.inventory.itemInMainHand == item -> "主手"
                            player.inventory.itemInOffHand == item -> "副手"
                            player.inventory.helmet == item -> "头盔"
                            player.inventory.chestplate == item -> "胸甲"
                            player.inventory.leggings == item -> "护腿"
                            player.inventory.boots == item -> "靴子"
                            else -> "未知"
                        }
                        val categoryNames = equipmentType?.categories?.joinToString(",") { it.name } ?: "未知"
                        "$slot(${categoryNames})"
                    }.distinct().joinToString(", ")
                    println("  词条: $affixId (数量: ${instances.size}, 计算方式: $calculationMode, 适用装备: $applicableTo, 装备: $equipmentInfo)")
                }
                println("================================")
            } else {
                println("=== 玩家 ${player.name} 身上没有词条 ===")
            }
        }
    }

    /**
     * 获取玩家的装备物品及其槽位
     * 规则：
     * 1. 护甲物品（轻重护甲和鞘翅）在护甲位自动触发，不检测槽位
     * 2. 其他类别物品（武器、工具、盾牌等）通过主副手判断
     * @param player 玩家
     * @return 装备物品及其槽位的列表
     */
    private fun getPlayerEquipmentItemsWithSlots(player: Player): List<Pair<ItemStack, String>> {
        val equipmentItems = mutableListOf<Pair<ItemStack, String>>()

        // 添加护甲物品（轻重护甲和鞘翅）
        player.inventory.armorContents.forEachIndexed { index, item ->
            if (item != null && !item.type.isAir && NBTEquipmentDataManager.isEquipment(item)) {
                val equipmentId = NBTEquipmentDataManager.getEquipmentId(item)
                if (equipmentId != null) {
                    val equipmentType = com.sky4th.equipment.registry.EquipmentRegistry.getEquipmentType(equipmentId)
                    if (equipmentType != null) {
                        // 检查是否是护甲类别
                        val isArmor = equipmentType.categories.any { 
                            it == EquipmentCategory.LIGHT_ARMOR || 
                            it == EquipmentCategory.HEAVY_ARMOR ||
                            it == EquipmentCategory.ELYTRA 
                        }
                        if (isArmor) {
                            val slot = when (index) {
                                0 -> "HEAD"      // 头盔
                                1 -> "CHEST"     // 胸甲
                                2 -> "LEGS"      // 护腿
                                3 -> "FEET"      // 靴子
                                else -> "UNKNOWN"
                            }
                            equipmentItems.add(item to slot)
                        }
                    }
                }
            }
        }

        // 添加主手物品
        val mainHandItem = player.inventory.itemInMainHand
        if (!mainHandItem.type.isAir && NBTEquipmentDataManager.isEquipment(mainHandItem)) {
            val equipmentId = NBTEquipmentDataManager.getEquipmentId(mainHandItem)
            if (equipmentId != null) {
                val equipmentType = com.sky4th.equipment.registry.EquipmentRegistry.getEquipmentType(equipmentId)
                if (equipmentType != null) {
                    // 检查是否不是护甲类别
                    val isArmor = equipmentType.categories.any { 
                        it == EquipmentCategory.LIGHT_ARMOR || 
                        it == EquipmentCategory.HEAVY_ARMOR ||
                        it == EquipmentCategory.ELYTRA 
                    }
                    if (!isArmor) {
                        equipmentItems.add(mainHandItem to "MAIN_HAND")
                    }
                }
            }
        }

        // 添加副手物品
        val offHandItem = player.inventory.itemInOffHand
        if (!offHandItem.type.isAir && NBTEquipmentDataManager.isEquipment(offHandItem)) {
            val equipmentId = NBTEquipmentDataManager.getEquipmentId(offHandItem)
            if (equipmentId != null) {
                val equipmentType = com.sky4th.equipment.registry.EquipmentRegistry.getEquipmentType(equipmentId)
                if (equipmentType != null) {
                    // 检查是否不是护甲类别
                    val isArmor = equipmentType.categories.any { 
                        it == EquipmentCategory.LIGHT_ARMOR || 
                        it == EquipmentCategory.HEAVY_ARMOR ||
                        it == EquipmentCategory.ELYTRA 
                    }
                    if (!isArmor) {
                        equipmentItems.add(offHandItem to "OFF_HAND")
                    }
                }
            }
        }

        return equipmentItems
    }

    /**
     * 检查词条是否应该在指定槽位触发
     * @param modifier 词条
     * @param slot 槽位
     * @return 如果应该触发返回true，否则返回false
     */
    private fun shouldTriggerInSlot(modifier: Modifier, slot: String): Boolean {
        val triggerSlot = modifier.getTriggerSlot().uppercase()

        // 护甲槽位（HEAD, CHEST, LEGS, FEET）不进行槽位检测，穿上就自动触发
        if (slot in listOf("HEAD", "CHEST", "LEGS", "FEET")) {
            return true
        }

        return when (triggerSlot) {
            "HAND" -> slot == "MAIN_HAND" || slot == "OFF_HAND"
            "MAIN_HAND" -> slot == "MAIN_HAND"
            "OFF_HAND" -> slot == "OFF_HAND"
            "ANY" -> true
            else -> slot == triggerSlot
        }
    }

    /**
     * 移除玩家特定的词条
     * @param player 玩家
     * @param affixIds 要移除的词条ID集合
     */
    private fun removeSpecificModifiers(player: Player, affixIds: Set<String>) {
        val playerUuid = player.uniqueId
        val instances = playerModifiers[playerUuid] ?: return

        // 过滤出需要移除的词条实例
        val toRemove = instances.filter { it.getAffixId() in affixIds }

        // 从事件类型索引中移除
        toRemove.forEach { instance ->
            // 调用词条的onRemove方法
            instance.modifier.onRemove(player)

            instance.modifier.getEventTypes().forEach { eventType ->
                val list = activeModifiers[eventType]
                list?.remove(instance)
                // 如果列表为空，移除该事件类型的键
                if (list?.isEmpty() == true) {
                    activeModifiers.remove(eventType)
                }
            }
        }
    }


    /**
     * 添加词条实例
     * @param instance 词条实例
     * @param isinit 是否初始化词条效果
     */
    private fun addModifierInstance(instance: ModifierInstance, isinit: Boolean = true) {
        val player = instance.player
        val modifier = instance.modifier
        val affixId = instance.getAffixId()

        // 添加到玩家索引
        playerModifiers.computeIfAbsent(player.uniqueId) { mutableListOf() }.add(instance)

        // 添加到事件类型索引
        modifier.getEventTypes().forEach { eventType ->
            activeModifiers.computeIfAbsent(eventType) { mutableListOf() }.add(instance)
        }
        if (isinit) {
            // 调用词条的onInit方法，初始化词条效果
            modifier.onInit(player, instance.item, instance.level)
        }
    }

    /**
     * 清除玩家的所有词条实例
     * @param player 玩家
     */
    fun clearPlayerModifiers(player: Player) {
        val playerUuid = player.uniqueId
        val instances = playerModifiers.remove(playerUuid) ?: return

        // 从事件类型索引中移除
        instances.forEach { instance ->
            // 调用词条的onRemove方法
            instance.modifier.onRemove(player)
            instance.modifier.getEventTypes().forEach { eventType ->
                val list = activeModifiers[eventType]
                list?.remove(instance)
                // 如果列表为空，移除该事件类型的键
                if (list?.isEmpty() == true) {
                    activeModifiers.remove(eventType)
                }
            }
        }
    }

    /**
     * 处理事件
     * @param event 触发的事件
     * 优化版本：添加性能监控，支持按计算方式合并相同词条，支持处理多个相关玩家，支持角色限制
     */
    fun handleEvent(event: Event) {
        com.sky4th.equipment.monitor.PerformanceMonitorHelper.monitor("modifier_handle_event") {
            
            // 除了右键空气，检查事件是否已取消，如果已取消则跳过处理
            if (event !is PlayerInteractEvent) {
                if (event is org.bukkit.event.Cancellable && event.isCancelled) return
            }

            // 如果属于攻击事件且攻击伤害为0，就跳过不用处理
            if (event is EntityDamageEvent && event.damage <= 0) {
                return
            }

            val eventClass = event.javaClass

            // 获取事件中的所有相关玩家
            val eventPlayers = extractPlayers(event) ?: return
            // 处理攻击者的词条
            eventPlayers.attacker?.let { attacker ->
                processPlayerModifiers(event, attacker, com.sky4th.equipment.modifier.config.PlayerRole.ATTACKER, eventClass)
            }

            // 处理受害者的词条
            eventPlayers.defender?.let { defender ->
                processPlayerModifiers(event, defender, com.sky4th.equipment.modifier.config.PlayerRole.DEFENDER, eventClass)
            }

            // 处理自身的词条（单个玩家事件）
            eventPlayers.self?.let { self ->
                processPlayerModifiers(event, self, com.sky4th.equipment.modifier.config.PlayerRole.SELF, eventClass)
            }

            // 处理其他玩家的词条
            eventPlayers.others.forEach { other ->
                processPlayerModifiers(event, other, com.sky4th.equipment.modifier.config.PlayerRole.OTHER, eventClass)
            }
        }
    }

    /**
     * 检测事件中的物品是否与词条物品匹配
     * @param event 事件
     * @param affixItem 词条所在的物品
     * @return 如果匹配返回true，否则返回false
     */
    private fun checkEventItemMatch(event: Event, affixItem: ItemStack): Boolean {
        // 对于涉及物品的事件，检查事件中的物品是否是词条所在的物品
        return when (event) {
            is org.bukkit.event.player.PlayerInteractEvent -> {
                // 玩家交互事件：检查交互的物品
                event.item == affixItem
            }
            is org.bukkit.event.player.PlayerInteractEntityEvent -> {
                // 玩家与实体交互事件：检查交互的物品
                event.player.inventory.itemInMainHand == affixItem || 
                event.player.inventory.itemInOffHand == affixItem
            }
            is org.bukkit.event.player.PlayerFishEvent -> {
                // 钓鱼事件：检查使用的钓鱼竿
                event.player.inventory.itemInMainHand == affixItem || 
                event.player.inventory.itemInOffHand == affixItem
            }
            is org.bukkit.event.player.PlayerItemDamageEvent -> {
                // 物品损坏事件：检查损坏的物品
                event.item == affixItem
            }
            is org.bukkit.event.entity.EntityDamageByEntityEvent -> {
                // 实体伤害事件：检查攻击者使用的物品
                val damager = event.damager
                if (damager is Player) {
                    damager.inventory.itemInMainHand == affixItem || 
                    damager.inventory.itemInOffHand == affixItem
                } else if (damager is org.bukkit.entity.Projectile) {
                    // 如果是投掷物，检查射击者使用的物品
                    val shooter = damager.shooter
                    if (shooter is Player) {
                        shooter.inventory.itemInMainHand == affixItem || 
                        shooter.inventory.itemInOffHand == affixItem
                    } else {
                        false
                    }
                } else {
                    true // 对于非玩家造成的伤害，不进行物品检查
                }
            }
            is org.bukkit.event.entity.EntityShootBowEvent -> {
                // 射箭事件：检查使用的弓
                event.bow == affixItem
            }
            is org.bukkit.event.block.BlockBreakEvent -> {
                // 破坏方块事件：检查使用的工具
                event.player.inventory.itemInMainHand == affixItem
            }
            is org.bukkit.event.block.BlockDamageEvent -> {
                // 损坏方块事件：检查使用的工具
                event.player.inventory.itemInMainHand == affixItem
            }
            else -> true // 对于其他事件，不进行物品检查
        }
    }

    /**
     * 处理单个玩家的词条
     * @param event 触发的事件
     * @param player 要处理的玩家
     * @param playerRole 玩家在事件中的角色
     * @param eventClass 事件类型
     */
    private fun processPlayerModifiers(
        event: Event,
        player: Player,
        playerRole: com.sky4th.equipment.modifier.config.PlayerRole,
        eventClass: Class<out Event>
    ) {
        // 获取该玩家的词条实例
        val playerInstances = playerModifiers[player.uniqueId] ?: return

        // 按词条ID分组，便于合并计算
        val groupedInstances = playerInstances.groupBy { it.getAffixId() }

        // 按优先级排序词条组（优先级数值越小越先执行）
        val sortedGroups = groupedInstances.entries.sortedBy { (_, instances) ->
            instances.first().getPriority()
        }

        // 处理相关词条
        sortedGroups.forEach { (affixId, instances) ->
            val modifier = getModifier(affixId) ?: return@forEach
            
            // 检查角色限制（使用事件特定的角色限制）
            // 需要检查所有父类，因为配置中可能使用父类（如EntityDamageEvent）
            var roleRestriction: com.sky4th.equipment.modifier.config.PlayerRole? = null
            var currentClass: Class<out Event>? = eventClass
            while (currentClass != null && roleRestriction == null) {
                roleRestriction = modifier.getRoleRestriction(currentClass)
                currentClass = currentClass.superclass as? Class<out Event>
            }
            if (roleRestriction != null && roleRestriction != playerRole) {
                return@forEach
            }
            
            // 检查事件类型是否匹配
            val eventTypes = modifier.getEventTypes()
            val matches = eventTypes.any { eventType ->
                eventType.isAssignableFrom(eventClass)
            }

            if (matches) {
                try {
                    // 根据计算方式计算有效等级
                    val calculationMode = modifier.getCalculationMode()
                    val effectiveLevel = when (calculationMode) {
                        com.sky4th.equipment.attributes.AffixCalculationMode.HIGHEST ->
                            instances.maxOf { it.level }
                        com.sky4th.equipment.attributes.AffixCalculationMode.SUM ->
                            instances.sumOf { it.level }
                        com.sky4th.equipment.attributes.AffixCalculationMode.AVERAGE ->
                            instances.map { it.level }.average().toInt()
                    }

                    // 使用第一个实例的物品和玩家
                    val firstInstance = instances.first()

                    // 检查事件中的物品是否与词条物品匹配
                    if (!checkEventItemMatch(event, firstInstance.item)) {
                        return@forEach
                    }

                    println("ModifierManager.handleEvent: 处理词条 $affixId (玩家: ${player.name}, 角色: $playerRole, 数量: ${instances.size}, 计算方式: $calculationMode, 有效等级: $effectiveLevel, 优先级: ${firstInstance.getPriority()})")
                    modifier.handle(event, player, firstInstance.item, effectiveLevel, playerRole)
                } catch (e: Exception) {
                    plugin.logger.warning("词条 $affixId 处理事件时出错: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }


    /**
     * 事件相关玩家数据类
     * @param attacker 攻击者（可能为null）
     * @param defender 防御者/受害者（可能为null）
     * @param self 自身（单个玩家事件，可能为null）
     * @param others 其他相关玩家列表
     */
    data class EventPlayers(
        val attacker: Player? = null,
        val defender: Player? = null,
        val self: Player? = null,
        val others: List<Player> = emptyList()
    ) {
        /**
         * 获取所有相关玩家（去重）
         */
        fun getAllPlayers(): List<Player> {
            val players = mutableListOf<Player>()
            attacker?.let { players.add(it) }
            defender?.let { players.add(it) }
            self?.let { players.add(it) }
            players.addAll(others)
            return players.distinctBy { it.uniqueId }
        }
    }

    /**
     * 关闭词条管理器
     * 清理所有被词条影响的实体上的equipment_affix命名空间的修饰符
     */
    fun shutdown() {
        // 只清理被跟踪的实体，避免遍历所有实体
        affectedEntities.values.forEach { entity ->
            if (entity.isValid) {
                removeAllAffixModifiers(entity)
            }
        }

        // 清空跟踪列表
        affectedEntities.clear()
    }

    /**
     * 移除实体上所有属于equipment_affix命名空间的修饰符
     * @param entity 实体
     */
    private fun removeAllAffixModifiers(entity: org.bukkit.entity.LivingEntity) {
        // 遍历所有属性，移除属于equipment_affix命名空间的修饰符
        Attribute.values().forEach { attribute ->
            val attributeInstance = entity.getAttribute(attribute) ?: return@forEach

            // 移除所有属于equipment_affix命名空间的修饰符
            val modifiers = attributeInstance.modifiers.toList()
            for (modifier in modifiers) {
                if (modifier.key.namespace == "equipment_affix") {
                    attributeInstance.removeModifier(modifier)
                }
            }
        }
    }

    /**
     * 跟踪被词条影响的实体
     * @param entity 被影响的实体
     */
    fun trackAffectedEntity(entity: org.bukkit.entity.LivingEntity) {
        affectedEntities[entity.uniqueId] = entity
    }

    /**
     * 停止跟踪实体（当实体不再受词条影响时调用）
     * @param entity 实体
     */
    fun untrackAffectedEntity(entity: org.bukkit.entity.LivingEntity) {
        affectedEntities.remove(entity.uniqueId)
    }

    /**
     * 根据事件语义提取所有相关玩家
     * @param event 事件对象
     * @return 事件相关玩家信息，若无则返回 null
     */
    fun extractPlayers(event: Event): EventPlayers? {
        return when (event) {
            // ========== 单个玩家事件（使用self角色） ==========
            is PlayerEvent -> EventPlayers(self = event.player)
            is BlockBreakEvent -> EventPlayers(self = event.player)
            is BlockPlaceEvent -> EventPlayers(self = event.player)
            is BlockDamageEvent -> EventPlayers(self = event.player)
            is BlockDropItemEvent -> EventPlayers(self = event.player)
            is EnchantItemEvent -> EventPlayers(self = event.enchanter)
            is PrepareItemEnchantEvent -> EventPlayers(self = event.enchanter)

            // ========== 实体事件（需要精细处理） ==========
            is EntityDamageEvent -> {
                if (event is EntityDamageByEntityEvent) {
                    // 检查是否是投掷物造成的伤害
                    val damager = event.damager
                    if (damager is org.bukkit.entity.Projectile) {
                        // 如果是投掷物，从投掷物获取射击者
                        EventPlayers(
                            attacker = damager.shooter as? Player,
                            defender = event.entity as? Player
                        )
                    } else {
                        // 否则直接使用damager作为攻击者
                        EventPlayers(
                            attacker = damager as? Player,
                            defender = event.entity as? Player
                        )
                    }
                } else {
                    // 环境伤害或其他无直接攻击者的事件，返回受害者
                    EventPlayers(defender = event.entity as? Player)
                }
            }

            is PlayerDodgeEvent -> {
                EventPlayers(defender = event.player)
            }

            is EntityDeathEvent -> {
                val victim = event.entity as? Player
                val killer = event.entity.killer
                EventPlayers(
                    attacker = killer,
                    defender = victim
                )
            }

            is EntityShootBowEvent -> {
                EventPlayers(self = event.entity as? Player)
            }

            is ProjectileHitEvent -> {
                EventPlayers(self = event.entity.shooter as? Player)
            }

            is EntityRegainHealthEvent -> {
                EventPlayers(self = event.entity as? Player)
            }

            is EntityPotionEffectEvent -> {
                EventPlayers(self = event.entity as? Player)
            }

            is EntityTargetEvent -> {
                val entity = event.entity as? Player
                val target = event.target as? Player
                if (entity != null && target != null) {
                    // 两个玩家，使用attacker和defender
                    EventPlayers(
                        attacker = entity,
                        defender = target
                    )
                } else if (entity != null) {
                    // 只有实体是玩家，使用self
                    EventPlayers(self = entity)
                } else if (target != null) {
                    // 只有目标是玩家，使用self
                    EventPlayers(self = target)
                } else {
                    null
                }
            }

            is EntityTameEvent -> {
                EventPlayers(self = event.owner as? Player)
            }

            is EntityTeleportEvent -> {
                EventPlayers(self = event.entity as? Player)
            }

            is EntityPickupItemEvent -> {
                EventPlayers(self = event.entity as? Player)
            }

            is ProjectileLaunchEvent -> {
                // 投掷物发射事件，获取发射者
                EventPlayers(self = event.entity.shooter as? Player)
            }

            is InventoryCloseEvent -> {
                EventPlayers(self = event.player as? Player)
            }
            is EntityToggleGlideEvent -> {
                EventPlayers(self = event.entity as? Player)
            }

            // 其他未明确处理的事件返回 null
            else -> null
        }
    }
}
