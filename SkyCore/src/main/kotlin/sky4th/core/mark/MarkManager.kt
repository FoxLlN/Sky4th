package sky4th.core.mark

import net.kyori.adventure.text.Component
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Transformation
import org.bukkit.util.Vector
import org.joml.Vector3f
import sky4th.core.api.MarkAPI
import java.util.UUID

/**
 * 标记显示管理器
 * 使用挂载系统 + 变换平移，实现平滑跟随头顶标记
 * 每个标记作为乘客挂载到目标实体上，并通过 transformation 设置偏移量
 * 
 * 对于玩家，会自动创建自定义名字标签，避免被标记遮挡
 */
object MarkManager {

    // 存储每个实体对应的标记信息
    private data class MarkInfo(
        val display: ItemDisplay,
        val markId: String,
        var xOffset: Double, // 水平偏移（用于排列）
        var expireTime: Long = Long.MAX_VALUE, // 过期时间戳（毫秒），默认为永不过期
        var taskId: Int? = null // 定时任务ID，用于取消任务
    )

    // 存储玩家的自定义名字标签
    private val playerNameTags = mutableMapOf<Player, TextDisplay>()

    // 存储实体的自定义名字标签
    private val entityNameTags = mutableMapOf<LivingEntity, TextDisplay>()

    private val entityMarks = mutableMapOf<Entity, MutableList<MarkInfo>>()
    private val markIdToInfo = mutableMapOf<String, MutableMap<UUID, MarkInfo>>()
    private val entityMarkIds = mutableMapOf<Entity, MutableSet<String>>()

    // 存储玩家的标记数据（用于玩家下线后恢复）
    private data class SavedMarkData(
        val markId: String,
        val itemStack: ItemStack,
        val showToAllPlayers: Boolean,
        val xOffset: Double,
        val expireTime: Long // 过期时间戳（毫秒）
    )
    private val playerMarkData = mutableMapOf<java.util.UUID, MutableList<SavedMarkData>>()

    // 存储插件实例
    private var pluginInstance: JavaPlugin? = null
    
    // PDC 命名空间键（延迟初始化）
    private var CUSTOM_NAME_KEY: NamespacedKey? = null

    // 获取插件实例
    fun getPlugin(): JavaPlugin? = pluginInstance

    // 初始化方法，需要在插件启用时调用
    fun init(plugin: JavaPlugin) {
        this.pluginInstance = plugin
        this.CUSTOM_NAME_KEY = NamespacedKey(plugin, "custom_name")
    }

    // 标记缩放比例
    private const val DISPLAY_SCALE = 0.25f
    // 标记之间的间距
    private const val MARK_SPACING = 0.3

