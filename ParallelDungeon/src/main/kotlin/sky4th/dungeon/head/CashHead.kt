package sky4th.dungeon.head

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import sky4th.dungeon.Dungeon
import sky4th.dungeon.util.CreditsHeadUtil
import sky4th.core.api.LanguageAPI

/**
 * 现金头颅
 * 用于创建各种现金物品（小堆铜币、小堆银币等）
 */
object CashHead {
    private val plugin = Dungeon.instance

    /**
     * 创建现金头颅物品
     *
     * @param name 头颅显示名称
     * @param description 头颅描述（lore）
     * @param texture 头颅皮肤纹理（Base64编码）
     * @param minCredits 最小信用点数量
     * @param maxCredits 最大信用点数量
     * @return 自定义头颅物品
     */
    fun createCashHead(
        name: String,
        description: String,
        texture: String,
        minCredits: Int,
        maxCredits: Int
    ): ItemStack {
        val head = CreditsHeadUtil.createCustomHead(name, texture)
        val meta = head.itemMeta ?: return head
        
        // 从语言文件中获取价值文本
        val valueText = LanguageAPI.getText(plugin, "search.cash-value",
            "min" to minCredits,
            "max" to maxCredits.coerceAtLeast(minCredits)
        )
        
        val lore = listOf(
            "§7$description",
            "",
            valueText
        )
        meta.lore(lore.map { LanguageAPI.toComponent(plugin, it) })

        head.itemMeta = meta
        return head
    }
}
