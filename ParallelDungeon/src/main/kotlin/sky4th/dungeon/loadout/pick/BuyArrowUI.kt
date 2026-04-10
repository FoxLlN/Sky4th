package sky4th.dungeon.loadout.pick

import sky4th.dungeon.loadout.screen.LoadoutScreenHolder
import sky4th.dungeon.loadout.screen.LoadoutUI
import sky4th.dungeon.loadout.shop.LoadoutShopAPI
import sky4th.dungeon.loadout.LoadoutCategory
import sky4th.dungeon.loadout.LoadoutShopItemConfig
import sky4th.core.api.LanguageAPI
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import sky4th.dungeon.config.ConfigManager

/**
 * “购买箭矢”子界面：根据当前远程武器列出可购买的箭矢及价格，首次点击选中、再次点击购买。
 */
object BuyArrowUI {

    private const val SIZE = 54
    const val SLOT_BACK = 53
    /** 内容区：每行 5 格，3 行（与商店一致） */
    private val CONTENT_SLOT_INDICES = intArrayOf(
        20, 21, 22, 23, 24,
        29, 30, 31, 32, 33,
        38, 39, 40, 41, 42
    )

    /** 根据当前远程武器得到可购买的箭矢 loadoutId 集合 */
    private fun compatibleArrowIds(rangedLoadoutId: String): Set<String> = when (rangedLoadoutId) {
        "explosive_crossbow" -> setOf("baozha_jian")
        "special_bow" -> setOf("normal_arrow") + LoadoutUI.SPECIAL_BOW_ARROW_IDS
        else -> setOf("normal_arrow")
    }

    @JvmStatic
    fun open(
        player: Player,
        loadoutHolder: LoadoutScreenHolder,
        rangedLoadoutId: String,
        plugin: JavaPlugin,
        configManager: ConfigManager
    ) {
        val supplies = configManager.getLoadoutShopItems(LoadoutCategory.SUPPLIES)
        val allowedIds = compatibleArrowIds(rangedLoadoutId)
        val arrowConfigs = supplies.filter { it.id in allowedIds }
        val holder = BuyArrowHolder(player.uniqueId, loadoutHolder, rangedLoadoutId, arrowConfigs, -1)
        val title = LanguageAPI.getText(plugin, "loadout.screen.buy-arrows-title")
        val inv = Bukkit.createInventory(holder, SIZE, MiniMessage.miniMessage().deserialize(title))
        holder.backingInv = inv

        fillDecoration(inv, plugin)
        fillContent(inv, holder, configManager, plugin)
        player.openInventory(inv)
    }

    private fun fillDecoration(inv: Inventory, plugin: JavaPlugin) {
        val gray = ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
            editMeta { it.displayName(MiniMessage.miniMessage().deserialize(" ")) }
        }
        for (slot in 0 until SIZE) {
            when {
                slot in CONTENT_SLOT_INDICES -> { /* 内容区在 fillContent 填充 */ }
                slot == SLOT_BACK -> inv.setItem(slot, ItemStack(Material.ARROW).apply {
                    editMeta { meta ->
                        meta.displayName(LanguageAPI.getComponent(plugin, "loadout.screen.back"))
                    }
                })
                else -> inv.setItem(slot, gray.clone())
            }
        }
    }

    private fun fillContent(
        inv: Inventory,
        holder: BuyArrowHolder,
        configManager: ConfigManager,
        plugin: JavaPlugin
    ) {
        val shopIdKey = NamespacedKey(plugin, "loadout_shop_id")
        val configs = holder.arrowConfigs
        val selected = holder.selectedIndex
        for (i in CONTENT_SLOT_INDICES.indices) {
            val slot = CONTENT_SLOT_INDICES[i]
            if (i >= configs.size) {
                inv.setItem(slot, null)
                continue
            }
            val config = configs[i]
            inv.setItem(slot, LoadoutShopAPI.createShopDisplayItem(plugin, configManager, config, shopIdKey, withGlow = (i == selected)))
        }
    }

    @JvmStatic
    fun refreshContent(inv: Inventory, configManager: ConfigManager, plugin: JavaPlugin) {
        val holder = inv.holder as? BuyArrowHolder ?: return
        fillContent(inv, holder, configManager, plugin)
    }

    @JvmStatic
    fun isBuyArrowGui(holder: Any?): Boolean = holder is BuyArrowHolder

    @JvmStatic
    fun getContentSlotIndex(slot: Int): Int? {
        val idx = CONTENT_SLOT_INDICES.indexOf(slot)
        return if (idx >= 0) idx else null
    }
}