    /**
     * 创建头顶标记
     * @param victim 被标记的实体
     * @param markId 标记唯一ID
     * @param itemStack 显示的物品
     * @param showToAllPlayers 是否对所有玩家可见
     * @param duration 持续时间（秒），0或负数表示永不过期
     * @return Display 实体ID，如果实体类型不允许标记则返回 -1
     */
    fun createMarkDisplay(
        victim: LivingEntity,
        markId: String,
        itemStack: ItemStack,
        showToAllPlayers: Boolean = false,
        duration: Long = 0
    ): Int {
        // 检查实体类型是否允许标记
        if (!isEntityMarkable(victim)) {
            return -1
        }

        // 如果是玩家且还没有自定义名字标签，则创建一个
        if (victim is Player && !playerNameTags.containsKey(victim)) {
            createPlayerNameTag(victim)
        }

        // 如果实体有自定义名字且还没有自定义名字标签，则创建一个
        if (victim !is Player && victim.customName() != null && !entityNameTags.containsKey(victim)) {
            // 将Component转换为String
            val name = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(victim.customName()!!)
            createEntityNameTag(victim, name)
        }

        // 检查是否已存在同名标记，如果存在则刷新时间
        val entityMarkMap = markIdToInfo.getOrPut(markId) { mutableMapOf() }
        val existingInfo = entityMarkMap[victim.uniqueId]

        if (existingInfo != null && existingInfo.display.isValid) {
            // 计算新的过期时间
            val newExpireTime = if (duration > 0) {
                System.currentTimeMillis() + duration * 1000
            } else {
                Long.MAX_VALUE
            }

            // 只有当新的过期时间比原标记的过期时间更长时，才更新过期时间和定时任务
            if (newExpireTime > existingInfo.expireTime) {
                existingInfo.expireTime = newExpireTime
                
                // 如果有过期时间，取消旧任务并创建新任务
                if (duration > 0) {
                    val plugin = pluginInstance ?: return existingInfo.display.entityId

                    // 取消旧的定时任务
                    existingInfo.taskId?.let {
                        plugin.server.scheduler.cancelTask(it)
                    }

                    // 创建新的定时任务
                    val task = plugin.server.scheduler.runTaskLater(plugin, Runnable {
                        removeMarkDisplay(markId)
                    }, duration * 20L)
                    existingInfo.taskId = task.taskId
                }
            }

            // 更新显示物品
            existingInfo.display.setItemStack(itemStack)
            existingInfo.display.isVisibleByDefault = showToAllPlayers

            // 重新排列标记
            rearrangeMarks(victim)

            return existingInfo.display.entityId
        }


        // 1. 创建 ItemDisplay 实体
        val display = victim.world.spawn(victim.location, ItemDisplay::class.java) { it ->
            it.setItemStack(itemStack)
            it.isVisibleByDefault = showToAllPlayers
            it.shadowRadius = 0f
            it.shadowStrength = 0f
            it.billboard = org.bukkit.entity.Display.Billboard.CENTER // 始终面向观察者
        }

        // 2. 计算头顶高度（使用 bounding box 动态计算）
        val height = getDisplayHeight(victim)

        // 3. 计算过期时间
        val expireTime = if (duration > 0) {
            System.currentTimeMillis() + duration * 1000
        } else {
            Long.MAX_VALUE
        }

        // 4. 设置变换：缩放 + 平移（初始 x=0，之后会重新排列）
        val translation = Vector3f(0f, height.toFloat(), 0f)
        val scale = Vector3f(DISPLAY_SCALE, DISPLAY_SCALE, DISPLAY_SCALE)
        val transformation = Transformation(
            translation,
            org.joml.Quaternionf(), // 无旋转
            scale,
            org.joml.Quaternionf()
        )
        display.transformation = transformation

        // 5. 将 Display 挂载到目标实体上
        victim.addPassenger(display)

        // 6. 存储信息
        val info = MarkInfo(display, markId, 0.0, expireTime)

        // 先清理无效的标记
        val existingMarks = entityMarks[victim]
        if (existingMarks != null) {
            // 过滤出有效的标记
            val validMarks = existingMarks.filter { it.display.isValid }.toMutableList()
            // 如果没有有效的标记，移除该实体的标记列表
            if (validMarks.isEmpty()) {
                entityMarks.remove(victim)
            } else {
                entityMarks[victim] = validMarks
            }
        }

        // 添加新标记
        entityMarks.getOrPut(victim) { mutableListOf() }.add(info)
        markIdToInfo.getOrPut(markId) { mutableMapOf() }[victim.uniqueId] = info
        entityMarkIds.getOrPut(victim) { mutableSetOf() }.add(markId)

        // 7. 重新排列所有标记（更新水平偏移）
        rearrangeMarks(victim)

        // 8. 如果有过期时间，设置定时移除
        if (duration > 0) {
            val plugin = pluginInstance ?: return display.entityId
            val task = plugin.server.scheduler.runTaskLater(plugin, Runnable {
                removeMarkDisplay(victim, markId)
            }, duration * 20L)
            info.taskId = task.taskId
        }

        return display.entityId
    }

    /**
     * 移除指定实体的指定标记
     */
    fun removeMarkDisplay(victim: Entity, markId: String) {
        val entityMarkMap = markIdToInfo[markId] ?: return
        val info = entityMarkMap.remove(victim.uniqueId) ?: return

        // 从实体标记列表中移除
        val infos = entityMarks[victim]
        if (infos != null) {
            infos.remove(info)
            // 从entityMarkIds中移除
            entityMarkIds[victim]?.remove(markId)
            if (entityMarkIds[victim]?.isEmpty() == true) {
                entityMarkIds.remove(victim)
            }
            // 清理无效的标记
            val validInfos = infos.filter { it.display.isValid }.toMutableList()
            if (validInfos.isEmpty()) {
                entityMarks.remove(victim)
            } else {
                entityMarks[victim] = validInfos
            }
        }

        // 如果该markId没有其他实体，则移除整个map
        if (entityMarkMap.isEmpty()) {
            markIdToInfo.remove(markId)
        }

        // 移除Display实体
        if (info.display.isValid) {
            info.display.remove()
        }

        // 重新排列剩余标记
        val remainingMarks = entityMarks[victim]
        if (remainingMarks != null && remainingMarks.isNotEmpty()) {
            rearrangeMarks(victim)
        }
    }

