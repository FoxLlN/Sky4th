package sky4th.dungeon.head

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import sky4th.dungeon.config.ConfigManager
import sky4th.dungeon.util.CreditsHeadUtil
import sky4th.dungeon.Dungeon
import sky4th.core.api.LanguageAPI

/**
 * 商店头颅
 * 用于创建和配置商店界面的按钮头颅
 */
object ShopHead {
    private val plugin = Dungeon.instance

    /**
     * 创建打开商店按钮头颅
     *
     * @param configManager 配置管理器
     * @return 自定义头颅物品
     */
    fun createOpenShopHead(configManager: ConfigManager): ItemStack {
        // 从配置文件中获取打开商店按钮头颅皮肤纹理
        val texture = configManager.getShopOpenHeadTexture()
        val name = LanguageAPI.getText(plugin, "storage.button.shop")
        // 使用通用方法创建基础头颅
        val head = CreditsHeadUtil.createCustomHead(name, texture)
        return head
    }
}
