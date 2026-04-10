package sky4th.core.ui

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import sky4th.core.ui.feature.UIFeatureManager

/**
 * UI页面类
 * 负责渲染和显示UI界面
 */
class UIPage(private val config: UIConfig, private val pluginName: String = "SkyCore") {

    private val inventory: Inventory
    private var currentPlayer: Player? = null

    // 保存每个槽位实际使用的template和物品信息
    private val slotTemplates = mutableMapOf<Int, UITemplate>()
    private val slotItems = mutableMapOf<Int, ItemStack>()

    init {
        // 根据shape确定箱子大小
        val size = config.shape.size * 9
        // 使用 ColorUtil 转换所有格式为 § 格式
        val convertedTitle = sky4th.core.util.ColorUtil.convertMiniMessageToLegacy(config.title)
        // 使用 LegacyComponentSerializer 解析 § 格式
        val titleComponent = LegacyComponentSerializer.legacySection().deserialize(convertedTitle)
        inventory = Bukkit.createInventory(null, size, titleComponent)

        // 不在init时渲染UI，因为此时currentPlayer为null
        // 等到open方法被调用时再渲染
    }

    /**
     * 渲染UI界面
     */
    private fun renderUI() {
        // 清除旧的映射
        slotTemplates.clear()
        slotItems.clear()

        config.shape.forEachIndexed { row, line ->
            line.forEachIndexed { col, char ->
                val slot = row * 9 + col
                if (char != ' ') {
                    val baseTemplate = config.templates[char.toString()]
                    if (baseTemplate != null) {
                        // 为每个槽位创建包含正确slot信息的template副本
                        val template = baseTemplate.copy(slot = slot)
                        val item = createItem(template)

                        // 保存template和物品信息
                        slotTemplates[slot] = template
                        slotItems[slot] = item

                        inventory.setItem(slot, item)
                    }
                }
            }
        }
    }

    /**
     * 根据模板创建物品
     */
    private fun createItem(template: UITemplate): ItemStack {
        val item = ItemStack(template.material)

        // 如果是AIR，直接调用UIFeatureHandler处理，不设置meta
        if (item.type == Material.AIR) {
            val player = currentPlayer
            if (player != null) {
                val processedItem = UIFeatureManager.handleItemCreation(
                    pluginName,
                    template,
                    item,
                    player
                )
                return processedItem
            }
            return item
        }

        val meta = item.itemMeta ?: return item

        // 设置显示名称
        if (template.name.isNotEmpty()) {
            val convertedName = sky4th.core.util.ColorUtil.convertMiniMessageToLegacy(template.name)
            meta.displayName(LegacyComponentSerializer.legacySection().deserialize(convertedName).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false))
        }

        // 设置描述
        if (template.lore.isNotEmpty()) {
            meta.lore(template.lore.map { 
                val convertedLore = sky4th.core.util.ColorUtil.convertMiniMessageToLegacy(it)
                LegacyComponentSerializer.legacySection().deserialize(convertedLore).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)
            })
        }
        
        item.itemMeta = meta

        // 调用插件处理器处理物品创建
        val player = currentPlayer
        if (player != null) {
            val processedItem = UIFeatureManager.handleItemCreation(
                pluginName,
                template,
                item,
                player
            )
            return processedItem
        }

        return item
    }

    /**
     * 打开UI
     */
    fun open(player: Player) {
        currentPlayer = player
        // 重新渲染UI，确保handleItemCreation被调用
        renderUI()
        player.openInventory(inventory)
    }

    /**
     * 更新UI内容（不重新打开inventory）
     */
    fun update() {
        // 重新渲染UI
        renderUI()
    }

    /**
     * 获取指定槽位的template
     */
    fun getTemplateAtSlot(slot: Int): UITemplate? {
        return slotTemplates[slot]
    }

    /**
     * 获取指定槽位的物品
     */
    fun getItemAtSlot(slot: Int): ItemStack? {
        return slotItems[slot]
    }
}
