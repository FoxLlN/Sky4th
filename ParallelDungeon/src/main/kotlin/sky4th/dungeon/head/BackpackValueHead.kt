package sky4th.dungeon.head

import org.bukkit.inventory.ItemStack
import sky4th.dungeon.config.ConfigManager
import sky4th.dungeon.Dungeon
import sky4th.dungeon.util.CreditsHeadUtil
import sky4th.core.api.LanguageAPI

/**
 * 背包价值头颅
 * 用于显示背包总价值
 */
object BackpackValueHead {
    private val plugin = Dungeon.instance

    /**
     * 创建背包价值头颅物品
     * @param configManager 配置管理器
     * @param value 背包价值
     * @return 自定义头颅物品
     */
    fun create(configManager: ConfigManager, value: Int): ItemStack {
        val texture = configManager.getBackpackValueHeadTexture()
        val name = LanguageAPI.getText(plugin, "backpack.value-solt.name")
        val head = CreditsHeadUtil.createCustomHead(name, texture)
        val meta = head.itemMeta ?: return head
        // 设置描述（lore）
        val lore = LanguageAPI.getComponentList(plugin, "backpack.value-solt.lore", "value" to value)
        meta.lore(lore)

        head.itemMeta = meta
        return head
    }
}
