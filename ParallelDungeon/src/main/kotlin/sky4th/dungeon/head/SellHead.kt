package sky4th.dungeon.head

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import sky4th.dungeon.config.ConfigManager
import sky4th.dungeon.util.CreditsHeadUtil
import sky4th.dungeon.Dungeon
import sky4th.core.api.LanguageAPI

/**
 * 出售按钮头颅
 * 用于创建仓库界面的出售按钮
 */
object SellHead {
    private val plugin = Dungeon.instance

    /**
     * 创建出售按钮头颅（默认状态）
     *
     * @param configManager 配置管理器
     * @return 自定义头颅物品
     */
    fun createSellHead(configManager: ConfigManager): ItemStack {
        // 从配置文件中获取出售按钮头颅皮肤纹理
        val texture = configManager.getSellDefaultHeadTexture()
        val name = LanguageAPI.getText(plugin, "storage.button.sell")
        val head = CreditsHeadUtil.createCustomHead(name, texture)
        val meta = head.itemMeta ?: return head

        // 添加 lore 提示
        val lore = LanguageAPI.getComponentList(plugin, "storage.sell-lore")
        meta.lore(lore)
        head.itemMeta = meta
        return head
    }

    /**
     * 创建出售确认按钮头颅（出售中状态）
     *
     * @param configManager 配置管理器
     * @return 自定义头颅物品
     */
    fun createSellConfirmHead(configManager: ConfigManager): ItemStack {
        // 从配置文件中获取出售确认按钮头颅皮肤纹理
        val texture = configManager.getSellConfirmHeadTexture()
        val name = LanguageAPI.getText(plugin, "storage.button.sell-confirm")
        val head = CreditsHeadUtil.createCustomHead(name, texture)
        val meta = head.itemMeta ?: return head

        // 添加 lore 提示
        val lore = LanguageAPI.getComponentList(plugin, "storage.sell-confirm-lore")
        meta.lore(lore)
        head.itemMeta = meta
        return head
    }
}
