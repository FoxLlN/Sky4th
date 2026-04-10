package sky4th.core.ui.feature

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import sky4th.core.ui.UITemplate
import sky4th.core.api.HeadAPI

/**
 * UI特性管理器
 * 负责管理所有UI特性处理器
 * 每个插件可以注册自己的处理器来处理自己的UI逻辑
 */
object UIFeatureManager {

    private val pluginHandlers = mutableMapOf<String, UIFeatureHandler>()

    /**
     * 注册插件特性处理器
     * @param pluginName 插件名称
     * @param handler 特性处理器
     */
    fun registerHandler(pluginName: String, handler: UIFeatureHandler) {
        pluginHandlers[pluginName] = handler
    }

    /**
     * 注销插件特性处理器
     * @param pluginName 插件名称
     */
    fun unregisterHandler(pluginName: String) {
        pluginHandlers.remove(pluginName)
    }

    /**
     * 处理UI物品创建
     * @param pluginName 插件名称
     * @param template UI模板
     * @param item 待处理的物品
     * @param player 打开UI的玩家
     * @return 处理后的物品，如果没有处理器则返回原物品
     */
    fun handleItemCreation(
        pluginName: String,
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): ItemStack {
        // 先尝试核心处理
        val coreResult = coreHandleItemCreation(template, item, player)
        
        val handler = pluginHandlers[pluginName]
        if (handler != null) {
            // 再尝试插件处理
            val result = handler.handleItemCreation(template, coreResult, player)
            return result ?: coreResult
        }
        
        return coreResult
    }

    /**
     * 处理UI通用点击事件
     * @param pluginName 插件名称
     * @param template UI模板
     * @param item 被点击的物品
     * @param player 点击的玩家
     * @return true表示已处理事件，false表示继续处理
     */
    fun handleClick(
        pluginName: String,
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): Boolean {
        // 先尝试核心处理
        if (coreHandleClick(pluginName,template, item, player)) {
            return true
        }

        // 再尝试插件处理
        val handler = pluginHandlers[pluginName]
        return handler?.handleClick(template, item, player) ?: false
    }

    
    /**
     * 处理UI左键点击事件
     * @param pluginName 插件名称
     * @param template UI模板
     * @param item 被点击的物品
     * @param player 点击的玩家
     * @return true表示已处理事件，false表示继续处理
     */
    fun handleLeftClick(
        pluginName: String,
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): Boolean {
        val handler = pluginHandlers[pluginName]
        return handler?.handleLeftClick(template, item, player) ?: false
    }

    /**
     * 处理UI右键点击事件
     * @param pluginName 插件名称
     * @param template UI模板
     * @param item 被点击的物品
     * @param player 点击的玩家
     * @return true表示已处理事件，false表示继续处理
     */
    fun handleRightClick(
        pluginName: String,
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): Boolean {
        val handler = pluginHandlers[pluginName]
        return handler?.handleRightClick(template, item, player) ?: false
    }

    /**
     * 处理UIShift点击事件
     * @param pluginName 插件名称
     * @param template UI模板
     * @param item 被点击的物品
     * @param player 点击的玩家
     * @return true表示已处理事件，false表示继续处理
     */
    fun handleShiftClick(
        pluginName: String,
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): Boolean {
        val handler = pluginHandlers[pluginName]
        return handler?.handleShiftClick(template, item, player) ?: false
    }

    /**
     * 处理UI关闭事件
     * @param pluginName 插件名称
     * @param uiId UI ID
     * @param player 关闭UI的玩家
     */
    fun handleUIClose(
        pluginName: String,
        uiId: String,
        player: Player
    ) {
        val handler = pluginHandlers[pluginName]
        handler?.handleUIClose(uiId, player)
    }

    /**
     * 处理UI切换事件
     * @param pluginName 插件名称
     * @param uiId UI ID
     * @param player 切换UI的玩家
     */
    fun handleUISwitch(
        pluginName: String,
        uiId: String,
        player: Player
    ) {
        val handler = pluginHandlers[pluginName]
        handler?.handleUISwitch(uiId, player)
    }

    
    /**
     * 处理UI物品创建
     * @param template UI模板
     * @param item 待处理的物品
     * @param player 打开UI的玩家
     * @return 处理后的物品，如果没有处理器则返回原物品
     */
    private fun coreHandleItemCreation(
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): ItemStack {

        // 检查functional特性
        val functional = template.features["functional"] as? String

        // 检查texture特性
        val texture = template.features["head_texture"] as? String
        if (texture != null) {
            val headName = template.name.ifEmpty { "Custom_Head" }
            val head = HeadAPI.createCustomHead(headName, texture)
            // 保留原有的lore
            if (template.lore.isNotEmpty()) {
                val meta = head.itemMeta ?: return head
                val miniMessage = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                meta.lore(template.lore.map {
                    val convertedLore = sky4th.core.util.ColorUtil.convertLegacyToMiniMessage(it)
                    miniMessage.deserialize(convertedLore).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)
                })
                head.itemMeta = meta
            }

            return head
        }

        return item
    }

    /**
     * 处理UI点击事件的核心逻辑
     * 
     * @param pluginName 插件名称
     * @param template UI模板
     * @param item 被点击的物品
     * @param player 点击的玩家
     * @return true表示已处理事件，false表示继续处理
     */
    private fun coreHandleClick(
        pluginName: String,
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): Boolean {
        // 检查functional特性
        val functional = template.features["functional"] as? String

        // 处理退出功能
        if (functional == "exit") {
            player.closeInventory()
            return true
        }

        // 检查是否有page特性
        val page = template.features["page"] as? String
        if (page != null) {
            // 跳转到新页面
            sky4th.core.api.UIAPI.openUI(player, page)
            return true
        }
        

        return false
    }
}
