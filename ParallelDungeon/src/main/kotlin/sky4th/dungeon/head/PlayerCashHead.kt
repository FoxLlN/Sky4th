package sky4th.dungeon.head

import org.bukkit.inventory.ItemStack
import sky4th.dungeon.config.ConfigManager
import sky4th.dungeon.Dungeon
import sky4th.dungeon.util.CreditsHeadUtil
import sky4th.core.api.LanguageAPI

/**
 * 玩家现金头颅
 * 用于在死亡UI中显示死亡玩家的背包现金
 */
object PlayerCashHead {
    private val plugin = Dungeon.instance

    /**
     * 创建满钱包头颅物品
     * @param configManager 配置管理器
     * @param cashAmount 现金金额
     * @return 自定义头颅物品
     */
    fun createFullWalletHead(
        configManager: ConfigManager,
        cashAmount: Int
    ): ItemStack {
        val texture = configManager.getFullWalletHeadTexture()
        val name = LanguageAPI.getText(plugin, "death.cash-title")
        val head = CreditsHeadUtil.createCustomHead(name, texture)
        val meta = head.itemMeta ?: return head
        // 设置描述（lore）
        val lore = LanguageAPI.getComponentList(plugin, "death.cash-lore", "amount" to cashAmount)
        meta.lore(lore)

        head.itemMeta = meta
        return head
    }

    /**
     * 创建空钱包头颅物品
     * @param configManager 配置管理器
     * @return 自定义头颅物品
     */
    fun createEmptyWalletHead(
        configManager: ConfigManager
    ): ItemStack {
        val texture = configManager.getEmptyWalletHeadTexture()
        val name = LanguageAPI.getText(plugin, "death.cash-title")
        val head = CreditsHeadUtil.createCustomHead(name, texture)
        val meta = head.itemMeta ?: return head
        // 设置描述（lore）
        val lore = LanguageAPI.getComponentList(plugin, "death.cash-empty-lore")
        meta.lore(lore)

        head.itemMeta = meta
        return head
    }
}
