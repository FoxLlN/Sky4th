package com.sky4th.equipment.modifier.manager

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.LivingEntity
import org.bukkit.scheduler.BukkitTask
import sky4th.core.api.MarkAPI
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 冰冻管理器
 * 负责管理所有当前被冰冻的实体，并定时检测冰冻状态以恢复移动速度
 */
object FreezeManager {
    // 被冰冻的实体集合（使用UUID避免内存泄漏）
    private val frozenEntities = ConcurrentHashMap<UUID, FrozenEntityInfo>()

    // 定时任务引用
    private var task: BukkitTask? = null

    // 冰冻效果的命名空间键
    private val FREEZE_KEY = NamespacedKey("equipment_affix", "freeze")

    // 检查间隔（tick），每5tick（0.25秒）检查一次
    private const val CHECK_INTERVAL = 5L

    /**
     * 冻结实体信息数据类
     * 存储实体及其冰冻信息
     */
    data class FrozenEntityInfo(
        val uuid: UUID,
        val entityId: Int,
        val affixId: String,
        var lastCheckTime: Long = System.currentTimeMillis()
    )

    /**
     * 初始化冰冻管理器
     * @param plugin 插件实例
     */
    fun initialize(plugin: org.bukkit.plugin.java.JavaPlugin) {
        // 如果任务已存在，先取消
        task?.cancel()

        // 注册定时任务，每0.25秒执行一次
        task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            processFrozenEntities()
        }, 0L, CHECK_INTERVAL)
    }

    /**
     * 关闭冰冻管理器
     */
    fun shutdown() {
        task?.cancel()
        task = null
        frozenEntities.clear()
    }

    /**
     * 添加实体到冰冻列表
     * @param entity 实体
     * @param affixId 词条ID
     */
    fun addEntity(entity: LivingEntity, affixId: String) {
        val uuid = entity.uniqueId
        val entityId = entity.entityId
        frozenEntities.computeIfAbsent(uuid) {
            FrozenEntityInfo(uuid, entityId, affixId)
        }
    }

    /**
     * 从冰冻列表中移除实体
     * @param entity 实体
     */
    fun removeEntity(entity: LivingEntity) {
        val uuid = entity.uniqueId
        val entityInfo = frozenEntities.remove(uuid)
        // 移除时清除属性修饰符和标记
        if (entityInfo != null) {
            removeFreezeModifier(entity)
            removeFreezeMark(entity, entityInfo.affixId)
            // 停止跟踪实体
            com.sky4th.equipment.modifier.ModifierManager.instance.untrackAffectedEntity(entity)
        }
    }

    /**
     * 检查实体是否在冰冻列表中
     * @param entity 实体
     * @return 如果在冰冻列表中返回true，否则返回false
     */
    fun isFrozenEntity(entity: LivingEntity): Boolean = frozenEntities.containsKey(entity.uniqueId)

    /**
     * 处理所有冰冻实体的状态检测
     */
    private fun processFrozenEntities() {
        // 遍历所有冰冻实体
        frozenEntities.values.toList().forEach { entityInfo ->
            val entity = Bukkit.getEntity(entityInfo.uuid) as? LivingEntity

            // 检查实体是否有效
            if (entity == null || entity.isDead) {
                frozenEntities.remove(entityInfo.uuid)
                return@forEach
            }

            // 检查实体的冰冻值是否为0
            if (entity.freezeTicks <= 0) {
                // 冰冻结束，移除修饰符、标记和记录
                removeFreezeModifier(entity)
                removeFreezeMark(entity, entityInfo.affixId)
                frozenEntities.remove(entityInfo.uuid)
            }
        }
    }

    /**
     * 移除冰冻标记
     * @param entity 实体
     * @param affixId 词条ID
     */
    private fun removeFreezeMark(entity: LivingEntity, affixId: String) {
        try {
            MarkAPI.removeMark(entity, affixId)
        } catch (e: Exception) {
            // 忽略移除标记时的异常
        }
    }

    /**
     * 移除冰冻修饰符
     * @param entity 实体
     */
    private fun removeFreezeModifier(entity: LivingEntity) {
        val movementSpeedAttribute = entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) ?: return

        // 移除冰冻修饰符
        val modifiers = movementSpeedAttribute.modifiers.toList()
        for (modifier in modifiers) {
            if (modifier.key == FREEZE_KEY) {
                movementSpeedAttribute.removeModifier(modifier)
            }
        }
    }
}