    /**
     * 移除指定实体的所有标记
     */
    fun removeMarkDisplay(victim: Entity) {
        val infos = entityMarks.remove(victim) ?: return
        entityMarkIds.remove(victim)  // 移除实体的所有标记ID
        infos.forEach { info ->
            info.display.remove()
            // 从markIdToInfo中移除该实体的标记
            val entityMarkMap = markIdToInfo[info.markId]
            if (entityMarkMap != null) {
                entityMarkMap.remove(victim.uniqueId)
                // 如果该markId没有其他实体，则移除整个map
                if (entityMarkMap.isEmpty()) {
                    markIdToInfo.remove(info.markId)
                }
            }
        }
        
        // 如果是玩家，移除自定义名字标签
        if (victim is Player) {
            removePlayerNameTag(victim)
        }
        // 如果是LivingEntity且有自定义名字标签，移除它
        if (victim is LivingEntity && entityNameTags.containsKey(victim)) {
            removeEntityNameTag(victim)
        }
    }

    /**
     * 移除指定 ID 的标记
     */
    fun removeMarkDisplay(markId: String) {
        val entityMarkMap = markIdToInfo.remove(markId) ?: return

        // 遍历所有具有该markId的实体
        entityMarkMap.forEach { (uuid, info) ->
            // 取消定时任务
            info.taskId?.let {
                pluginInstance?.server?.scheduler?.cancelTask(it)
            }

            // 获取实体引用
            val entity = org.bukkit.Bukkit.getEntity(uuid)

            // 取消定时任务
            info.taskId?.let { 
                pluginInstance?.server?.scheduler?.cancelTask(it) 
            }

            // 从实体标记列表中移除
            if (entity != null) {
                val infos = entityMarks[entity]
                if (infos != null) {
                    // 移除指定的标记
                    infos.remove(info)
                    // 从entityMarkIds中移除
                    entityMarkIds[entity]?.remove(markId)
                    if (entityMarkIds[entity]?.isEmpty() == true) {
                        entityMarkIds.remove(entity)
                    }
                    // 清理无效的标记
                    val validInfos = infos.filter { it.display.isValid }.toMutableList()
                    if (validInfos.isEmpty()) {
                        entityMarks.remove(entity)
                    } else {
                        entityMarks[entity] = validInfos
                    }
                }
            } else {
                // 如果Display没有挂载到任何实体，尝试从所有实体的标记列表中查找并移除
                entityMarks.forEach { (_, marks) ->
                    marks.remove(info)
                }
                // 从所有实体的entityMarkIds中移除
                entityMarkIds.forEach { (_, ids) ->
                    ids.remove(markId)
                }
                // 清理空列表
                val emptyEntities = entityMarks.filter { (_, marks) -> marks.isEmpty() }.keys.toList()
                emptyEntities.forEach { entityMarks.remove(it) }
            }

            // 移除Display实体（在清理列表之后）
            if (info.display.isValid) {
                info.display.remove()
            }

            // 重新排列剩余标记（在移除Display之后）
            if (entity != null) {
                val remainingMarks = entityMarks[entity]
                if (remainingMarks != null && remainingMarks.isNotEmpty()) {
                    rearrangeMarks(entity)
                }
            }
        }
    }

