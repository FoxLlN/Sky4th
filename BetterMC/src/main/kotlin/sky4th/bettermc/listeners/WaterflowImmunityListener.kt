package sky4th.bettermc.listeners

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import com.destroystokyo.paper.event.server.ServerTickStartEvent
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.IronGolem
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.util.Vector
import sky4th.bettermc.BetterMC
import sky4th.bettermc.command.FeatureManager
import java.util.*

class WaterflowImmunityListener : Listener {

    companion object {
        // 存储每个实体在 tick 开始时的位置（用于锚定）
        private val startLocations = mutableMapOf<UUID, Location>()
    }

    // 初始化：为新生成的实体设置水流免疫属性
    @EventHandler
    fun onEntitySpawn(event: EntitySpawnEvent) {
        val entity = event.entity
        when (entity) {
            is IronGolem -> setWaterImmunity(entity)
            is ArmorStand -> setWaterImmunity(entity)
        }
    }

    // 为已加载区块中的实体设置水流免疫属性
    @EventHandler
    fun onChunkLoad(event: ChunkLoadEvent) {
        for (entity in event.chunk.entities) {
            when (entity) {
                is IronGolem -> setWaterImmunity(entity)
                is ArmorStand -> setWaterImmunity(entity)
            }
        }
    }

    private fun setWaterImmunity(entity: LivingEntity) {
        if (!entity.isValid) return
        val attribute = entity.getAttribute(org.bukkit.attribute.Attribute.GENERIC_WATER_MOVEMENT_EFFICIENCY)
        attribute?.baseValue = 1.0
    }

    @EventHandler
    fun onServerTickStart(event: ServerTickStartEvent) {
        if (!FeatureManager.isFeatureEnabled("waterflow-immunity")) return

        for (world in BetterMC.instance.server.worlds) {
            // 记录铁傀儡的起始位置
            for (golem in world.getEntitiesByClass(IronGolem::class.java)) {
                if (golem.isValid && !golem.isDead) {
                    startLocations[golem.uniqueId] = golem.location.clone()
                }
            }
            // 记录盔甲架的起始位置
            for (stand in world.getEntitiesByClass(ArmorStand::class.java)) {
                if (stand.isValid && !stand.isDead) {
                    startLocations[stand.uniqueId] = stand.location.clone()
                }
            }
        }
    }

    @EventHandler
    fun onServerTickEnd(event: ServerTickEndEvent) {
        if (!FeatureManager.isFeatureEnabled("waterflow-immunity")) return

        val currentTick = event.tickNumber.toLong() // 转换为 Long

        for (world in BetterMC.instance.server.worlds) {
            // 处理铁傀儡
            for (golem in world.getEntitiesByClass(IronGolem::class.java)) {
                if (!golem.isValid || golem.isDead) {
                    startLocations.remove(golem.uniqueId)
                    continue
                }
                val speed = golem.velocity.clone().setY(0.0).length()
                if (golem.isInWater || golem.isInLava) {
                    if (golem.target == null) {
                        anchorEntity(golem)
                    }
                }
            }
            // 处理盔甲架
            for (stand in world.getEntitiesByClass(ArmorStand::class.java)) {
                if (!stand.isValid || stand.isDead) {
                    startLocations.remove(stand.uniqueId)
                    continue
                }
                val speed = stand.velocity.clone().setY(0.0).length()
                if (stand.isInWater || stand.isInLava) {
                    anchorEntity(stand)
                }
            }
        }
    }

    /**
     * 将实体锚定在水平方向（允许垂直移动）
     */
    private fun anchorEntity(entity: Entity) {
        val startLoc = startLocations[entity.uniqueId] ?: return
        if (entity.world != startLoc.world) return

        val currentLoc = entity.location
        val dx = startLoc.x - currentLoc.x
        val dz = startLoc.z - currentLoc.z
        if (dx * dx + dz * dz > 0.0001) {
            val targetLoc = Location(startLoc.world, startLoc.x, currentLoc.y, startLoc.z)
            entity.teleport(targetLoc)
        }
        // 清零水平速度，保留垂直速度（允许沉浮）
        entity.velocity = entity.velocity.clone().setX(0.0).setZ(0.0)
    }
}