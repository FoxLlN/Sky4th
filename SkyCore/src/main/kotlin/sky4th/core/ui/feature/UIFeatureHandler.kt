package sky4th.core.ui.feature

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import sky4th.core.ui.UITemplate

/**
 * UI特性处理器接口
 * 用于处理UI模板中的自定义特性
 * 每个插件可以实现此接口来处理自己的UI逻辑
 */
interface UIFeatureHandler {

    /**
     * 处理UI物品创建
     * 在创建UI物品时调用，插件可以自定义物品属性
     * 
     * @param template UI模板
     * @param item 待处理的物品
     * @param player 打开UI的玩家
     * @return 处理后的物品，返回null表示不处理
     */
    fun handleItemCreation(
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): ItemStack? 

    /**
     * 处理UI任意点击事件
     * 在玩家任意方式点击UI物品时调用
     * 
     * @param template UI模板
     * @param item 被点击的物品
     * @param player 点击的玩家
     * @return true表示已处理事件，false表示继续处理
     */
    fun handleClick(
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): Boolean 

    /**
     * 处理UI左键点击事件
     * 在玩家左键点击UI物品时调用
     * 
     * @param template UI模板
     * @param item 被点击的物品
     * @param player 点击的玩家
     * @return true表示已处理事件，false表示继续处理
     */
    fun handleLeftClick(
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): Boolean 

    /**
     * 处理UI右键点击事件
     * 在玩家右键点击UI物品时调用
     * 
     * @param template UI模板
     * @param item 被点击的物品
     * @param player 点击的玩家
     * @return true表示已处理事件，false表示继续处理
     */
    fun handleRightClick(
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): Boolean

    /**
     * 处理UIShift点击事件
     * 在玩家Shift点击UI物品时调用
     * 
     * @param template UI模板
     * @param item 被点击的物品
     * @param player 点击的玩家
     * @return true表示已处理事件，false表示继续处理
     */
    fun handleShiftClick(
        template: UITemplate,
        item: ItemStack,
        player: Player
    ): Boolean

    /**
     * 处理UI关闭事件
     * 在玩家关闭UI时调用
     *
     * @param uiId UI ID
     * @param player 关闭UI的玩家
     */
    fun handleUIClose(
        uiId: String,
        player: Player
    )

    /**
     * 处理UI切换事件
     * 在UI页面切换时调用
     *
     * @param uiId UI ID
     * @param player 打开UI的玩家
     */
    fun handleUISwitch(
        uiId: String, 
        player: Player
    )
}