    /**
     * 重新排列指定实体上的所有标记（计算水平偏移和高度）
     */
    fun rearrangeMarks(entity: Entity) {
        val infos = entityMarks[entity] ?: return

        // 过滤出有效的标记
        val validInfos = infos.filter { it.display.isValid }

        // 如果没有有效的标记，从列表中移除该实体
        if (validInfos.isEmpty()) {
            entityMarks.remove(entity)
            return
        }

        // 更新列表，只保留有效的标记
        entityMarks[entity] = validInfos.toMutableList()

        val size = validInfos.size
        val totalWidth = (size - 1) * MARK_SPACING
        val startX = -totalWidth / 2.0

        // 获取当前变换（用于保留旋转和缩放）
        val sampleDisplay = validInfos.first().display
        val currentTrans = sampleDisplay.transformation

        // 重新计算高度（考虑是否有名字标签）
        val height = if (entity is LivingEntity) {
            getDisplayHeight(entity)
        } else {
            currentTrans.translation.y.toDouble()
        }

        validInfos.forEachIndexed { index, info ->
            val x = startX + index * MARK_SPACING
            info.xOffset = x

            // 更新 Display 的变换平移
            val translation = Vector3f(x.toFloat(), height.toFloat(), 0f)
            val newTrans = Transformation(
                translation,
                currentTrans.leftRotation,
                currentTrans.scale,
                currentTrans.rightRotation
            )
            info.display.transformation = newTrans
        }
    }

    /**
     * 更新标记位置（兼容旧系统 API）
     * 由于使用挂载系统，标记会自动跟随实体，此方法为空实现
     */
    fun updateMarkPosition(entity: Entity) {
        // 挂载系统会自动处理位置更新，无需手动操作
    }

    /**
     * 检查实体是否有标记
     */
    fun hasMark(entity: Entity): Boolean = entityMarks.containsKey(entity)

    /**
     * 检查特定 ID 的标记是否存在
     */
    fun hasMark(markId: String): Boolean {
        val entityMarkMap = markIdToInfo[markId] ?: return false
        return entityMarkMap.isNotEmpty()
    }

    /**
     * 检查实体是否有特定的标记
     * @param entity 要检查的实体
     * @param markId 标记的唯一标识符
     * @return 是否有该标记
     */
    fun hasMark(entity: Entity, markId: String): Boolean {
        return entityMarkIds[entity]?.contains(markId) ?: false
    }

    /**
     * 获取实体的所有 Display 实体
     */
    fun getMarkDisplays(entity: Entity): List<ItemDisplay> =
        entityMarks[entity]?.map { it.display } ?: emptyList()

    /**
     * 获取特定 ID 的 Display 实体（返回第一个实体的Display）
     */
    fun getMarkDisplay(markId: String): ItemDisplay? {
        val entityMarkMap = markIdToInfo[markId] ?: return null
        return entityMarkMap.values.firstOrNull()?.display
    }

    /**
     * 清理所有标记
     */
    fun clearAllMarks() {
        entityMarks.values.forEach { infos -> infos.forEach { it.display.remove() } }
        entityMarks.clear()
        markIdToInfo.clear()
        entityMarkIds.clear()
    }

    /**
     * 为玩家创建自定义名字标签
     * 使用 TextDisplay 实体显示玩家名字，避免被标记遮挡
     */
    fun createPlayerNameTag(player: Player) {
        // 隐藏玩家的原始名字标签
        player.isCustomNameVisible = false
        player.playerListName(net.kyori.adventure.text.Component.text(player.name))

        // 创建 TextDisplay 实体
        val nameTag = player.world.spawn(player.location, TextDisplay::class.java) { it ->
            // 设置显示的文本（玩家名字）
            it.text(Component.text(player.name))
            // 设置为始终面向玩家
            it.billboard = org.bukkit.entity.Display.Billboard.CENTER
            // 设置黑色半透明背景（更不透明，减少透视效果）
            it.setBackgroundColor(org.bukkit.Color.fromARGB(64, 0, 0, 0))
            // 关闭透视效果
            it.setSeeThrough(false)
            // 设置阴影
            it.shadowRadius = 0f
            it.shadowStrength = 0f
            // 设置文本对齐方式
            it.alignment = org.bukkit.entity.TextDisplay.TextAlignment.CENTER
            // 设置为可见
            it.isVisibleByDefault = true
            // 设置缩放
            it.transformation = Transformation(
                Vector3f(0f, 0.2f, 0f), // 位置：玩家头顶上方 0.2 格
                org.joml.Quaternionf(),
                Vector3f(1f, 1f, 1f),
                org.joml.Quaternionf()
            )
        }

        // 将名字标签挂载到玩家身上
        player.addPassenger(nameTag)

        // 存储名字标签
        playerNameTags[player] = nameTag
    }

    /**
     * 移除玩家的自定义名字标签
     */
    fun removePlayerNameTag(player: Player) {
        val nameTag = playerNameTags.remove(player) ?: return
        nameTag.remove()
        
        // 恢复玩家的原始名字标签
        player.isCustomNameVisible = true
    }

