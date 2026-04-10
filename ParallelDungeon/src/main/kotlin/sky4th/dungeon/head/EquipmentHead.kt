package sky4th.dungeon.head

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import sky4th.dungeon.Dungeon
import sky4th.dungeon.config.ConfigManager
import sky4th.dungeon.util.CreditsHeadUtil
import sky4th.core.api.LanguageAPI

/**
 * 装备界面头颅
 * 用于创建和配置装备界面的按钮头颅
 */
object EquipmentHead {
    private val plugin = Dungeon.instance

    /**
     * 创建返回装配按钮头颅
     *
     * @param configManager 配置管理器
     * @return 自定义头颅物品
     */
    fun createBackToEquipmentHead(configManager: ConfigManager): ItemStack {
        // 从配置文件中获取返回装配按钮头颅皮肤纹理
        val texture = configManager.getEquipmentBackHeadTexture()
        val name = LanguageAPI.getText(plugin, "loadout.screen.back")
        // 使用通用方法创建基础头颅
        val head = CreditsHeadUtil.createCustomHead(name, texture)
        return head
    }
}
