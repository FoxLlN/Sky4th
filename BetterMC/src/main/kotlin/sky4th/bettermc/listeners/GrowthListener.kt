
package sky4th.bettermc.listeners

import sky4th.bettermc.command.FeatureManager
import sky4th.bettermc.config.ConfigManager
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Ageable
import org.bukkit.entity.Animals
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityBreedEvent
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import sky4th.core.api.PlayerPermissionsAPI
import sky4th.bettermc.util.LanguageUtil.sendLang
import sky4th.bettermc.BetterMC
import kotlin.random.Random

/**
 * 动物村民成长事件监听器
 * 
 * 用于调整：
 * 1. 动物繁衍冷却时间×3
 * 2. 动物长大时间×3
 * 3. 村民长大时间×5
 */
class GrowthListener : Listener {

    // 繁衍冷却相关
    private lateinit var cooldownKey: NamespacedKey

    /**
     * 初始化冷却键
     */
    fun initCooldownKey(plugin: JavaPlugin) {
        cooldownKey = NamespacedKey(plugin, "last_breed_time")
    }

    @EventHandler
    fun onEntityBreed(event: EntityBreedEvent) {
        if (!FeatureManager.isFeatureEnabled("growth")) return
        val mother = event.mother
        val father = event.father

        // 检查父母是否在冷却期内
        if (mother is Animals && isOnCooldown(mother) || father is Animals && isOnCooldown(father)) {
            event.isCancelled = true
            return
        }

        // 检查玩家是否有breed权限
        val breeder = event.breeder
        val hasBreedPermission = breeder != null && PlayerPermissionsAPI.isAvailable() && 
            PlayerPermissionsAPI.hasPermission(breeder.uniqueId, "breed")

        // 如果没有breed权限，根据配置概率繁衍失败
        if (!hasBreedPermission) {
            if (Random.nextDouble() < ConfigManager.breedFailureChance) {
                // 繁衍失败，取消事件
                event.isCancelled = true
                breeder?.sendLang(BetterMC.instance, "growth.breed-failed")
            }
        }

        // 无论成功失败，都设置冷却时间
        if (mother is Animals) setCooldown(mother)
        if (father is Animals) setCooldown(father)
    }

    /**
     * 检查动物是否在冷却期内
     */
    private fun isOnCooldown(animal: Animals): Boolean {
        val last = animal.persistentDataContainer.get(cooldownKey, PersistentDataType.LONG) ?: return false
        return animal.world.fullTime - last < ConfigManager.breedCooldownTicks
    }

    /**
     * 设置动物的繁衍冷却时间
     */
    private fun setCooldown(animal: Animals) {
        animal.persistentDataContainer.set(cooldownKey, PersistentDataType.LONG, animal.world.fullTime)
    }

    @EventHandler
    fun onCreatureSpawn(event: CreatureSpawnEvent) {
        if (!FeatureManager.isFeatureEnabled("growth")) return    
        val entity = event.entity

        // 如果是可成长的生物（包括动物和村民），增加长大时间
        if (entity is Ageable) {
            // 检查是否是幼年生物
            if (!entity.isAdult) {
                // 根据生物类型设置不同的长大时间
                if (entity is Animals) {
                    // 动物长大时间
                    entity.age = -ConfigManager.animalGrowthTicks
                } else if (entity.type.name == "VILLAGER") {
                    // 村民长大时间
                    entity.age = -ConfigManager.villagerGrowthTicks
                }
            }
        }
    }
}