    /**
     * 为实体创建自定义名字标签
     * 使用 TextDisplay 实体显示实体名字，避免被标记遮挡
     */
    fun createEntityNameTag(entity: LivingEntity, name: String) {
        // 隐藏实体的原始名字标签
        entity.isCustomNameVisible = false

        // 将名字存入PDC
        val pdc = entity.persistentDataContainer
        val key = CUSTOM_NAME_KEY ?: return
        pdc.set(key, PersistentDataType.STRING, name)

        // 创建 TextDisplay 实体
        val nameTag = entity.world.spawn(entity.location, TextDisplay::class.java) { it ->
            // 设置显示的文本（实体名字）
            it.text(Component.text(name))
            // 设置为始终面向观察者
            it.billboard = org.bukkit.entity.Display.Billboard.CENTER
            // 设置黑色半透明背景
            it.setBackgroundColor(org.bukkit.Color.fromARGB(64, 0, 0, 0))
            // 关闭透视效果
            it.setSeeThrough(false)
            // 设置阴影
            it.shadowRadius = 0f
            it.shadowStrength = 0f
            // 设置文本对齐方式
            it.alignment = org.bukkit.entity.TextDisplay.TextAlignment.CENTER
            // 设置为可见
            it.isVisibleByDefault = true
            // 设置缩放
            it.transformation = Transformation(
                Vector3f(0f, 0.2f, 0f), // 位置：实体头顶上方 0.2 格
                org.joml.Quaternionf(),
                Vector3f(1f, 1f, 1f),
                org.joml.Quaternionf()
            )
        }

        // 将名字标签挂载到实体身上
        entity.addPassenger(nameTag)

        // 存储名字标签
        entityNameTags[entity] = nameTag
    }

    /**
     * 移除实体的自定义名字标签
     */
    fun removeEntityNameTag(entity: LivingEntity) {
        val nameTag = entityNameTags.remove(entity) ?: return
        nameTag.remove()

        // 从PDC中移除名字
        val pdc = entity.persistentDataContainer
        val key = CUSTOM_NAME_KEY
        if (key != null) {
            pdc.remove(key)
        }

        // 恢复实体的原始名字标签
        entity.isCustomNameVisible = true
    }

    /**
     * 检查实体是否有标记
     */
    fun hasEntityMarks(entity: Entity): Boolean {
        return entityMarks.containsKey(entity)
    }

    /**
     * 检查实体是否有自定义名字标签
     */
    fun hasEntityNameTag(entity: LivingEntity): Boolean {
        return entityNameTags.containsKey(entity)
    }

    /**
     * 获取实体的自定义名字标签
     * @param entity 实体
     * @return 名字标签TextDisplay实体，如果不存在则返回null
     */
    fun getEntityNameTag(entity: LivingEntity): org.bukkit.entity.TextDisplay? {
        return entityNameTags[entity]
    }

    /**
     * 恢复玩家的标记显示
     */
    fun restorePlayerMarks(player: Player) {
        val savedData = playerMarkData.remove(player.uniqueId)
        if (savedData != null && savedData.isNotEmpty()) {
            val plugin = pluginInstance ?: return
            val currentTime = System.currentTimeMillis()

            // 过滤出未过期的标记
            val validData = savedData.filter { it.expireTime > currentTime }

            if (validData.isNotEmpty()) {
                // 延迟一tick创建标记，确保玩家已完全加载
                plugin.server.scheduler.runTaskLater(plugin, Runnable {
                    validData.forEach { data ->
                        // 创建新的Display
                        val display = player.world.spawn(player.location, ItemDisplay::class.java) { it ->
                            it.setItemStack(data.itemStack)
                            it.isVisibleByDefault = data.showToAllPlayers
                            it.shadowRadius = 0f
                            it.shadowStrength = 0f
                            it.billboard = org.bukkit.entity.Display.Billboard.CENTER
                        }

                        // 计算高度
                        val height = getDisplayHeight(player)

                        // 设置变换
                        val translation = Vector3f(data.xOffset.toFloat(), height.toFloat(), 0f)
                        val scale = Vector3f(DISPLAY_SCALE, DISPLAY_SCALE, DISPLAY_SCALE)
                        val transformation = Transformation(
                            translation,
                            org.joml.Quaternionf(),
                            scale,
                            org.joml.Quaternionf()
                        )
                        display.transformation = transformation

                        // 将Display挂载到玩家身上
                        player.addPassenger(display)

                        // 存储信息
                        val info = MarkInfo(display, data.markId, data.xOffset, data.expireTime)
                        entityMarks.getOrPut(player) { mutableListOf() }.add(info)
                        val entityMarkMap = markIdToInfo.getOrPut(data.markId) { mutableMapOf() }
                        entityMarkMap[player.uniqueId] = info
                        entityMarkIds.getOrPut(player) { mutableSetOf() }.add(data.markId)

                        // 如果有过期时间，设置定时移除
                        if (data.expireTime != Long.MAX_VALUE) {
                            val remainingSeconds = (data.expireTime - currentTime) / 1000
                            if (remainingSeconds > 0) {
                                val task = plugin.server.scheduler.runTaskLater(plugin, Runnable {
                                    removeMarkDisplay(player, data.markId)
                                }, remainingSeconds * 20L)
                                info.taskId = task.taskId
                            } else {
                                // 如果已经过期，立即移除
                                removeMarkDisplay(player, data.markId)
                            }
                        }
                    }

                    // 重新排列标记
                    rearrangeMarks(player)
                }, 1L)
            }
        }
    }

