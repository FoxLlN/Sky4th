package sky4th.core.listener

import org.bukkit.Chunk
import org.bukkit.Material
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityTeleportEvent
import org.bukkit.event.entity.EntityTransformEvent
import org.bukkit.event.entity.EntityTameEvent
import org.bukkit.event.entity.EntityBreedEvent
import org.bukkit.event.player.PlayerPortalEvent
import org.bukkit.event.entity.EntityPortalEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import sky4th.core.mark.MarkManager

/**
 * 标记清理监听器
 * 负责处理标记的创建、移除、保存和恢复等事件
 */
class MarkListener : Listener {

    /**
     * 监听玩家加入事件，为玩家创建自定义名字标签并恢复标记显示
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        // 为玩家创建自定义名字标签
        MarkManager.createPlayerNameTag(player)

        // 恢复玩家的标记显示
        MarkManager.restorePlayerMarks(player)
    }

    /**
     * 监听玩家退出事件，保存玩家的标记数据并清除显示
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        // 保存玩家的标记数据并清除显示
        MarkManager.saveAndClearPlayerMarks(player)
        // 移除玩家的名字标签
        MarkManager.removePlayerNameTag(player)
    }

    /**
     * 监听玩家与实体交互事件，处理命名牌使用
     */
    @EventHandler
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val player = event.player
        val entity = event.rightClicked

        // 只处理LivingEntity
        if (entity !is LivingEntity) return

        // 检查玩家手持的是否为命名牌
        val item = player.inventory.itemInMainHand
        if (item.type != Material.NAME_TAG) return

        // 检查命名牌是否有自定义名称
        if (!item.hasItemMeta() || !item.itemMeta!!.hasDisplayName()) return

        // 获取命名牌上的名称
        val displayName = item.itemMeta!!.displayName()
        if (displayName == null) return
        val name = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(displayName)

        // 检查实体是否有标记
        val hasMarks = MarkManager.hasEntityMarks(entity)

        // 如果实体已经有自定义名字标签，先移除
        if (MarkManager.hasEntityNameTag(entity)) {
            MarkManager.removeEntityNameTag(entity)
        }

