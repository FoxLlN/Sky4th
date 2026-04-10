
package com.sky4th.equipment.modifier

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack

/**
 * 词条效果接口
 * 定义词条如何响应游戏事件
 */
interface Modifier {
    /**
     * 获取该词条监听的事件类型列表
     * @return 事件类型列表
     */
    fun getEventTypes(): List<Class<out Event>>

    /**
     * 当词条进入活跃状态时调用
     * 用于初始化词条效果，例如根据当前状态设置属性修饰符
     * @param player 玩家
     * @param item 触发词条的物品
     * @param level 词条等级
     */
    fun onInit(player: Player, item: ItemStack, level: Int) {}

    /**
     * 处理事件
     * @param event 触发的事件
     * @param player 相关玩家
     * @param item 触发词条的物品
     * @param level 词条等级
     * @param playerRole 玩家在事件中的角色
     */
    fun handle(event: Event, player: Player, item: ItemStack, level: Int, playerRole: com.sky4th.equipment.modifier.config.PlayerRole = com.sky4th.equipment.modifier.config.PlayerRole.OTHER)

    /**
     * 获取词条ID
     * @return 词条ID
     */
    fun getAffixId(): String

    /**
     * 获取词条优先级
     * 数值越小优先级越高，默认为0
     * @return 优先级值
     */
    fun getPriority(): Int = 0

    /**
     * 获取多个相同词条的计算方式
     * @return 计算方式，默认为HIGHEST
     */
    fun getCalculationMode(): com.sky4th.equipment.attributes.AffixCalculationMode {
        return com.sky4th.equipment.attributes.AffixCalculationMode.HIGHEST
    }

    /**
     * 获取指定事件类型的角色限制
     * @param eventClass 事件类型
     * @return 角色限制，null表示不限制
     */
    fun getRoleRestriction(eventClass: Class<out Event>): com.sky4th.equipment.modifier.config.PlayerRole? = null

    /**
     * 获取词条适用的装备类型
     * @return 装备类型列表，空列表表示不限制
     */
    fun getApplicableTo(): List<com.sky4th.equipment.attributes.EquipmentCategory> = emptyList()

    /**
     * 获取词条的触发槽位
     * @return 触发槽位，默认为 MAIN_HAND（主手)
     */
    fun getTriggerSlot(): String = "MAIN_HAND"

    /**
     * 检查词条是否只需要初始化一次
     * @return 如果只需要初始化返回true，否则返回false
     */
    fun isInitModifier(): Boolean = false

    /**
     * 当词条被锻造到装备上时调用
     * 用于一次性初始化词条效果
     * @param item 装备物品
     * @param level 词条等级
     * @param isInit 是否为初始化（true=首次打上，false=升级）
     */
    fun onSmithing(item: ItemStack, level: Int, isInit: Boolean = true) {}

    /**
     * 当词条从装备上移除时调用
     * 用于清理词条效果
     * @param item 装备物品
     * @param level 词条等级
     */
    fun onUnsmithing(item: ItemStack, level: Int) {}

    /**
     * 获取该词条的冲突词条列表
     * @return 冲突词条ID列表，空列表表示没有冲突
     */
    fun getConflictingAffixes(): List<String> = emptyList()

    /**
     * 获取该词条的冲突附魔列表
     * @return 冲突附魔列表，空列表表示没有冲突
     */
    fun getConflictingEnchantments(): List<org.bukkit.enchantments.Enchantment> = emptyList()

    /**
     * 当词条被移除时调用
     * @param player 玩家
     */
    fun onRemove(player: Player) {}
}