    /**
     * 保存玩家的标记数据并清除显示
     */
    fun saveAndClearPlayerMarks(player: Player) {
        // 保存玩家的标记数据
        val marks = entityMarks[player]
        if (marks != null && marks.isNotEmpty()) {
            val savedData = marks.map { info ->
                SavedMarkData(
                    markId = info.markId,
                    itemStack = info.display.itemStack,
                    showToAllPlayers = info.display.isVisibleByDefault,
                    xOffset = info.xOffset,
                    expireTime = info.expireTime
                )
            }.toMutableList()
            playerMarkData[player.uniqueId] = savedData
        }

        // 移除玩家的所有标记显示
        val marksToRemove = entityMarks[player]?.toList() ?: emptyList()
        marksToRemove.forEach { info ->
            // 从markIdToInfo中移除
            val entityMarkMap = markIdToInfo[info.markId]
            if (entityMarkMap != null) {
                entityMarkMap.remove(player.uniqueId)
                // 如果该markId没有其他实体，则移除整个map
                if (entityMarkMap.isEmpty()) {
                    markIdToInfo.remove(info.markId)
                }
            }
            // 移除Display实体
            if (info.display.isValid) {
                info.display.remove()
            }
        }
        // 从entityMarks和entityMarkIds中移除
        entityMarks.remove(player)
        entityMarkIds.remove(player)
    }

    /**
     * 实体瞬移后重新创建标记
     */
    fun recreateMarksAfterTeleport(entity: LivingEntity) {
        // 先清理无效的标记
        val marks = entityMarks[entity]?.toList() ?: return
        val validMarks = marks.filter { it.display.isValid }

        if (validMarks.isEmpty()) {
            // 如果没有有效的标记，移除该实体的标记列表
            entityMarks.remove(entity)
            entityMarkIds.remove(entity)
            return
        }

        // 重新创建有效的标记
        val newMarks = mutableListOf<MarkInfo>()
        val newMarkIds = mutableSetOf<String>()
        validMarks.forEach { info ->
            // 移除旧的Display
            info.display.remove()
            // 从markIdToInfo中移除旧信息
            val entityMarkMap = markIdToInfo[info.markId]
            if (entityMarkMap != null) {
                entityMarkMap.remove(entity.uniqueId)
            }
            // 创建新的Display
            val newDisplay = entity.world.spawn(entity.location, ItemDisplay::class.java) { it ->
                it.setItemStack(org.bukkit.inventory.ItemStack(info.display.itemStack.type))
                it.isVisibleByDefault = info.display.isVisibleByDefault
                it.shadowRadius = info.display.shadowRadius
                it.shadowStrength = info.display.shadowStrength
                it.billboard = info.display.billboard
                it.transformation = info.display.transformation
            }
            // 将新的Display挂载到实体上
            entity.addPassenger(newDisplay)
            // 创建新的信息
            val newInfo = MarkInfo(newDisplay, info.markId, info.xOffset)
            newMarks.add(newInfo)
            newMarkIds.add(info.markId)
            val newEntityMarkMap = markIdToInfo.getOrPut(info.markId) { mutableMapOf() }
            newEntityMarkMap[entity.uniqueId] = newInfo
        }
        // 更新entityMarks和entityMarkIds
        entityMarks[entity] = newMarks
        entityMarkIds[entity] = newMarkIds
        // 重新排列标记
        rearrangeMarks(entity)

        // 重新创建名字标签（从PDC中读取）
        if (entityNameTags.containsKey(entity)) {
            val nameTag = entityNameTags[entity]!!
            val name = nameTag.text()
            if (name != null) {
                removeEntityNameTag(entity)
                val nameString = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(name)
                createEntityNameTag(entity, nameString)
            }
        } else {
            // 如果没有名字标签，尝试从PDC中恢复
            val pdc = entity.persistentDataContainer
            val key = CUSTOM_NAME_KEY
            if (key != null) {
                val customName = pdc.get(key, PersistentDataType.STRING)
                if (customName != null) {
                    createEntityNameTag(entity, customName)
                }
            }
        }
    }

