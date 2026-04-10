package sky4th.bettervillage.listeners

import org.bukkit.Bukkit
import org.bukkit.entity.Animals
import org.bukkit.entity.Villager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityBreedEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityTransformEvent
import org.bukkit.event.entity.VillagerAcquireTradeEvent
import org.bukkit.event.entity.VillagerCareerChangeEvent
import sky4th.bettervillage.BetterVillage
import sky4th.bettervillage.manager.VillageManager
import sky4th.bettervillage.manager.VillagerStateManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 村民事件监听器
 * 职责：
 * 1. 监听村民等级变化（升级/转职），维护真实等级和村庄统计
 * 2. 实施村庄等级限制：村民显示等级 ≤ 村庄等级 + 1
 * 3. 超过限制时降级并刷新交易，真实等级保持不变
 * 4. 监听村民出生、繁殖、死亡事件，增量更新村庄村民统计
 */
class VillagerEventListener : Listener {

    // 用于存储职业变更前的状态（职业 + 等级），仅用于职业变更事件
    private data class VillagerState(val profession: Villager.Profession, val level: Int)
    private val preChangeState = ConcurrentHashMap<UUID, VillagerState>()

    /**
     * 职业变更前保存状态（最低优先级，确保在其他插件修改前记录）
     */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onVillagerCareerChangePre(event: VillagerCareerChangeEvent) {
        val villager = event.entity
        preChangeState[villager.uniqueId] = VillagerState(villager.profession, villager.villagerLevel)
    }

    /**
     * 职业变更后处理（监视器优先级）
     * 更新村庄统计、真实等级、刷新交易
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onVillagerCareerChange(event: VillagerCareerChangeEvent) {
        val villager = event.entity
        try {
            if (event.isCancelled) return

            val village = VillageManager.getVillageByLocation(villager.location) ?: return
            val oldState = preChangeState[villager.uniqueId] ?: return

            val oldLevel = if (oldState.profession == Villager.Profession.NONE) 0 else oldState.level
            val newLevel = villager.villagerLevel

            // 更新村庄统计
            VillageManager.updateVillagerProfession(village.id, oldLevel, newLevel)

            // 更新真实等级为当前显示等级
            VillagerCustomizer.setRealLevel(villager, newLevel)
        } finally {
            preChangeState.remove(villager.uniqueId)
        }
    }

    /**
     * 村民获得新交易事件（升级触发）
     * 实施等级限制，更新真实等级/统计，刷新交易
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onVillagerAcquireTrade(event: VillagerAcquireTradeEvent) {
        event.isCancelled = true
        val villager = event.entity as? Villager ?: return
        val village = VillageManager.getVillageByLocation(villager.location) ?: return

        // 职业变更导致的升级由职业变更事件处理，此处跳过
        if (preChangeState.containsKey(villager.uniqueId)) return
        
        
        val displayLevel = villager.villagerLevel
        val realLevel = VillagerCustomizer.getRealLevel(villager)

        // 如果显示等级 <= 真实等级，说明不是真正的升级（可能是降级后的刷新或重复事件）
        if (displayLevel <= realLevel) {
            if (displayLevel == 1 && villager.recipes.size == 0) {
                // 初始化交易（基于新职业和等级）
                VillagerCustomizer().applyCustomTrades(villager, true)
            } 
            return
        }

        // 真正的等级提升
        val maxAllowed = village.level + 1

        if (displayLevel > maxAllowed) {
            // 超过限制：降级显示等级，真实等级不变
            villager.setVillagerLevel(maxAllowed)
            return
        }

        // 允许升级：更新真实等级和村庄统计
        val oldLevel = realLevel
        VillagerCustomizer.setRealLevel(villager, displayLevel)
        VillageManager.updateVillagerProfession(village.id, oldLevel, displayLevel)

        VillagerCustomizer().applyCustomTrades(villager)
    }

    /**
     * 村民出生事件监听
     * 包括自然生成、繁殖、刷怪笼等所有生成方式
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onCreatureSpawn(event: CreatureSpawnEvent) {
        if (event.isCancelled) return

        val villager = event.entity as? Villager ?: return

        // 获取村庄并更新统计
        val village = VillageManager.getVillageByLocation(villager.location) ?: return

        // 记录村民所属村庄
        VillagerStateManager.updateVillagerStatus(villager)

        // 异步更新统计，避免阻塞主线程
        Bukkit.getScheduler().runTaskAsynchronously(BetterVillage.instance, Runnable {
            VillageManager.updateVillagerBreeding(
                village.id,
                isBaby = !villager.isAdult
            )
        })
    }

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        if (event.entity is Villager) {
            val villager = event.entity as Villager

            // 移除职业变更前的状态
            preChangeState.remove(villager.uniqueId)

            // 清理村民状态
            VillagerStateManager.removeVillagerState(villager.uniqueId)

            // 获取村庄并更新统计
            val village = VillageManager.getVillageByLocation(villager.location)
            if (village != null) {
                val level = villager.villagerLevel
                val profession = villager.profession

                // 根据村民状态减少对应统计
                VillageManager.updateVillagerDeath(
                    village.id,
                    isBaby = !villager.isAdult,
                    isUnemployed = profession == Villager.Profession.NONE,
                    level = level
                )
            }
        }
    }

    /**
     * 村民成长事件监听（幼年→成年）
     * 当幼年村民长大时更新村庄统计
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onVillagerGrowth(event: EntityTransformEvent) {
        if (event.isCancelled) return

        // 检查是否是村民成长事件（幼年→成年）
        if (event.entity !is Villager) return
        if (event.transformedEntity !is Villager) return

        val babyVillager = event.entity as Villager
        val adultVillager = event.transformedEntity as Villager

        // 只有幼年村民成长为成年村民时才处理
        if (babyVillager.isAdult || !adultVillager.isAdult) return

        // 获取村庄并更新统计
        val village = VillageManager.getVillageByLocation(adultVillager.location) ?: return

        // 异步更新统计，避免阻塞主线程
        Bukkit.getScheduler().runTaskAsynchronously(BetterVillage.instance, Runnable {
            VillageManager.updateVillagerGrowth(village.id)
        })
    }

    /**
     * 村民繁衍事件监听
     * 控制村庄内村民数量不超过上限
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityBreed(event: EntityBreedEvent) {
        if (event.entity !is Villager) return
        val village = VillageManager.getVillageByLocation(event.entity.location) ?: return

        val currentCount = village.getTotalVillagerCount()
        val maxVillagers = VillageManager.calculateMaxVillagers(village)

        if (currentCount >= maxVillagers) {
            event.isCancelled = true

            // 强制结束父母双方的发情状态
            (event.father as? Animals)?.setLoveModeTicks(0)
            (event.mother as? Animals)?.setLoveModeTicks(0)

            BetterVillage.instance.logger.info("村庄 ${village.id} 村民数量已达上限 ($maxVillagers)，取消繁衍")
        }
    }

    /**
     * 清理所有缓存
     * 在服务器关闭时调用
     */
    fun cleanupAll() {
        preChangeState.clear()
    }
}