        // 延迟一tick创建自定义名字标签，确保原版命名已经完成
        val plugin = MarkManager.getPlugin()
        if (plugin != null) {
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                plugin,
                Runnable {
                    MarkManager.createEntityNameTag(entity, name)
                    // 如果实体有标记，重新排列标记位置
                    if (hasMarks) {
                        MarkManager.rearrangeMarks(entity)
                    }
                },
                1L
            )
        }
    }

    /**
     * 监听实体死亡事件，清理其自定义名字标签
     */
    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        val entity = event.entity

        // 清理实体的所有标记
        if (MarkManager.hasEntityMarks(entity)) {
            MarkManager.removeMarkDisplay(entity)
        }

        // 如果实体有自定义名字标签，移除它
        if (MarkManager.hasEntityNameTag(entity)) {
            MarkManager.removeEntityNameTag(entity)
        }
    }

    /**
     * 监听实体瞬移事件，重新创建标记和名字标签
     * 处理潜影贝、末影人等会瞬移的实体
     */
    @EventHandler
    fun onEntityTeleport(event: EntityTeleportEvent) {
        val entity = event.entity

        // 只处理LivingEntity
        if (entity !is LivingEntity) return

        // 移除旧的Display实体，避免浮空
        entity.passengers.forEach { passenger ->
            if (passenger is org.bukkit.entity.ItemDisplay || passenger is org.bukkit.entity.TextDisplay) {
                if (passenger.isValid) {
                    passenger.remove()
                }
            }
        }

        // 延迟一tick重新创建标记和名字标签，确保瞬移完成
        val plugin = MarkManager.getPlugin()
        if (plugin != null) {
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                plugin,
                Runnable {
                    MarkManager.recreateMarksAfterTeleport(entity)
                },
                1L
            )
        }
    }

    /**
     * 监听玩家传送门事件，保存和恢复标记及名字标签
     * 处理玩家穿越传送门的情况
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerPortal(event: PlayerPortalEvent) {
        val player = event.player
        val fromWorld = event.from.world?.name ?: "unknown"
        val toWorld = event.to?.world?.name ?: "unknown"
        val plugin = MarkManager.getPlugin()

        // 在传送前保存标记和名字标签
        MarkManager.saveEntityMarks(player)

        // 移除旧的Display实体，避免浮空
        player.passengers.forEach { passenger ->
            if (passenger is org.bukkit.entity.ItemDisplay || passenger is org.bukkit.entity.TextDisplay) {
                if (passenger.isValid) {
                    passenger.remove()
                }
            }
        }

        // 移除旧的名字标签，避免浮空
        if (MarkManager.hasEntityNameTag(player)) {
            // 玩家使用removePlayerNameTag而不是removeEntityNameTag
            MarkManager.removePlayerNameTag(player)
        }

        // 在传送后恢复标记和名字标签
        if (plugin != null) {
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                MarkManager.restoreEntityMarks(player)
                // 玩家使用createPlayerNameTag而不是restoreEntityNameTag
                MarkManager.createPlayerNameTag(player)
            }, 1L)
        }
    }

    /**
     * 监听实体传送门事件，保存和恢复标记及名字标签
     * 处理实体穿越传送门的情况
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onEntityPortal(event: EntityPortalEvent) {
        val entity = event.entity

        // 只处理LivingEntity
        if (entity !is LivingEntity) return

        val fromWorld = event.from.world?.name ?: "unknown"
        val toWorld = event.to?.world?.name ?: "unknown"
        val plugin = MarkManager.getPlugin()

        // 在传送前保存标记和名字标签
        MarkManager.saveEntityMarks(entity)

        // 移除旧的Display实体，避免浮空
        entity.passengers.forEach { passenger ->
            if (passenger is org.bukkit.entity.ItemDisplay || passenger is org.bukkit.entity.TextDisplay) {
                if (passenger.isValid) {
                    passenger.remove()
                }
            }
        }

        // 移除旧的名字标签，避免浮空
        if (MarkManager.hasEntityNameTag(entity)) {
            MarkManager.removeEntityNameTag(entity)
        }

        // 在传送后恢复标记和名字标签
        if (plugin != null) {
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                MarkManager.restoreEntityMarks(entity)
                MarkManager.restoreEntityNameTag(entity)
            }, 1L)
        }
    }

    /**
     * 监听玩家重生事件，恢复玩家的自定义名字标签
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val player = event.player
        // 为玩家创建自定义名字标签
        MarkManager.createPlayerNameTag(player)
    }

    /**
     * 监听区块卸载事件，清理区块内实体的标记和名字标签
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onChunkUnload(event: ChunkUnloadEvent) {
        val chunk = event.chunk
        // 遍历区块内的所有实体
        chunk.entities.forEach { entity ->
            if (entity is LivingEntity) {
                // 清理实体的标记
                if (MarkManager.hasEntityMarks(entity)) {
                    MarkManager.removeMarkDisplay(entity)
                }
                // 清理实体的名字标签
                if (MarkManager.hasEntityNameTag(entity)) {
                    MarkManager.removeEntityNameTag(entity)
                }
            }
        }
    }

    /**
     * 监听区块加载事件，恢复区块内实体的标记和名字标签
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onChunkLoad(event: ChunkLoadEvent) {
        val chunk = event.chunk
        val plugin = MarkManager.getPlugin() ?: return

        // 延迟一tick处理，确保实体完全加载
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            chunk.entities.forEach { entity ->
                if (entity is LivingEntity) {
                    // 检查实体是否有自定义名字
                    if (entity.customName() != null && !MarkManager.hasEntityNameTag(entity)) {
                        val name = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(entity.customName()!!)
                        MarkManager.createEntityNameTag(entity, name)
                    }
                    // 如果实体有标记，重新排列标记位置
                    if (MarkManager.hasEntityMarks(entity)) {
                        MarkManager.rearrangeMarks(entity)
                    }
                }
            }
        }, 1L)
    }

    /**
     * 监听实体转换事件，重新创建标记和名字标签
     * 处理猪灵僵尸化、村民僵尸化等情况
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onEntityTransform(event: EntityTransformEvent) {
        val transformedEntity = event.entity
        val finalEntity = event.transformedEntity

        // 转换前的实体清理标记和名字标签
        if (transformedEntity is LivingEntity) {
            // 保存名字标签信息（如果需要恢复）
            val hasNameTag = MarkManager.hasEntityNameTag(transformedEntity)
            val customName = if (hasNameTag) {
                val nameTag = MarkManager.getEntityNameTag(transformedEntity)
                nameTag?.text()?.let { 
                    net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(it)
                }
            } else null

            // 移除旧的Display实体，避免浮空
            transformedEntity.passengers.forEach { passenger ->
                if (passenger is org.bukkit.entity.ItemDisplay || passenger is org.bukkit.entity.TextDisplay) {
                    if (passenger.isValid) {
                        passenger.remove()
                    }
                }
            }

            // 清理转换前实体的标记和名字标签
            MarkManager.removeMarkDisplay(transformedEntity)
            if (hasNameTag) {
                MarkManager.removeEntityNameTag(transformedEntity)
            }

            // 延迟一tick为转换后的实体恢复名字标签
            val plugin = MarkManager.getPlugin()
            if (plugin != null && finalEntity is LivingEntity) {
                plugin.server.scheduler.runTaskLater(plugin, Runnable {
                    // 恢复名字标签
                    if (customName != null) {
                        MarkManager.createEntityNameTag(finalEntity, customName)
                    }
                    // 重新排列标记（如果有的话）
                    if (MarkManager.hasEntityMarks(finalEntity)) {
                        MarkManager.rearrangeMarks(finalEntity)
                    }
                }, 1L)
            }
        }
    }
}