    /**
     * 保存实体的标记数据到内存（用于传送门等场景）
     */
    fun saveEntityMarks(entity: LivingEntity) {
        val marks = entityMarks[entity] ?: return
        val savedData = marks.map { info ->
            SavedMarkData(
                markId = info.markId,
                itemStack = info.display.itemStack,
                showToAllPlayers = info.display.isVisibleByDefault,
                xOffset = info.xOffset,
                expireTime = info.expireTime
            )
        }.toMutableList()
        // 使用实体的UUID作为key存储
        playerMarkData[entity.uniqueId] = savedData
        
        // 从entityMarks中移除标记，避免重复保存
        entityMarks.remove(entity)
        entityMarkIds.remove(entity)
    }

    /**
     * 保存实体的名字标签信息到PDC（已在createEntityNameTag中实现）
     */

    /**
     * 恢复实体的标记显示
     */
    fun restoreEntityMarks(entity: LivingEntity) {
        val savedData = playerMarkData.remove(entity.uniqueId)
        if (savedData != null && savedData.isNotEmpty()) {
            val plugin = pluginInstance ?: return
            val currentTime = System.currentTimeMillis()

            // 过滤出未过期的标记
            val validData = savedData.filter { it.expireTime > currentTime }

            if (validData.isNotEmpty()) {
                validData.forEach { data ->
                    // 创建新的Display
                    val display = entity.world.spawn(entity.location, ItemDisplay::class.java) { it ->
                        it.setItemStack(data.itemStack)
                        it.isVisibleByDefault = data.showToAllPlayers
                        it.shadowRadius = 0f
                        it.shadowStrength = 0f
                        it.billboard = org.bukkit.entity.Display.Billboard.CENTER
                    }

                    // 计算高度
                    val height = getDisplayHeight(entity)

                    // 设置变换
                    val translation = Vector3f(data.xOffset.toFloat(), height.toFloat(), 0f)
                    val scale = Vector3f(DISPLAY_SCALE, DISPLAY_SCALE, DISPLAY_SCALE)
                    val transformation = Transformation(
                        translation,
                        org.joml.Quaternionf(),
                        scale,
                        org.joml.Quaternionf()
                    )
                    display.transformation = transformation

                    // 将Display挂载到实体上
                    entity.addPassenger(display)

                    // 存储信息
                    val info = MarkInfo(display, data.markId, data.xOffset, data.expireTime)
                    entityMarks.getOrPut(entity) { mutableListOf() }.add(info)
                    val entityMarkMap = markIdToInfo.getOrPut(data.markId) { mutableMapOf() }
                    entityMarkMap[entity.uniqueId] = info
                    entityMarkIds.getOrPut(entity) { mutableSetOf() }.add(data.markId)

                    // 如果有过期时间，设置定时移除
                    if (data.expireTime != Long.MAX_VALUE) {
                        val remainingSeconds = (data.expireTime - currentTime) / 1000
                        if (remainingSeconds > 0) {
                            val task = plugin.server.scheduler.runTaskLater(plugin, Runnable {
                                removeMarkDisplay(entity, data.markId)
                            }, remainingSeconds * 20L)
                            info.taskId = task.taskId
                        } else {
                            // 如果已经过期，立即移除
                            removeMarkDisplay(entity, data.markId)
                        }
                    }
                }

                // 重新排列标记
                rearrangeMarks(entity)
            }
        }
    }

