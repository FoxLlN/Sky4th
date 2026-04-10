package sky4th.bettervillage.manager

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.MerchantRecipe
import sky4th.bettervillage.BetterVillage
import sky4th.bettervillage.listeners.VillagerCustomizer
import sky4th.bettervillage.util.LanguageUtil.sendLang

/**
 * 村民管理器
 * 
 * 功能：
 * 1. 村民升级管理
 * 2. 村民交易管理
 */
object VillagerManager {

    private val villagerCustomizer = VillagerCustomizer()

    /**
     * 刷新村民交易
     * 
     * @param player 执行刷新的玩家
     * @param villager 要刷新的村民
     */
    fun refreshTrades(player: Player, villager: Villager) {
        // 重新应用自定义交易
        villagerCustomizer.applyCustomTrades(villager)

        // 重置已使用次数（补货）
        villager.recipes.forEach { it.uses = 0 }

        // 发送成功消息
        player.sendLang(BetterVillage.instance, "villager.trades_refreshed")

        BetterVillage.instance.logger.info("村民 ${villager.uniqueId} 的交易已被玩家 ${player.name} 刷新")
    }

    /**
     * 获取村民信息
     * 
     * @param player 查询信息的玩家
     * @param villager 要查询的村民
     */
    fun getVillagerInfo(player: Player, villager: Villager) {
        val profession = villager.profession.toString()
        val level = villager.villagerLevel
        val type = villager.villagerType.toString()
        val experience = villager.villagerExperience

        player.sendLang(BetterVillage.instance, "villager.info", 
                "profession" to profession,
                "level" to level.toString(),
                "type" to type,
                "experience" to experience.toString()
            )
    }

    /**
     * 创建交易配方
     * 
     * @param cost 代价物品
     * @param costAmt 代价数量
     * @param result 结果物品
     * @param resAmt 结果数量
     * @param maxUses 最大使用次数
     * @return 交易配方
     */
    fun createRecipe(cost: Material, costAmt: Int, result: Material, resAmt: Int, maxUses: Int, experience: Int = 0): MerchantRecipe {
        val costItem = ItemStack(cost, costAmt)
        val resultItem = ItemStack(result, resAmt)
        val recipe = MerchantRecipe(resultItem, maxUses)
        recipe.addIngredient(costItem)
        // 设置村民获得的经验值
        recipe.villagerExperience = experience
        return recipe
    }
}
