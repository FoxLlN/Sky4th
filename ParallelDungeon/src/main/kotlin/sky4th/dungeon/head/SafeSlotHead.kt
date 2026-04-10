package sky4th.dungeon.head

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import sky4th.dungeon.config.ConfigManager
import sky4th.dungeon.util.CreditsHeadUtil
import sky4th.dungeon.Dungeon
import sky4th.core.api.LanguageAPI

/**
 * 安全箱头颅工具类
 * 用于创建安全箱提示头颅
 */
object SafeSlotHead {
    private val plugin = Dungeon.instance
    private val blockedSlotKey = org.bukkit.NamespacedKey(plugin,"dungeon_blocked_slot")

    /**
     * 创建安全箱提示头颅
     * @param configManager 配置管理器
     * @return 安全箱头颅物品
     */
    fun create(configManager: ConfigManager): ItemStack {
        val texture = configManager.getSafeSlotHeadTexture()
        val head = CreditsHeadUtil.createCustomHead("backpack.safe-slot.name", texture)
        head.editMeta { meta ->
            meta.lore(
                LanguageAPI.getComponentList(plugin, "backpack.safe-slot.lore")
            )
            meta.persistentDataContainer.set(blockedSlotKey, PersistentDataType.BYTE, 1)
        }
        return head
    }
}