    /**
     * 恢复实体的名字标签（从PDC中读取）
     */
    fun restoreEntityNameTag(entity: LivingEntity) {
        val pdc = entity.persistentDataContainer
        val key = CUSTOM_NAME_KEY ?: return
        val customName = pdc.get(key, PersistentDataType.STRING)
        
        if (customName != null && !entityNameTags.containsKey(entity)) {
            createEntityNameTag(entity, customName)
        }
    }

    /**
     * 清理所有标记和名字标签，在插件关闭时调用
     */
    fun cleanup() {
        // 清除所有标记
        val entitiesToRemove = entityMarks.keys.toList()
        entitiesToRemove.forEach { entity ->
            removeMarkDisplay(entity)
        }
        // 清除所有玩家名字标签
        val playersToRemove = playerNameTags.keys.toList()
        playersToRemove.forEach { player ->
            removePlayerNameTag(player)
        }
        // 清除所有实体名字标签
        val entitiesWithNames = entityNameTags.keys.toList()
        entitiesWithNames.forEach { entity ->
            removeEntityNameTag(entity)
        }
    }

    /**
     * 检查实体是否可以被标记
     * 只允许对敌对生物、玩家、傀儡类、可攻击的宠物创建标记
     * @param entity 要检查的实体
     * @return true 如果实体可以被标记，否则返回 false
     */
    private fun isEntityMarkable(entity: LivingEntity): Boolean {
        // 玩家始终可以被标记
        if (entity is Player) {
            return true
        }

        // 检查实体类型
        return when (entity) {
            // 傀儡类（铁傀儡、雪傀儡）
            is org.bukkit.entity.IronGolem,
            is org.bukkit.entity.Snowman -> true

            // 敌对生物（排除僵尸马和骷髅马）
            is org.bukkit.entity.Monster -> {
                // 排除坐骑类生物（僵尸马、骷髅马）
                entity.type != org.bukkit.entity.EntityType.ZOMBIE_HORSE &&
                entity.type != org.bukkit.entity.EntityType.SKELETON_HORSE
            }

            // 可攻击的宠物（狼、豹猫、猫、鹦鹉）
            is org.bukkit.entity.Wolf,
            is org.bukkit.entity.Ocelot,
            is org.bukkit.entity.Cat,
            is org.bukkit.entity.Parrot -> true

            // 其他可标记生物
            is org.bukkit.entity.Phantom,        // 幻翼
            is org.bukkit.entity.PolarBear,      // 北极熊
            is org.bukkit.entity.Shulker,        // 潜影贝
            is org.bukkit.entity.Ghast,          // 恶魂
            is org.bukkit.entity.Hoglin,         // 疣猪兽
            is org.bukkit.entity.Slime,          // 史莱姆
            is org.bukkit.entity.MagmaCube -> true// 岩浆怪

            // 其他实体不允许标记
            else -> false
        }
    }

    /**
     * 获取标记显示高度
     * 使用Bukkit提供的Entity.getHeight()方法自动获取实体的碰撞箱高度
     * @param entity 实体
     * @return 标记显示高度（相对于实体位置）
     */
    private fun getDisplayHeight(entity: LivingEntity): Double {
        // 根据实体类型添加特定的偏移量
        val offset = when (entity) {
            // 偏移量 0.55
            is org.bukkit.entity.Witch -> 0.55
            // 偏移量 0.5
            is org.bukkit.entity.Player -> 0.6

            // 偏移量 0.4
            is org.bukkit.entity.ZombieVillager -> 0.4

            // 偏移量 0.3
            is org.bukkit.entity.Wither,
            is org.bukkit.entity.Husk,
            is org.bukkit.entity.Villager,
            is org.bukkit.entity.Turtle -> 0.3

            // 偏移量 0.1
            is org.bukkit.entity.Phantom,
            is org.bukkit.entity.Allay -> 0.1

            is org.bukkit.entity.Parrot -> 0.0
            // 默认偏移量 0.2
            else -> 0.2
        }

        // 如果实体有自定义名字标签，额外增加 0.4
        val nameTagOffset = if (entityNameTags.containsKey(entity)) 0.4 else 0.0

        return offset + nameTagOffset
    }
}