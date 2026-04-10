package sky4th.dungeon.head

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import sky4th.dungeon.Dungeon
import sky4th.dungeon.config.ConfigManager
import sky4th.dungeon.util.CreditsHeadUtil
import sky4th.core.api.LanguageAPI

/**
 * 信用点头颅
 * 用于创建和配置信用点展示的自定义头颅
 */
object CreditsHead {
    private val plugin = Dungeon.instance

    /**
     * 创建信用点展示头颅
     *
     * @param name 头颅显示名称
     * @param balance 玩家信用点余额
     * @param configManager 配置管理器
     * @return 自定义头颅物品
     */
    fun createCreditsHead(balance: String, configManager: ConfigManager): ItemStack {
        // 从配置文件中获取信用点头颅皮肤纹理
        val texture = configManager.getCreditsHeadTexture()
        val name = LanguageAPI.getText(plugin, "ui.credits-title")
        // 使用通用方法创建基础头颅
        val head = CreditsHeadUtil.createCustomHead(name, texture)
        val meta = head.itemMeta ?: return head

        // 设置描述（lore）
        val lore = LanguageAPI.getComponentList(plugin, "ui.credits", "balance" to balance)
        meta.lore(lore)

        head.itemMeta = meta
        return head
    }
}
