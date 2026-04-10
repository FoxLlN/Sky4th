package sky4th.core.ui

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.ItemStack
import sky4th.core.ui.feature.UIFeatureManager

/**
 * UI事件监听器
 * 处理UI界面的点击和关闭事件
 */
class UIListener : Listener {

    private val playerUIs = mutableMapOf<org.bukkit.entity.Player, String>()

    // 记录玩家最后点击的时间（用于防止快速重复点击）
    private val lastClickTimes = mutableMapOf<org.bukkit.entity.Player, Long>()

    // 防止快速重复点击的时间间隔（毫秒）
    private val CLICK_COOLDOWN = 100L

    // 记录玩家是否正在切换页面
    private val isSwitchingPage = mutableMapOf<org.bukkit.entity.Player, Boolean>()

    /**
     * 处理库存点击事件
     */
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? org.bukkit.entity.Player ?: return
        val uiId = playerUIs[player] ?: return

        // 阻止点击事件
        event.isCancelled = true

        // 检查是否在冷却时间内，防止快速重复点击
        val currentTime = System.currentTimeMillis()
        val lastClickTime = lastClickTimes[player] ?: 0L
        if (currentTime - lastClickTime < CLICK_COOLDOWN) {
            // 在冷却时间内，忽略这次点击
            return
        }
        // 更新最后点击时间
        lastClickTimes[player] = currentTime

        val clickedItem = event.currentItem ?: return
        val slot = event.slot

        // 获取UI配置
        val config = UIManager.getUI(uiId) ?: return

        // 尝试从UIPage获取实际的template
        val uiPage = UIManager.getUIPage(player, uiId)
        val template = if (uiPage != null) {
            uiPage.getTemplateAtSlot(slot)
        } else {
            // 如果UIPage不可用，回退到旧方法
            val row = slot / 9
            val col = slot % 9

            if (row >= config.shape.size) {
                return
            }

            val line = config.shape[row]
            if (col >= line.length) {
                return
            }

            val templateKey = line[col].toString()
            config.templates[templateKey]
        }

        if (template == null) {
            return
        }

        // 判断点击类型
        val isLeftClick = event.isLeftClick
        val isRightClick = event.isRightClick
        val isShiftClick = event.isShiftClick
        
        // 调用插件处理器处理点击事件，传递点击类型信息
        if (isShiftClick) {
            if (UIFeatureManager.handleShiftClick(
                config.pluginName,
                template,
                clickedItem,
                player
            )) {
                // 插件已处理事件，不再继续
                return
            }
        }
        if (isLeftClick) {
            if (UIFeatureManager.handleLeftClick(
                config.pluginName,
                template,
                clickedItem,
                player
            )) {
                // 插件已处理事件，不再继续
                return
            }
        }
        if (isRightClick) {
            if (UIFeatureManager.handleRightClick(
                config.pluginName,
                template,
                clickedItem,
                player
            )) {
                // 插件已处理事件，不再继续
                return
            }
        }

        val handled = UIFeatureManager.handleClick(
            config.pluginName,
            template,
            clickedItem,
            player
        )

        if (handled) {
            // 插件已处理事件，不再继续
            return
        }
    }

    /**
     * 处理库存关闭事件
     */
    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? org.bukkit.entity.Player ?: return

        // 获取UI ID，在移除之前
        val uiId = playerUIs[player]

        // 检查是否正在切换页面
        if (isSwitchingPage[player] == false) {
            // 移除玩家UI记录
            playerUIs.remove(player)

            // 如果有UI ID，通知插件处理器
            if (uiId != null) {
                val config = UIManager.getUI(uiId)
                if (config != null) {
                    // 通知插件处理器UI关闭
                    UIFeatureManager.handleUIClose(config.pluginName, uiId, player)
                }
            }
            // 清除该玩家的UI缓存
            UIManager.clearPlayerCache(player)
        } 
        // else {
        isSwitchingPage[player] = false
        //    
        //    // 如果有UI ID，通知插件处理器
        //    if (uiId != null) {
        //        val config = UIManager.getUI(uiId)
        //        if (config != null) {
        //            // 通知插件处理器UI切换页面了
        //            UIFeatureManager.handleUISwitch(config.pluginName, uiId, player)
        //        }
        //    }
        //}
    }

    /**
     * 记录玩家打开的UI
     */
    fun recordPlayerUI(player: org.bukkit.entity.Player, uiId: String, switching: Boolean = false) {
        playerUIs[player] = uiId
        if (switching) {
            isSwitchingPage[player] = true
        } else {
            isSwitchingPage[player] = false
        }
    }

    /**
     * 获取玩家当前打开的UI ID
     */
    fun getPlayerUI(player: org.bukkit.entity.Player): String? {
        return playerUIs[player]
    }
}
